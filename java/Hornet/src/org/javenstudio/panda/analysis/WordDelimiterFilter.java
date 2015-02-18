package org.javenstudio.panda.analysis;

import java.io.IOException;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.common.indexdb.analysis.CharToken;
import org.javenstudio.common.indexdb.analysis.TokenFilter;
import org.javenstudio.common.indexdb.util.ArrayUtil;
import org.javenstudio.common.indexdb.util.JvmUtil;
import org.javenstudio.panda.util.CharArraySet;

/**
 * Splits words into subwords and performs optional transformations on subword groups.
 * Words are split into subwords with the following rules:
 *  - split on intra-word delimiters (by default, all non alpha-numeric characters).
 *     - "Wi-Fi" -> "Wi", "Fi"
 *  - split on case transitions
 *     - "PowerShot" -> "Power", "Shot"
 *  - split on letter-number transitions
 *     - "SD500" -> "SD", "500"
 *  - leading and trailing intra-word delimiters on each subword are ignored
 *     - "//hello---there, 'dude'" -> "hello", "there", "dude"
 *  - trailing "'s" are removed for each subword
 *     - "O'Neil's" -> "O", "Neil"
 *     - Note: this step isn't performed in a separate filter because of possible subword combinations.
 *
 * The <b>combinations</b> parameter affects how subwords are combined:
 *  - combinations="0" causes no subword combinations.
 *     - "PowerShot" -> 0:"Power", 1:"Shot"  (0 and 1 are the token positions)
 *  - combinations="1" means that in addition to the subwords, maximum runs of non-numeric subwords 
 *  are catenated and produced at the same position of the last subword in the run.
 *     - "PowerShot" -> 0:"Power", 1:"Shot" 1:"PowerShot"
 *     - "A's+B's&C's" -> 0:"A", 1:"B", 2:"C", 2:"ABC"
 *     - "Super-Duper-XL500-42-AutoCoder!" -> 0:"Super", 1:"Duper", 2:"XL", 2:"SuperDuperXL", 
 *     3:"500" 4:"42", 5:"Auto", 6:"Coder", 6:"AutoCoder"
 *
 *  One use for WordDelimiterFilter is to help match words with different subword delimiters.
 *  For example, if the source text contained "wi-fi" one may want "wifi" "WiFi" "wi-fi" "wi+fi" 
 *  queries to all match.
 *  One way of doing so is to specify combinations="1" in the analyzer used for indexing, 
 *  and combinations="0" (the default)
 *  in the analyzer used for querying.  Given that the current StandardTokenizer immediately 
 *  removes many intra-word
 *  delimiters, it is recommended that this filter be used after a tokenizer that does not 
 *  do this (such as WhitespaceTokenizer).
 *
 */
public final class WordDelimiterFilter extends TokenFilter {
  
	public static final int LOWER = 0x01;
	public static final int UPPER = 0x02;
	public static final int DIGIT = 0x04;
	public static final int SUBWORD_DELIM = 0x08;

	// combinations: for testing, not for setting bits
	public static final int ALPHA = 0x03;
	public static final int ALPHANUM = 0x07;

	/**
	 * Causes parts of words to be generated:
	 * <p/>
	 * "PowerShot" => "Power" "Shot"
	 */
	public static final int GENERATE_WORD_PARTS = 1;

	/**
	 * Causes number subwords to be generated:
	 * <p/>
	 * "500-42" => "500" "42"
	 */
	public static final int GENERATE_NUMBER_PARTS = 2;

	/**
	 * Causes maximum runs of word parts to be catenated:
	 * <p/>
	 * "wi-fi" => "wifi"
	 */
	public static final int CATENATE_WORDS = 4;

	/**
	 * Causes maximum runs of word parts to be catenated:
	 * <p/>
	 * "wi-fi" => "wifi"
	 */
	public static final int CATENATE_NUMBERS = 8;

	/**
	 * Causes all subword parts to be catenated:
	 * <p/>
	 * "wi-fi-4000" => "wifi4000"
	 */
	public static final int CATENATE_ALL = 16;

	/**
	 * Causes original words are preserved and added to the subword list (Defaults to false)
	 * <p/>
	 * "500-42" => "500" "42" "500-42"
	 */
	public static final int PRESERVE_ORIGINAL = 32;

	/**
	 * If not set, causes case changes to be ignored (subwords will only be generated
	 * given SUBWORD_DELIM tokens)
	 */
	public static final int SPLIT_ON_CASE_CHANGE = 64;

	/**
	 * If not set, causes numeric changes to be ignored (subwords will only be generated
	 * given SUBWORD_DELIM tokens).
	 */
	public static final int SPLIT_ON_NUMERICS = 128;

	/**
	 * Causes trailing "'s" to be removed for each subword
	 * <p/>
	 * "O'Neil's" => "O", "Neil"
	 */
	public static final int STEM_ENGLISH_POSSESSIVE = 256;
  
	/**
	 * If not null is the set of tokens to protect from being delimited
	 *
	 */
	private final CharArraySet mProtWords;

	private final int mFlags;
    
	private CharToken mToken = null;
  
	// used for iterating word delimiter breaks
	private final WordDelimiterIterator mIterator;

	// used for concatenating runs of similar typed subwords (word,number)
	private final WordDelimiterConcatenation mConcat = 
			new WordDelimiterConcatenation();
	
	// number of subwords last output by concat.
	private int mLastConcatCount = 0;

	// used for catenate all
	private final WordDelimiterConcatenation mConcatAll = 
			new WordDelimiterConcatenation();

	// used for accumulating position increment gaps
	private int mAccumPosInc = 0;

	private char mSavedBuffer[] = new char[1024];
	private int mSavedStartOffset;
	private int mSavedEndOffset;
	private String mSavedType;
	
	private boolean mHasSavedState = false;
	
	// if length by start + end offsets doesn't match the term text then assume
	// this is a synonym and don't adjust the offsets.
	private boolean mHasIllegalOffsets = false;

	// for a run of the same subword type within a word, have we output anything?
	private boolean mHasOutputToken = false;
	
	// when preserve original is on, have we output any token following it?
	// this token must have posInc=0!
	private boolean mHasOutputFollowingOriginal = false;

	/**
	 * Creates a new WordDelimiterFilter
	 *
	 * @param in TokenStream to be filtered
	 * @param charTypeTable table containing character types
	 * @param configurationFlags Flags configuring the filter
   		* @param protWords If not null is the set of tokens to protect from being delimited
   		*/
	public WordDelimiterFilter(ITokenStream in, byte[] charTypeTable, 
			int configurationFlags, CharArraySet protWords) {
		super(in);
		mFlags = configurationFlags;
		mProtWords = protWords;
		mIterator = new WordDelimiterIterator(charTypeTable, 
				has(SPLIT_ON_CASE_CHANGE), has(SPLIT_ON_NUMERICS), has(STEM_ENGLISH_POSSESSIVE));
	}

	/**
	 * Creates a new WordDelimiterFilter using {@link WordDelimiterIterator#DEFAULT_WORD_DELIM_TABLE}
	 * as its charTypeTable
	 *
	 * @param in TokenStream to be filtered
	 * @param configurationFlags Flags configuring the filter
	 * @param protWords If not null is the set of tokens to protect from being delimited
	 */
	public WordDelimiterFilter(ITokenStream in, int configurationFlags, CharArraySet protWords) {
		this(in, WordDelimiterIterator.DEFAULT_WORD_DELIM_TABLE, configurationFlags, protWords);
	}

	@Override
	public IToken nextToken() throws IOException {
		while (true) {
			if (!mHasSavedState) {
				// process a new input word
				IToken token = super.nextToken();
				if (token == null) 
					return null;

				mToken = (CharToken)token;
				int termLength = mToken.getTerm().length();
				char[] termBuffer = mToken.getTerm().buffer();
        
				mAccumPosInc += mToken.getPositionIncrement();

				mIterator.setText(termBuffer, termLength);
				mIterator.next();

				// word of no delimiters, or protected word: just return it
				if ((mIterator.getCurrent() == 0 && mIterator.getEnd() == termLength) ||
					(mProtWords != null && mProtWords.contains(termBuffer, 0, termLength))) {
					mToken.setPositionIncrement(mAccumPosInc);
					mAccumPosInc = 0;
					
					return mToken;
				}
        
				// word of simply delimiters
				if (mIterator.getEnd() == WordDelimiterIterator.DONE && !has(PRESERVE_ORIGINAL)) {
					// if the posInc is 1, simply ignore it in the accumulation
					if (mToken.getPositionIncrement() == 1) 
						mAccumPosInc --;
					
					continue;
				}

				saveState();

				mHasOutputToken = false;
				mHasOutputFollowingOriginal = !has(PRESERVE_ORIGINAL);
				mLastConcatCount = 0;
        
				if (has(PRESERVE_ORIGINAL)) {
					mToken.setPositionIncrement(mAccumPosInc);
					mAccumPosInc = 0;
					
					return mToken;
				}
			}
      
			// at the end of the string, output any concatenations
			if (mIterator.getEnd() == WordDelimiterIterator.DONE) {
				if (!mConcat.isEmpty()) {
					if (flushConcatenation(mConcat)) 
						return mToken;
				}
        
				if (!mConcatAll.isEmpty()) {
					// only if we haven't output this same combo above!
					if (mConcatAll.mSubwordCount > mLastConcatCount) {
						mConcatAll.writeAndClear();
						
						return mToken;
					}
					
					mConcatAll.clear();
				}
        
				// no saved concatenations, on to the next input word
				mHasSavedState = false;
				
				continue;
			}
      
			// word surrounded by delimiters: always output
			if (mIterator.isSingleWord()) {
				generatePart(true);
				mIterator.next();
				
				return mToken;
			}
      
			int wordType = mIterator.type();
      
			// do we already have queued up incompatible concatenations?
			if (!mConcat.isEmpty() && (mConcat.mType & wordType) == 0) {
				if (flushConcatenation(mConcat)) {
					mHasOutputToken = false;
					return mToken;
				}
				
				mHasOutputToken = false;
			}
      
			// add subwords depending upon options
			if (shouldConcatenate(wordType)) {
				if (mConcat.isEmpty()) 
					mConcat.mType = wordType;
				
				concatenate(mConcat);
			}
      
			// add all subwords (catenateAll)
			if (has(CATENATE_ALL)) 
				concatenate(mConcatAll);
      
			// if we should output the word or number part
			if (shouldGenerateParts(wordType)) {
				generatePart(false);
				mIterator.next();
				
				return mToken;
			}
        
			mIterator.next();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void reset() throws IOException {
		super.reset();
		mHasSavedState = false;
		mConcat.clear();
		mConcatAll.clear();
		mAccumPosInc = 0;
	}

	/**
	 * Saves the existing attribute states
	 */
	private void saveState() {
		// otherwise, we have delimiters, save state
		mSavedStartOffset = mToken.getStartOffset();
		mSavedEndOffset = mToken.getEndOffset();
		// if length by start + end offsets doesn't match the term text then assume 
		// this is a synonym and don't adjust the offsets.
		mHasIllegalOffsets = (mSavedEndOffset - mSavedStartOffset != mToken.getTerm().length());
		mSavedType = mToken.getType();

		if (mSavedBuffer.length < mToken.getTerm().length()) 
			mSavedBuffer = new char[ArrayUtil.oversize(mToken.getTerm().length(), JvmUtil.NUM_BYTES_CHAR)];
		
		System.arraycopy(mToken.getTerm().buffer(), 0, mSavedBuffer, 0, mToken.getTerm().length());
		mIterator.setText(mSavedBuffer);

		mHasSavedState = true;
	}

	/**
	 * Flushes the given WordDelimiterConcatenation by either writing its concat and then clearing, 
	 * or just clearing.
	 *
	 * @param concatenation WordDelimiterConcatenation that will be flushed
	 * @return {@code true} if the concatenation was written before it was cleared, {@code false} otherwise
	 */
	private boolean flushConcatenation(WordDelimiterConcatenation concatenation) {
		mLastConcatCount = concatenation.mSubwordCount;
		if (concatenation.mSubwordCount != 1 || !shouldGenerateParts(concatenation.mType)) {
			concatenation.writeAndClear();
			return true;
		}
		concatenation.clear();
		return false;
	}

	/**
	 * Determines whether to concatenate a word or number if the current word is the given type
	 *
	 * @param wordType Type of the current word used to determine if it should be concatenated
	 * @return {@code true} if concatenation should occur, {@code false} otherwise
	 */
	private boolean shouldConcatenate(int wordType) {
		return (has(CATENATE_WORDS) && isAlpha(wordType)) || 
			   (has(CATENATE_NUMBERS) && isDigit(wordType));
	}

	/**
	 * Determines whether a word/number part should be generated for a word of the given type
	 *
	 * @param wordType Type of the word used to determine if a word/number part should be generated
	 * @return {@code true} if a word/number part should be generated, {@code false} otherwise
	 */
	private boolean shouldGenerateParts(int wordType) {
		return (has(GENERATE_WORD_PARTS) && isAlpha(wordType)) || 
			   (has(GENERATE_NUMBER_PARTS) && isDigit(wordType));
	}

	/**
	 * Concatenates the saved buffer to the given WordDelimiterConcatenation
	 *
	 * @param concatenation WordDelimiterConcatenation to concatenate the buffer to
	 */
	private void concatenate(WordDelimiterConcatenation concatenation) {
		if (concatenation.isEmpty()) 
			concatenation.mStartOffset = mSavedStartOffset + mIterator.getCurrent();
		
		concatenation.append(mSavedBuffer, mIterator.getCurrent(), mIterator.getEnd() - mIterator.getCurrent());
		concatenation.mEndOffset = mSavedStartOffset + mIterator.getEnd();
	}

	/**
	 * Generates a word/number part, updating the appropriate attributes
	 *
	 * @param isSingleWord {@code true} if the generation is occurring from a single word, 
	 * {@code false} otherwise
	 */
	private void generatePart(boolean isSingleWord) {
		mToken.clear();
		mToken.getTerm().copyBuffer(mSavedBuffer, mIterator.getCurrent(), 
				mIterator.getEnd() - mIterator.getCurrent());

		int startOffset = mSavedStartOffset + mIterator.getCurrent();
		int endOffset = mSavedStartOffset + mIterator.getEnd();
    
		if (mHasIllegalOffsets) {
			// historically this filter did this regardless for 'isSingleWord', 
			// but we must do a sanity check:
			if (isSingleWord && startOffset <= mSavedEndOffset) 
				mToken.setOffset(startOffset, mSavedEndOffset);
			else 
				mToken.setOffset(mSavedStartOffset, mSavedEndOffset);
			
		} else {
			mToken.setOffset(startOffset, endOffset);
		}
		
		mToken.setPositionIncrement(position(false));
		mToken.setType(mSavedType);
	}

	/**
	 * Get the position increment gap for a subword or concatenation
	 *
	 * @param inject true if this token wants to be injected
	 * @return position increment gap
	 */
	private int position(boolean inject) {
		int posInc = mAccumPosInc;

		if (mHasOutputToken) {
			mAccumPosInc = 0;
			return inject ? 0 : Math.max(1, posInc);
		}

		mHasOutputToken = true;
    
		if (!mHasOutputFollowingOriginal) {
			// the first token following the original is 0 regardless
			mHasOutputFollowingOriginal = true;
			return 0;
		}
		
		// clear the accumulated position increment
		mAccumPosInc = 0;
		
		return Math.max(1, posInc);
	}

	/**
	 * Checks if the given word type includes {@link #ALPHA}
	 *
	 * @param type Word type to check
	 * @return {@code true} if the type contains ALPHA, {@code false} otherwise
	 */
	static boolean isAlpha(int type) {
		return (type & ALPHA) != 0;
	}

	/**
	 * Checks if the given word type includes {@link #DIGIT}
	 *
	 * @param type Word type to check
	 * @return {@code true} if the type contains DIGIT, {@code false} otherwise
	 */
	static boolean isDigit(int type) {
		return (type & DIGIT) != 0;
	}

	/**
	 * Checks if the given word type includes {@link #SUBWORD_DELIM}
	 *
	 * @param type Word type to check
	 * @return {@code true} if the type contains SUBWORD_DELIM, {@code false} otherwise
	 */
	static boolean isSubwordDelim(int type) {
		return (type & SUBWORD_DELIM) != 0;
	}

	/**
	 * Checks if the given word type includes {@link #UPPER}
	 *
	 * @param type Word type to check
	 * @return {@code true} if the type contains UPPER, {@code false} otherwise
	 */
	static boolean isUpper(int type) {
		return (type & UPPER) != 0;
	}

	/**
	 * Determines whether the given flag is set
	 *
	 * @param flag Flag to see if set
	 * @return {@code true} if flag is set
	 */
	private boolean has(int flag) {
		return (mFlags & flag) != 0;
	}

	/**
	 * A WDF concatenated 'run'
	 */
	final class WordDelimiterConcatenation {
		private final StringBuilder mBuffer = new StringBuilder();
		private int mStartOffset;
		private int mEndOffset;
		private int mType;
		private int mSubwordCount;

		/**
		 * Appends the given text of the given length, to the concetenation at the given offset
		 *
		 * @param text Text to append
		 * @param offset Offset in the concetenation to add the text
		 * @param length Length of the text to append
		 */
		public void append(char text[], int offset, int length) {
			mBuffer.append(text, offset, length);
			mSubwordCount ++;
		}

		/**
		 * Writes the concatenation to the attributes
		 */
		public void write() {
			mToken.clear();
			if (mToken.getTerm().length() < mBuffer.length()) 
				mToken.getTerm().resizeBuffer(mBuffer.length());
			
			char termbuffer[] = mToken.getTerm().buffer();
      
			mBuffer.getChars(0, mBuffer.length(), termbuffer, 0);
			mToken.getTerm().setLength(mBuffer.length());
        
			if (mHasIllegalOffsets) 
				mToken.setOffset(mSavedStartOffset, mSavedEndOffset);
			else 
				mToken.setOffset(mStartOffset, mEndOffset);
			
			mToken.setPositionIncrement(position(true));
			mToken.setType(mSavedType);
			
			mAccumPosInc = 0;
		}

		/**
		 * Determines if the concatenation is empty
		 *
		 * @return {@code true} if the concatenation is empty, {@code false} otherwise
		 */
		public boolean isEmpty() {
			return mBuffer.length() == 0;
		}

		/**
		 * Clears the concatenation and resets its state
		 */
		public void clear() {
			mBuffer.setLength(0);
			mStartOffset = mEndOffset = mType = mSubwordCount = 0;
		}

		/**
		 * Convenience method for the common scenario of having to write 
		 * the concetenation and then clearing its state
		 */
		public void writeAndClear() {
			write();
			clear();
		}
	}
	
	// questions:
	// negative numbers?  -42 indexed as just 42?
	// dollar sign?  $42
	// percent sign?  33%
	// downsides:  if source text is "powershot" then a query of "PowerShot" won't match!
	
}
