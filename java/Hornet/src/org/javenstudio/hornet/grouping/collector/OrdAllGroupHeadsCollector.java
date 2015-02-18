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
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.search.cache.FieldCache;
import org.javenstudio.hornet.util.SentinelIntSet;

//AbstractAllGroupHeadsCollector optimized for ord fields.
public class OrdAllGroupHeadsCollector extends TermAllGroupHeadsCollector<OrdAllGroupHead> {
	
    protected final SentinelIntSet mOrdSet;
    protected final List<OrdAllGroupHead> mCollectedGroups;
    protected final ISortField[] mFields;

    protected IDocTermsIndex[] mSortsIndex;
    protected OrdAllGroupHead[] mSegmentGroupHeads;

    public OrdAllGroupHeadsCollector(String groupField, ISort sortWithinGroup, int initialSize) {
    	super(groupField, sortWithinGroup.getSortFields().length);
    	
    	mOrdSet = new SentinelIntSet(initialSize, -1);
    	mCollectedGroups = new ArrayList<OrdAllGroupHead>(initialSize);

    	final ISortField[] sortFields = sortWithinGroup.getSortFields();
    	
    	mFields = new ISortField[sortFields.length];
    	mSortsIndex = new IDocTermsIndex[sortFields.length];
      
    	for (int i = 0; i < sortFields.length; i++) {
    		mReversed[i] = sortFields[i].getReverse() ? -1 : 1;
    		mFields[i] = sortFields[i];
    	}
    }

    @Override
	protected Collection<OrdAllGroupHead> getCollectedGroupHeads() {
		return mCollectedGroups;
    }

    @Override
    public void setScorer(IScorer scorer) throws IOException {
    	// do nothing
    }

    @Override
    protected void retrieveGroupHeadAndAddIfNotExist(int doc) throws IOException {
    	int key = mGroupIndex.getOrd(doc);
    	OrdAllGroupHead groupHead;
    	
    	if (!mOrdSet.exists(key)) {
    		mOrdSet.put(key);
    		
    		BytesRef term = key == 0 ? null : mGroupIndex.getTerm(doc, new BytesRef());
    		groupHead = new OrdAllGroupHead(this, doc, term);
    		
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
    	  mSortsIndex[i] = FieldCache.DEFAULT.getTermsIndex(
    			  context.getReader(), mFields[i].getField());
      }

      // Clear ordSet and fill it with previous encountered groups that can occur in the current segment.
      mOrdSet.clear();
      mSegmentGroupHeads = new OrdAllGroupHead[mGroupIndex.getNumOrd()];
      
      for (OrdAllGroupHead collectedGroup : mCollectedGroups) {
    	  int groupOrd = mGroupIndex.binarySearch(collectedGroup.getGroupValue(), mScratchBytesRef);
    	  if (groupOrd >= 0) {
    		  mOrdSet.put(groupOrd);
    		  mSegmentGroupHeads[groupOrd] = collectedGroup;

    		  for (int i = 0; i < mSortsIndex.length; i++) {
    			  collectedGroup.mSortOrds[i] = 
    					  mSortsIndex[i].binarySearch(collectedGroup.mSortValues[i], mScratchBytesRef);
    		  }
    	  }
      	}
    }

}
