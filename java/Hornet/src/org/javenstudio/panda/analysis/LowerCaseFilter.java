package org.javenstudio.panda.analysis;

import java.io.IOException;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.common.indexdb.analysis.CharToken;
import org.javenstudio.common.indexdb.analysis.CharacterUtils;
import org.javenstudio.common.indexdb.analysis.TokenFilter;

/**
 * Normalizes token text to lower case.
 * <a name="version"/>
 * <p>You must specify the required {@link Version}
 * compatibility when creating LowerCaseFilter:
 * <ul>
 *   <li> As of 3.1, supplementary characters are properly lowercased.
 * </ul>
 */
public final class LowerCaseFilter extends TokenFilter {
  
	/**
	 * Create a new LowerCaseFilter, that normalizes token text to lower case.
	 * 
	 * @param matchVersion See <a href="#version">above</a>
	 * @param in TokenStream to filter
	 */
	public LowerCaseFilter(ITokenStream in) {
		super(in);
	}
  
	@Override
	public final IToken nextToken() throws IOException {
		CharToken token = (CharToken)super.nextToken();
		if (token != null) {
			final char[] buffer = token.getTerm().buffer();
			final int length = token.getTerm().length();
			
			for (int i = 0; i < length;) {
				i += Character.toChars(Character.toLowerCase(
						CharacterUtils.getInstance().codePointAt(buffer, i)), buffer, i);
			}
			
			return token;
		}
		
		return null;
	}
	
}
