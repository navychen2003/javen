package org.javenstudio.common.indexdb.analysis;

import java.io.IOException;

import org.javenstudio.common.indexdb.IToken;

/**
 * For Not-tokenized string field
 */
public class SingleTokenStream extends TokenStream {

	private final CharToken mToken = new CharToken();
	private final String mValue;
	private boolean mUsed;

	public SingleTokenStream(String value) { 
		mValue = value;
		
		if (value == null) 
			throw new IllegalArgumentException("String value must not be null");
	}
	
	@Override
	public IToken nextToken() throws IOException {
		mToken.clear();
		if (mUsed) return null;
		mToken.getTerm().append(mValue);
		mToken.setOffset(0, mValue.length());
		mUsed = true;
		return mToken;
	}

	@Override
	public void reset() throws IOException {
		mUsed = false;
	}
	
}
