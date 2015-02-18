package org.javenstudio.panda.analysis;

import java.io.IOException;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.common.indexdb.analysis.CharToken;
import org.javenstudio.common.indexdb.analysis.TokenFilter;
import org.javenstudio.panda.util.CharArraySet;

/**
 * Marks terms as keywords via the {@link KeywordAttribute}. Each token
 * contained in the provided is marked as a keyword by setting
 * {@link KeywordAttribute#setKeyword(boolean)} to <code>true</code>.
 * 
 * @see KeywordAttribute
 */
public final class KeywordMarkerFilter extends TokenFilter {

	private final CharArraySet mKeywordSet;

	/**
	 * Create a new KeywordMarkerFilter, that marks the current token as a
	 * keyword if the tokens term buffer is contained in the given set via the
	 * {@link KeywordAttribute}.
	 * 
	 * @param in
	 *          TokenStream to filter
	 * @param keywordSet
	 *          the keywords set to lookup the current termbuffer
	 */
	public KeywordMarkerFilter(ITokenStream in, CharArraySet keywordSet) {
		super(in);
		mKeywordSet = keywordSet;
	}

	@Override
	public final IToken nextToken() throws IOException {
		CharToken token = (CharToken)super.nextToken();
		if (token != null) {
			if (mKeywordSet.contains(token.getTerm().buffer(), 0, token.getTerm().length())) 
				token.setKeyword(true);
			
			return token;
		}
		
		return null;
	}
	
}
