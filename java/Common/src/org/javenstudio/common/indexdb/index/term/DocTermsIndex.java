package org.javenstudio.common.indexdb.index.term;

import org.javenstudio.common.indexdb.IDocTermsIndex;
import org.javenstudio.common.indexdb.IIntsReader;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.util.BytesRef;

/** Returned by {@link #getTermsIndex} */
public abstract class DocTermsIndex implements IDocTermsIndex {
	
	@Override
	public int binarySearch(BytesRef key, BytesRef spare) {
		// this special case is the reason that Arrays.binarySearch() isn't useful.
		if (key == null)
			return 0;
	  
		int low = 1;
		int high = getNumOrd()-1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			int cmp = lookup(mid, spare).compareTo(key);

			if (cmp < 0)
				low = mid + 1;
			else if (cmp > 0)
				high = mid - 1;
			else
				return mid; // key found
		}
		
		return -(low + 1);  // key not found.
	}

	/** 
	 * The BytesRef argument must not be null; the method
	 *  returns the same BytesRef, or an empty (length=0)
	 *  BytesRef if this ord is the null ord (0). 
	 */
	public abstract BytesRef lookup(int ord, BytesRef reuse);

	/** 
	 * Convenience method, to lookup the Term for a doc.
	 *  If this doc is deleted or did not have this field,
	 *  this will return an empty (length=0) BytesRef. 
	 */
	public BytesRef getTerm(int docID, BytesRef reuse) {
		return lookup(getOrd(docID), reuse);
	}

	/** 
	 * Returns sort ord for this document.  Ord 0 is
	 *  reserved for docs that are deleted or did not have
	 *  this field.
	 */
	public abstract int getOrd(int docID);

	/** Returns total unique ord count; this includes +1 for
	 *  the null ord (always 0). */
	public abstract int getNumOrd();

	/** Number of documents */
	public abstract int size();

	/** Returns a TermsEnum that can iterate over the values in this index entry */
	public abstract ITermsEnum getTermsEnum();

	public abstract IIntsReader getDocToOrd();
	
}
