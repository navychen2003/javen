package org.javenstudio.panda.analysis;

import java.io.Reader;

import org.javenstudio.common.indexdb.analysis.CharTokenizer;

/**
 * A WhitespaceTokenizer is a tokenizer that divides text at whitespace.
 * Adjacent sequences of non-Whitespace characters form tokens. <a
 * name="version"/>
 * <p>
 * You must specify the required {@link Version} compatibility when creating
 * {@link WhitespaceTokenizer}:
 * <ul>
 * <li>As of 3.1, {@link CharTokenizer} uses an int based API to normalize and
 * detect token characters. See {@link CharTokenizer#isTokenChar(int)} and
 * {@link CharTokenizer#normalize(int)} for details.</li>
 * </ul>
 */
public final class WhitespaceTokenizer extends CharTokenizer {
  
	/**
	 * Construct a new WhitespaceTokenizer. * @param matchVersion Lucene version
	 * to match See {@link <a href="#version">above</a>}
	 * 
	 * @param in
	 *          the input to split up into tokens
	 */
	public WhitespaceTokenizer(Reader in) {
		super(in);
	}

	/** 
	 * Collects only characters which do not satisfy
	 * {@link Character#isWhitespace(int)}. 
	 */
	@Override
	protected boolean isTokenChar(int c) {
		return !Character.isWhitespace(c);
	}
	
}
