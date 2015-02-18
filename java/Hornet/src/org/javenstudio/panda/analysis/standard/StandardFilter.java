package org.javenstudio.panda.analysis.standard;

import java.io.IOException;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.common.indexdb.analysis.TokenFilter;

/**
 * Normalizes tokens extracted with {@link StandardTokenizer}.
 */
public class StandardFilter extends TokenFilter {
  
	//private static final String APOSTROPHE_TYPE = 
	//		ClassicTokenizer.TOKEN_TYPES[ClassicTokenizer.APOSTROPHE];
	//private static final String ACRONYM_TYPE = 
	//		ClassicTokenizer.TOKEN_TYPES[ClassicTokenizer.ACRONYM];
	  
	public StandardFilter(ITokenStream in) {
		super(in);
	}
  
	@Override
	public final IToken nextToken() throws IOException {
		return super.nextToken(); // TODO: add some niceties for the new grammar
	}
  
}
