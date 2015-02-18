package org.javenstudio.falcon.search.grouping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.javenstudio.common.indexdb.ICollector;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.search.Sort;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.hornet.grouping.AbstractAllGroupHeadsCollector;
import org.javenstudio.hornet.grouping.GroupDocs;
import org.javenstudio.hornet.grouping.SearchGroup;
import org.javenstudio.hornet.grouping.collector.FunctionAllGroupHeadsCollector;
import org.javenstudio.hornet.grouping.collector.FunctionAllGroupsCollector;
import org.javenstudio.hornet.grouping.collector.FunctionFirstPassGroupingCollector;
import org.javenstudio.hornet.grouping.collector.FunctionSecondPassGroupingCollector;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.hornet.search.AdvancedSort;
import org.javenstudio.hornet.search.collector.MultiCollector;
import org.javenstudio.hornet.search.collector.TotalHitCountCollector;
import org.javenstudio.hornet.util.MutableValue;

/**
 * A command for grouping on a function.
 */
public class GroupingCommandFunc extends GroupingCommand<MutableValue> {

    protected ValueSource mGroupBy;
    protected ValueSourceContext mContext;

    protected FunctionFirstPassGroupingCollector mFirstPass;
    protected FunctionSecondPassGroupingCollector mSecondPass;
    
    // If offset falls outside the number of documents a group can provide 
    // use this collector instead of secondPass
    protected TotalHitCountCollector mFallBackCollector;
    protected FunctionAllGroupsCollector mAllGroupsCollector;
    protected Collection<SearchGroup<MutableValue>> mTopGroups;

    public GroupingCommandFunc(Grouping grouping) { 
    	super(grouping);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void prepare() throws ErrorException {
    	ValueSourceContext context = mGrouping.mSearcher.createValueSourceContext();
    	mGrouping.mSearcher.createValueSourceWeight(context, mGroupBy);
    	mActualGroupsToFind = mGrouping.getMax(mOffset, mNumGroups, mGrouping.mMaxDoc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ICollector createFirstPassCollector() throws ErrorException {
    	// Ok we don't want groups, but do want a total count
    	if (mActualGroupsToFind <= 0) {
    		mFallBackCollector = new TotalHitCountCollector();
    		return mFallBackCollector;
    	}

    	try {
	    	mSort = (mSort == null) ? AdvancedSort.RELEVANCE : mSort;
	    	mFirstPass = new FunctionFirstPassGroupingCollector(mGroupBy, mContext, 
	    			mGrouping.mSearcher.weightSort(mSort), mActualGroupsToFind);
    	} catch (IOException ex) { 
    		throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
    	}
    	
    	return mFirstPass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ICollector createSecondPassCollector() throws ErrorException {
    	if (mActualGroupsToFind <= 0) {
    		mAllGroupsCollector = new FunctionAllGroupsCollector(mGroupBy, mContext);
    		return mTotalCount == Grouping.TotalCount.GROUPED ? mAllGroupsCollector : null;
    	}

    	mTopGroups = mFormat == Grouping.Format.GROUPED ? 
    			mFirstPass.getTopGroups(mOffset, false) : mFirstPass.getTopGroups(0, false);
    			
    	if (mTopGroups == null) {
    		if (mTotalCount == Grouping.TotalCount.GROUPED) {
    			mAllGroupsCollector = new FunctionAllGroupsCollector(mGroupBy, mContext);
    			mFallBackCollector = new TotalHitCountCollector();
    			
    			return MultiCollector.wrap(mAllGroupsCollector, mFallBackCollector);
    		} else {
    			mFallBackCollector = new TotalHitCountCollector();
    			
    			return mFallBackCollector;
    		}
    	}

    	int groupdDocsToCollect = mGrouping.getMax(mGroupOffset, mDocsPerGroup, mGrouping.mMaxDoc);
    	groupdDocsToCollect = Math.max(groupdDocsToCollect, 1);
    	
    	try {
	    	mSecondPass = new FunctionSecondPassGroupingCollector(
	    			mTopGroups, mSort, mGroupSort, groupdDocsToCollect, 
	    			mGrouping.mNeedScores, mGrouping.mNeedScores, false, mGroupBy, mContext
	    			);
    	} catch (IOException ex) { 
    		throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
    	}

    	if (mTotalCount == Grouping.TotalCount.GROUPED) {
    		mAllGroupsCollector = new FunctionAllGroupsCollector(mGroupBy, mContext);
    		
    		return MultiCollector.wrap(mSecondPass, mAllGroupsCollector);
    	}
    	
    	return mSecondPass;
    }

    @Override
    public AbstractAllGroupHeadsCollector<?> createAllGroupCollector() throws ErrorException {
    	ISort sortWithinGroup = mGroupSort != null ? mGroupSort : new Sort();
    	
    	return new FunctionAllGroupHeadsCollector(mGroupBy, mContext, sortWithinGroup);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void finish() throws ErrorException {
    	mResult = mSecondPass != null ? mSecondPass.getTopGroups(0) : null;
    	if (mIsMain) {
    		mGrouping.mMainResult = createSimpleResponse();
    		return;
    	}

    	NamedList<Object> groupResult = commonResponse();

    	if (mFormat == Grouping.Format.SIMPLE) {
    		groupResult.add("doclist", createSimpleResponse());
    		return;
    	}

    	List<Object> groupList = new ArrayList<Object>();
    	groupResult.add("groups", groupList); // grouped={ key={ groups=[

    	if (mResult == null) 
    		return;

    	// handle case of rows=0
    	if (mNumGroups == 0) 
    		return;

    	for (GroupDocs<MutableValue> group : mResult.getGroupDocs()) {
    		NamedList<Object> nl = new NamedMap<Object>();
    		groupList.add(nl);  // grouped={ key={ groups=[ {
    		
    		nl.add("groupValue", group.getGroupValue().toObject());
    		addDocList(nl, group);
    	}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMatches() {
    	if (mResult == null && mFallBackCollector == null) 
    		return 0;

    	return mResult != null ? mResult.getTotalHitCount() : 
    		mFallBackCollector.getTotalHits();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Integer getNumberOfGroups() {
    	return mAllGroupsCollector == null ? null : 
    		mAllGroupsCollector.getGroupCount();
    }

}
