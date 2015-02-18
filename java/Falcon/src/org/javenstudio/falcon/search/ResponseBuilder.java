package org.javenstudio.falcon.search;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.IScoreDoc;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.util.RecursiveTimer;
import org.javenstudio.falcon.util.ResultItem;
import org.javenstudio.falcon.util.ResultList;
import org.javenstudio.hornet.grouping.SearchGroup;
import org.javenstudio.hornet.grouping.TopGroups;
import org.javenstudio.falcon.search.component.SearchComponent;
import org.javenstudio.falcon.search.component.SearchComponents;
import org.javenstudio.falcon.search.component.TermsResponse;
import org.javenstudio.falcon.search.facet.FacetInfo;
import org.javenstudio.falcon.search.grouping.GroupingSpecification;
import org.javenstudio.falcon.search.hits.DocListAndSet;
import org.javenstudio.falcon.search.hits.QueryCommand;
import org.javenstudio.falcon.search.hits.QueryResult;
import org.javenstudio.falcon.search.hits.ShardDoc;
import org.javenstudio.falcon.search.hits.SortSpec;
import org.javenstudio.falcon.search.query.QueryBuilder;
import org.javenstudio.falcon.search.shard.ShardRequest;
import org.javenstudio.falcon.search.stats.StatsInfo;
import org.javenstudio.falcon.search.transformer.QueryFieldResult;

/**
 * This class is experimental and will be changing in the future.
 *
 * @since 1.3
 */
public class ResponseBuilder {
	
  	//////////////////////////////////////////////////////////
  	//////////////////////////////////////////////////////////
  	//// Distributed Search section
  	//////////////////////////////////////////////////////////
  	//////////////////////////////////////////////////////////

  	public static final String FIELD_SORT_VALUES = "fsv";
  	public static final String SHARDS = "shards";
  	public static final String IDS = "ids";

  	/**
  	 * public static final String NUMDOCS = "nd";
  	 * public static final String DOCFREQS = "tdf";
  	 * public static final String TERMS = "terms";
  	 * public static final String EXTRACT_QUERY_TERMS = "eqt";
  	 * public static final String LOCAL_SHARD = "local";
  	 * public static final String DOC_QUERY = "dq";
  	 */

  	public static int STAGE_START = 0;
  	public static int STAGE_PARSE_QUERY = 1000;
  	public static int STAGE_TOP_GROUPS = 1500;
  	public static int STAGE_EXECUTE_QUERY = 2000;
  	public static int STAGE_GET_FIELDS = 3000;
  	public static int STAGE_DONE = Integer.MAX_VALUE;
	
  	private final ISearchRequest mRequest;
  	private final ISearchResponse mResponse;
  	
  	// Context fields for grouping
  	private final Map<String, Collection<SearchGroup<BytesRef>>> mMergedSearchGroups = 
  			new HashMap<String, Collection<SearchGroup<BytesRef>>>();
  	
  	private final Map<String, Integer> mMergedGroupCounts = 
  			new HashMap<String, Integer>();
  	
  	private final Map<String, Map<SearchGroup<BytesRef>, Set<String>>> mSearchGroupToShards = 
  			new HashMap<String, Map<SearchGroup<BytesRef>, Set<String>>>();
  	
  	private final Map<String, TopGroups<BytesRef>> mMergedTopGroups = 
  			new HashMap<String, TopGroups<BytesRef>>();
  	
  	private final Map<String, QueryFieldResult> mMergedQueryCommandResults = 
  			new HashMap<String, QueryFieldResult>();
  	
  	private final Map<Object, ResultItem> mRetrievedDocuments = 
  			new HashMap<Object, ResultItem>();
  	
  	//private GlobalCollectionStat mGlobalCollectionStat;

  	private Map<Object, ShardDoc> mResultIds;
  	// Maps uniqueKeyValue to ShardDoc, which may be used to
  	// determine order of the doc or uniqueKey in the final
  	// returned sequence.
  	// Only valid after STAGE_EXECUTE_QUERY has completed.

  	// private... components that don't own these shouldn't use them 
  	private ResultList mResponseDocs;
  	private StatsInfo mStatsInfo;
  	private FacetInfo mFacetInfo;
  	private TermsResponse mTermsHelper;
  	
  	//private NamedMap<List<NamedList<Object>>> mPivots;
  	
  	// Hit count used when distributed grouping is performed.
  	private int mTotalHitCount; 
  	
  	// Used for timeAllowed parameter. First phase elapsed time is subtracted 
  	// from the time allowed for the second phase.
  	private int mFirstPhaseElapsedTime;

  	// requests to be sent
  	private List<ShardRequest> mOutgoingRequests; 
  	// requests that have received responses from all shards
  	private List<ShardRequest> mFinishedRequests; 

  	//The address of the Shard
  	private boolean mIsDistrib; // is this a distributed search?
  	
  	private String[] mShards;
  	private String[] mSlices; // the optional logical ids of the shards
  	
  	private int mShardsRows = -1;
  	private int mShardsStart = -1;
  	
  	private QueryBuilder mQueryBuilder = null;
  	private String mQueryString = null;
  	private IQuery mQuery = null;
  	
  	private List<IQuery> mFilters = null;
  	private SortSpec mSortSpec = null;
  	private GroupingSpecification mGroupingSpec;
  	
  	//used for handling deep paging
  	private IScoreDoc mScoreDoc;

  	private DocListAndSet mResults = null;
  	private NamedList<Object> mDebugInfo = null;
  	private RecursiveTimer mTimer = null;

  	private IQuery mHighlightQuery = null;
  	private SearchComponents mComponents;
  	//private RequestInfo mRequestInfo;

  	// What stage is this current request at?
  	private int mStage; 
  	
  	private boolean mDoHighlights;
  	private boolean mDoFacets;
  	private boolean mDoStats;
  	private boolean mDoTerms;

  	private boolean mNeedDocList = false;
  	private boolean mNeedDocSet = false;
  	private int mFieldFlags = 0;
  	//private boolean mDebug = false;
  	private boolean mDebugTimings; 
  	private boolean mDebugQuery; 
  	private boolean mDebugResults;
  	
  	public ResponseBuilder(ISearchRequest req, ISearchResponse rsp, 
  			SearchComponents components) {
  		mRequest = req;
  		mResponse = rsp;
  		mComponents = components;
  		//mRequestInfo = SearchRequestInfo.getRequestInfo();
  	}
  	
  	public int getFirstPhaseElapsedTime() { return mFirstPhaseElapsedTime; }
  	public void setFirstPhaseElapsedTime(int time) { mFirstPhaseElapsedTime = time; }
  	
  	public int getTotalHitCount() { return mTotalHitCount; }
  	public void setTotalHitCount(int count) { mTotalHitCount = count; }
  	
  	public List<ShardRequest> getOutgoingRequests() { return mOutgoingRequests; }
  	public List<ShardRequest> getFinishedRequests() { return mFinishedRequests; }
  	
  	public ResultList getResponseDocs() { return mResponseDocs; }
  	public void setResponseDocs(ResultList docs) { mResponseDocs = docs; }
  	
  	public ISearchRequest getRequest() { return mRequest; }
  	public ISearchResponse getResponse() { return mResponse; }
  
  	public boolean isDistributed() { return mIsDistrib; }
  	public void setDistributed(boolean b) { mIsDistrib = b; }
  	
  	public int getStage() { return mStage; }
  	public void setStage(int stage) { mStage = stage; }
  	
  	public Map<Object, ShardDoc> getResultIds() { return mResultIds; }
  	public void setResultIds(Map<Object, ShardDoc> ids) { mResultIds = ids; }
  	
  	public void setDoTerms(boolean b) { mDoTerms = b; }
  	public boolean isDoTerms() { return mDoTerms; }
  	
  	public void setDoFacets(boolean b) { mDoFacets = b; }
  	public boolean isDoFacets() { return mDoFacets; }
  	
  	public void setDoHighlights(boolean b) { mDoHighlights = b; }
  	public boolean isDoHighlights() { return mDoHighlights; }
  	
  	public void setDoStats(boolean b) { mDoStats = b; }
  	public boolean isDoStats() { return mDoStats; }
  
  	//public String[] getShards() { return mShards; }
  	public void setShards(String[] vals) { mShards = vals; }
  	
  	public int getShardCount() { return mShards != null ? mShards.length : 0; }
  	public String getShardAt(int index) { return mShards[index]; }
  	
  	//public String[] getSlices() { return mSlices; }
  	public void setSlices(String[] vals) { mSlices = vals; }
  	
  	public int getSliceCount() { return mSlices != null ? mSlices.length : 0; }
  	public String getSliceAt(int index) { return mSlices[index]; }
  	
  	public int getShardsStart() { return mShardsStart; }
  	public int getShardsRows() { return mShardsRows; }
  	
  	public FacetInfo getFacetInfo() { return mFacetInfo; }
  	public void setFacetInfo(FacetInfo info) { mFacetInfo = info; }
  	
  	public StatsInfo getStatsInfo() { return mStatsInfo; }
  	public void setStatsInfo(StatsInfo info) { mStatsInfo = info; }
  	
  	public TermsResponse getTermsHelper() { return mTermsHelper; }
  	public void setTermsHelper(TermsResponse helper) { mTermsHelper = helper; }

  	public boolean isDebugTimings() { return mDebugTimings; }
  	public void setDebugTimings(boolean debugTimings) { mDebugTimings = debugTimings; }

  	public boolean isDebugQuery() { return mDebugQuery; }
  	public void setDebugQuery(boolean debugQuery) { mDebugQuery = debugQuery; }

  	public boolean isDebugResults() { return mDebugResults; }
  	public void setDebugResults(boolean debugResults) { mDebugResults = debugResults; }

  	public NamedList<Object> getDebugInfo() { return mDebugInfo; }
  	public void setDebugInfo(NamedList<Object> debugInfo) { mDebugInfo = debugInfo; }

  	public int getFieldFlags() { return mFieldFlags; }
  	public void setFieldFlags(int fieldFlags) { mFieldFlags = fieldFlags; }

  	public List<IQuery> getFilters() { return mFilters; }
  	public void setFilters(List<IQuery> filters) { mFilters = filters; }

  	public IQuery getHighlightQuery() { return mHighlightQuery; }
  	public void setHighlightQuery(IQuery highlightQuery) { mHighlightQuery = highlightQuery; }

  	public boolean isNeedDocList() { return mNeedDocList; }
  	public void setNeedDocList(boolean needDocList) { mNeedDocList = needDocList; }

  	public boolean isNeedDocSet() { return mNeedDocSet; }
  	public void setNeedDocSet(boolean needDocSet) { mNeedDocSet = needDocSet; }

  	public QueryBuilder getQueryBuilder() { return mQueryBuilder; }
  	public void setQueryBuilder(QueryBuilder qparser) { mQueryBuilder = qparser; }

  	public String getQueryString() { return mQueryString; }
  	public void setQueryString(String qstr) { mQueryString = qstr; }

  	public IQuery getQuery() { return mQuery; }
  	public void setQuery(IQuery query) { mQuery = query; }

  	public DocListAndSet getResults() { return mResults; }
  	public void setResults(DocListAndSet results) { mResults = results; }

  	public SortSpec getSortSpec() { return mSortSpec; }
  	public void setSortSpec(SortSpec sort) { mSortSpec = sort; }

  	public GroupingSpecification getGroupingSpec() { return mGroupingSpec; }
  	public void setGroupingSpec(GroupingSpecification groupingSpec) { mGroupingSpec = groupingSpec; }

  	public boolean isGrouping() { return mGroupingSpec != null; }

  	public RecursiveTimer getTimer() { return mTimer; }
  	public void setTimer(RecursiveTimer timer) { mTimer = timer; }

  	public IScoreDoc getScoreDoc() { return mScoreDoc; }
  	public void setScoreDoc(IScoreDoc scoreDoc) { mScoreDoc = scoreDoc; }

  	public Map<String, TopGroups<BytesRef>> getMergedTopGroups() { return mMergedTopGroups; }
  	public Map<String, QueryFieldResult> getMergedQueryCommandResults() { return mMergedQueryCommandResults; }
  	//public Map<Object, ResultDocument> getRetrievedDocuments() { return mRetrievedDocuments; }
  	
  	public Integer getMergedGroupCount(String field) { 
  		return mMergedGroupCounts.get(field);
  	}
  	
  	public void addMergedGroupCount(String field, Integer value) { 
  		mMergedGroupCounts.put(field, value);
  	}
  	
  	public ResultItem getRetrievedDocument(Object id) { 
  		return mRetrievedDocuments.get(id);
  	}
  	
  	public void addRetrievedDocument(Object id, ResultItem doc) { 
  		mRetrievedDocuments.put(id, doc);
  	}
  	
  	public boolean isMergedTopGroupsEmpty() { 
  		return mMergedTopGroups.isEmpty();
  	}
  	
  	public void addMergedTopGroup(String field, TopGroups<BytesRef> groups) { 
  		mMergedTopGroups.put(field, groups);
  	}
  	
  	public Collection<TopGroups<BytesRef>> getMergedTopGroupsValues() { 
  		return mMergedTopGroups.values();
  	}
  	
  	public Collection<QueryFieldResult> getMergedQueryCommandResultsValues() { 
  		return mMergedQueryCommandResults.values();
  	}
  	
  	public void addMergedQueryCommandResult(String field, QueryFieldResult result) { 
  		mMergedQueryCommandResults.put(field, result);
  	}
  	
  	public Collection<String> getSearchGroupToShardsKeySet() { 
  		return mSearchGroupToShards.keySet();
  	}
  	
  	public boolean containsSearchGroupToShardsKey(String key) { 
  		return mSearchGroupToShards.containsKey(key);
  	}
  	
  	public void addSearchGroupToShard(String key, Map<SearchGroup<BytesRef>, Set<String>> value) { 
  		mSearchGroupToShards.put(key, value);
  	}
  	
  	public Map<SearchGroup<BytesRef>, Set<String>> getSearchGroupToShard(String key) { 
  		return mSearchGroupToShards.get(key);
  	}
  	
  	public Collection<String> getMergedSearchGroupsKeySet() { 
  		return mMergedSearchGroups.keySet();
  	}
  	
  	public Collection<SearchGroup<BytesRef>> getMergedSearchGroup(String field) { 
  		return mMergedSearchGroups.get(field);
  	}
  	
  	public Collection<Collection<SearchGroup<BytesRef>>> getMergedSearchGroupsValues() { 
  		return mMergedSearchGroups.values();
  	}
  	
  	public void addMergedSearchGroup(String field, Collection<SearchGroup<BytesRef>> value) { 
  		mMergedSearchGroups.put(field, value);
  	}
  	
  	public int getShardNum(String shard) {
  		for (int i = 0; i < mShards.length; i++) {
  			if (mShards[i] == shard || mShards[i].equals(shard)) 
  				return i;
  		}
  		return -1;
  	}

  	public void addRequest(SearchComponent me, ShardRequest sreq) throws ErrorException {
  		mOutgoingRequests.add(sreq);
  		
  		if ((sreq.getPurpose() & ShardRequest.PURPOSE_PRIVATE) == 0) 
  			mComponents.modifyRequest(this, me, sreq);
  	}
  	
  	/**
  	 * Utility function to add debugging info.  This will make sure a valid
  	 * debugInfo exists before adding to it.
  	 */
  	public void addDebugInfo(String name, Object val) {
  		if (mDebugInfo == null) 
  			mDebugInfo = new NamedMap<Object>();
  		
  		mDebugInfo.add(name, val);
  	}

  	@SuppressWarnings("unchecked")
	public void addDebug(Object val, String... path) {
  		if (mDebugInfo == null) 
  			mDebugInfo = new NamedMap<Object>();

  		NamedList<Object> target = mDebugInfo;
  		
  		for (int i=0; i < path.length-1; i++) {
  			String elem = path[i];
  			
  			NamedList<Object> newTarget = (NamedList<Object>)mDebugInfo.get(elem);
  			if (newTarget == null) {
  				newTarget = new NamedMap<Object>();
  				target.add(elem, newTarget);
  			}
  			
  			target = newTarget;
  		}

  		target.add(path[path.length-1], val);
  	}

  	public boolean isDebug() {
  		return mDebugQuery || mDebugTimings || mDebugResults;
  	}

  	/** @return true if all debugging options are on */
  	public boolean isDebugAll() {
  		return mDebugQuery && mDebugTimings && mDebugResults;
  	}
  
  	public void setDebug(boolean dbg){
  		mDebugQuery = dbg;
  		mDebugTimings = dbg;
  		mDebugResults = dbg;
  	}
  	
  	public static class GlobalCollectionStat {
  		
  		public final Map<String, Long> mDfMap;
  		public final long mNumDocs;

  		public GlobalCollectionStat(int numDocs, Map<String, Long> dfMap) {
  			mNumDocs = numDocs;
  			mDfMap = dfMap;
  		}
  	}

  	/**
  	 * Creates a Searcher.QueryCommand from this
  	 * ResponseBuilder.  TimeAllowed is left unset.
  	 */
  	public QueryCommand getQueryCommand() {
  		QueryCommand cmd = new QueryCommand();
  		
  		cmd.setQuery(getQuery())
  			.setFilterList(getFilters())
            .setSort(getSortSpec().getSort())
            .setOffset(getSortSpec().getOffset())
            .setLength(getSortSpec().getCount())
            .setFlags(getFieldFlags())
            .setNeedDocSet(isNeedDocSet())
            .setScoreDoc(getScoreDoc()); //Issue 1726
  		
  		return cmd;
  	}

  	/**
  	 * Sets results from a Searcher.QueryResult.
  	 */
  	public void setResult(QueryResult result) {
  		setResults(result.getDocListAndSet());
  		if (result.isPartialResults()) {
  			//rsp.getResponseHeader().add("partialResults", Boolean.TRUE);
  		}
  	}
  
  	public long getNumberDocumentsFound() {
  		if (mResponseDocs == null) 
  			return 0;
  		
  		return mResponseDocs.getNumFound();
  	}

  	public Searcher getSearcher() throws ErrorException { 
  		return mRequest.getSearcher();
  	}
  	
  	public ISearchCore getSearchCore() { 
  		return mRequest.getSearchCore();
  	}
  	
}
