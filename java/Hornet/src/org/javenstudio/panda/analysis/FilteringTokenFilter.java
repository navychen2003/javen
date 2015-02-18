package org.javenstudio.panda.analysis;

import java.io.IOException;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.common.indexdb.analysis.Token;
import org.javenstudio.common.indexdb.analysis.TokenFilter;

/**
 * Abstract base class for TokenFilters that may remove tokens.
 * You have to implement {@link #accept} and return a boolean if the current
 * token should be preserved. {@link #incrementToken} uses this method
 * to decide if a token should be passed to the caller.
 */
public abstract class FilteringTokenFilter extends TokenFilter {

	private boolean mEnablePositionIncrements; // no init needed, as ctor enforces setting value!
	private boolean mFirst = true; // only used when not preserving gaps

	public FilteringTokenFilter(boolean enablePositionIncrements, ITokenStream input){
		super(input);
		mEnablePositionIncrements = enablePositionIncrements;
	}

	/** 
	 * Override this method and return if the current input token should 
	 * be returned by {@link #incrementToken}. 
	 */
	protected abstract boolean accept(IToken token) throws IOException;

	@Override
	public final IToken nextToken() throws IOException {
		if (mEnablePositionIncrements) {
			int skippedPositions = 0;
			Token token = null;
			while ((token = (Token)super.nextToken()) != null) {
				if (accept(token)) {
					if (skippedPositions != 0) 
						token.setPositionIncrement(token.getPositionIncrement() + skippedPositions);
					
					return token;
				}
				skippedPositions += token.getPositionIncrement();
			}
			
		} else {
			Token token = null;
			while ((token = (Token)super.nextToken()) != null) {
				if (accept(token)) {
					if (mFirst) {
						// first token having posinc=0 is illegal.
						if (token.getPositionIncrement() == 0) 
							token.setPositionIncrement(1);
						
						mFirst = false;
					}
					
					return token;
				}
			}
		}
		
		// reached EOS -- return false
		return null;
	}

	@Override
	public void reset() throws IOException {
		super.reset();
		mFirst = true;
	}

	/**
	 * @see #setEnablePositionIncrements(boolean)
	 */
	public boolean getEnablePositionIncrements() {
		return mEnablePositionIncrements;
	}

	/**
	 * If <code>true</code>, this TokenFilter will preserve
	 * positions of the incoming tokens (ie, accumulate and
	 * set position increments of the removed tokens).
	 * Generally, <code>true</code> is best as it does not
	 * lose information (positions of the original tokens)
	 * during indexing.
	 * 
	 * <p> When set, when a token is stopped
	 * (omitted), the position increment of the following
	 * token is incremented.
	 *
	 * <p> <b>NOTE</b>: be sure to also
	 * set org.apache.lucene.queryparser.classic.QueryParser#setEnablePositionIncrements if
	 * you use QueryParser to create queries.
	 */
	public void setEnablePositionIncrements(boolean enable) {
		mEnablePositionIncrements = enable;
	}
	
}
