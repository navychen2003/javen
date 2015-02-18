package org.javenstudio.panda.util;

import java.text.BreakIterator;
import java.text.CharacterIterator;
import java.util.Locale;

/** 
 * A CharacterIterator used internally for use with {@link BreakIterator}
 * 
 */
public abstract class CharArrayIterator implements CharacterIterator {
	
	private char mArray[];
	private int mStart;
	private int mIndex;
	private int mLength;
	private int mLimit;

	public char[] getText() { return mArray; }
	public int getStart() { return mStart; }
	public int getLength() { return mLength; }
  
	/**
	 * Set a new region of text to be examined by this iterator
	 * 
	 * @param array text buffer to examine
	 * @param start offset into buffer
	 * @param length maximum length to examine
	 */
	public void setText(final char array[], int start, int length) {
		mArray = array;
		mStart = start;
		mIndex = start;
		mLength = length;
		mLimit = start + length;
	}

	@Override
	public char current() {
		return (mIndex == mLimit) ? DONE : jreBugWorkaround(mArray[mIndex]);
	}
  
	protected abstract char jreBugWorkaround(char ch);

	@Override
	public char first() {
		mIndex = mStart;
		return current();
	}

	@Override
	public int getBeginIndex() {
		return 0;
	}

	@Override
	public int getEndIndex() {
		return mLength;
	}

	@Override
	public int getIndex() {
		return mIndex - mStart;
	}

	@Override
	public char last() {
		mIndex = (mLimit == mStart) ? mLimit : mLimit - 1;
		return current();
	}

	@Override
	public char next() {
		if (++mIndex >= mLimit) {
			mIndex = mLimit;
			return DONE;
		} else {
			return current();
		}
	}

	@Override
	public char previous() {
		if (--mIndex < mStart) {
			mIndex = mStart;
			return DONE;
		} else {
			return current();
		}
	}

	@Override
	public char setIndex(int position) {
		if (position < getBeginIndex() || position > getEndIndex())
			throw new IllegalArgumentException("Illegal Position: " + position);
		
		mIndex = mStart + position;
		return current();
	}
  
	@Override
	public CharArrayIterator clone() {
		try {
			return (CharArrayIterator)super.clone();
		} catch (CloneNotSupportedException e) {
			// CharacterIterator does not allow you to throw CloneNotSupported
			throw new RuntimeException(e);
		}
	}
  
	/**
	 * Create a new CharArrayIterator that works around JRE bugs
	 * in a manner suitable for {@link BreakIterator#getSentenceInstance()}
	 */
	public static CharArrayIterator newSentenceInstance() {
		if (HAS_BUGGY_BREAKITERATORS) {
			return new CharArrayIterator() {
				// work around this for now by lying about all surrogates to 
				// the sentence tokenizer, instead we treat them all as 
				// SContinue so we won't break around them.
				@Override
				protected char jreBugWorkaround(char ch) {
					return ch >= 0xD800 && ch <= 0xDFFF ? 0x002C : ch;
				}
			};
		} else {
			return new CharArrayIterator() {
				// no bugs
				@Override
				protected char jreBugWorkaround(char ch) {
					return ch;
				}
			};
		}
	}
  
	/**
	 * Create a new CharArrayIterator that works around JRE bugs
	 * in a manner suitable for {@link BreakIterator#getWordInstance()}
	 */
	public static CharArrayIterator newWordInstance() {
		if (HAS_BUGGY_BREAKITERATORS) {
			return new CharArrayIterator() {
				// work around this for now by lying about all surrogates to the word, 
				// instead we treat them all as ALetter so we won't break around them.
				@Override
				protected char jreBugWorkaround(char ch) {
					return ch >= 0xD800 && ch <= 0xDFFF ? 0x0041 : ch;
				}
			};
		} else {
			return new CharArrayIterator() {
				// no bugs
				@Override
				protected char jreBugWorkaround(char ch) {
					return ch;
				}
			};
		}
	}
  
	/**
	 * True if this JRE has a buggy BreakIterator implementation
	 */
	public static final boolean HAS_BUGGY_BREAKITERATORS;
	static {
		boolean v;
		try {
			BreakIterator bi = BreakIterator.getSentenceInstance(Locale.US);
			bi.setText("\udb40\udc53");
			bi.next();
			v = false;
		} catch (Exception e) {
			v = true;
		}
		HAS_BUGGY_BREAKITERATORS = v;
	}
	
}
