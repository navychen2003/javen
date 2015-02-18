package org.javenstudio.hornet.grouping.collector;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IFieldComparator;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.search.cache.FieldCache;

//A general impl that works for any group sort.
public class GeneralAllGroupHeadsCollector 
		extends TermAllGroupHeadsCollector<GeneralAllGroupHead> {
    
	protected final ISort mSortWithinGroup;
	protected final Map<BytesRef, GeneralAllGroupHead> mGroups;

    protected IScorer mScorer;

    public GeneralAllGroupHeadsCollector(String groupField, ISort sortWithinGroup) {
    	super(groupField, sortWithinGroup.getSortFields().length);
    	
    	mSortWithinGroup = sortWithinGroup;
    	mGroups = new HashMap<BytesRef, GeneralAllGroupHead>();

    	final ISortField[] sortFields = sortWithinGroup.getSortFields();
    	for (int i = 0; i < sortFields.length; i++) {
    		mReversed[i] = sortFields[i].getReverse() ? -1 : 1;
    	}
    }

    @Override
    protected void retrieveGroupHeadAndAddIfNotExist(int doc) throws IOException {
    	final int ord = mGroupIndex.getOrd(doc);
    	final BytesRef groupValue = ord == 0 ? null : mGroupIndex.lookup(ord, mScratchBytesRef);
    	
    	GeneralAllGroupHead groupHead = mGroups.get(groupValue);
    	if (groupHead == null) {
    		groupHead = new GeneralAllGroupHead(this, groupValue, mSortWithinGroup, doc);
    		mGroups.put(groupValue == null ? null : BytesRef.deepCopyOf(groupValue), groupHead);
    		mTemporalResult.setStop(true);
    		
    	} else {
    		mTemporalResult.setStop(false);
    	}
    	
    	mTemporalResult.setGroupHead(groupHead);
    }

    @Override
    protected Collection<GeneralAllGroupHead> getCollectedGroupHeads() {
    	return mGroups.values();
    }

    @Override
    public void setNextReader(IAtomicReaderRef context) throws IOException {
      	mReaderContext = context;
      	mGroupIndex = FieldCache.DEFAULT.getTermsIndex(context.getReader(), mGroupField);

      	for (GeneralAllGroupHead groupHead : mGroups.values()) {
      		for (int i = 0; i < groupHead.mComparators.length; i++) {
      			groupHead.mComparators[i] = groupHead.mComparators[i].setNextReader(context);
      		}
      	}
    }

    @Override
    public void setScorer(IScorer scorer) throws IOException {
    	mScorer = scorer;
    	for (GeneralAllGroupHead groupHead : mGroups.values()) {
    		for (IFieldComparator<?> comparator : groupHead.mComparators) {
    			comparator.setScorer(scorer);
    		}
    	}
    }

}
