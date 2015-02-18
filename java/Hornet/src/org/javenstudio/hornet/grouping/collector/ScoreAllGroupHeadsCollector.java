package org.javenstudio.hornet.grouping.collector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.search.cache.FieldCache;
import org.javenstudio.hornet.util.SentinelIntSet;

//AbstractAllGroupHeadsCollector optimized for scores.
public class ScoreAllGroupHeadsCollector 
		extends TermAllGroupHeadsCollector<ScoreAllGroupHead> {

    private final SentinelIntSet mOrdSet;
    private final List<ScoreAllGroupHead> mCollectedGroups;
    private final ISortField[] mFields;

    private ScoreAllGroupHead[] mSegmentGroupHeads;
    private IScorer mScorer;

    public ScoreAllGroupHeadsCollector(String groupField, ISort sortWithinGroup, int initialSize) {
      super(groupField, sortWithinGroup.getSortFields().length);
      
      mOrdSet = new SentinelIntSet(initialSize, -1);
      mCollectedGroups = new ArrayList<ScoreAllGroupHead>(initialSize);

      final ISortField[] sortFields = sortWithinGroup.getSortFields();
      mFields = new ISortField[sortFields.length];
      
      for (int i = 0; i < sortFields.length; i++) {
    	  mReversed[i] = sortFields[i].getReverse() ? -1 : 1;
    	  mFields[i] = sortFields[i];
      }
    }

    public final IScorer getScorer() { return mScorer; }
    
    @Override
    protected Collection<ScoreAllGroupHead> getCollectedGroupHeads() {
    	return mCollectedGroups;
    }

    @Override
    public void setScorer(IScorer scorer) throws IOException {
    	mScorer = scorer;
    }

    @Override
    protected void retrieveGroupHeadAndAddIfNotExist(int doc) throws IOException {
    	int key = mGroupIndex.getOrd(doc);
    	ScoreAllGroupHead groupHead;
    	
    	if (!mOrdSet.exists(key)) {
    		mOrdSet.put(key);
    		
    		BytesRef term = key == 0 ? null : mGroupIndex.getTerm(doc, new BytesRef());
    		groupHead = new ScoreAllGroupHead(this, doc, term, mFields.length);
    		
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

    	// Clear ordSet and fill it with previous encountered groups that can occur in the current segment.
    	mOrdSet.clear();
    	mSegmentGroupHeads = new ScoreAllGroupHead[mGroupIndex.getNumOrd()];
    	
    	for (ScoreAllGroupHead collectedGroup : mCollectedGroups) {
    		int ord = mGroupIndex.binarySearch(collectedGroup.getGroupValue(), mScratchBytesRef);
    		if (ord >= 0) {
    			mOrdSet.put(ord);
    			mSegmentGroupHeads[ord] = collectedGroup;
    		}
    	}
    }

}
