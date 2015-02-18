package org.javenstudio.panda.analysis.synonym;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.common.indexdb.analysis.Analyzer;
import org.javenstudio.common.indexdb.analysis.CharToken;
import org.javenstudio.common.indexdb.store.ByteArrayDataOutput;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.BytesRefHash;
import org.javenstudio.common.indexdb.util.CharTerm;
import org.javenstudio.common.indexdb.util.CharsRef;
import org.javenstudio.common.indexdb.util.IntsRef;
import org.javenstudio.common.indexdb.util.UnicodeUtil;
import org.javenstudio.hornet.store.fst.ByteSequenceOutputs;
import org.javenstudio.hornet.store.fst.FST;
import org.javenstudio.hornet.store.fst.FSTUtil;

/**
 * A map of synonyms, keys and values are phrases.
 * 
 */
public class SynonymMap {
	
	/** for multiword support, you must separate words with this separator */
	public static final char WORD_SEPARATOR = 0;
	
	/** map&lt;input word, list&lt;ord&gt;&gt; */
	protected final FST<BytesRef> mFst;
	
	/** map&lt;ord, outputword&gt; */
	protected final BytesRefHash mWords;
	
	/** maxHorizontalContext: maximum context we need on the tokenstream */
	protected final int mMaxHorizontalContext;

	public SynonymMap(FST<BytesRef> fst, BytesRefHash words, 
			int maxHorizontalContext) {
		mFst = fst;
		mWords = words;
		mMaxHorizontalContext = maxHorizontalContext;
	}
  
	/**
	 * Builds an FSTSynonymMap.
	 * <p>
	 * Call add() until you have added all the mappings, then call build() to get an FSTSynonymMap
	 */
	public static class Builder {
		private final HashMap<CharsRef,MapEntry> mWorkingSet = 
				new HashMap<CharsRef,MapEntry>();
		
		private final BytesRefHash mWords = new BytesRefHash();
		private final BytesRef mUtf8Scratch = new BytesRef(8);
		private final boolean mDedup;
		private int mMaxHorizontalContext;

		/** 
		 * If dedup is true then identical rules (same input,
		 *  same output) will be added only once. 
		 */
		public Builder(boolean dedup) {
			mDedup = dedup;
		}

		private static class MapEntry {
			// we could sort for better sharing ultimately, but it could confuse people
			private final List<Integer> mOrds = new ArrayList<Integer>();
			private boolean mIncludeOrig;
		}

		/** 
		 * Sugar: just joins the provided terms with {@link
		 *  SynonymMap#WORD_SEPARATOR}.  reuse and its chars
		 *  must not be null. 
		 */
		public static CharsRef join(String[] words, CharsRef reuse) {
			int upto = 0;
			char[] buffer = reuse.getChars();
			
			for (String word : words) {
				if (upto > 0) {
					if (upto >= buffer.length) {
						reuse.grow(upto);
						buffer = reuse.getChars();
					}
					
					buffer[upto++] = SynonymMap.WORD_SEPARATOR;
				}

				final int wordLen =  word.length();
				final int needed = upto + wordLen;
				
				if (needed > buffer.length) {
					reuse.grow(needed);
					buffer = reuse.getChars();
				}

				word.getChars(0, wordLen, buffer, upto);
				upto += wordLen;
			}

			return reuse;
		}
    
		/** 
		 * Sugar: analyzes the text with the analyzer and
		 *  separates by {@link SynonymMap#WORD_SEPARATOR}.
		 *  reuse and its chars must not be null. 
		 */
		public static CharsRef analyze(Analyzer analyzer, String text, 
				CharsRef reuse) throws IOException {
			ITokenStream ts = analyzer.tokenStream("", new StringReader(text));
			ts.reset();
			
			reuse.mLength = 0;
			IToken token = null;
			
			while ((token = ts.nextToken()) != null) {
				CharTerm term = ((CharToken)token).getTerm();
				int length = term.length();
				
				if (length == 0) 
					throw new IllegalArgumentException("term: " + text + " analyzed to a zero-length token");
				
				if (token.getPositionIncrement() != 1) 
					throw new IllegalArgumentException("term: " + text + " analyzed to a token with posinc != 1");
				
				reuse.grow(reuse.mLength + length + 1); /** current + word + separator */
				int end = reuse.mOffset + reuse.mLength;
				
				if (reuse.mLength > 0) {
					reuse.mChars[end++] = SynonymMap.WORD_SEPARATOR;
					reuse.mLength ++;
				}
				
				System.arraycopy(term.buffer(), 0, reuse.mChars, end, length);
				reuse.mLength += length;
			}
			
			ts.end();
			ts.close();
			
			if (reuse.mLength == 0) 
				throw new IllegalArgumentException("term: " + text + " was completely eliminated by analyzer");
			
			return reuse;
		}

		/** only used for asserting! */
		private boolean hasHoles(CharsRef chars) {
			final int end = chars.mOffset + chars.mLength;
			
			for (int idx = chars.mOffset+1; idx < end; idx++) {
				if (chars.mChars[idx] == SynonymMap.WORD_SEPARATOR && 
					chars.mChars[idx-1] == SynonymMap.WORD_SEPARATOR) 
					return true;
			}
			
			if (chars.mChars[chars.mOffset] == '\u0000') 
				return true;
			
			if (chars.mChars[chars.mOffset + chars.mLength - 1] == '\u0000') 
				return true;
			
			return false;
		}

		// NOTE: while it's tempting to make this public, since
		// caller's parser likely knows the
		// numInput/numOutputWords, sneaky exceptions, much later
		// on, will result if these values are wrong; so we always
		// recompute ourselves to be safe:
		private void add(CharsRef input, int numInputWords, CharsRef output, 
				int numOutputWords, boolean includeOrig) {
			// first convert to UTF-8
			if (numInputWords <= 0) 
				throw new IllegalArgumentException("numInputWords must be > 0 (got " + numInputWords + ")");
			
			if (input.getLength() <= 0) 
				throw new IllegalArgumentException("input.length must be > 0 (got " + input.getLength() + ")");
			
			if (numOutputWords <= 0) 
				throw new IllegalArgumentException("numOutputWords must be > 0 (got " + numOutputWords + ")");
			
			if (output.getLength() <= 0) 
				throw new IllegalArgumentException("output.length must be > 0 (got " + output.getLength() + ")");
			
			assert !hasHoles(input): "input has holes: " + input;
			assert !hasHoles(output): "output has holes: " + output;

			final int hashCode = UnicodeUtil.UTF16toUTF8WithHash(
					output.getChars(), output.getOffset(), output.getLength(), mUtf8Scratch);
			
			// lookup in hash
			int ord = mWords.add(mUtf8Scratch, hashCode);
			if (ord < 0) {
				// already exists in our hash
				ord = (-ord)-1;
			}
      
			MapEntry e = mWorkingSet.get(input);
			if (e == null) {
				e = new MapEntry();
				mWorkingSet.put(CharsRef.deepCopyOf(input), e); 
				// make a copy, since we will keep around in our map    
			}
      
			e.mOrds.add(ord);
			e.mIncludeOrig |= includeOrig;
			
			mMaxHorizontalContext = Math.max(mMaxHorizontalContext, numInputWords);
			mMaxHorizontalContext = Math.max(mMaxHorizontalContext, numOutputWords);
		}

		private int countWords(CharsRef chars) {
			int wordCount = 1;
			int upto = chars.mOffset;
			
			final int limit = chars.mOffset + chars.mLength;
			while (upto < limit) {
				if (chars.mChars[upto++] == SynonymMap.WORD_SEPARATOR) 
					wordCount ++;
			}
			
			return wordCount;
		}
    
		/**
		 * Add a phrase->phrase synonym mapping.
		 * Phrases are character sequences where words are
		 * separated with character zero (U+0000).  Empty words
		 * (two U+0000s in a row) are not allowed in the input nor
		 * the output!
		 * 
		 * @param input input phrase
		 * @param output output phrase
		 * @param includeOrig true if the original should be included
		 */
		public void add(CharsRef input, CharsRef output, boolean includeOrig) {
			add(input, countWords(input), output, countWords(output), includeOrig);
		}
    
		/**
		 * Builds an {@link SynonymMap} and returns it.
		 */
		@SuppressWarnings("deprecation")
		public SynonymMap build() throws IOException {
			ByteSequenceOutputs outputs = ByteSequenceOutputs.getSingleton();
			
			// TODO: are we using the best sharing options?
			org.javenstudio.hornet.store.fst.Builder<BytesRef> builder = 
					new org.javenstudio.hornet.store.fst.Builder<BytesRef>(FST.INPUT_TYPE.BYTE4, outputs);
      
			BytesRef scratch = new BytesRef(64);
			ByteArrayDataOutput scratchOutput = new ByteArrayDataOutput();

			final Set<Integer> dedupSet;
			if (mDedup) 
				dedupSet = new HashSet<Integer>();
			else 
				dedupSet = null;

			final byte[] spare = new byte[5];
      
			Set<CharsRef> keys = mWorkingSet.keySet();
			CharsRef sortedKeys[] = keys.toArray(new CharsRef[keys.size()]);
			Arrays.sort(sortedKeys, CharsRef.getUTF16SortedAsUTF8Comparator());

			final IntsRef scratchIntsRef = new IntsRef();
      
			for (int keyIdx = 0; keyIdx < sortedKeys.length; keyIdx++) {
				CharsRef input = sortedKeys[keyIdx];
				MapEntry output = mWorkingSet.get(input);

				int numEntries = output.mOrds.size();
				// output size, assume the worst case
				int estimatedSize = 5 + numEntries * 5; // numEntries + one ord for each entry
        
				scratch.grow(estimatedSize);
				scratchOutput.reset(scratch.getBytes(), scratch.getOffset(), scratch.getBytes().length);
				assert scratch.getOffset() == 0;

				// now write our output data:
				int count = 0;
				
				for (int i = 0; i < numEntries; i++) {
					if (dedupSet != null) {
						// box once
						final Integer ent = output.mOrds.get(i);
						if (dedupSet.contains(ent)) 
							continue;
						
						dedupSet.add(ent);
					}
					
					scratchOutput.writeVInt(output.mOrds.get(i));   
					count ++;
				}

				final int pos = scratchOutput.getPosition();
				scratchOutput.writeVInt(count << 1 | (output.mIncludeOrig ? 0 : 1));
				
				final int pos2 = scratchOutput.getPosition();
				final int vIntLen = pos2-pos;

				// Move the count + includeOrig to the front of the byte[]:
				System.arraycopy(scratch.getBytes(), pos, spare, 0, vIntLen);
				System.arraycopy(scratch.getBytes(), 0, scratch.getBytes(), vIntLen, pos);
				System.arraycopy(spare, 0, scratch.getBytes(), 0, vIntLen);

				if (dedupSet != null) 
					dedupSet.clear();
        
				scratch.mLength = scratchOutput.getPosition() - scratch.mOffset;
				builder.add(FSTUtil.toUTF32(input, scratchIntsRef), BytesRef.deepCopyOf(scratch));
			}
      
			FST<BytesRef> fst = builder.finish();
			return new SynonymMap(fst, mWords, mMaxHorizontalContext);
		}
	}
	
}
