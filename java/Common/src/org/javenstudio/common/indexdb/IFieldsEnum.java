package org.javenstudio.common.indexdb;

import java.io.IOException;

public interface IFieldsEnum {

	/** 
	 * Increments the enumeration to the next field. Returns
	 * null when there are no more fields.
	 */
	public String next();

	/** 
	 * Get {@link Terms} for the current field.  After {@link #next} returns
	 *  null this method should not be called. This method may
	 *  return null in some cases, which means the provided
	 *  field does not have any terms. 
	 */
	public ITerms getTerms() throws IOException;
	
}
