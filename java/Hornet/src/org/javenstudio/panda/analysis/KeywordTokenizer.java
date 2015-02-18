package org.javenstudio.panda.analysis;

import java.io.IOException;
import java.io.Reader;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.analysis.CharToken;
import org.javenstudio.common.indexdb.analysis.Tokenizer;
import org.javenstudio.common.indexdb.util.CharTerm;

/**
 * Emits the entire input as a single token.
 */
public final class KeywordTokenizer extends Tokenizer {
	
	/** Default read buffer size */ 
	public static final int DEFAULT_BUFFER_SIZE = 256;

	private final CharTerm mTerm = new CharTerm();
	private final CharToken mToken = new CharToken(mTerm);
	
	private boolean mDone = false;
	private int mFinalOffset;
  
	public KeywordTokenizer(Reader input) {
		this(input, DEFAULT_BUFFER_SIZE);
	}

	public KeywordTokenizer(Reader input, int bufferSize) {
		super(input);
		if (bufferSize <= 0) 
			throw new IllegalArgumentException("bufferSize must be > 0");
		
		mTerm.resizeBuffer(bufferSize);
	}

	@Override
	public final IToken nextToken() throws IOException {
		if (!mDone) {
			mToken.clear();
			mDone = true;
			
			int upto = 0;
			char[] buffer = mTerm.buffer();
			while (true) {
				final int length = getInput().read(buffer, upto, buffer.length-upto);
				if (length == -1) break;
				upto += length;
				if (upto == buffer.length)
					buffer = mTerm.resizeBuffer(1+buffer.length);
			}
			
			mTerm.setLength(upto);
			mFinalOffset = correctOffset(upto);
			mToken.setOffset(correctOffset(0), mFinalOffset);
			
			return mToken;
		}
		
		return null;
	}
  
	@Override
	public final int end() {
		// set final offset 
		return mFinalOffset;
	}

	@Override
	public void reset() throws IOException {
		mDone = false;
	}
	
}
