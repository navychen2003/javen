package org.javenstudio.hornet.search;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDocIdSetIterator;
import org.javenstudio.common.indexdb.search.DocIdSet;
import org.javenstudio.common.indexdb.search.DocIdSetIterator;
import org.javenstudio.common.indexdb.util.Bits;

/**
 * Abstract decorator class for a DocIdSet implementation
 * that provides on-demand filtering/validation
 * mechanism on a given DocIdSet.
 *
 * <p/>
 *
 * Technically, this same functionality could be achieved
 * with ChainedFilter (under queries/), however the
 * benefit of this class is it never materializes the full
 * bitset for the filter.  Instead, the {@link #match}
 * method is invoked on-demand, per docID visited during
 * searching.  If you know few docIDs will be visited, and
 * the logic behind {@link #match} is relatively costly,
 * this may be a better way to filter than ChainedFilter.
 *
 * @see DocIdSet
 */
public abstract class FilteredDocIdSet extends DocIdSet {
	
	private final DocIdSet mInnerSet;
  
	/**
	 * Constructor.
	 * @param innerSet Underlying DocIdSet
	 */
	public FilteredDocIdSet(DocIdSet innerSet) {
		mInnerSet = innerSet;
	}
  
	/** This DocIdSet implementation is cacheable if the inner set is cacheable. */
	@Override
	public boolean isCacheable() {
		return mInnerSet.isCacheable();
	}
  
	@Override
	public Bits getBits() throws IOException {
		final Bits bits = mInnerSet.getBits();
		return (bits == null) ? null : new Bits() {
				public boolean get(int docid) {
					return bits.get(docid) && FilteredDocIdSet.this.match(docid);
				}
	
				public int length() {
					return bits.length();
				}
			};
	}

	/**
	 * Validation method to determine whether a docid should be in the result set.
	 * @param docid docid to be tested
	 * @return true if input docid should be in the result set, false otherwise.
	 */
	protected abstract boolean match(int docid);

	/**
	 * Implementation of the contract to build a DocIdSetIterator.
	 * @see DocIdSetIterator
	 * @see FilteredDocIdSetIterator
	 */
	@Override
	public IDocIdSetIterator iterator() throws IOException {
		final IDocIdSetIterator iterator = mInnerSet.iterator();
		if (iterator == null) 
			return null;
		
		return new FilteredDocIdSetIterator((DocIdSetIterator)iterator) {
			@Override
			protected boolean match(int docid) {
				return FilteredDocIdSet.this.match(docid);
			}
		};
	}
	
}
