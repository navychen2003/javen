package org.javenstudio.falcon.search.grouping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.javenstudio.common.indexdb.ICollector;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.document.Fieldable;
import org.javenstudio.common.indexdb.search.Sort;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.hornet.grouping.AbstractAllGroupHeadsCollector;
import org.javenstudio.hornet.grouping.GroupDocs;
import org.javenstudio.hornet.grouping.SearchGroup;
import org.javenstudio.hornet.grouping.collector.TermAllGroupHeadsCollector;
import org.javenstudio.hornet.grouping.collector.TermAllGroupsCollector;
import org.javenstudio.hornet.grouping.collector.TermFirstPassGroupingCollector;
import org.javenstudio.hornet.grouping.collector.TermSecondPassGroupingCollector;
import org.javenstudio.hornet.search.AdvancedSort;
import org.javenstudio.hornet.search.collector.MultiCollector;
import org.javenstudio.hornet.search.collector.TotalHitCountCollector;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.search.schema.SchemaFieldType;

/**
 * A group command for grouping on a field.
 */
public class GroupingCommandField extends GroupingCommand<BytesRef> {

	protected String mGroupBy;
	
	protected TermFirstPassGroupingCollector mFirstPass;
	protected TermSecondPassGroupingCollector mSecondPass;

	protected TermAllGroupsCollector mAllGroupsCollector;

    // If offset falls outside the number of documents a group can provide 
	// use this collector instead of secondPass
	protected TotalHitCountCollector mFallBackCollector;
	protected Collection<SearchGroup<BytesRef>> mTopGroups;

    public GroupingCommandField(Grouping grouping) { 
    	super(grouping);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void prepare() throws ErrorException {
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
    		mSort = mSort == null ? AdvancedSort.RELEVANCE : mSort;
    		mFirstPass = new TermFirstPassGroupingCollector(mGroupBy, mSort, mActualGroupsToFind);
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
    		mAllGroupsCollector = new TermAllGroupsCollector(mGroupBy);
    		return mTotalCount == Grouping.TotalCount.GROUPED ? mAllGroupsCollector : null;
    	}

    	mTopGroups = mFormat == Grouping.Format.GROUPED ? 
    			mFirstPass.getTopGroups(mOffset, false) : mFirstPass.getTopGroups(0, false);
    			
    	if (mTopGroups == null) {
    		if (mTotalCount == Grouping.TotalCount.GROUPED) {
    			mAllGroupsCollector = new TermAllGroupsCollector(mGroupBy);
    			mFallBackCollector = new TotalHitCountCollector();
    			
    			return MultiCollector.wrap(mAllGroupsCollector, mFallBackCollector);
    			
    		} else {
    			mFallBackCollector = new TotalHitCountCollector();
    			
    			return mFallBackCollector;
    		}
    	}

    	int groupedDocsToCollect = mGrouping.getMax(mGroupOffset, mDocsPerGroup, mGrouping.mMaxDoc);
    	groupedDocsToCollect = Math.max(groupedDocsToCollect, 1);
    	
    	try {
	    	mSecondPass = new TermSecondPassGroupingCollector(
	    			mGroupBy, mTopGroups, mSort, mGroupSort, groupedDocsToCollect, 
	    			mGrouping.mNeedScores, mGrouping.mNeedScores, false
	    			);
    	} catch (IOException ex) { 
    		throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
    	}

    	if (mTotalCount == Grouping.TotalCount.GROUPED) {
    		mAllGroupsCollector = new TermAllGroupsCollector(mGroupBy);
    		
    		return MultiCollector.wrap(mSecondPass, mAllGroupsCollector);
    	}
    	
    	return mSecondPass;
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractAllGroupHeadsCollector<?> createAllGroupCollector() throws ErrorException {
    	ISort sortWithinGroup = mGroupSort != null ? mGroupSort : new Sort();
    	
    	return TermAllGroupHeadsCollector.create(mGroupBy, sortWithinGroup);
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

    	for (GroupDocs<BytesRef> group : mResult.getGroupDocs()) {
    		NamedList<Object> nl = new NamedMap<Object>();
    		groupList.add(nl); 	// grouped={ key={ groups=[ {

    		// To keep the response format compatable with trunk.
    		// In trunk MutableValue can convert an indexed value to its native type. E.g. string to int
    		// The only option I currently see is the use the FieldType for this
    		if (group.getGroupValue() != null) {
    			SchemaField schemaField = mGrouping.mSearcher.getSchema().getField(mGroupBy);
    			SchemaFieldType fieldType = schemaField.getType();
    			
    			String readableValue = fieldType.indexedToReadable(group.getGroupValue().utf8ToString());
    			Fieldable field = schemaField.createField(readableValue, 1.0f);
    			
    			nl.add("groupValue", fieldType.toObject(field));
    		} else {
    			nl.add("groupValue", null);
    		}

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

      return (mResult != null) ? mResult.getTotalHitCount() 
    		  : mFallBackCollector.getTotalHits();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Integer getNumberOfGroups() {
    	return (mAllGroupsCollector == null) ? null : 
    		mAllGroupsCollector.getGroupCount();
    }
    
}
