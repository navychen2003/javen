package org.javenstudio.common.indexdb;

import java.io.IOException;

public interface IFields {

	/** 
	 * Returns an iterator that will step through all fields
	 *  names.  This will not return null.
	 */
	public IFieldsEnum iterator();

	/** 
	 * Get the {@link Terms} for this field.  This will return
	 *  null if the field does not exist. 
	 */
	public ITerms getTerms(String field) throws IOException;

	/** 
	 * Returns the number of terms for all fields, or -1 if this 
	 *  measure isn't stored by the codec. Note that, just like 
	 *  other term measures, this measure does not take deleted 
	 *  documents into account. 
	 */
	public int size() throws IOException;
	
	public long getUniqueTermCount() throws IOException;
	
}
