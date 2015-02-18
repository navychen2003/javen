package org.javenstudio.common.indexdb;

import org.javenstudio.common.indexdb.util.BytesRef;

public interface IDocTerms {

	/** 
	 * The BytesRef argument must not be null; the method
	 *  returns the same BytesRef, or an empty (length=0)
	 *  BytesRef if the doc did not have this field or was
	 *  deleted. 
	 */
	public BytesRef getTerm(int docID, BytesRef ret);

	/** 
	 * Returns true if this doc has this field and is not
	 *  deleted. 
	 */
	public boolean exists(int docID);

	/** Number of documents */
	public int size();
	
}
