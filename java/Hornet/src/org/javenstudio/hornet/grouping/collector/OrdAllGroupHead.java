package org.javenstudio.hornet.grouping.collector;

import java.io.IOException;

import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.grouping.GroupHead;

public class OrdAllGroupHead extends GroupHead<BytesRef> {
	
	protected final OrdAllGroupHeadsCollector mCollector;
    protected BytesRef[] mSortValues;
    protected int[] mSortOrds;

    public OrdAllGroupHead(OrdAllGroupHeadsCollector collector, int doc, BytesRef groupValue) {
    	super(groupValue, doc + collector.getReaderContext().getDocBase());
    	
    	mCollector = collector;
    	mSortValues = new BytesRef[collector.mSortsIndex.length];
    	mSortOrds = new int[collector.mSortsIndex.length];
    	
    	for (int i = 0; i < collector.mSortsIndex.length; i++) {
    		mSortValues[i] = collector.mSortsIndex[i].getTerm(doc, new BytesRef());
    		mSortOrds[i] = collector.mSortsIndex[i].getOrd(doc);
    	}
    }

    @Override
    public int compare(int compIDX, int doc) throws IOException {
    	if (mSortOrds[compIDX] < 0) {
    		// The current segment doesn't contain the sort value we encountered before. 
    		// Therefore the ord is negative.
    		return mSortValues[compIDX].compareTo(
    				mCollector.mSortsIndex[compIDX].getTerm(doc, mCollector.mScratchBytesRef));
    		
    	} else {
    		return mSortOrds[compIDX] - mCollector.mSortsIndex[compIDX].getOrd(doc);
    	}
    }

    @Override
    public void updateDocHead(int doc) throws IOException {
    	for (int i = 0; i < mCollector.mSortsIndex.length; i++) {
    		mSortValues[i] = mCollector.mSortsIndex[i].getTerm(doc, mSortValues[i]);
    		mSortOrds[i] = mCollector.mSortsIndex[i].getOrd(doc);
    	}
    	
    	mDoc = doc + mCollector.getReaderContext().getDocBase();
    }
    
}
