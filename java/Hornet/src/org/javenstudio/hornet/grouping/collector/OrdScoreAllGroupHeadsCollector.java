package org.javenstudio.hornet.grouping.collector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDocTermsIndex;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.index.term.DocTermsIndex;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.search.cache.FieldCache;
import org.javenstudio.hornet.util.SentinelIntSet;

//AbstractAllGroupHeadsCollector optimized for ord fields and scores.
public class OrdScoreAllGroupHeadsCollector 
		extends TermAllGroupHeadsCollector<OrdScoreAllGroupHead> {
    
	protected final SentinelIntSet mOrdSet;
	protected final List<OrdScoreAllGroupHead> mCollectedGroups;
	protected final ISortField[] mFields;

	protected OrdScoreAllGroupHead[] mSegmentGroupHeads;
	protected IDocTermsIndex[] mSortsIndex;
	protected IScorer mScorer;
	

    public OrdScoreAllGroupHeadsCollector(String groupField, ISort sortWithinGroup, int initialSize) {
    	super(groupField, sortWithinGroup.getSortFields().length);
    	
    	mOrdSet = new SentinelIntSet(initialSize, -1);
    	mCollectedGroups = new ArrayList<OrdScoreAllGroupHead>(initialSize);

    	final ISortField[] sortFields = sortWithinGroup.getSortFields();
    	
    	mFields = new ISortField[sortFields.length];
    	mSortsIndex = new DocTermsIndex[sortFields.length];
    	
    	for (int i = 0; i < sortFields.length; i++) {
    		mReversed[i] = sortFields[i].getReverse() ? -1 : 1;
    		mFields[i] = sortFields[i];
    	}
    }

    @Override
    protected Collection<OrdScoreAllGroupHead> getCollectedGroupHeads() {
    	return mCollectedGroups;
    }

    @Override
    public void setScorer(IScorer scorer) throws IOException {
    	mScorer = scorer;
    }

    @Override
    protected void retrieveGroupHeadAndAddIfNotExist(int doc) throws IOException {
    	int key = mGroupIndex.getOrd(doc);
    	OrdScoreAllGroupHead groupHead;
    	
    	if (!mOrdSet.exists(key)) {
    		mOrdSet.put(key);
    		
    		BytesRef term = key == 0 ? null : mGroupIndex.getTerm(doc, new BytesRef());
    		groupHead = new OrdScoreAllGroupHead(this, doc, term);
    		
    		mCollectedGroups.add(groupHead);
    		mSegmentGroupHeads[key] = groupHead;
    		mTemporalResult.setStop(true);
    		
    	} else {
    		mTemporalResult.setStop(false);
    		groupHead = mSegmentGroupHeads[key];
    	}
    	
    	mTemporalResult.setGroupHead(groupHead);
    }

    @Override
    public void setNextReader(IAtomicReaderRef context) throws IOException {
    	mReaderContext = context;
    	mGroupIndex = FieldCache.DEFAULT.getTermsIndex(context.getReader(), mGroupField);
    	
    	for (int i = 0; i < mFields.length; i++) {
    		if (mFields[i].getType() == ISortField.Type.SCORE) 
    			continue;

    		mSortsIndex[i] = FieldCache.DEFAULT.getTermsIndex(
    				context.getReader(), mFields[i].getField());
    	}

    	// Clear ordSet and fill it with previous encountered groups that can occur in the current segment.
    	mOrdSet.clear();
    	mSegmentGroupHeads = new OrdScoreAllGroupHead[mGroupIndex.getNumOrd()];
    	
    	for (OrdScoreAllGroupHead collectedGroup : mCollectedGroups) {
    		int ord = mGroupIndex.binarySearch(collectedGroup.getGroupValue(), mScratchBytesRef);
    		if (ord >= 0) {
    			mOrdSet.put(ord);
    			mSegmentGroupHeads[ord] = collectedGroup;

    			for (int i = 0; i < mSortsIndex.length; i++) {
    				if (mFields[i].getType() == ISortField.Type.SCORE) 
    					continue;

    				collectedGroup.mSortOrds[i] = mSortsIndex[i].binarySearch(
    						collectedGroup.mSortValues[i], mScratchBytesRef);
    			}
    		}
    	}
    }

}
