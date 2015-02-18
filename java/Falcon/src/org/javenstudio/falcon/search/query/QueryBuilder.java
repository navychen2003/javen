package org.javenstudio.falcon.search.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.IScoreDoc;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.CommonParams;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.util.StrHelper;
import org.javenstudio.falcon.search.ISearchCore;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.hits.SortSpec;

/**
 * <b>Note: This API is experimental and may change in 
 * non backward-compatible ways in the future</b>
 * 
 */
public abstract class QueryBuilder {

	protected QueryBuilderFactory mFactory;
	
	protected String mStringIncludingLocalParams;   // the original query string including any local params
	protected boolean mValFollowedParams;           // true if the value "qstr" followed the localParams
	protected int mLocalParamsEnd;                  // the position one past where the localParams ended 
	
	protected String mQueryString;
	protected Params mParams;
	protected Params mLocalParams;
	protected ISearchRequest mRequest;
	protected int mRecurseCount;

	protected IQuery mQuery;

	/**
	 * Constructor for the QueryParser
	 * @param qstr The part of the query string specific to this parser
	 * @param localParams The set of parameters that are specific to this QueryParser. 
	 * @param params The rest of the {@link Params}
	 * @param req The original {@link Request}.
	 */
	public QueryBuilder(String qstr, Params localParams, Params params, 
			ISearchRequest req) throws ErrorException {
		mQueryString = qstr;
		mLocalParams = localParams;

		// insert tags into tagmap.
		// WARNING: the internal representation of tagged objects in the request context is
		// experimental and subject to change!
		if (localParams != null) {
			String tagStr = localParams.get(CommonParams.TAG);
			if (tagStr != null) {
				Map<Object,Object> context = req.getContextMap();
				
				@SuppressWarnings("unchecked")
				Map<Object,Collection<Object>> tagMap = (Map<Object, Collection<Object>>) 
					req.getContextMap().get("tags");
				
				if (tagMap == null) {
					tagMap = new HashMap<Object,Collection<Object>>();
					context.put("tags", tagMap);          
				}
				
				if (tagStr.indexOf(',') >= 0) {
					List<String> tags = StrHelper.splitSmart(tagStr, ',');
					for (String tag : tags) {
						addTag(tagMap, tag, this);
					}
					
				} else {
					addTag(tagMap, tagStr, this);
				}
			}
		}

		mParams = params;
		mRequest = req;
	}

	public ISearchCore getSearchCore() { 
		if (mFactory == null) 
			throw new NullPointerException("factory not set");
		
		return mFactory.getSearchCore();
	}
	
	private static void addTag(Map<Object,Collection<Object>> tagMap, 
			Object key, Object val) {
		Collection<Object> lst = tagMap.get(key);
		if (lst == null) {
			lst = new ArrayList<Object>(2);
			tagMap.put(key, lst);
		}
		lst.add(val);
	}

	/** 
	 * Create and return the <code>Query</code> object represented by <code>qstr</code>. 
	 * Null MAY be returned to signify
	 * there was no input (e.g. no query string) to parse.
	 * @see #getQuery()
	 */
	public abstract IQuery parse() throws ErrorException;

	public Params getLocalParams() { return mLocalParams; }
	public void setLocalParams(Params localParams) { mLocalParams = localParams; }

	public Params getParams() { return mParams; }
	public void setParams(Params params) { mParams = params; }

	public ISearchRequest getRequest() { return mRequest; }
	public void setRequest(ISearchRequest req) { mRequest = req; }

	public String getQueryString() { return mQueryString; }
	public void setQueryString(String s) { mQueryString = s; }
	
	public boolean isFollowedParams() { return mValFollowedParams; }
	public int getLocalParamsEnd() { return mLocalParamsEnd; }

	/**
	 * Returns the resulting query from this QueryParser, calling parse() only the
	 * first time and caching the Query result.
	 */
	public IQuery getQuery() throws ErrorException {
		if (mQuery == null) {
			mQuery = parse();

			if (mLocalParams != null) {
				String cacheStr = mLocalParams.get(CommonParams.CACHE);
				if (cacheStr != null) {
					if (CommonParams.FALSE.equals(cacheStr)) {
						getExtendedQuery().setCache(false);
					} else if (CommonParams.TRUE.equals(cacheStr)) {
						getExtendedQuery().setCache(true);
					} else if ("sep".equals(cacheStr)) {
						getExtendedQuery().setCacheSep(true);
					}
				}

				int cost = mLocalParams.getInt(CommonParams.COST, Integer.MIN_VALUE);
				if (cost != Integer.MIN_VALUE) {
					getExtendedQuery().setCost(cost);
				}
			}
		}
		
		return mQuery;
	}

	// returns an extended query (and sets "query" to a new wrapped query if necessary)
	private ExtendedQuery getExtendedQuery() {
		if (mQuery instanceof ExtendedQuery) {
			return (ExtendedQuery)mQuery;
			
		} else {
			WrappedQuery wq = new WrappedQuery(mQuery);
			mQuery = wq;
			return wq;
		}
	}

	private void checkRecurse() throws ErrorException {
		if (mRecurseCount++ >= 100) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Infinite Recursion detected parsing query '" + mQueryString + "'");
		}
	}

	// TODO: replace with a Params that defaults to checking localParams first?
	// ideas..
	//   create params that satisfy field-specific overrides
	//   overrideable syntax $x=foo  (set global for limited scope) (invariants & security?)
	//                       $x+=foo (append to global for limited scope)

	/** check both local and global params */
	public String getParam(String name) throws ErrorException {
		String val;
		if (mLocalParams != null) {
			val = mLocalParams.get(name);
			if (val != null) 
				return val;
		}
		return mParams.get(name);
	}

	/** Create a new QueryParser for parsing an embedded sub-query */
	public QueryBuilder subQuery(String q, String defaultType) throws ErrorException {
		checkRecurse();
		if (defaultType == null && mLocalParams != null) {
			// if not passed, try and get the defaultType from local params
			defaultType = mLocalParams.get(QueryParsing.DEFTYPE);
		}
		
		QueryBuilder nestedParser = mFactory.getQueryBuilder(q, defaultType, getRequest());
		nestedParser.mRecurseCount = mRecurseCount;
		mRecurseCount--;
		
		return nestedParser;
	}

	/**
	 * use common params to look up pageScore and pageDoc in global params
	 * @return the ScoreDoc
	 */
	public IScoreDoc getPaging() throws ErrorException {
		return null;

	    /*** This is not ready for prime-time... see BUG-1726
	
	    String pageScoreS = null;
	    String pageDocS = null;
	
	    pageScoreS = params.get(CommonParams.PAGESCORE);
	    pageDocS = params.get(CommonParams.PAGEDOC);
	
	    if (pageScoreS == null || pageDocS == null)
	      return null;
	
	    int pageDoc = pageDocS != null ? Integer.parseInt(pageDocS) : -1;
	    float pageScore = pageScoreS != null ? new Float(pageScoreS) : -1;
	    if(pageDoc != -1 && pageScore != -1){
	      return new ScoreDoc(pageDoc, pageScore);
	    }
	    else {
	      return null;
	    }
	
	    ***/
	}
  
	/**
	 * @param useGlobalParams look up sort, start, rows in global params 
	 * if not in local params
	 * @return the sort specification
	 */
	public SortSpec getSort(boolean useGlobalParams) throws ErrorException {
		getQuery(); // ensure query is parsed first

		String sortStr = null;
		String startS = null;
		String rowsS = null;

		if (mLocalParams != null) {
			sortStr = mLocalParams.get(CommonParams.SORT);
			startS = mLocalParams.get(CommonParams.START);
			rowsS = mLocalParams.get(CommonParams.ROWS);

			// if any of these parameters are present, don't go back to the global params
			if (sortStr != null || startS != null || rowsS != null) 
				useGlobalParams = false;
		}

		if (useGlobalParams) {
			if (sortStr == null) 
				sortStr = mParams.get(CommonParams.SORT);
      
			if (startS == null) 
				startS = mParams.get(CommonParams.START);
      
			if (rowsS == null) 
				rowsS = mParams.get(CommonParams.ROWS);
		}

		int start = startS != null ? Integer.parseInt(startS) : 0;
		int rows = rowsS != null ? Integer.parseInt(rowsS) : 10;

		ISort sort = null;
		if (sortStr != null) 
			sort = QueryParsing.parseSort(sortStr, mRequest, mFactory);
    
		return new SortSpec(sort, start, rows);
	}

	public String[] getDefaultHighlightFields() {
		return new String[]{};
	}

	public IQuery getHighlightQuery() throws ErrorException {
		IQuery query = getQuery();
		
		return query instanceof WrappedQuery ? 
				((WrappedQuery)query).getWrappedQuery() : query;
	}

	public void addDebugInfo(NamedList<Object> debugInfo) throws ErrorException {
		debugInfo.add("QueryParser", this.getClass().getSimpleName());
	}

}
