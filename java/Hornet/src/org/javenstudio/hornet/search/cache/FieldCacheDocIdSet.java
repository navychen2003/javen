package org.javenstudio.hornet.search.cache;

import java.io.IOException;

import org.javenstudio.common.indexdb.search.DocIdSet;
import org.javenstudio.common.indexdb.search.DocIdSetIterator;
import org.javenstudio.common.indexdb.search.FixedBitSet;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.hornet.search.FilteredDocIdSetIterator;
import org.javenstudio.hornet.search.OpenBitSet;

/**
 * Base class for DocIdSet to be used with FieldCache. The implementation
 * of its iterator is very stupid and slow if the implementation of the
 * {@link #matchDoc} method is not optimized, as iterators simply increment
 * the document id until {@code matchDoc(int)} returns true. Because of this
 * {@code matchDoc(int)} must be as fast as possible and in no case do any
 * I/O.
 */
public abstract class FieldCacheDocIdSet extends DocIdSet {

	protected final int mMaxDoc;
	protected final Bits mAcceptDocs;

	public FieldCacheDocIdSet(int maxDoc, Bits acceptDocs) {
		mMaxDoc = maxDoc;
		mAcceptDocs = acceptDocs;
	}

	/**
	 * this method checks, if a doc is a hit
	 */
	protected abstract boolean matchDoc(int doc);

	/**
	 * this DocIdSet is always cacheable (does not go back
	 * to the reader for iteration)
	 */
	@Override
	public final boolean isCacheable() {
		return true;
	}

	@Override
	public final Bits getBits() {
		return (mAcceptDocs == null) ? new Bits() {
				public boolean get(int docid) {
					return matchDoc(docid);
				}
	
				public int length() {
					return mMaxDoc;
				}
			} : new Bits() {
				public boolean get(int docid) {
					return matchDoc(docid) && mAcceptDocs.get(docid);
				}
	
				public int length() {
					return mMaxDoc;
				}
			};
	}

	@Override
	public final DocIdSetIterator iterator() throws IOException {
		if (mAcceptDocs == null) {
			// Specialization optimization disregard acceptDocs
			return new DocIdSetIterator() {
					private int mDoc = -1;
	        
					@Override
					public int getDocID() {
						return mDoc;
					}
	      
					@Override
					public int nextDoc() {
						do {
							mDoc ++;
							if (mDoc >= mMaxDoc) 
								return mDoc = NO_MORE_DOCS;
						} while (!matchDoc(mDoc));
						return mDoc;
					}
	      
					@Override
					public int advance(int target) {
						for (mDoc = target; mDoc < mMaxDoc; mDoc++) {
							if (matchDoc(mDoc)) 
								return mDoc;
						}
						return mDoc = NO_MORE_DOCS;
					}
				};
			
		} else if (mAcceptDocs instanceof FixedBitSet || mAcceptDocs instanceof OpenBitSet) {
			// special case for FixedBitSet / OpenBitSet: use the iterator and filter it
			// (used e.g. when Filters are chained by FilteredQuery)
			return new FilteredDocIdSetIterator((DocIdSetIterator)((DocIdSet)mAcceptDocs).iterator()) {
					@Override
					protected boolean match(int doc) {
						return FieldCacheDocIdSet.this.matchDoc(doc);
					}
				};
				
		} else {
			// Stupid consultation of acceptDocs and matchDoc()
			return new DocIdSetIterator() {
					private int mDoc = -1;
	        
					@Override
					public int getDocID() {
						return mDoc;
					}
	      
					@Override
					public int nextDoc() {
						do {
							mDoc ++;
							if (mDoc >= mMaxDoc) 
								return mDoc = NO_MORE_DOCS;
						} while (!(matchDoc(mDoc) && mAcceptDocs.get(mDoc)));
						return mDoc;
					}
	      
					@Override
					public int advance(int target) {
						for (mDoc = target; mDoc < mMaxDoc; mDoc++) {
							if (matchDoc(mDoc) && mAcceptDocs.get(mDoc)) 
								return mDoc;
						}
						return mDoc = NO_MORE_DOCS;
					}
				};
		}
	}
	
}
