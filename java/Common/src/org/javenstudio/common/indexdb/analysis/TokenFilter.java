package org.javenstudio.common.indexdb.analysis;

import java.io.IOException;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.common.indexdb.analysis.TokenStream;

/** 
 * A TokenFilter is a TokenStream whose input is another TokenStream.
 * <p>
 * This is an abstract class; subclasses must override {@link #incrementToken()}.
 * @see TokenStream
 */
public abstract class TokenFilter extends TokenStream {
	
	/** The source of tokens for this filter. */
	protected final ITokenStream mInput;

	/** Construct a token stream filtering the given input. */
	protected TokenFilter(ITokenStream input) {
		mInput = input;
		
		if (input == null)
			throw new NullPointerException("input is null");
	}
  
	@Override
	public IToken nextToken() throws IOException { 
		return mInput.nextToken();
	}
	
	/** 
	 * {@inheritDoc}
	 * <p> 
	 * <b>NOTE:</b> 
	 * The default implementation chains the call to the input TokenStream, so
	 * be sure to call <code>super.end()</code> first when overriding this method.
	 */
	@Override
	public int end() throws IOException {
		return mInput.end();
	}
  
	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>NOTE:</b> 
	 * The default implementation chains the call to the input TokenStream, so
	 * be sure to call <code>super.close()</code> when overriding this method.
	 */
	@Override
	public void close() throws IOException {
		mInput.close();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>NOTE:</b> 
	 * The default implementation chains the call to the input TokenStream, so
	 * be sure to call <code>super.reset()</code> when overriding this method.
	 */
	@Override
	public void reset() throws IOException {
		mInput.reset();
	}
	
}
