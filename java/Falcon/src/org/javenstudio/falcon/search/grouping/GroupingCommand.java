package org.javenstudio.falcon.search.grouping;

import java.util.ArrayList;
import java.util.List;

import org.javenstudio.common.indexdb.ICollector;
import org.javenstudio.common.indexdb.IScoreDoc;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ArrayHelper;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.hornet.grouping.AbstractAllGroupHeadsCollector;
import org.javenstudio.hornet.grouping.GroupDocs;
import org.javenstudio.hornet.grouping.TopGroups;
import org.javenstudio.falcon.search.hits.DocIterator;
import org.javenstudio.falcon.search.hits.DocList;
import org.javenstudio.falcon.search.hits.DocSlice;

/**
 * General group command. A group command is responsible for 
 * creating the first and second pass collectors.
 * A group command is also responsible for creating the response structure.
 * <p/>
 * Note: Maybe the creating the response structure should be done 
 * in something like a ReponseBuilder???
 * Warning NOT thread save!
 */
public abstract class GroupingCommand<GT> {

	protected final Grouping mGrouping;
	
	protected Grouping.TotalCount mTotalCount = Grouping.TotalCount.UNGROUPED;
	protected Grouping.Format mFormat;
	protected TopGroups<GT> mResult;
	
	protected String mKey;       // the name to use for this group in the response
	protected ISort mGroupSort;  // the sort of the documents *within* a single group.
	protected ISort mSort;       // the sort between groups
	
	protected int mDocsPerGroup; // how many docs in each group - from "group.limit" param, default=1
	protected int mGroupOffset;  // the offset within each group (for paging within each group)
	protected int mNumGroups;    // how many groups - defaults to the "rows" parameter
	protected int mActualGroupsToFind; // How many groups should actually be found. Based on groupOffset and numGroups.
	protected int mOffset;       // offset into the list of groups
	
	protected boolean mIsMain;   // use as the main result in simple format (grouped.main=true param)
	
    public GroupingCommand(Grouping grouping) { 
    	mGrouping = grouping;
    }

    /**
     * Prepare this <code>Command</code> for execution.
     *
     * @throws ErrorException If I/O related errors occur
     */
    protected abstract void prepare() throws ErrorException;

    /**
     * Returns one or more {@link Collector} instances that 
     * are needed to perform the first pass search.
     * If multiple Collectors are returned then these wrapped 
     * in a {@link MultiCollector}.
     *
     * @return one or more {@link Collector} instances that 
     * are need to perform the first pass search
     * @throws ErrorException If I/O related errors occur
     */
    protected abstract ICollector createFirstPassCollector() throws ErrorException;

    /**
     * Returns zero or more {@link Collector} instances that are needed to 
     * perform the second pass search.
     * In the case when no {@link Collector} instances are created 
     * <code>null</code> is returned.
     * If multiple Collectors are returned then these wrapped in a {@link MultiCollector}.
     *
     * @return zero or more {@link Collector} instances that are needed 
     * to perform the second pass search
     * @throws ErrorException If I/O related errors occur
     */
    protected ICollector createSecondPassCollector() throws ErrorException {
    	return null;
    }

    /**
     * Returns a collector that is able to return the most relevant document of all groups.
     * Returns <code>null</code> if the command doesn't support this type of collector.
     *
     * @return a collector that is able to return the most relevant document of all groups.
     * @throws ErrorException If I/O related errors occur
     */
    public AbstractAllGroupHeadsCollector<?> createAllGroupCollector() throws ErrorException {
    	return null;
    }

    /**
     * Performs any necessary post actions to prepare the response.
     *
     * @throws ErrorException If I/O related errors occur
     */
    protected abstract void finish() throws ErrorException;

    /**
     * Returns the number of matches for this <code>Command</code>.
     *
     * @return the number of matches for this <code>Command</code>
     */
    public abstract int getMatches();

    /**
     * Returns the number of groups found for this <code>Command</code>.
     * If the command doesn't support counting the groups <code>null</code> is returned.
     *
     * @return the number of groups found for this <code>Command</code>
     */
    protected Integer getNumberOfGroups() {
    	return null;
    }

	protected NamedList<Object> commonResponse() {
    	NamedList<Object> groupResult = new NamedMap<Object>();
    	mGrouping.mGrouped.add(mKey, groupResult);  // grouped={ key={

    	int matches = getMatches();
    	groupResult.add("matches", matches);
    	
    	if (mTotalCount == Grouping.TotalCount.GROUPED) {
    		Integer totalNrOfGroups = getNumberOfGroups();
    		groupResult.add("ngroups", totalNrOfGroups == null ? 0 : totalNrOfGroups);
    	}
    	
    	mGrouping.mMaxMatches = Math.max(mGrouping.mMaxMatches, matches);
    	
    	return groupResult;
    }

    protected DocList getDocList(GroupDocs<?> groups) {
    	int max = groups.getTotalHits();
    	int off = mGroupOffset;
    	int len = mDocsPerGroup;
    	
    	if (mFormat == Grouping.Format.SIMPLE) {
    		off = mOffset;
    		len = mNumGroups;
    	}
    	
    	int docsToCollect = mGrouping.getMax(off, len, max);

    	// TODO: implement a DocList impl that doesn't need to start at offset=0
    	int docsCollected = Math.min(docsToCollect, groups.getScoreDocsSize());

    	int ids[] = new int[docsCollected];
    	float[] scores = mGrouping.mNeedScores ? new float[docsCollected] : null;
    	
    	for (int i = 0; i < ids.length; i++) {
    		ids[i] = groups.getScoreDocAt(i).getDoc();
    		if (scores != null)
    			scores[i] = groups.getScoreDocAt(i).getScore();
    	}

    	float score = groups.getMaxScore();
    	mGrouping.mMaxScore = Math.max(mGrouping.mMaxScore, score);
    	
    	DocSlice docs = new DocSlice(off, Math.max(0, ids.length - off), 
    			ids, scores, groups.getTotalHits(), score);

    	if (mGrouping.mGetDocList) {
    		DocIterator iter = docs.iterator();
    		while (iter.hasNext()) {
    			mGrouping.mIdSet.add(iter.nextDoc());
    		}
    	}
    	
    	return docs;
	}

    protected void addDocList(NamedList<Object> rsp, GroupDocs<?> groups) {
    	rsp.add("doclist", getDocList(groups));
    }

    // Flatten the groups and get up offset + rows documents
    protected DocList createSimpleResponse() {
    	GroupDocs<?>[] groups = (mResult != null) ? mResult.getGroupDocs() : new GroupDocs[0];

    	List<Integer> ids = new ArrayList<Integer>();
    	List<Float> scores = new ArrayList<Float>();
    	
    	int docsToGather = mGrouping.getMax(mOffset, mNumGroups, mGrouping.mMaxDoc);
    	int docsGathered = 0;
    	
    	float maxScore = Float.NEGATIVE_INFINITY;

    	outer:
    	for (GroupDocs<?> group : groups) {
    		if (group.getMaxScore() > maxScore) 
    			maxScore = group.getMaxScore();
    		
    		for (IScoreDoc scoreDoc : group.getScoreDocs()) {
    			if (docsGathered >= docsToGather) 
    				break outer;

    			ids.add(scoreDoc.getDoc());
    			scores.add(scoreDoc.getScore());
    			
    			docsGathered ++;
    		}
    	}

    	int len = docsGathered > mOffset ? docsGathered - mOffset : 0;
    	int[] docs = ArrayHelper.toPrimitive(ids.toArray(new Integer[ids.size()]));
    	float[] docScores = ArrayHelper.toPrimitive(scores.toArray(new Float[scores.size()]));
    	
    	DocSlice docSlice = new DocSlice(mOffset, len, docs, 
    			docScores, getMatches(), maxScore);

    	if (mGrouping.mGetDocList) {
    		for (int i = mOffset; i < docs.length; i++) {
    			mGrouping.mIdSet.add(docs[i]);
    		}
    	}

    	return docSlice;
    }

}
