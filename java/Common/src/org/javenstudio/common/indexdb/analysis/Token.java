package org.javenstudio.common.indexdb.analysis;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.util.BytesRef;

public abstract class Token implements IToken {

	/** the default type */
	public static final String DEFAULT_TYPE = "word";
	
	protected int mStartOffset = 0; 
	protected int mEndOffset = 0;
	protected String mType = DEFAULT_TYPE;
	protected int mFlags = 0;
	protected BytesRef mPayload = null;
	protected int mPositionIncrement = 1;
	protected boolean mKeyword = false;
	
	/** Constructs a Token will null text. */
	public Token() {
		this(0, 0, 0, null);
	}

	/** 
	 * Constructs a Token with null text and start & end
	 *  offsets.
	 *  @param start start offset in the source text
	 *  @param end end offset in the source text 
	 */
	public Token(int start, int end) {
		this(start, end, 0, null);
	}

	/** 
	 * Constructs a Token with null text and start & end
	 *  offsets plus the Token type.
	 *  @param start start offset in the source text
	 *  @param end end offset in the source text
	 *  @param type the lexical type of this Token 
	 */
	public Token(int start, int end, String type) {
		this(start, end, 0, type);
	}

	/**
	 * Constructs a Token with null text and start & end
	 *  offsets plus flags. NOTE: flags is EXPERIMENTAL.
	 *  @param start start offset in the source text
	 *  @param end end offset in the source text
	 *  @param flags The bits to set for this token
	 */
	public Token(int start, int end, int flags) {
	    this(start, end, flags, null);
	}
	
	/**
	 * Constructs a Token with null text and start & end
	 *  offsets plus flags. NOTE: flags is EXPERIMENTAL.
	 *  @param start start offset in the source text
	 *  @param end end offset in the source text
	 *  @param flags The bits to set for this token
	 *  @param type the lexical type of this Token 
	 */
	public Token(int start, int end, int flags, String type) {
	    mStartOffset = start;
	    mEndOffset = end;
	    mFlags = flags;
	    mType = (type != null && type.length() > 0) ? type : DEFAULT_TYPE;
	    mKeyword = false;
	}
	
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
	@Override
	public abstract int fillBytesRef(BytesRef bytes);
	
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
	@Override
	public abstract int fillBytesRef();
  
	/**
	 * Retrieve this attribute's BytesRef. The bytes are updated 
	 * from the current term when the consumer calls {@link #fillBytesRef()}.
	 * @return this Attributes internal BytesRef.
	 */
	@Override
	public abstract BytesRef getBytesRef();
	
	/** 
	 * Returns this Token's starting offset, the position of the first character
	 * corresponding to this token in the source text.
	 *
  	 * Note that the difference between endOffset() and startOffset() may not be
	 * equal to {@link #length}, as the term text may have been altered by a
	 * stemmer or some other filter. 
	 */
	@Override
	public final int getStartOffset() {
	    return mStartOffset;
	}

	/** 
	 * Set the starting offset.
	 * @see #startOffset() 
	 */
	public void setStartOffset(int offset) {
	    mStartOffset = offset;
	}
	
	/** 
	 * Returns this Token's ending offset, one greater than the position of the
	 * last character corresponding to this token in the source text. The length
	 * of the token in the source text is (endOffset - startOffset). 
	 */
	@Override
	public final int getEndOffset() {
	    return mEndOffset;
	}

	/** 
	 * Set the ending offset.
	 * @see #endOffset() 
	 */
	public void setEndOffset(int offset) {
	    mEndOffset = offset;
	}
	
	/** 
	 * Set the starting and ending offset.
     * @see #getStartOffset() and #getEndOffset()
     */
	public void setOffset(int startOffset, int endOffset) {
		// TODO: we could assert that this is set-once, ie,
		// current values are -1?  Very few token filters should
		// change offsets once set by the tokenizer... and
		// tokenizer should call clearAtts before re-using
		// OffsetAtt
		if (startOffset < 0 || endOffset < startOffset) {
			throw new IllegalArgumentException(
					"startOffset must be non-negative, and endOffset must be >= startOffset, "
					+ "startOffset=" + startOffset + ",endOffset=" + endOffset);
		}

		mStartOffset = startOffset;
		mEndOffset = endOffset;
	}
	
	/** 
	 * Returns this Token's lexical type.  Defaults to "word". 
	 */
	@Override
	public final String getType() {
	    return mType;
	}

	/** 
	 * Set the lexical type.
	 * @see #type() 
	 */
	public final void setType(String type) {
	    mType = type;
	}
	
	/**
	 * Get the bitset for any bits that have been set.  This is completely distinct 
	 * from {@link #type()}, although they do share similar purposes.
	 * The flags can be used to encode information about the token for use by other 
	 * {@link TokenFilter}s.
	 *
	 * @return The bits. While we think this is here to stay, we may want to change 
	 * it to be a long.
	 */
	@Override
	public int getFlags() {
	    return mFlags;
	}

	/**
	 * @see #getFlags()
	 */
	public void setFlags(int flags) {
	    mFlags = flags;
	}
	
	/**
	 * Returns this Token's payload.
	 */
	@Override
	public BytesRef getPayload() {
	    return mPayload;
	}

	/** 
	 * Sets this Token's payload.
	 */
	public void setPayload(BytesRef payload) {
		mPayload = payload;
	}
	
	/** 
	 * Set the position increment.  This determines the position of this token
	 * relative to the previous Token in a {@link TokenStream}, used in phrase
	 * searching.
	 *
	 * <p>The default value is one.
	 *
	 * <p>Some common uses for this are:<ul>
	 *
	 * <li>Set it to zero to put multiple terms in the same position.  This is
	 * useful if, e.g., a word has multiple stems.  Searches for phrases
	 * including either stem will match.  In this case, all but the first stem's
	 * increment should be set to zero: the increment of the first instance
	 * should be one.  Repeating a token with an increment of zero can also be
	 * used to boost the scores of matches on that token.
	 *
	 * <li>Set it to values greater than one to inhibit exact phrase matches.
	 * If, for example, one does not want phrases to match across removed stop
	 * words, then one could build a stop word filter that removes stop words and
	 * also sets the increment to the number of stop words removed before each
	 * non-stop word.  Then exact phrase queries will only match when the terms
	 * occur with no intervening stop words.
	 *
	 * </ul>
	 * @param positionIncrement the distance from the prior term
	 * @see org.apache.lucene.index.TermPositions
	 */
	public void setPositionIncrement(int positionIncrement) {
		if (positionIncrement < 0)
			throw new IllegalArgumentException("Increment must be zero or greater: " + positionIncrement);
	    mPositionIncrement = positionIncrement;
	}
	
	/** 
	 * Returns the position increment of this Token.
	 * @see #setPositionIncrement
	 */
	@Override
	public final int getPositionIncrement() {
	    return mPositionIncrement;
	}
	
	/**
	 * Returns <code>true</code> if the current token is a keyword, otherwise
	 * <code>false</code>
	 * 
	 * @return <code>true</code> if the current token is a keyword, otherwise
	 *         <code>false</code>
	 * @see #setKeyword(boolean)
	 */
	@Override
	public boolean isKeyword() { 
		return mKeyword;
	}

	/**
	 * Marks the current token as keyword if set to <code>true</code>.
	 * 
	 * @param isKeyword
	 *          <code>true</code> if the current token is a keyword, otherwise
	 *          <code>false</code>.
	 * @see #isKeyword()
	 */
	public void setKeyword(boolean isKeyword) { 
		mKeyword = isKeyword;
	}
	
	/** 
	 * Resets the term text, payload, flags, and positionIncrement,
	 * startOffset, endOffset and token type to default.
	 */
	public void clear() {
	    mPayload = null;
	    mPositionIncrement = 1;
	    mFlags = 0;
	    mStartOffset = mEndOffset = 0;
	    mType = DEFAULT_TYPE;
	    mKeyword = false;
	}
	
	@Override
	public Object clone() {
		Token t;
		try {
			t = (Token)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
		
	    // Do a deep clone
	    t.mPayload = clonePayload();
	    
	    return t;
	}
	
	protected BytesRef clonePayload() { 
		return mPayload != null ? mPayload.clone() : null;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;

	    if (obj instanceof Token) {
	    	final Token other = (Token) obj;
	    	return (mStartOffset == other.mStartOffset &&
	    			mEndOffset == other.mEndOffset && 
	    			mFlags == other.mFlags &&
	    			mPositionIncrement == other.mPositionIncrement &&
	    			mKeyword == other.mKeyword &&
	    			(mType == null ? other.mType == null : mType.equals(other.mType)) &&
	    			(mPayload == null ? other.mPayload == null : mPayload.equals(other.mPayload))
	    		);
	      
	    }
	    
	    return false;
	}

	@Override
	public int hashCode() {
	    int code = super.hashCode();
	    code = code * 31 + mStartOffset;
	    code = code * 31 + mEndOffset;
	    code = code * 31 + mFlags;
	    code = code * 31 + mPositionIncrement;
	    code = code * 31 + (mKeyword?1:0);
	    if (mType != null)
	    	code = code * 31 + mType.hashCode();
	    if (mPayload != null)
	    	code = code * 31 + mPayload.hashCode();
	    return code;
	}
	
	@Override
	public String toString() { 
		StringBuilder sbuf = new StringBuilder();
		sbuf.append(getClass().getSimpleName());
		sbuf.append("{");
		toString(sbuf);
		sbuf.append("}");
		return sbuf.toString();
	}
	
	protected void toString(StringBuilder sbuf) { 
		sbuf.append("startOffset=").append(mStartOffset);
		sbuf.append(",endOffset=").append(mEndOffset);
		sbuf.append(",flags=").append(mFlags);
		sbuf.append(",positionIncrement=").append(mPositionIncrement);
		sbuf.append(",type=").append(mType);
		sbuf.append(",keyword=").append(mKeyword);
	}
	
}
