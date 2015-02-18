package org.javenstudio.panda.language.english;

import java.io.IOException;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.common.indexdb.analysis.CharToken;
import org.javenstudio.common.indexdb.analysis.TokenFilter;

/**
 * A {@link TokenFilter} that applies {@link EnglishMinimalStemmer} to stem 
 * English words.
 * <p>
 * To prevent terms from being stemmed use an instance of
 * {@link KeywordMarkerFilter} or a custom {@link TokenFilter} that sets
 * the {@link KeywordAttribute} before this {@link TokenStream}.
 * </p>
 */
public final class EnglishMinimalStemFilter extends TokenFilter {
	
	private final EnglishMinimalStemmer mStemmer = new EnglishMinimalStemmer();

	public EnglishMinimalStemFilter(ITokenStream input) {
		super(input);
	}
  
	@Override
	public IToken nextToken() throws IOException {
		CharToken token = (CharToken)super.nextToken();
		if (token != null) {
			if (!token.isKeyword()) {
				final int newlen = mStemmer.stem(token.getTerm().buffer(), token.getTerm().length());
				token.getTerm().setLength(newlen);
			}
		}
		
		return token;
	}
	
}
