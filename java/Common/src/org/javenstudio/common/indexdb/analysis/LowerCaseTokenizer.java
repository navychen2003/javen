package org.javenstudio.common.indexdb.analysis;

import java.io.Reader;

import org.javenstudio.common.indexdb.Version;

/**
 * LowerCaseTokenizer performs the function of LetterTokenizer
 * and LowerCaseFilter together.  It divides text at non-letters and converts
 * them to lower case.  While it is functionally equivalent to the combination
 * of LetterTokenizer and LowerCaseFilter, there is a performance advantage
 * to doing the two tasks at once, hence this (redundant) implementation.
 * <P>
 * Note: this does a decent job for most European languages, but does a terrible
 * job for some Asian languages, where words are not separated by spaces.
 * </p>
 * <p>
 * <a name="version"/>
 * You must specify the required {@link Version} compatibility when creating
 * {@link LowerCaseTokenizer}:
 * <ul>
 * <li>As of 3.1, {@link CharTokenizer} uses an int based API to normalize and
 * detect token characters. See {@link CharTokenizer#isTokenChar(int)} and
 * {@link CharTokenizer#normalize(int)} for details.</li>
 * </ul>
 * </p>
 */
public final class LowerCaseTokenizer extends LetterTokenizer {

	/**
	 * Construct a new LowerCaseTokenizer.
	 * 
	 * @param matchVersion
	 *          Indexdb version to match See {@link <a href="#version">above</a>}
	 * 
	 * @param in
	 *          the input to split up into tokens
	 */
	public LowerCaseTokenizer(Reader in) {
		super(in);
	}

	/** 
	 * Converts char to lower case
	 * {@link Character#toLowerCase(int)}.
	 */
	@Override
	protected int normalize(int c) {
		return Character.toLowerCase(c);
	}
	
}
