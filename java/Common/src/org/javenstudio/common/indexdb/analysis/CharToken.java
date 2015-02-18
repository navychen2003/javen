package org.javenstudio.common.indexdb.analysis;

import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.CharTerm;
import org.javenstudio.common.indexdb.util.UnicodeUtil;

/**
 * Character Token implements
 */
public final class CharToken extends Token {

	private CharTerm mTerm;
	private BytesRef mBytes = new BytesRef(CharTerm.MIN_BUFFER_SIZE);
	
	/** Constructs a Token will null text. */
	public CharToken() {
		this(0, 0, 0, null);
	}

	/** Constructs a Token with reference term. */
	public CharToken(CharTerm term) {
		super();
		mTerm = term;
		
		if (term == null) 
			throw new NullPointerException();
	}
	
	/** 
	 * Constructs a Token with null text and start & end
	 *  offsets.
	 *  @param start start offset in the source text
	 *  @param end end offset in the source text 
	 */
	public CharToken(int start, int end) {
		this(start, end, 0, null);
	}

	/** 
	 * Constructs a Token with null text and start & end
	 *  offsets plus the Token type.
	 *  @param start start offset in the source text
	 *  @param end end offset in the source text
	 *  @param typ the lexical type of this Token 
	 */
	public CharToken(int start, int end, String type) {
		this(start, end, 0, type);
	}

	/**
	 * Constructs a Token with null text and start & end
	 *  offsets plus flags. NOTE: flags is EXPERIMENTAL.
	 *  @param start start offset in the source text
	 *  @param end end offset in the source text
	 *  @param flags The bits to set for this token
	 */
	public CharToken(int start, int end, int flags) {
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
	public CharToken(int start, int end, int flags, String type) {
		super(start, end, flags, type);
		mTerm = new CharTerm();
	}
	
	/** 
	 * Constructs a Token with the given term text, and start
	 *  & end offsets.  The type defaults to "word."
	 *  <b>NOTE:</b> for better indexing speed you should
	 *  instead use the char[] termBuffer methods to set the
	 *  term text.
	 *  @param text term text
	 *  @param start start offset
	 *  @param end end offset
	 */
	public CharToken(String text, int start, int end) {
		this(start, end);
		mTerm.append(text);
	}

	/** 
	 * Constructs a Token with the given text, start and end
	 *  offsets, & type.  <b>NOTE:</b> for better indexing
	 *  speed you should instead use the char[] termBuffer
	 *  methods to set the term text.
	 *  @param text term text
	 *  @param start start offset
	 *  @param end end offset
	 *  @param typ token type
	 */
	public CharToken(String text, int start, int end, String type) {
		this(start, end, type);
		mTerm.append(text);
	}

	/**
	 *  Constructs a Token with the given text, start and end
	 *  offsets, & type.  <b>NOTE:</b> for better indexing
	 *  speed you should instead use the char[] termBuffer
	 *  methods to set the term text.
	 * @param text
	 * @param start
	 * @param end
	 * @param flags token type bits
	 */
	public CharToken(String text, int start, int end, int flags) {
		this(start, end, flags);
		mTerm.append(text);
	}

	/**
	 *  Constructs a Token with the given term buffer (offset
	 *  & length), start and end
	 *  offsets
	 * @param startTermBuffer
	 * @param termBufferOffset
	 * @param termBufferLength
	 * @param start
	 * @param end
	 */
	public CharToken(char[] startTermBuffer, int termBufferOffset, int termBufferLength, 
			int start, int end) {
		this(start, end);
		mTerm.copyBuffer(startTermBuffer, termBufferOffset, termBufferLength);
	}

	public final CharTerm getTerm() { 
		return mTerm;
	}
	
	@Override
	public int fillBytesRef(BytesRef bytes) {
	    return UnicodeUtil.UTF16toUTF8WithHash(mTerm.buffer(), 0, mTerm.length(), bytes);
	}
	
	@Override
	public int fillBytesRef() {
	    return fillBytesRef(mBytes);
	}

	@Override
	public BytesRef getBytesRef() {
	    return mBytes;
	}
	
	/** 
	 * Resets the term text, payload, flags, and positionIncrement,
	 * startOffset, endOffset and token type to default.
	 */
	@Override
	public void clear() {
	    super.clear();
	    mTerm.clear();
	}

	@Override
	public Object clone() {
		CharToken t = (CharToken)super.clone();
		t.mTerm = new CharTerm();
		t.mTerm.copyBuffer(mTerm.buffer(), 0, mTerm.length());
		t.mBytes = BytesRef.deepCopyOf(mBytes);
	    
	    return t;
	}

	/** 
	 * Makes a clone, but replaces the term buffer &
	 * start/end offset in the process.  This is more
	 * efficient than doing a full clone (and then calling
	 * {@link #copyBuffer}) because it saves a wasted copy of the old
	 * termBuffer. 
	 */
	public CharToken clone(char[] newTermBuffer, int newTermOffset, int newTermLength, 
			int newStartOffset, int newEndOffset) {
	    final CharToken t = new CharToken(
	    		newTermBuffer, newTermOffset, newTermLength, 
	    		newStartOffset, newEndOffset);
	    
	    t.mPositionIncrement = mPositionIncrement;
	    t.mFlags = mFlags;
	    t.mType = mType;
	    
	    if (mPayload != null)
	    	t.mPayload = (BytesRef) mPayload.clone();
	    
	    return t;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;

	    if (obj instanceof CharToken) {
	    	final CharToken other = (CharToken) obj;
	    	return (mTerm.equals(other.mTerm) &&
	    			super.equals(obj)
	    		);
	      
	    }
	    
	    return false;
	}

	@Override
	public int hashCode() {
	    int code = super.hashCode();
	    code = code * 31 + mTerm.hashCode();
	    return code;
	}
	
	// like clear() but doesn't clear termBuffer/text
	public void clearNoTermBuffer() {
	    super.clear();
	}

	/** 
	 * Shorthand for calling {@link #clear},
	 *  {@link #copyBuffer(char[], int, int)},
	 *  {@link #setStartOffset},
	 *  {@link #setEndOffset},
	 *  {@link #setType}
	 *  @return this Token instance 
	 */
	public CharToken reinit(char[] newTermBuffer, int newTermOffset, int newTermLength, 
			int newStartOffset, int newEndOffset, String newType) {
	    clearNoTermBuffer();
	    mTerm.copyBuffer(newTermBuffer, newTermOffset, newTermLength);
	    mPayload = null;
	    mPositionIncrement = 1;
	    mStartOffset = newStartOffset;
	    mEndOffset = newEndOffset;
	    mType = newType;
	    return this;
	}

	/** 
	 * Shorthand for calling {@link #clear},
	 *  {@link #copyBuffer(char[], int, int)},
	 *  {@link #setStartOffset},
	 *  {@link #setEndOffset}
	 *  {@link #setType} on Token.DEFAULT_TYPE
	 *  @return this Token instance 
	 */
	public CharToken reinit(char[] newTermBuffer, int newTermOffset, int newTermLength, 
			int newStartOffset, int newEndOffset) {
	    clearNoTermBuffer();
	    mTerm.copyBuffer(newTermBuffer, newTermOffset, newTermLength);
	    mStartOffset = newStartOffset;
	    mEndOffset = newEndOffset;
	    mType = DEFAULT_TYPE;
	    return this;
	}

	/** 
	 * Shorthand for calling {@link #clear},
	 *  {@link #append(CharSequence)},
	 *  {@link #setStartOffset},
	 *  {@link #setEndOffset}
	 *  {@link #setType}
	 *  @return this Token instance 
	 */
	public CharToken reinit(String newTerm, int newStartOffset, int newEndOffset, String newType) {
	    clear();
	    mTerm.append(newTerm);
	    mStartOffset = newStartOffset;
	    mEndOffset = newEndOffset;
	    mType = newType;
	    return this;
	}

	/** 
	 * Shorthand for calling {@link #clear},
	 *  {@link #append(CharSequence, int, int)},
	 *  {@link #setStartOffset},
	 *  {@link #setEndOffset}
	 *  {@link #setType}
	 *  @return this Token instance 
	 */
	public CharToken reinit(String newTerm, int newTermOffset, int newTermLength, 
			int newStartOffset, int newEndOffset, String newType) {
	    clear();
	    mTerm.append(newTerm, newTermOffset, newTermOffset + newTermLength);
	    mStartOffset = newStartOffset;
	    mEndOffset = newEndOffset;
	    mType = newType;
	    return this;
	}

	/** 
	 * Shorthand for calling {@link #clear},
	 *  {@link #append(CharSequence)},
	 *  {@link #setStartOffset},
	 *  {@link #setEndOffset}
	 *  {@link #setType} on Token.DEFAULT_TYPE
	 *  @return this Token instance 
	 */
	public CharToken reinit(String newTerm, int newStartOffset, int newEndOffset) {
	    clear();
	    mTerm.append(newTerm);
	    mStartOffset = newStartOffset;
	    mEndOffset = newEndOffset;
	    mType = DEFAULT_TYPE;
	    return this;
	}

	/** 
	 * Shorthand for calling {@link #clear},
	 *  {@link #append(CharSequence, int, int)},
	 *  {@link #setStartOffset},
	 *  {@link #setEndOffset}
	 *  {@link #setType} on Token.DEFAULT_TYPE
	 *  @return this Token instance 
	 */
	public CharToken reinit(String newTerm, int newTermOffset, int newTermLength, 
			int newStartOffset, int newEndOffset) {
	    clear();
	    mTerm.append(newTerm, newTermOffset, newTermOffset + newTermLength);
	    mStartOffset = newStartOffset;
	    mEndOffset = newEndOffset;
	    mType = DEFAULT_TYPE;
	    return this;
	}

	/**
	 * Copy the prototype token's fields into this one. Note: Payloads are shared.
	 * @param prototype
	 */
	public void reinit(CharToken prototype) {
		mTerm.copyBuffer(prototype.mTerm.buffer(), 0, prototype.mTerm.length());
	    mPositionIncrement = prototype.mPositionIncrement;
	    mFlags = prototype.mFlags;
	    mStartOffset = prototype.mStartOffset;
	    mEndOffset = prototype.mEndOffset;
	    mType = prototype.mType;
	    mPayload =  prototype.mPayload;
	}

	/**
	 * Copy the prototype token's fields into this one, with a different term. 
	 * Note: Payloads are shared.
	 * @param prototype
	 * @param newTerm
	 */
	public void reinit(CharToken prototype, String newTerm) {
	    mTerm.setEmpty().append(newTerm);
	    mPositionIncrement = prototype.mPositionIncrement;
	    mFlags = prototype.mFlags;
	    mStartOffset = prototype.mStartOffset;
	    mEndOffset = prototype.mEndOffset;
	    mType = prototype.mType;
	    mPayload =  prototype.mPayload;
	}

	/**
	 * Copy the prototype token's fields into this one, with a different term. 
	 * Note: Payloads are shared.
	 * @param prototype
	 * @param newTermBuffer
	 * @param offset
	 * @param length
	 */
	public void reinit(CharToken prototype, char[] newTermBuffer, int offset, int length) {
	    mTerm.copyBuffer(newTermBuffer, offset, length);
	    mPositionIncrement = prototype.mPositionIncrement;
	    mFlags = prototype.mFlags;
	    mStartOffset = prototype.mStartOffset;
	    mEndOffset = prototype.mEndOffset;
	    mType = prototype.mType;
	    mPayload =  prototype.mPayload;
	}

	public void copyTo(CharToken to) {
		to.reinit(this);
		// reinit shares the payload, so clone it:
		to.mPayload = clonePayload();
	}
	
	@Override
	protected void toString(StringBuilder sbuf) { 
		super.toString(sbuf);
		sbuf.append(",value=").append(mTerm.toString());
	}
	
}
