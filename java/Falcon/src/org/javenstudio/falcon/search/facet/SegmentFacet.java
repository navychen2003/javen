package org.javenstudio.falcon.search.facet;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDocIdSet;
import org.javenstudio.common.indexdb.IDocIdSetIterator;
import org.javenstudio.common.indexdb.IDocTermsIndex;
import org.javenstudio.common.indexdb.IIntsReader;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.search.DocIdSetIterator;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.UnicodeUtil;
import org.javenstudio.hornet.search.cache.FieldCache;

public class SegmentFacet {
	
	protected final SingleValuedFaceting mFaceting;
    protected final IAtomicReaderRef mContext;
    
    protected BytesRef mTempBR = new BytesRef();
    protected IDocTermsIndex mTermsIndex;
    
    protected int mStartTermIndex;
    protected int mEndTermIndex;
    protected int[] mCounts;

    protected int mPos; // only used when merging
    protected ITermsEnum mTermsEnum; // only used when merging

    public SegmentFacet(SingleValuedFaceting faceting, IAtomicReaderRef context) {
    	mFaceting = faceting;
    	mContext = context;
    }

    public void countTerms() throws IOException {
    	mTermsIndex = FieldCache.DEFAULT.getTermsIndex(
    			mContext.getReader(), mFaceting.mFieldName);

    	if (mFaceting.mPrefix != null) {
    		BytesRef prefixRef = new BytesRef(mFaceting.mPrefix);
    		mStartTermIndex = mTermsIndex.binarySearch(prefixRef, mTempBR);
    		
    		if (mStartTermIndex < 0) 
    			mStartTermIndex = -mStartTermIndex-1;
    		
    		prefixRef.append(UnicodeUtil.BIG_TERM);
    		// TODO: we could constrain the lower endpoint if we had a binarySearch method 
    		// that allowed passing start/end
    		mEndTermIndex = mTermsIndex.binarySearch(prefixRef, mTempBR);
    		assert mEndTermIndex < 0;
    		mEndTermIndex = -mEndTermIndex-1;
    		
    	} else {
    		mStartTermIndex = 0;
    		mEndTermIndex = mTermsIndex.getNumOrd();
    	}

    	final int nTerms = mEndTermIndex - mStartTermIndex;
    	if (nTerms > 0) {
    		// count collection array only needs to be as big as the number of terms we are
    		// going to collect counts for.
    		final int[] counts = mCounts = new int[nTerms];
    		// this set only includes live docs
    		IDocIdSet idSet = mFaceting.mBaseSet.getDocIdSet(mContext, null); 
    		IDocIdSetIterator iter = idSet.iterator();

    		IIntsReader ordReader = mTermsIndex.getDocToOrd();
    		int doc;

    		final Object arr;
    		if (ordReader.hasArray()) 
    			arr = ordReader.getArray();
    		else 
    			arr = null;

    		if (arr instanceof int[]) {
    			int[] ords = (int[]) arr;
    			if (mFaceting.mPrefix == null) {
    				while ((doc = iter.nextDoc()) < DocIdSetIterator.NO_MORE_DOCS) {
    					counts[ords[doc]]++;
    				}
    			} else {
    				while ((doc = iter.nextDoc()) < DocIdSetIterator.NO_MORE_DOCS) {
    					int term = ords[doc];
    					int arrIdx = term - mStartTermIndex;
    					if (arrIdx >= 0 && arrIdx < nTerms) 
    						counts[arrIdx]++;
    				}
    			}
    			
    		} else if (arr instanceof short[]) {
    			short[] ords = (short[]) arr;
    			if (mFaceting.mPrefix == null) {
    				while ((doc = iter.nextDoc()) < DocIdSetIterator.NO_MORE_DOCS) {
    					counts[ords[doc] & 0xffff]++;
    				}
    			} else {
    				while ((doc = iter.nextDoc()) < DocIdSetIterator.NO_MORE_DOCS) {
    					int term = ords[doc] & 0xffff;
    					int arrIdx = term-mStartTermIndex;
    					if (arrIdx >= 0 && arrIdx < nTerms) 
    						counts[arrIdx]++;
    				}
    			}
    			
    		} else if (arr instanceof byte[]) {
    			byte[] ords = (byte[]) arr;
    			if (mFaceting.mPrefix == null) {
    				while ((doc = iter.nextDoc()) < DocIdSetIterator.NO_MORE_DOCS) {
    					counts[ords[doc] & 0xff]++;
    				}
    			} else {
    				while ((doc = iter.nextDoc()) < DocIdSetIterator.NO_MORE_DOCS) {
    					int term = ords[doc] & 0xff;
    					int arrIdx = term-mStartTermIndex;
    					if (arrIdx >= 0 && arrIdx < nTerms) 
    						counts[arrIdx]++;
    				}
    			}
    			
    		} else {
    			if (mFaceting.mPrefix == null) {
    				// specialized version when collecting counts for all terms
    				while ((doc = iter.nextDoc()) < DocIdSetIterator.NO_MORE_DOCS) {
    					counts[mTermsIndex.getOrd(doc)]++;
    				}
    			} else {
    				// version that adjusts term numbers because we aren't collecting the full range
    				while ((doc = iter.nextDoc()) < DocIdSetIterator.NO_MORE_DOCS) {
    					int term = mTermsIndex.getOrd(doc);
    					int arrIdx = term-mStartTermIndex;
    					if (arrIdx>=0 && arrIdx<nTerms) 
    						counts[arrIdx]++;
    				}
    			}
    		}
    	}
	}
    
}
