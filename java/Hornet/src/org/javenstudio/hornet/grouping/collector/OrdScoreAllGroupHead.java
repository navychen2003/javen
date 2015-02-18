package org.javenstudio.hornet.grouping.collector;

import java.io.IOException;

import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.grouping.GroupHead;

public class OrdScoreAllGroupHead extends GroupHead<BytesRef> {
	
	protected final OrdScoreAllGroupHeadsCollector mCollector;
    protected BytesRef[] mSortValues;
    protected int[] mSortOrds;
    protected float[] mScores;

    public OrdScoreAllGroupHead(OrdScoreAllGroupHeadsCollector collector, 
    		int doc, BytesRef groupValue) throws IOException {
    	super(groupValue, doc + collector.getReaderContext().getDocBase());
    	
    	mCollector = collector;
    	mSortValues = new BytesRef[collector.mSortsIndex.length];
    	mSortOrds = new int[collector.mSortsIndex.length];
    	mScores = new float[collector.mSortsIndex.length];
    	
    	for (int i = 0; i < collector.mSortsIndex.length; i++) {
    		if (collector.mFields[i].getType() == ISortField.Type.SCORE) {
    			mScores[i] = collector.mScorer.getScore();
    		} else {
    			mSortValues[i] = collector.mSortsIndex[i].getTerm(doc, new BytesRef());
    			mSortOrds[i] = collector.mSortsIndex[i].getOrd(doc);
    		}
    	}
    }

    @Override
    public int compare(int compIDX, int doc) throws IOException {
    	if (mCollector.mFields[compIDX].getType() == ISortField.Type.SCORE) {
    		float score = mCollector.mScorer.getScore();
    		if (mScores[compIDX] < score) 
    			return 1;
    		else if (mScores[compIDX] > score) 
    			return -1;
    		else 
    			return 0;
    		
    	} else {
    		if (mSortOrds[compIDX] < 0) {
    			// The current segment doesn't contain the sort value we encountered before. 
    			// Therefore the ord is negative.
    			return mSortValues[compIDX].compareTo(
    					mCollector.mSortsIndex[compIDX].getTerm(doc, mCollector.mScratchBytesRef));
    			
    		} else {
    			return mSortOrds[compIDX] - mCollector.mSortsIndex[compIDX].getOrd(doc);
    		}
    	}
    }

    @Override
    public void updateDocHead(int doc) throws IOException {
    	for (int i = 0; i < mCollector.mSortsIndex.length; i++) {
    		if (mCollector.mFields[i].getType() == ISortField.Type.SCORE) {
    			mScores[i] = mCollector.mScorer.getScore();
    			
    		} else {
    			mSortValues[i] = mCollector.mSortsIndex[i].getTerm(doc, mSortValues[i]);
    			mSortOrds[i] = mCollector.mSortsIndex[i].getOrd(doc);
    		}
    	}
    	
    	mDoc = doc + mCollector.getReaderContext().getDocBase();
    }
    
}
