package org.javenstudio.panda.analysis;

import java.io.Reader;

import org.javenstudio.common.indexdb.analysis.Analyzer;
import org.javenstudio.common.indexdb.analysis.TokenComponents;

/**
 * "Tokenizes" the entire stream as a single token. This is useful
 * for data like zip codes, ids, and some product names.
 */
public final class KeywordAnalyzer extends Analyzer {
	
	public KeywordAnalyzer() {}

	@Override
	public TokenComponents createComponents(final String fieldName, final Reader reader) {
		return new TokenComponents(new KeywordTokenizer(reader));
	}
	
}
