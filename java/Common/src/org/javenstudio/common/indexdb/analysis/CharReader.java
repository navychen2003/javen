package org.javenstudio.common.indexdb.analysis;

import java.io.Reader;

/**
 * CharReader adds {@link #correctOffset}
 * functionality over {@link Reader}.  All Tokenizers accept a
 * CharStream instead of {@link Reader} as input, which enables
 * arbitrary character based filtering before tokenization. 
 * The {@link #correctOffset} method fixed offsets to account for
 * removal or insertion of characters, so that the offsets
 * reported in the tokens match the character offsets of the
 * original Reader.
 */
public abstract class CharReader extends Reader {

	/**
	 * Called by CharFilter(s) and Tokenizer to correct token offset.
	 *
	 * @param currentOff offset as seen in the output
	 * @return corrected offset based on the input
	 */
	public abstract int correctOffset(int currentOff);
	
}