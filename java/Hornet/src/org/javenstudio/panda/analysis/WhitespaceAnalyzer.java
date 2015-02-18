package org.javenstudio.panda.analysis;

import java.io.Reader;

import org.javenstudio.common.indexdb.analysis.Analyzer;
import org.javenstudio.common.indexdb.analysis.TokenComponents;

/**
 * An Analyzer that uses {@link WhitespaceTokenizer}.
 * <p>
 * <a name="version">You must specify the required {@link Version} compatibility
 * when creating {@link CharTokenizer}:
 * <ul>
 * <li>As of 3.1, {@link WhitespaceTokenizer} uses an int based API to normalize and
 * detect token codepoints. See {@link CharTokenizer#isTokenChar(int)} and
 * {@link CharTokenizer#normalize(int)} for details.</li>
 * </ul>
 * <p>
 **/
public final class WhitespaceAnalyzer extends Analyzer {
  
	/**
	 * Creates a new {@link WhitespaceAnalyzer}
	 * @param matchVersion Lucene version to match See {@link <a href="#version">above</a>}
	 */
	public WhitespaceAnalyzer() {
	}
  
	@Override
	public TokenComponents createComponents(final String fieldName, final Reader reader) {
		return new TokenComponents(new WhitespaceTokenizer(reader));
	}
	
}
