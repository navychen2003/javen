package org.javenstudio.panda.language.english;

import java.io.IOException;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.common.indexdb.analysis.CharToken;
import org.javenstudio.common.indexdb.analysis.TokenFilter;

/**
 * TokenFilter that removes possessives (trailing 's) from words.
 * <a name="version"/>
 * <p>You must specify the required {@link Version}
 * compatibility when creating EnglishPossessiveFilter:
 * <ul>
 *    <li> As of 3.6, U+2019 RIGHT SINGLE QUOTATION MARK and 
 *         U+FF07 FULLWIDTH APOSTROPHE are also treated as
 *         quotation marks.
 * </ul>
 */
public final class EnglishPossessiveFilter extends TokenFilter {

	public EnglishPossessiveFilter(ITokenStream input) {
		super(input);
	}

	@Override
	public IToken nextToken() throws IOException {
		CharToken token = (CharToken)super.nextToken();
		
		if (token != null) {
			final char[] buffer = token.getTerm().buffer();
			final int bufferLength = token.getTerm().length();
	    
			if (bufferLength >= 2 && (buffer[bufferLength-2] == '\'' || 
				(buffer[bufferLength-2] == '\u2019' || buffer[bufferLength-2] == '\uFF07')) &&
				(buffer[bufferLength-1] == 's' || buffer[bufferLength-1] == 'S')) {
				token.getTerm().setLength(bufferLength - 2); // Strip last 2 characters off
			}
		}

		return token;
	}
	
}
