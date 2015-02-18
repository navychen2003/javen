package org.javenstudio.hornet.search;

import java.io.IOException;

import org.javenstudio.common.indexdb.search.DocIdSetIterator;

/**
 * Abstract decorator class of a DocIdSetIterator
 * implementation that provides on-demand filter/validation
 * mechanism on an underlying DocIdSetIterator.  See {@link
 * FilteredDocIdSet}.
 */
public abstract class FilteredDocIdSetIterator extends DocIdSetIterator {
	
	protected DocIdSetIterator mInnerIter;
	private int mDoc;
	
	/**
	 * Constructor.
	 * @param innerIter Underlying DocIdSetIterator.
	 */
	public FilteredDocIdSetIterator(DocIdSetIterator innerIter) {
		if (innerIter == null) 
			throw new IllegalArgumentException("null iterator");
		
		mInnerIter = innerIter;
		mDoc = -1;
	}
	
	/**
	 * Validation method to determine whether a docid should be in the result set.
	 * @param doc docid to be tested
	 * @return true if input docid should be in the result set, false otherwise.
	 * @see #FilteredDocIdSetIterator(DocIdSetIterator)
	 */
	protected abstract boolean match(int doc);
	
	@Override
	public int getDocID() {
		return mDoc;
	}
  
	@Override
	public int nextDoc() throws IOException {
		while ((mDoc = mInnerIter.nextDoc()) != NO_MORE_DOCS) {
			if (match(mDoc)) 
				return mDoc;
		}
		return mDoc;
	}
  
	@Override
	public int advance(int target) throws IOException {
		mDoc = mInnerIter.advance(target);
		if (mDoc != NO_MORE_DOCS) {
			if (match(mDoc)) {
				return mDoc;
				
			} else {
				while ((mDoc = mInnerIter.nextDoc()) != NO_MORE_DOCS) {
					if (match(mDoc)) 
						return mDoc;
				}
				return mDoc;
			}
		}
		return mDoc;
	}
  
}
