package org.javenstudio.common.indexdb.analysis;

import java.io.Reader;

import org.javenstudio.common.indexdb.Version;

/**
 * A LetterTokenizer is a tokenizer that divides text at non-letters. That's to
 * say, it defines tokens as maximal strings of adjacent letters, as defined by
 * java.lang.Character.isLetter() predicate.
 * <p>
 * Note: this does a decent job for most European languages, but does a terrible
 * job for some Asian languages, where words are not separated by spaces.
 * </p>
 * <p>
 * <a name="version"/>
 * You must specify the required {@link Version} compatibility when creating
 * {@link LetterTokenizer}:
 * <ul>
 * <li>As of 3.1, {@link CharTokenizer} uses an int based API to normalize and
 * detect token characters. See {@link CharTokenizer#isTokenChar(int)} and
 * {@link CharTokenizer#normalize(int)} for details.</li>
 * </ul>
 * </p>
 */
public class LetterTokenizer extends CharTokenizer {
  
	/**
	 * Construct a new LetterTokenizer.
	 * 
	 * @param matchVersion
	 *          Indexdb version to match See {@link <a href="#version">above</a>}
	 * @param in
	 *          the input to split up into tokens
	 */
	public LetterTokenizer(Reader in) {
		super(in);
	}

	/** 
	 * Collects only characters which satisfy
	 * {@link Character#isLetter(int)}.
	 */
	@Override
	protected boolean isTokenChar(int c) {
		//return Character.isLetter(c);
		
		if ((c >=0 && c <= 31) || c == 127 || c == ' ' || c == '\t' || c == '\r' || c == '\n')
			return false;
		
		return true;
	}
	
	@Override
	protected boolean isTokenSeperator(int c) { 
		if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9'))
			return false;
		
		return true;
	}
	
}
