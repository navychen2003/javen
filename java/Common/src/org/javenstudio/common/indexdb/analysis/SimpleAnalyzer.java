package org.javenstudio.common.indexdb.analysis;

import java.io.Reader;

import org.javenstudio.common.indexdb.Version;

/** 
 * An {@link Analyzer} that filters {@link LetterTokenizer} 
 *  with {@link LowerCaseFilter} 
 * <p>
 * <a name="version">You must specify the required {@link Version} compatibility
 * when creating {@link CharTokenizer}:
 * <ul>
 * <li>As of 3.1, {@link LowerCaseTokenizer} uses an int based API to normalize and
 * detect token codepoints. See {@link CharTokenizer#isTokenChar(int)} and
 * {@link CharTokenizer#normalize(int)} for details.</li>
 * </ul>
 * <p>
 */
public final class SimpleAnalyzer extends Analyzer {
  
	/**
	 * Creates a new {@link SimpleAnalyzer}
	 * @param matchVersion Indexdb version to match See {@link <a href="#version">above</a>}
	 */
	public SimpleAnalyzer() {
	}
  
	@Override
	public TokenComponents createComponents(final String fieldName, final Reader reader) {
		return new TokenComponents(new LowerCaseTokenizer(reader));
	}
	
}