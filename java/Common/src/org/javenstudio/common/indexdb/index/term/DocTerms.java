package org.javenstudio.common.indexdb.index.term;

import org.javenstudio.common.indexdb.IDocTerms;
import org.javenstudio.common.indexdb.util.BytesRef;

/** Returned by {@link #getTerms} */
public abstract class DocTerms implements IDocTerms {
	
	/** 
	 * The BytesRef argument must not be null; the method
	 *  returns the same BytesRef, or an empty (length=0)
	 *  BytesRef if the doc did not have this field or was
	 *  deleted. 
	 */
	public abstract BytesRef getTerm(int docID, BytesRef ret);

	/** 
	 * Returns true if this doc has this field and is not
	 *  deleted. 
	 */
	public abstract boolean exists(int docID);

	/** Number of documents */
	public abstract int size();
	
}
