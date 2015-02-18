package org.javenstudio.lightning.core.search;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.search.Searcher;
import org.javenstudio.falcon.search.SearcherRef;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.SearchResolver;
import org.javenstudio.falcon.search.ISearchResponse;
import org.javenstudio.lightning.request.QueryRequest;
import org.javenstudio.lightning.request.RequestInput;
import org.javenstudio.lightning.response.Response;

public class SearchRequest extends QueryRequest implements ISearchRequest {
	static final Logger LOG = Logger.getLogger(SearchRequest.class);

	private SearcherRef mSearcherRef = null;
	
	public SearchRequest(SearchCore core, RequestInput input, Params params) {
		super(core, input, params);
	}
	
	@Override
	public final SearchCore getSearchCore() { 
		return (SearchCore)getCore();
	}
	
	@Override
	public synchronized Searcher getSearcher() throws ErrorException { 
		if (mSearcherRef == null) {
			if (LOG.isDebugEnabled())
				LOG.debug("getSearcher for request");
			
			mSearcherRef = getSearchCore().getSearcherRef();
		}
		
		return mSearcherRef.get();
	}
	
	@Override
	public synchronized void close() throws ErrorException { 
		if (mSearcherRef != null) { 
			if (LOG.isDebugEnabled())
				LOG.debug("close Searcher from request");
			
			mSearcherRef.decreaseRef();
			mSearcherRef = null;
		}
		
		super.close();
	}
	
	@Override
	public NamedList<Object> getParsedResponse(Response rsp) throws ErrorException { 
		return SearchResolver.getParsedResponse(this, (ISearchResponse)rsp);
	}
	
}
