package org.javenstudio.common.indexdb;

import org.javenstudio.common.indexdb.util.BytesRef;

public interface ITerm extends Comparable<ITerm> {

  	/** 
  	 * Returns the field of this term.   The field indicates
     * the part of a document which this term came from. 
     */
  	public String getField();
  	
  	/** 
  	 * Returns the text of this term.  In the case of words, this is simply the
     * text of the word.  In the case of dates and other types, this is an
     * encoding of the object as a string.
     */
  	public String getText();
  	
  	/** Returns the bytes of this term. */
  	public BytesRef getBytes();
  	
}
