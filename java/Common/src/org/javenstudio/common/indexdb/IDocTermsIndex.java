package org.javenstudio.common.indexdb;

import org.javenstudio.common.indexdb.util.BytesRef;

public interface IDocTermsIndex {

	/** 
	 * Returns total unique ord count; this includes +1 for
	 *  the null ord (always 0). 
	 */
	public int getNumOrd();
	
	/** 
	 * Returns sort ord for this document.  Ord 0 is
	 *  reserved for docs that are deleted or did not have
	 *  this field.
	 */
	public int getOrd(int docID);
	
	/** 
	 * Convenience method, to lookup the Term for a doc.
	 *  If this doc is deleted or did not have this field,
	 *  this will return an empty (length=0) BytesRef. 
	 */
	public BytesRef getTerm(int docID, BytesRef reuse);
	
	/** 
	 * The BytesRef argument must not be null; the method
	 *  returns the same BytesRef, or an empty (length=0)
	 *  BytesRef if this ord is the null ord (0). 
	 */
	public BytesRef lookup(int ord, BytesRef reuse);
	
	public int binarySearch(BytesRef key, BytesRef spare);
	
	/** Number of documents */
	public int size();

	/** Returns a TermsEnum that can iterate over the values in this index entry */
	public ITermsEnum getTermsEnum();
	
	public IIntsReader getDocToOrd();
	
}
