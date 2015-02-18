package org.javenstudio.panda.analysis.synonym;

import java.io.IOException;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.common.indexdb.analysis.CharToken;
import org.javenstudio.common.indexdb.analysis.TokenFilter;
import org.javenstudio.common.indexdb.store.ByteArrayDataInput;
import org.javenstudio.common.indexdb.util.ArrayUtil;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.CharTerm;
import org.javenstudio.common.indexdb.util.CharsRef;
import org.javenstudio.common.indexdb.util.JvmUtil;
import org.javenstudio.common.indexdb.util.UnicodeUtil;
import org.javenstudio.hornet.store.fst.BytesReader;
import org.javenstudio.hornet.store.fst.FST;
import org.javenstudio.hornet.store.fst.FSTArc;

/**
 * Matches single or multi word synonyms in a token stream.
 * This token stream cannot properly handle position
 * increments != 1, ie, you should place this filter before
 * filtering out stop words.
 * 
 * <p>Note that with the current implementation, parsing is
 * greedy, so whenever multiple parses would apply, the rule
 * starting the earliest and parsing the most tokens wins.
 * For example if you have these rules:
 *      
 * <pre>
 *   a -> x
 *   a b -> y
 *   b c d -> z
 * </pre>
 *
 * Then input <code>a b c d e</code> parses to <code>y b c
 * d</code>, ie the 2nd rule "wins" because it started
 * earliest and matched the most input tokens of other rules
 * starting at that point.</p>
 *
 * <p>A future improvement to this filter could allow
 * non-greedy parsing, such that the 3rd rule would win, and
 * also separately allow multiple parses, such that all 3
 * rules would match, perhaps even on a rule by rule
 * basis.</p>
 *
 * <p><b>NOTE</b>: when a match occurs, the output tokens
 * associated with the matching rule are "stacked" on top of
 * the input stream (if the rule had
 * <code>keepOrig=true</code>) and also on top of another
 * matched rule's output tokens.  This is not a correct
 * solution, as really the output should be an arbitrary
 * graph/lattice.  For example, with the above match, you
 * would expect an exact <code>PhraseQuery</code> <code>"y b
 * c"</code> to match the parsed tokens, but it will fail to
 * do so.  This limitation is necessary because Lucene's
 * TokenStream (and index) cannot yet represent an arbitrary
 * graph.</p>
 *
 * <p><b>NOTE</b>: If multiple incoming tokens arrive on the
 * same position, only the first token at that position is
 * used for parsing.  Subsequent tokens simply pass through
 * and are not parsed.  A future improvement would be to
 * allow these tokens to also be matched.</p>
 */ 

// TODO: maybe we should resolve token -> wordID then run
// FST on wordIDs, for better perf?

// TODO: a more efficient approach would be Aho/Corasick's
// algorithm
// http://en.wikipedia.org/wiki/Aho%E2%80%93Corasick_string_matching_algorithm
// It improves over the current approach here
// because it does not fully re-start matching at every
// token.  For example if one pattern is "a b c x"
// and another is "b c d" and the input is "a b c d", on
// trying to parse "a b c x" but failing when you got to x,
// rather than starting over again your really should
// immediately recognize that "b c d" matches at the next
// input.  I suspect this won't matter that much in
// practice, but it's possible on some set of synonyms it
// will.  We'd have to modify Aho/Corasick to enforce our
// conflict resolving (eg greedy matching) because that algo
// finds all matches.  This really amounts to adding a .*
// closure to the FST and then determinizing it.

public final class SynonymFilter extends TokenFilter {

	public static final String TYPE_SYNONYM = "SYNONYM";

	private final SynonymMap mSynonyms;

	private final boolean mIgnoreCase;
	private final int mRollBufferSize;

	private int mCaptureCount;

	// TODO: we should set PositionLengthAttr too...

	private CharToken mToken = null;
  
	// How many future input tokens have already been matched
	// to a synonym; because the matching is "greedy" we don't
	// try to do any more matching for such tokens:
	private int mInputSkipCount;

	// Hold all buffered (read ahead) stacked input tokens for
	// a future position.  When multiple tokens are at the
	// same position, we only store (and match against) the
	// term for the first token at the position, but capture
	// state for (and enumerate) all other tokens at this
	// position:
	private static class PendingInput {
		private final CharsRef mTerm = new CharsRef();
		//AttributeSource.State state;
		private boolean mKeepOrig;
		private boolean mMatched;
		private boolean mConsumed = true;
		private int mStartOffset;
		private int mEndOffset;
    
		public void reset() {
			//state = null;
			mConsumed = true;
			mKeepOrig = false;
			mMatched = false;
		}
	};

	// Rolling buffer, holding pending input tokens we had to
	// clone because we needed to look ahead, indexed by
	// position:
	private final PendingInput[] mFutureInputs;

	// Holds pending output synonyms for one future position:
	private static class PendingOutputs {
		private CharsRef[] mOutputs;
		private int[] mEndOffsets;
		private int[] mPosLengths;
		private int mUpto;
		private int mCount;
		private int mPosIncr = 1;
		private int mLastEndOffset;
		private int mLastPosLength;

		public PendingOutputs() {
			mOutputs = new CharsRef[1];
			mEndOffsets = new int[1];
			mPosLengths = new int[1];
		}

		public void reset() {
			mUpto = mCount = 0;
			mPosIncr = 1;
		}

		public CharsRef pullNext() {
			assert mUpto < mCount;
			
			mLastEndOffset = mEndOffsets[mUpto];
			mLastPosLength = mPosLengths[mUpto];
			
			final CharsRef result = mOutputs[mUpto++];
			
			mPosIncr = 0;
			if (mUpto == mCount) 
				reset();
			
			return result;
		}

		public int getLastEndOffset() {
			return mLastEndOffset;
		}

		@SuppressWarnings("unused")
		public int getLastPosLength() {
			return mLastPosLength;
		}

		public void add(char[] output, int offset, int len, int endOffset, int posLength) {
			if (mCount == mOutputs.length) {
				final CharsRef[] next = new CharsRef[ArrayUtil.oversize(1+mCount, JvmUtil.NUM_BYTES_OBJECT_REF)];
				System.arraycopy(mOutputs, 0, next, 0, mCount);
				mOutputs = next;
			}
			
			if (mCount == mEndOffsets.length) {
				final int[] next = new int[ArrayUtil.oversize(1+mCount, JvmUtil.NUM_BYTES_INT)];
				System.arraycopy(mEndOffsets, 0, next, 0, mCount);
				mEndOffsets = next;
			}
			
			if (mCount == mPosLengths.length) {
				final int[] next = new int[ArrayUtil.oversize(1+mCount, JvmUtil.NUM_BYTES_INT)];
				System.arraycopy(mPosLengths, 0, next, 0, mCount);
				mPosLengths = next;
			}
			
			if (mOutputs[mCount] == null) 
				mOutputs[mCount] = new CharsRef();
			
			mOutputs[mCount].copyChars(output, offset, len);
			
			// endOffset can be -1, in which case we should simply
			// use the endOffset of the input token, or X >= 0, in
			// which case we use X as the endOffset for this output
			mEndOffsets[mCount] = endOffset;
			mPosLengths[mCount] = posLength;
			
			mCount ++;
		}
	};

	private final ByteArrayDataInput mBytesReader = new ByteArrayDataInput();

	// Rolling buffer, holding stack of pending synonym
	// outputs, indexed by position:
	private final PendingOutputs[] mFutureOutputs;

	// Where (in rolling buffers) to write next input saved state:
	private int mNextWrite;

	// Where (in rolling buffers) to read next input saved state:
	private int mNextRead;

	// True once we've read last token
	private boolean mFinished;

	private final FSTArc<BytesRef> mScratchArc;

	private final FST<BytesRef> mFst;

	private final BytesReader mFstReader;

	private final BytesRef mScratchBytes = new BytesRef();
	private final CharsRef mScratchChars = new CharsRef();

	/**
	 * @param input input tokenstream
	 * @param synonyms synonym map
	 * @param ignoreCase case-folds input for matching with {@link Character#toLowerCase(int)}.
	 *                   Note, if you set this to true, its your responsibility to lowercase
	 *                   the input entries when you create the {@link SynonymMap}
	 */
	public SynonymFilter(ITokenStream input, SynonymMap synonyms, boolean ignoreCase) {
		super(input);
		
		mSynonyms = synonyms;
		mIgnoreCase = ignoreCase;
		mFst = synonyms.mFst;
		mFstReader = mFst.getBytesReader(0);
		
		if (mFst == null) 
			throw new IllegalArgumentException("fst must be non-null");

		// Must be 1+ so that when roll buffer is at full
		// lookahead we can distinguish this full buffer from
		// the empty buffer:
		mRollBufferSize = 1 + synonyms.mMaxHorizontalContext;

		mFutureInputs = new PendingInput[mRollBufferSize];
		mFutureOutputs = new PendingOutputs[mRollBufferSize];
		
		for (int pos=0; pos < mRollBufferSize; pos++) {
			mFutureInputs[pos] = new PendingInput();
			mFutureOutputs[pos] = new PendingOutputs();
		}

		mScratchArc = new FSTArc<BytesRef>();
	}

	private void capture() {
		mCaptureCount ++;
		
		final PendingInput input = mFutureInputs[mNextWrite];

		//input.state = captureState();
		input.mConsumed = false;
		//input.term.copyChars(termAtt.buffer(), 0, termAtt.length());

		mNextWrite = rollIncr(mNextWrite);

		// Buffer head should never catch up to tail:
		assert mNextWrite != mNextRead;
	}

	/**
	 * This is the core of this TokenFilter: it locates the
	 * synonym matches and buffers up the results into
	 * futureInputs/Outputs.
	 * 
	 * NOTE: this calls input.incrementToken and does not
	 * capture the state if no further tokens were checked.  So
	 * caller must then forward state to our caller, or capture:
	 */
	private int mLastStartOffset;
	private int mLastEndOffset;

	private void parse() throws IOException {
		mToken = null;
		assert mInputSkipCount == 0;

		int curNextRead = mNextRead;

		// Holds the longest match we've seen so far:
		BytesRef matchOutput = null;
		int matchInputLength = 0;
		int matchEndOffset = -1;

		BytesRef pendingOutput = mFst.getOutputs().getNoOutput();
		mFst.getFirstArc(mScratchArc);

		assert mScratchArc.getOutput() == mFst.getOutputs().getNoOutput();
		int tokenCount = 0;

		byToken:
		while (true) {
			// Pull next token's chars:
			final char[] buffer;
			final int bufferLen;
			int inputEndOffset = 0;

			if (curNextRead == mNextWrite) {
				// We used up our lookahead buffer of input tokens
				// -- pull next real input token:

				if (mFinished) {
					mToken = null;
					break;
					
				} else {
					assert mFutureInputs[mNextWrite].mConsumed;
					
					// Not correct: a syn match whose output is longer
					// than its input can set future inputs keepOrig
					// to true:
					//assert !futureInputs[nextWrite].keepOrig;
					mToken = (CharToken)super.nextToken();
					
					if (mToken != null) {
						CharTerm term = mToken.getTerm();
						
						buffer = term.buffer();
						bufferLen = term.length();
						
						final PendingInput input = mFutureInputs[mNextWrite];
						
						mLastStartOffset = input.mStartOffset = mToken.getStartOffset();
						mLastEndOffset = input.mEndOffset = mToken.getEndOffset();
						inputEndOffset = input.mEndOffset;
						
						if (mNextRead != mNextWrite) 
							capture();
						else 
							input.mConsumed = false;

					} else {
						// No more input tokens
						mFinished = true;
						mToken = null;
						
						break;
					}
				}
			} else {
				// Still in our lookahead
				buffer = mFutureInputs[curNextRead].mTerm.mChars;
				bufferLen = mFutureInputs[curNextRead].mTerm.mLength;
				inputEndOffset = mFutureInputs[curNextRead].mEndOffset;
			}

			tokenCount ++;

			// Run each char in this token through the FST:
			int bufUpto = 0;
			
			while (bufUpto < bufferLen) {
				final int codePoint = Character.codePointAt(buffer, bufUpto, bufferLen);
				if (mFst.findTargetArc(mIgnoreCase ? Character.toLowerCase(codePoint) : codePoint, 
						mScratchArc, mScratchArc, mFstReader) == null) {
					break byToken;
				}

				// Accum the output
				pendingOutput = mFst.getOutputs().add(pendingOutput, mScratchArc.getOutput());
				bufUpto += Character.charCount(codePoint);
			}

			// OK, entire token matched; now see if this is a final
			// state:
			if (mScratchArc.isFinal()) {
				matchOutput = mFst.getOutputs().add(pendingOutput, mScratchArc.getNextFinalOutput());
				matchInputLength = tokenCount;
				matchEndOffset = inputEndOffset;
			}

			// See if the FST wants to continue matching (ie, needs to
			// see the next input token):
			if (mFst.findTargetArc(SynonymMap.WORD_SEPARATOR, mScratchArc, mScratchArc, mFstReader) == null) {
				// No further rules can match here; we're done
				// searching for matching rules starting at the
				// current input position.
				break;
				
			} else {
				// More matching is possible -- accum the output (if
				// any) of the WORD_SEP arc:
				pendingOutput = mFst.getOutputs().add(pendingOutput, mScratchArc.getOutput());
				if (mNextRead == mNextWrite) 
					capture();
			}

			curNextRead = rollIncr(curNextRead);
		}

		if (mNextRead == mNextWrite && !mFinished) 
			mNextWrite = rollIncr(mNextWrite);

		if (matchOutput != null) {
			mInputSkipCount = matchInputLength;
			addOutput(matchOutput, matchInputLength, matchEndOffset);
			
		} else if (mNextRead != mNextWrite) {
			// Even though we had no match here, we set to 1
			// because we need to skip current input token before
			// trying to match again:
			mInputSkipCount = 1;
			
		} else {
			assert mFinished;
			mToken = null;
		}
	}

	// Interleaves all output tokens onto the futureOutputs:
	private void addOutput(BytesRef bytes, int matchInputLength, int matchEndOffset) throws IOException {
		mBytesReader.reset(bytes.getBytes(), bytes.getOffset(), bytes.getLength());

		final int code = mBytesReader.readVInt();
		final boolean keepOrig = (code & 0x1) == 0;
		final int count = code >>> 1;
		
		for (int outputIDX=0; outputIDX < count; outputIDX++) {
			mSynonyms.mWords.get(mBytesReader.readVInt(), mScratchBytes);
			UnicodeUtil.UTF8toUTF16(mScratchBytes, mScratchChars);
			
			int lastStart = mScratchChars.getOffset();
			final int chEnd = lastStart + mScratchChars.getLength();
			int outputUpto = mNextRead;
			
			for (int chIDX=lastStart; chIDX <= chEnd; chIDX++) {
				if (chIDX == chEnd || mScratchChars.mChars[chIDX] == SynonymMap.WORD_SEPARATOR) {
					final int outputLen = chIDX - lastStart;
					// Caller is not allowed to have empty string in
					// the output:
					assert outputLen > 0: "output contains empty string: " + mScratchChars;
					
					final int endOffset;
					final int posLen;
					
					if (chIDX == chEnd && lastStart == mScratchChars.getOffset()) {
						// This rule had a single output token, so, we set
						// this output's endOffset to the current
						// endOffset (ie, endOffset of the last input
						// token it matched):
						endOffset = matchEndOffset;
						posLen = keepOrig ? matchInputLength : 1;
						
					} else {
						// This rule has more than one output token; we
						// can't pick any particular endOffset for this
						// case, so, we inherit the endOffset for the
						// input token which this output overlaps:
						endOffset = -1;
						posLen = 1;
					}
					
					mFutureOutputs[outputUpto].add(mScratchChars.getChars(), 
							lastStart, outputLen, endOffset, posLen);
					
					lastStart = 1 + chIDX;
					outputUpto = rollIncr(outputUpto);
					
					assert mFutureOutputs[outputUpto].mPosIncr == 1: 
						"outputUpto=" + outputUpto + " vs nextWrite=" + mNextWrite;
				}
			}
		}

		int upto = mNextRead;
		
		for (int idx=0; idx < matchInputLength; idx ++) {
			mFutureInputs[upto].mKeepOrig |= keepOrig;
			mFutureInputs[upto].mMatched = true;
			
			upto = rollIncr(upto);
		}
	}

	// ++ mod rollBufferSize
	private int rollIncr(int count) {
		count ++;
		if (count == mRollBufferSize) 
			return 0;
		else 
			return count;
	}

	// for testing
	final int getCaptureCount() { return mCaptureCount; }

	@Override
	public IToken nextToken() throws IOException {
		while (true) {

			// First play back any buffered future inputs/outputs
			// w/o running parsing again:
			while (mInputSkipCount != 0) {
				// At each position, we first output the original
				// token

				// TODO: maybe just a PendingState class, holding
				// both input & outputs?
				final PendingInput input = mFutureInputs[mNextRead];
				final PendingOutputs outputs = mFutureOutputs[mNextRead];
        
				if (!input.mConsumed && (input.mKeepOrig || !input.mMatched)) {
					//if (input.state != null) {
					// Return a previously saved token (because we
					// had to lookahead):
					//restoreState(input.state);
					//} else {
					// Pass-through case: return token we just pulled
					// but didn't capture:
					assert mInputSkipCount == 1: "inputSkipCount=" + mInputSkipCount + " nextRead=" + mNextRead;
					//}
					
					input.reset();
					
					if (outputs.mCount > 0) {
						outputs.mPosIncr = 0;
						
					} else {
						mNextRead = rollIncr(mNextRead);
						mInputSkipCount --;
					}
					
					return mToken;
					
				} else if (outputs.mUpto < outputs.mCount) {
					// Still have pending outputs to replay at this
					// position
					input.reset();
					
					final int posIncr = outputs.mPosIncr;
					final CharsRef output = outputs.pullNext();
          
					mToken.clear();
					mToken.getTerm().copyBuffer(output.getChars(), output.getOffset(), output.getLength());
					mToken.setType(TYPE_SYNONYM);
					
					int endOffset = outputs.getLastEndOffset();
					if (endOffset == -1) 
						endOffset = input.mEndOffset;
					
					mToken.setOffset(input.mStartOffset, endOffset);
					mToken.setPositionIncrement(posIncr);
					//mToken.setPositionLength(outputs.getLastPosLength());
					
					if (outputs.mCount == 0) {
						// Done with the buffered input and all outputs at
						// this position
						mNextRead = rollIncr(mNextRead);
						mInputSkipCount --;
					}
					
					return mToken;
					
				} else {
					// Done with the buffered input and all outputs at
					// this position
					input.reset();
					mNextRead = rollIncr(mNextRead);
					mInputSkipCount --;
				}
			}

			if (mFinished && mNextRead == mNextWrite) {
				// End case: if any output syns went beyond end of
				// input stream, enumerate them now:
				final PendingOutputs outputs = mFutureOutputs[mNextRead];
				
				if (outputs.mUpto < outputs.mCount) {
					final int posIncr = outputs.mPosIncr;
					final CharsRef output = outputs.pullNext();
					
					mFutureInputs[mNextRead].reset();
					if (outputs.mCount == 0) 
						mNextWrite = mNextRead = rollIncr(mNextRead);
					
					mToken.clear();
					// Keep offset from last input token:
					mToken.setOffset(mLastStartOffset, mLastEndOffset);
					mToken.getTerm().copyBuffer(output.getChars(), output.getOffset(), output.getLength());
					mToken.setType(TYPE_SYNONYM);
					mToken.setPositionIncrement(posIncr);
					
					return mToken;
					
				} else {
					return null;
				}
			}

			// Find new synonym matches:
			parse();
		}
	}

	@Override
	public void reset() throws IOException {
		super.reset();
		
		mCaptureCount = 0;
		mFinished = false;
		mInputSkipCount = 0;
		mNextRead = mNextWrite = 0;

		// In normal usage these resets would not be needed,
		// since they reset-as-they-are-consumed, but the app
		// may not consume all input tokens (or we might hit an
		// exception), in which case we have leftover state
		// here:
		for (PendingInput input : mFutureInputs) {
			input.reset();
		}
		
		for (PendingOutputs output : mFutureOutputs) {
			output.reset();
		}
	}
	
}
