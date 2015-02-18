package org.javenstudio.falcon.search.comparator;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.index.term.DocTermsIndex;
import org.javenstudio.common.indexdb.search.FieldComparator;
import org.javenstudio.common.indexdb.util.BytesRef;

// Base class for specialized (per bit width of the
// ords) per-segment comparator.  NOTE: this is messy;
// we do this only because hotspot can't reliably inline
// the underlying array access when looking up doc->ord
public abstract class PerSegmentComparator extends FieldComparator<BytesRef> {

    protected final TermOrdValComparator mParent;
    protected final BytesRef mTempBR = new BytesRef();
    
    protected final int[] mOrds;
    protected final BytesRef[] mValues;
    protected final int[] mReaderGen;

    protected int mCurrentReaderGen = -1;
    protected DocTermsIndex mTermsIndex;

    protected boolean mBottomSameReader = false;
    protected BytesRef mBottomValue;
    protected int mBottomSlot = -1;
    protected int mBottomOrd;
    
    public PerSegmentComparator(TermOrdValComparator parent) {
    	mParent = parent;
    	
    	PerSegmentComparator previous = parent.mCurrent;
    	if (previous != null) {
    		mCurrentReaderGen = previous.mCurrentReaderGen;
    		mBottomSlot = previous.mBottomSlot;
    		mBottomOrd = previous.mBottomOrd;
    		mBottomValue = previous.mBottomValue;
    	}
    	
    	mOrds = parent.mOrds;
      	mValues = parent.mValues;
      	mReaderGen = parent.mReaderGen;
      	mTermsIndex = parent.mTermsIndex;
      	
      	mCurrentReaderGen ++;
	}

    @Override
    public FieldComparator<BytesRef> setNextReader(IAtomicReaderRef context) throws IOException {
    	return TermOrdValComparator.createComparator(context.getReader(), mParent);
    }

    @Override
    public int compare(int slot1, int slot2) {
    	if (mReaderGen[slot1] == mReaderGen[slot2]) 
    		return mOrds[slot1] - mOrds[slot2];
    	
    	final BytesRef val1 = mValues[slot1];
    	final BytesRef val2 = mValues[slot2];
    	
    	if (val1 == null) {
    		if (val2 == null) 
    			return 0;
    		
    		return 1;
    		
    	} else if (val2 == null) {
    		return -1;
    	}
    	
    	return val1.compareTo(val2);
    }

	@Override
    public void setBottom(final int bottom) {
    	mBottomSlot = bottom;
    	mBottomValue = mValues[mBottomSlot];
    	
    	if (mCurrentReaderGen == mReaderGen[mBottomSlot]) {
    		mBottomOrd = mOrds[mBottomSlot];
    		mBottomSameReader = true;
    		
    	} else {
    		if (mBottomValue == null) {
    			// 0 ord is null for all segments
    			assert mOrds[mBottomSlot] == TermOrdValComparator.NULL_ORD;
    			mBottomOrd = TermOrdValComparator.NULL_ORD;
    			mBottomSameReader = true;
    			mReaderGen[mBottomSlot] = mCurrentReaderGen;
    			
    		} else {
    			final int index = mTermsIndex.binarySearch(mTempBR, mBottomValue);
    			if (index < 0) {
    				mBottomOrd = -index - 2;
    				mBottomSameReader = false;
    				
    			} else {
    				mBottomOrd = index;
    				// exact value match
    				mBottomSameReader = true;
    				mReaderGen[mBottomSlot] = mCurrentReaderGen;
    				mOrds[mBottomSlot] = mBottomOrd;
    			}
    		}
    	}
	}

    @Override
    public BytesRef getValue(int slot) {
    	return mValues == null ? mParent.mNullVal : mValues[slot];
    }

    @Override
    public int compareDocToValue(int doc, BytesRef value) {
    	final BytesRef docValue = mTermsIndex.getTerm(doc, mTempBR);
    	if (docValue == null) {
    		if (value == null) 
    			return 0;
    		
    		return 1;
    		
    	} else if (value == null) {
    		return -1;
    	}
    	
    	return docValue.compareTo(value);
    }
	
}
