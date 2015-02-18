package org.javenstudio.panda.analysis.standard;

import java.io.Reader;
import java.io.IOException;

import org.javenstudio.common.indexdb.util.CharTerm;

/** 
 * Internal interface for supporting versioned grammars.
 * 
 */
public interface StandardTokenizerInterface {

	/** This character denotes the end of file */
	public static final int YYEOF = -1;

	/**
	 * Copies the matched text into the CharTermAttribute
	 */
	public void getText(CharTerm term);

	/**
	 * Returns the current position.
	 */
	public int yychar();

	/**
	 * Resets the scanner to read from a new input stream.
	 * Does not close the old reader.
	 *
	 * All internal variables are reset, the old input stream 
	 * <b>cannot</b> be reused (internal buffer is discarded and lost).
	 * Lexical state is set to <tt>ZZ_INITIAL</tt>.
	 *
	 * @param reader   the new input stream 
	 */
	public void yyreset(Reader reader);

	/**
	 * Returns the length of the matched text region.
	 */
	public int yylength();

	/**
	 * Resumes scanning until the next regular expression is matched,
	 * the end of input is encountered or an I/O-Error occurs.
	 *
	 * @return      the next token, {@link #YYEOF} on end of stream
	 * @exception   IOException  if any I/O-Error occurs
	 */
	public int getNextToken() throws IOException;

}
