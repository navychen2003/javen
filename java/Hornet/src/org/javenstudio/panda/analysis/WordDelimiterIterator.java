package org.javenstudio.panda.analysis;

/**
 * A BreakIterator-like API for iterating over subwords in text, 
 * according to WordDelimiterFilter rules.
 * 
 */
public final class WordDelimiterIterator {

	/** Indicates the end of iteration */
	public static final int DONE = -1;
  
	public static final byte[] DEFAULT_WORD_DELIM_TABLE;

	private char mText[];
	private int mLength;
  
	/** start position of text, excluding leading delimiters */
	private int mStartBounds;
	
	/** end position of text, excluding trailing delimiters */
	private int mEndBounds;
  
	/** Beginning of subword */
	private int mCurrent;
	
	/** End of subword */
	private int mEnd;
  
	/** does this string end with a possessive such as 's */
	private boolean mHasFinalPossessive = false;
  
	/**
	 * If false, causes case changes to be ignored (subwords will only be generated
	 * given SUBWORD_DELIM tokens). (Defaults to true)
	 */
	private final boolean mSplitOnCaseChange;
  
	/**
	 * If false, causes numeric changes to be ignored (subwords will only be generated
	 * given SUBWORD_DELIM tokens). (Defaults to true)
	 */
	private final boolean mSplitOnNumerics;

	/**
	 * If true, causes trailing "'s" to be removed for each subword. (Defaults to true)
	 * <p/>
	 * "O'Neil's" => "O", "Neil"
	 */
	private final boolean mStemEnglishPossessive;

	private final byte[] mCharTypeTable;
  
	/** if true, need to skip over a possessive found in the last call to next() */
	private boolean mSkipPossessive = false;

	// TODO: should there be a WORD_DELIM category for chars that only separate words 
	// (no catenation of subwords will be
	// done if separated by these chars?) "," would be an obvious candidate...
	static {
		byte[] tab = new byte[256];
		for (int i = 0; i < 256; i++) {
			byte code = 0;
			if (Character.isLowerCase(i)) {
				code |= WordDelimiterFilter.LOWER;
				
			} else if (Character.isUpperCase(i)) {
				code |= WordDelimiterFilter.UPPER;
				
			} else if (Character.isDigit(i)) {
				code |= WordDelimiterFilter.DIGIT;
			}
			
			if (code == 0) 
				code = WordDelimiterFilter.SUBWORD_DELIM;
			
			tab[i] = code;
		}
		
		DEFAULT_WORD_DELIM_TABLE = tab;
	}

	/**
	 * Create a new WordDelimiterIterator operating with the supplied rules.
	 * 
	 * @param charTypeTable table containing character types
	 * @param splitOnCaseChange if true, causes "PowerShot" to be two tokens; 
	 * ("Power-Shot" remains two parts regards)
	 * @param splitOnNumerics if true, causes "j2se" to be three tokens; "j" "2" "se"
	 * @param stemEnglishPossessive if true, causes trailing "'s" to be removed 
	 * for each subword: "O'Neil's" => "O", "Neil"
	 */
	public WordDelimiterIterator(byte[] charTypeTable, boolean splitOnCaseChange, 
			boolean splitOnNumerics, boolean stemEnglishPossessive) {
		mCharTypeTable = charTypeTable;
		mSplitOnCaseChange = splitOnCaseChange;
		mSplitOnNumerics = splitOnNumerics;
		mStemEnglishPossessive = stemEnglishPossessive;
	}
  
	public int getEnd() { return mEnd; }
	public int getCurrent() { return mCurrent; }
	
	final void setText(char[] text) { mText = text; }
	
	/**
	 * Advance to the next subword in the string.
	 *
	 * @return index of the next subword, or {@link #DONE} if all subwords have been returned
	 */
	protected int next() {
		mCurrent = mEnd;
		if (mCurrent == DONE) 
			return DONE;
		
		if (mSkipPossessive) {
			mCurrent += 2;
			mSkipPossessive = false;
		}

		int lastType = 0;
    
		while (mCurrent < mEndBounds && (WordDelimiterFilter.isSubwordDelim(
				lastType = charType(mText[mCurrent])))) {
			mCurrent ++;
		}

		if (mCurrent >= mEndBounds) 
			return mEnd = DONE;
    
		for (mEnd = mCurrent + 1; mEnd < mEndBounds; mEnd++) {
			int type = charType(mText[mEnd]);
			if (isBreak(lastType, type)) 
				break;
			
			lastType = type;
		}
    
		if (mEnd < mEndBounds - 1 && endsWithPossessive(mEnd + 2)) 
			mSkipPossessive = true;
    
		return mEnd;
	}

	/**
	 * Return the type of the current subword.
	 * This currently uses the type of the first character in the subword.
	 *
	 * @return type of the current word
	 */
	protected int type() {
		if (mEnd == DONE) 
			return 0;
    
		int type = charType(mText[mCurrent]);
		
		switch (type) {
		// return ALPHA word type for both lower and upper
		case WordDelimiterFilter.LOWER:
		case WordDelimiterFilter.UPPER:
			return WordDelimiterFilter.ALPHA;
			
		default:
			return type;
		}
	}

	/**
	 * Reset the text to a new value, and reset all state
	 *
	 * @param text New text
	 * @param length length of the text
	 */
	protected void setText(char text[], int length) {
		mText = text;
		mLength = mEndBounds = length;
		mCurrent = mStartBounds = mEnd = 0;
		mSkipPossessive = mHasFinalPossessive = false;
		
		setBounds();
	}

	/**
	 * Determines whether the transition from lastType to type indicates a break
	 *
	 * @param lastType Last subword type
	 * @param type Current subword type
	 * @return {@code true} if the transition indicates a break, {@code false} otherwise
	 */
	private boolean isBreak(int lastType, int type) {
		if ((type & lastType) != 0) 
			return false;
    
		if (!mSplitOnCaseChange && WordDelimiterFilter.isAlpha(lastType) && 
			WordDelimiterFilter.isAlpha(type)) {
			// ALPHA->ALPHA: always ignore if case isn't considered.
			return false;
			
		} else if (WordDelimiterFilter.isUpper(lastType) && WordDelimiterFilter.isAlpha(type)) {
			// UPPER->letter: Don't split
			return false;
			
		} else if (!mSplitOnNumerics && 
			((WordDelimiterFilter.isAlpha(lastType) && WordDelimiterFilter.isDigit(type)) || 
			 (WordDelimiterFilter.isDigit(lastType) && WordDelimiterFilter.isAlpha(type)))) {
			// ALPHA->NUMERIC, NUMERIC->ALPHA :Don't split
			return false;
		}

		return true;
	}
  
	/**
	 * Determines if the current word contains only one subword. 
	 * Note, it could be potentially surrounded by delimiters
	 *
	 * @return {@code true} if the current word contains only one subword, 
	 * {@code false} otherwise
	 */
	protected boolean isSingleWord() {
		if (mHasFinalPossessive) 
			return mCurrent == mStartBounds && mEnd == mEndBounds - 2;
		else 
			return mCurrent == mStartBounds && mEnd == mEndBounds;
	}
   
	/**
	 * Set the internal word bounds (remove leading and trailing delimiters). 
	 * Note, if a possessive is found, don't remove
	 * it yet, simply note it.
	 */
	private void setBounds() {
		while (mStartBounds < mLength && 
			(WordDelimiterFilter.isSubwordDelim(charType(mText[mStartBounds])))) {
			mStartBounds ++;
		}
    
		while (mEndBounds > mStartBounds && 
			(WordDelimiterFilter.isSubwordDelim(charType(mText[mEndBounds - 1])))) {
			mEndBounds --;
		}
		
		if (endsWithPossessive(mEndBounds)) 
			mHasFinalPossessive = true;
		
		mCurrent = mStartBounds;
	}
  
	/**
	 * Determines if the text at the given position indicates an English possessive which should be removed
	 *
	 * @param pos Position in the text to check if it indicates an English possessive
	 * @return {@code true} if the text at the position indicates an English posessive, {@code false} otherwise
	 */
	private boolean endsWithPossessive(int pos) {
		return (mStemEnglishPossessive && pos > 2 &&
				mText[pos - 2] == '\'' && (mText[pos - 1] == 's' || mText[pos - 1] == 'S') &&
				WordDelimiterFilter.isAlpha(charType(mText[pos - 3])) &&
			   (pos == mEndBounds || WordDelimiterFilter.isSubwordDelim(charType(mText[pos]))));
	}

	/**
	 * Determines the type of the given character
	 *
	 * @param ch Character whose type is to be determined
	 * @return Type of the character
	 */
	private int charType(int ch) {
		if (ch < mCharTypeTable.length) 
			return mCharTypeTable[ch];
		
		return getType(ch);
	}
  
	/**
	 * Computes the type of the given character
	 *
	 * @param ch Character whose type is to be determined
	 * @return Type of the character
	 */
	public static byte getType(int ch) {
		switch (Character.getType(ch)) {
		case Character.UPPERCASE_LETTER: 
			return WordDelimiterFilter.UPPER;
			
		case Character.LOWERCASE_LETTER: 
			return WordDelimiterFilter.LOWER;

		case Character.TITLECASE_LETTER:
		case Character.MODIFIER_LETTER:
		case Character.OTHER_LETTER:
		case Character.NON_SPACING_MARK:
		case Character.ENCLOSING_MARK:  // depends what it encloses?
		case Character.COMBINING_SPACING_MARK:
			return WordDelimiterFilter.ALPHA; 

		case Character.DECIMAL_DIGIT_NUMBER:
		case Character.LETTER_NUMBER:
		case Character.OTHER_NUMBER:
			return WordDelimiterFilter.DIGIT;

		//case Character.SPACE_SEPARATOR:
		//case Character.LINE_SEPARATOR:
		//case Character.PARAGRAPH_SEPARATOR:
		//case Character.CONTROL:
		//case Character.FORMAT:
		//case Character.PRIVATE_USE:

		case Character.SURROGATE:  // prevent splitting
			return WordDelimiterFilter.ALPHA|WordDelimiterFilter.DIGIT;  

		//case Character.DASH_PUNCTUATION:
		//case Character.START_PUNCTUATION:
		//case Character.END_PUNCTUATION:
		//case Character.CONNECTOR_PUNCTUATION:
		//case Character.OTHER_PUNCTUATION:
		//case Character.MATH_SYMBOL:
		//case Character.CURRENCY_SYMBOL:
		//case Character.MODIFIER_SYMBOL:
		//case Character.OTHER_SYMBOL:
		//case Character.INITIAL_QUOTE_PUNCTUATION:
		//case Character.FINAL_QUOTE_PUNCTUATION:

		default: 
			return WordDelimiterFilter.SUBWORD_DELIM;
		}
	}
	
}