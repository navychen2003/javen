package org.javenstudio.falcon.search.comparator;

import org.javenstudio.common.indexdb.IIntsReader;
import org.javenstudio.common.indexdb.util.BytesRef;

// Used per-segment when bit width is not a native array
// size (8, 16, 32):
public class AnyOrdComparator extends PerSegmentComparator {
	
    private final IIntsReader mReaderOrds;

    public AnyOrdComparator(IIntsReader readerOrds, TermOrdValComparator parent) {
    	super(parent);
    	mReaderOrds = readerOrds;
    }

    @Override
    public int compareBottom(int doc) {
    	assert mBottomSlot != -1;
    	int order = (int) mReaderOrds.get(doc);
    	if (order == 0) 
    		order = TermOrdValComparator.NULL_ORD;
    	
    	if (mBottomSameReader) {
    		// ord is precisely comparable, even in the equal case
    		return mBottomOrd - order;
    		
    	} else {
    		// ord is only approx comparable: if they are not
    		// equal, we can use that; if they are equal, we
    		// must fallback to compare by value
    		final int cmp = mBottomOrd - order;
    		if (cmp != 0) 
    			return cmp;

    		// take care of the case where both vals are null
    		if (order == TermOrdValComparator.NULL_ORD) 
    			return 0;

    		// and at this point we know that neither value is null, so safe to compare
    		mTermsIndex.lookup(order, mTempBR);
    		
    		return mBottomValue.compareTo(mTempBR);
    	}
    }

    @Override
    public void copy(int slot, int doc) {
    	int ord = (int) mReaderOrds.get(doc);
    	if (ord == 0) {
    		mOrds[slot] = TermOrdValComparator.NULL_ORD;
    		mValues[slot] = null;
    		
    	} else {
    		mOrds[slot] = ord;
    		assert ord > 0;
    		if (mValues[slot] == null) 
    			mValues[slot] = new BytesRef();
    		
    		mTermsIndex.lookup(ord, mValues[slot]);
    	}
    	
    	mReaderGen[slot] = mCurrentReaderGen;
    }
    
}
