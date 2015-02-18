package org.javenstudio.common.indexdb.index.term;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDocsEnum;

/** 
 * Iterates through the documents and term freqs.
 *  NOTE: you must first call {@link #nextDoc} before using
 *  any of the per-doc methods. 
 */
public abstract class DocsEnum implements IDocsEnum {

	/**
	 * Returns term frequency in the current document.  Do
	 *  not call this before {@link #nextDoc} is first called,
	 *  nor after {@link #nextDoc} returns NO_MORE_DOCS. 
	 */
	public abstract int getFreq() throws IOException;
  
}
