package org.javenstudio.common.indexdb.analysis;

import java.io.IOException;
import java.io.Reader;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.Version;
import org.javenstudio.common.indexdb.util.CharTerm;

/**
 * An abstract base class for simple, character-oriented tokenizers. 
 * <p>
 * <a name="version">You must specify the required {@link Version} compatibility
 * when creating {@link CharTokenizer}:
 * <ul>
 * <li>As of 3.1, {@link CharTokenizer} uses an int based API to normalize and
 * detect token codepoints. See {@link #isTokenChar(int)} and
 * {@link #normalize(int)} for details.</li>
 * </ul>
 * <p>
 * A new {@link CharTokenizer} API has been introduced with Indexdb 3.1. This API
 * moved from UTF-16 code units to UTF-32 codepoints to eventually add support
 * for <a href=
 * "http://java.sun.com/j2se/1.5.0/docs/api/java/lang/Character.html#supplementary"
 * >supplementary characters</a>. The old <i>char</i> based API has been
 * deprecated and should be replaced with the <i>int</i> based methods
 * {@link #isTokenChar(int)} and {@link #normalize(int)}.
 * </p>
 * <p>
 * As of Indexdb 3.1 each {@link CharTokenizer} - constructor expects a
 * {@link Version} argument. Based on the given {@link Version} either the new
 * API or a backwards compatibility layer is used at runtime. For
 * {@link Version} < 3.1 the backwards compatibility layer ensures correct
 * behavior even for indexes build with previous versions of Indexdb. If a
 * {@link Version} >= 3.1 is used {@link CharTokenizer} requires the new API to
 * be implemented by the instantiated class. Yet, the old <i>char</i> based API
 * is not required anymore even if backwards compatibility must be preserved.
 * {@link CharTokenizer} subclasses implementing the new API are fully backwards
 * compatible if instantiated with {@link Version} < 3.1.
 * </p>
 * <p>
 * <strong>Note:</strong> If you use a subclass of {@link CharTokenizer} with {@link Version} >=
 * 3.1 on an index build with a version < 3.1, created tokens might not be
 * compatible with the terms in your index.
 * </p>
 */
public abstract class CharTokenizer extends Tokenizer {
	private static final int MAX_WORD_LEN = 255;
	private static final int IO_BUFFER_SIZE = 4096;
	
	private final CharTerm mTerm = new CharTerm();
	private final CharToken mToken = new CharToken(mTerm);
	private final CharacterUtils.CharacterBuffer mIoBuffer = 
			CharacterUtils.newCharacterBuffer(IO_BUFFER_SIZE);

	private int mOffset = 0; 
	private int mBufferIndex = 0; 
	private int mDataLen = 0; 
	private int mFinalOffset = 0;
	
	/**
	 * Creates a new {@link CharTokenizer} instance
	 * 
	 * @param matchVersion
	 *          Indexdb version to match See {@link <a href="#version">above</a>}
	 * @param input
	 *          the input to split up into tokens
	 */
	public CharTokenizer(Reader input) {
		super(input);
	}
  
	/**
	 * Returns true if a codepoint should be included in a token. This tokenizer
	 * generates as tokens adjacent sequences of codepoints which satisfy this
	 * predicate. Codepoints for which this is false are used to define token
	 * boundaries and are not included in tokens.
	 */
	protected abstract boolean isTokenChar(int c);

	/**
	 * Returns true if a codepoint should be a char to separate token. 
	 * Note that is must be included in a token.
	 */
	protected boolean isTokenSeperator(int c) { 
		return false;
	}
	
	/**
	 * Called on each token character to normalize it before it is added to the
	 * token. The default implementation does nothing. Subclasses may use this to,
	 * e.g., lowercase tokens.
	 */
	protected int normalize(int c) {
		return c;
	}

	@Override
	public final IToken nextToken() throws IOException {
		mToken.clear();
    
		int length = 0;
		int start = -1; // this variable is always initialized
		int end = -1;
		char[] buffer = mTerm.buffer();
		while (true) {
			if (mBufferIndex >= mDataLen) {
				mOffset += mDataLen;
				// read supplementary char aware with CharacterUtils
				if(!CharacterUtils.getInstance().fill(mIoBuffer, getInput())) { 
					mDataLen = 0; // so next offset += dataLen won't decrement offset
					if (length > 0) 
						break;
				  
					mFinalOffset = correctOffset(mOffset);
					return null;
				}
				mDataLen = mIoBuffer.getLength();
				mBufferIndex = 0;
			}
			
			// use CharacterUtils here to support < 3.1 UTF-16 code unit behavior if the char based methods are gone
			final int c = CharacterUtils.getInstance().codePointAt(mIoBuffer.getBuffer(), mBufferIndex);
			final int charCount = Character.charCount(c);
			mBufferIndex += charCount;

			if (isTokenChar(c)) {      		// if it's a token char
				boolean isSeperator = isTokenSeperator(c);
				if (isSeperator && length > 0) {	// seperate the token
					mBufferIndex -= charCount; 	// go back
					break;
				}
				
				if (length == 0) {    		// start of token
					assert start == -1;
					start = mOffset + mBufferIndex - charCount;
					end = start;
				} else if (length >= buffer.length-1) { // check if a supplementary could run out of bounds
					buffer = mTerm.resizeBuffer(2+length); // make sure a supplementary fits in the buffer
				}
				end += charCount;
				length += Character.toChars(normalize(c), buffer, length); // buffer it, normalized
				if (isSeperator || length >= MAX_WORD_LEN)	// buffer overflow! make sure to check for >= surrogate pair could break == test
					break;
				
			} else if (length > 0)    		// at non-Letter w/ chars
				break;                    	// return 'em
		}

		mTerm.setLength(length);
		assert start != -1;
		mToken.setOffset(correctOffset(start), mFinalOffset = correctOffset(end));
	  
		return mToken;
	}
  
	@Override
	public final int end() {
		// set final offset
		//mToken.setOffset(mFinalOffset, mFinalOffset);
		return mFinalOffset;
	}

	@Override
	public void reset(Reader input) throws IOException {
		super.reset(input);
		mBufferIndex = 0;
		mOffset = 0;
		mDataLen = 0;
		mFinalOffset = 0;
		mIoBuffer.reset(); // make sure to reset the IO buffer!!
	}
	
}