package org.javenstudio.hornet.search;

import org.javenstudio.common.indexdb.search.DocIdSet;
import org.javenstudio.common.indexdb.util.Bits;

/**
 * This implementation supplies a filtered DocIdSet, that excludes all
 * docids which are not in a Bits instance. This is especially useful in
 * {@link org.apache.lucene.search.Filter} to apply the {@code acceptDocs}
 * passed to {@code getDocIdSet()} before returning the final DocIdSet.
 *
 * @see DocIdSet
 * @see Filter
 */
public final class BitsFilteredDocIdSet extends FilteredDocIdSet {

	private final Bits mAcceptDocs;
  
	/**
	 * Convenience wrapper method: If {@code acceptDocs == null} it returns the original set without wrapping.
	 * @param set Underlying DocIdSet. If {@code null}, this method returns {@code null}
	 * @param acceptDocs Allowed docs, all docids not in this set will not be returned by this DocIdSet.
	 * If {@code null}, this method returns the original set without wrapping.
	 */
	public static DocIdSet wrap(DocIdSet set, Bits acceptDocs) {
		return (set == null || acceptDocs == null) ? set : new BitsFilteredDocIdSet(set, acceptDocs);
	}
  
	/**
	 * Constructor.
	 * @param innerSet Underlying DocIdSet
	 * @param acceptDocs Allowed docs, all docids not in this set will not be returned by this DocIdSet
	 */
	public BitsFilteredDocIdSet(DocIdSet innerSet, Bits acceptDocs) {
		super(innerSet);
		if (acceptDocs == null)
			throw new NullPointerException("acceptDocs is null");
		
		mAcceptDocs = acceptDocs;
	}

	@Override
	protected boolean match(int docid) {
		return mAcceptDocs.get(docid);
	}

}
