package org.javenstudio.common.indexdb;

import java.io.IOException;

public interface IDocsEnum extends IDocIdSetIterator {

	/** 
	 * Flag to pass to {@link TermsEnum#docs(Bits,DocsEnum,int)}
	 *  if you require term frequencies in the returned enum. 
	 */
	public static final int FLAG_FREQS = 0x1;
	
	/**
	 * Returns term frequency in the current document.  Do
	 *  not call this before {@link #nextDoc} is first called,
	 *  nor after {@link #nextDoc} returns NO_MORE_DOCS. 
	 */
	public int getFreq() throws IOException;
	
}
