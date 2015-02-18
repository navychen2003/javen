package org.javenstudio.common.indexdb.util;

import java.nio.CharBuffer;

/**
 * The term text of a Token.
 */
public class CharTerm implements CharSequence, Appendable {
	public static int MIN_BUFFER_SIZE = 10;
	  
	private char[] mTermBuffer = new char[ArrayUtil.oversize(
			MIN_BUFFER_SIZE, JvmUtil.NUM_BYTES_CHAR)];
	private int mTermLength = 0;
	
	public CharTerm() {}
	
	private void growTermBuffer(int newSize) {
		if (mTermBuffer.length < newSize){
			// Not big enough; create a new array with slight
			// over allocation:
			mTermBuffer = new char[ArrayUtil.oversize(newSize, JvmUtil.NUM_BYTES_CHAR)];
		}
	}
	
	/** 
	 * Copies the contents of buffer, starting at offset for
	 *  length characters, into the termBuffer array.
	 *  @param buffer the buffer to copy
	 *  @param offset the index in the buffer of the first character to copy
	 *  @param length the number of characters to copy
	 */
	public final void copyBuffer(char[] buffer, int offset, int length) { 
		growTermBuffer(length);
	    System.arraycopy(buffer, offset, mTermBuffer, 0, length);
	    mTermLength = length;
	}
  
	/** 
	 * Returns the internal termBuffer character array which
	 *  you can then directly alter.  If the array is too
	 *  small for your token, use {@link
	 *  #resizeBuffer(int)} to increase it.  After
	 *  altering the buffer be sure to call {@link
	 *  #setLength} to record the number of valid
	 *  characters that were placed into the termBuffer. 
	 */
	public final char[] buffer() { 
		return mTermBuffer;
	}

	/** 
	 * Grows the termBuffer to at least size newSize, preserving the
	 *  existing content.
	 *  @param newSize minimum size of the new termBuffer
	 *  @return newly created termBuffer with length >= newSize
	 */
	public final char[] resizeBuffer(int newSize) { 
		if (mTermBuffer.length < newSize){
			// Not big enough; create a new array with slight
			// over allocation and preserve content
			final char[] newCharBuffer = new char[ArrayUtil.oversize(newSize, JvmUtil.NUM_BYTES_CHAR)];
	        System.arraycopy(mTermBuffer, 0, newCharBuffer, 0, mTermBuffer.length);
	        mTermBuffer = newCharBuffer;
		}
		return mTermBuffer;
	}

	/** 
	 * Set number of valid characters (length of the term) in
	 *  the termBuffer array. Use this to truncate the termBuffer
	 *  or to synchronize with external manipulation of the termBuffer.
	 *  Note: to grow the size of the array,
	 *  use {@link #resizeBuffer(int)} first.
	 *  @param length the truncated length
	 */
	public final CharTerm setLength(int length) { 
		if (length > mTermBuffer.length) { 
			throw new IllegalArgumentException("length " + length 
					+ " exceeds the size of the termBuffer (" + mTermBuffer.length + ")");
		}
		
		mTermLength = length;
		return this;
	}
  
	/** 
	 * Sets the length of the termBuffer to zero.
	 * Use this method before appending contents
	 * using the {@link Appendable} interface.
	 */
	public final CharTerm setEmpty() { 
		mTermLength = 0;
	    return this;
	}
  
	// the following methods are redefined to get rid of IOException declaration:
	@Override
	public final CharTerm append(CharSequence csq) { 
		if (csq == null) // needed for Appendable compliance
			return appendNull();
		
		return append(csq, 0, csq.length());
	}
	
	@Override
	public final CharTerm append(CharSequence csq, int start, int end) { 
		if (csq == null) // needed for Appendable compliance
			csq = "null";
		
		final int len = end - start, csqlen = csq.length();
		if (len < 0 || start > csqlen || end > csqlen)
			throw new IndexOutOfBoundsException();
		
		if (len == 0)
			return this;
		
		resizeBuffer(mTermLength + len);
		
		if (len > 4) { // only use instanceof check series for longer CSQs, else simply iterate
			if (csq instanceof String) {
		        ((String) csq).getChars(start, end, mTermBuffer, mTermLength);
			} else if (csq instanceof StringBuilder) {
		        ((StringBuilder) csq).getChars(start, end, mTermBuffer, mTermLength);
			} else if (csq instanceof CharTerm) {
		        System.arraycopy(((CharTerm) csq).buffer(), start, mTermBuffer, mTermLength, len);
			} else if (csq instanceof CharBuffer && ((CharBuffer) csq).hasArray()) {
		        final CharBuffer cb = (CharBuffer) csq;
		        System.arraycopy(cb.array(), cb.arrayOffset() + cb.position() + start, mTermBuffer, mTermLength, len);
			} else if (csq instanceof StringBuffer) {
		        ((StringBuffer) csq).getChars(start, end, mTermBuffer, mTermLength);
			} else {
		        while (start < end) { 
		        	mTermBuffer[mTermLength++] = csq.charAt(start++);
		        }
		        // no fall-through here, as termLength is updated!
		        return this;
			}
			
			mTermLength += len;
			return this;
			
		} else {
			while (start < end) { 
		        mTermBuffer[mTermLength++] = csq.charAt(start++);
			}
			return this;
		}
	}
	
	@Override
	public final CharTerm append(char c) { 
		resizeBuffer(mTermLength + 1)[mTermLength++] = c;
	    return this;
	}

	/** 
	 * Appends the specified {@code String} to this character sequence. 
	 * <p>The characters of the {@code String} argument are appended, in order, increasing the length of
	 * this sequence by the length of the argument. If argument is {@code null}, then the four
	 * characters {@code "null"} are appended. 
	 */
	public final CharTerm append(String s) { 
		if (s == null) // needed for Appendable compliance
			return appendNull();
		
		final int len = s.length();
		s.getChars(0, len, resizeBuffer(mTermLength + len), mTermLength);
		mTermLength += len;
		return this;
	}

	/** 
	 * Appends the specified {@code StringBuilder} to this character sequence. 
	 * <p>The characters of the {@code StringBuilder} argument are appended, in order, increasing the length of
	 * this sequence by the length of the argument. If argument is {@code null}, then the four
	 * characters {@code "null"} are appended. 
	 */
	public final CharTerm append(StringBuilder s) { 
	    if (s == null) // needed for Appendable compliance
	    	return appendNull();
	    
	    final int len = s.length();
	    s.getChars(0, len, resizeBuffer(mTermLength + len), mTermLength);
	    mTermLength += len;
	    return this;
	}

	/** 
	 * Appends the contents of the other {@code CharTerm} to this character sequence. 
	 * <p>The characters of the {@code CharTerm} argument are appended, in order, increasing the length of
	 * this sequence by the length of the argument. If argument is {@code null}, then the four
	 * characters {@code "null"} are appended. 
	 */
	public final CharTerm append(CharTerm termAtt) { 
		if (termAtt == null) // needed for Appendable compliance
			return appendNull();
		
		final int len = termAtt.length();
		System.arraycopy(termAtt.buffer(), 0, resizeBuffer(mTermLength + len), mTermLength, len);
		mTermLength += len;
		return this;
	}
	
	private CharTerm appendNull() {
		resizeBuffer(mTermLength + 4);
		mTermBuffer[mTermLength++] = 'n';
		mTermBuffer[mTermLength++] = 'u';
		mTermBuffer[mTermLength++] = 'l';
		mTermBuffer[mTermLength++] = 'l';
		return this;
	}
	
	@Override
	public int hashCode() {
	    int code = mTermLength;
	    code = code * 31 + ArrayUtil.hashCode(mTermBuffer, 0, mTermLength);
	    return code;
	}

	public void clear() {
	    mTermLength = 0;    
	}

	@Override
	public final int length() {
		return mTermLength;
	}

	@Override
	public final char charAt(int index) {
		if (index >= mTermLength)
			throw new IndexOutOfBoundsException();
		return mTermBuffer[index];
	}

	@Override
	public final CharSequence subSequence(final int start, final int end) {
		if (start > mTermLength || end > mTermLength)
			throw new IndexOutOfBoundsException();
		return new String(mTermBuffer, start, end - start);
	}
	
	@Override
	public Object clone() {
		Object clone = null;
	    try {
	    	clone = super.clone();
	    } catch (CloneNotSupportedException e) {
	    	throw new RuntimeException(e);  // shouldn't happen
	    }
	    
		CharTerm t = (CharTerm)clone;
	    // Do a deep clone
	    t.mTermBuffer = new char[this.mTermLength];
	    System.arraycopy(this.mTermBuffer, 0, t.mTermBuffer, 0, this.mTermLength);
	    
	    return t;
	}
	  
	@Override
	public boolean equals(Object other) {
	    if (other == this) 
	    	return true;
	    
	    if (other instanceof CharTerm) {
	    	final CharTerm o = ((CharTerm) other);
	    	if (mTermLength != o.mTermLength)
	    		return false;
	    	
	    	for (int i=0; i < mTermLength; i++) {
	    		if (mTermBuffer[i] != o.mTermBuffer[i]) 
	    			return false;
	    	}
	    	
	    	return true;
	    }
	    
	    return false;
	}

	/** 
	 * Returns solely the term text as specified by the
	 * {@link CharSequence} interface.
	 * <p>This method changed the behavior with Indexdb 3.1,
	 * before it returned a String representation of the whole
	 * term with all attributes.
	 * This affects especially the
	 * {@link Token} subclass.
	 */
	@Override
	public String toString() {
	    // CharSequence requires that only the contents are returned, 
		// but this is orginal code: "term=" + new String(termBuffer, 0, termLength)
	    return new String(mTermBuffer, 0, mTermLength);
	}
	
}
