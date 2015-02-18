package org.javenstudio.panda.analysis;

import java.io.IOException;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.common.indexdb.analysis.CharToken;
import org.javenstudio.common.indexdb.analysis.TokenFilter;
import org.javenstudio.panda.util.CharArraySet;

/**
 * A TokenFilter which filters out Tokens at the same position and Term text 
 * as the previous token in the stream.
 */
public final class RemoveDuplicatesTokenFilter extends TokenFilter {

	// use a fixed version, as we don't care about case sensitivity.
	private final CharArraySet mPrevious = new CharArraySet(8, false);

	/**
	 * Creates a new RemoveDuplicatesTokenFilter
	 *
	 * @param in TokenStream that will be filtered
	 */
	public RemoveDuplicatesTokenFilter(ITokenStream in) {
		super(in);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IToken nextToken() throws IOException {
		CharToken token = null;
		while ((token = (CharToken)super.nextToken()) != null) {
			final char term[] = token.getTerm().buffer();
			final int length = token.getTerm().length();
			final int posIncrement = token.getPositionIncrement();
      
			if (posIncrement > 0) 
				mPrevious.clear();
      
			boolean duplicate = (posIncrement == 0 && mPrevious.contains(term, 0, length));
      
			// clone the term, and add to the set of seen terms.
			char saved[] = new char[length];
			System.arraycopy(term, 0, saved, 0, length);
			mPrevious.add(saved);
      
			if (!duplicate) 
				return token;
		}
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void reset() throws IOException {
		super.reset();
		mPrevious.clear();
	}
	
} 
