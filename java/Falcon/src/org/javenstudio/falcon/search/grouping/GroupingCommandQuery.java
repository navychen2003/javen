package org.javenstudio.falcon.search.grouping;

import java.io.IOException;

import org.javenstudio.common.indexdb.ICollector;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.ITopDocs;
import org.javenstudio.common.indexdb.search.TopDocsCollector;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.hornet.grouping.GroupDocs;
import org.javenstudio.hornet.search.AdvancedSort;
import org.javenstudio.hornet.search.collector.TopFieldCollector;
import org.javenstudio.hornet.search.collector.TopScoreDocCollector;
import org.javenstudio.falcon.search.hits.DocSet;
import org.javenstudio.falcon.search.hits.FilterCollector;

/**
 * A group command for grouping on a query.
 */
// NOTE: doesn't need to be generic. 
// Maybe Command interface --> First / Second pass abstract impl.
public class GroupingCommandQuery extends GroupingCommand<Object> {

    protected IQuery mQuery;
    protected TopDocsCollector<?> mTopCollector;
    protected FilterCollector mCollector;

    public GroupingCommandQuery(Grouping grouping) { 
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
    	DocSet groupFilt = mGrouping.mSearcher.getDocSet(mQuery);
    	
    	mTopCollector = newCollector(mGroupSort, mGrouping.mNeedScores);
    	mCollector = new FilterCollector(groupFilt, mTopCollector);
    	
    	return mCollector;
    }

    protected TopDocsCollector<?> newCollector(ISort sort, boolean needScores) throws ErrorException {
    	int groupDocsToCollect = mGrouping.getMax(mGroupOffset, mDocsPerGroup, mGrouping.mMaxDoc);
    	if (sort == null || sort == AdvancedSort.RELEVANCE) 
    		return TopScoreDocCollector.create(groupDocsToCollect, true);
    	
    	try {
    		return TopFieldCollector.create(
    				mGrouping.mSearcher.weightSort(sort), groupDocsToCollect, 
    				false, needScores, needScores, true);
    		
    	} catch (IOException ex) { 
    		throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
    	}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void finish() throws ErrorException {
    	TopDocsCollector<?> topDocsCollector = (TopDocsCollector<?>) mCollector.getDelegate();
    	ITopDocs topDocs = topDocsCollector.getTopDocs();
    	
    	GroupDocs<?> groupDocs = new GroupDocs<String>(Float.NaN, 
    			topDocs.getMaxScore(), topDocs.getTotalHits(), topDocs.getScoreDocs(), 
    			mQuery.toString(), null);
    	
    	if (mIsMain) {
    		mGrouping.mMainResult = getDocList(groupDocs);
    		
    	} else {
    		NamedList<Object> rsp = commonResponse();
    		addDocList(rsp, groupDocs);
    	}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMatches() {
    	return mCollector.getMatches();
    }
    
}
