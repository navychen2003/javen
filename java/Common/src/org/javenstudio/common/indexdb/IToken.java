package org.javenstudio.common.indexdb;

import org.javenstudio.common.indexdb.util.BytesRef;

public interface IToken extends Cloneable {

	/** 
	 * Updates the bytes {@link #getBytesRef()} to contain this term's
	 * final encoding, and returns its hashcode.
	 * @return the hashcode as defined by {@link BytesRef#hashCode}:
	 * <pre>
	 *  int hash = 0;
	 *  for (int i = termBytes.offset; i &lt; termBytes.offset+termBytes.length; i++) {
	 *    hash = 31*hash + termBytes.bytes[i];
	 *  }
	 * </pre>
	 * Implement this for performance reasons, if your code can calculate
	 * the hash on-the-fly. If this is not the case, just return
	 * {@code termBytes.hashCode()}.
	 */
	public int fillBytesRef(BytesRef bytes);
	
	/** 
	 * Updates the bytes {@link #getBytesRef()} to contain this term's
	 * final encoding, and returns its hashcode.
	 * @return the hashcode as defined by {@link BytesRef#hashCode}:
	 * <pre>
	 *  int hash = 0;
	 *  for (int i = termBytes.offset; i &lt; termBytes.offset+termBytes.length; i++) {
	 *    hash = 31*hash + termBytes.bytes[i];
	 *  }
	 * </pre>
	 * Implement this for performance reasons, if your code can calculate
	 * the hash on-the-fly. If this is not the case, just return
	 * {@code termBytes.hashCode()}.
	 */
	public int fillBytesRef();
  
	/**
	 * Retrieve this attribute's BytesRef. The bytes are updated 
	 * from the current term when the consumer calls {@link #fillBytesRef()}.
	 * @return this Attributes internal BytesRef.
	 */
	public BytesRef getBytesRef();
	
	/** 
	 * Returns the position increment of this Token.
	 * @see #setPositionIncrement
	 */
	public int getPositionIncrement();

	/** 
	 * Returns this Token's starting offset, the position of the first character
	 * corresponding to this token in the source text.
	 *
  	 * Note that the difference between endOffset() and startOffset() may not be
	 * equal to {@link #length}, as the term text may have been altered by a
	 * stemmer or some other filter. 
	 */
	public int getStartOffset();
	
	/** 
	 * Returns this Token's ending offset, one greater than the position of the
	 * last character corresponding to this token in the source text. The length
	 * of the token in the source text is (endOffset - startOffset). 
	 */
	public int getEndOffset();

	/** 
	 * Returns this Token's lexical type.  Defaults to "word". 
	 */
	public String getType();
	
	/**
	 * Get the bitset for any bits that have been set.  This is completely distinct 
	 * from {@link #type()}, although they do share similar purposes.
	 * The flags can be used to encode information about the token for use by other 
	 * {@link TokenFilter}s.
	 *
	 * @return The bits. While we think this is here to stay, we may want to change 
	 * it to be a long.
	 */
	public int getFlags();

	/**
	 * Returns <code>true</code> if the current token is a keyword, otherwise
	 * <code>false</code>
	 * 
	 * @return <code>true</code> if the current token is a keyword, otherwise
	 *         <code>false</code>
	 * @see #setKeyword(boolean)
	 */
	public boolean isKeyword();
	
	/**
	 * Returns this Token's payload.
	 */
	public BytesRef getPayload();

	/**
	 * Returns this Token's copy.
	 */
	public Object clone();
	
}
