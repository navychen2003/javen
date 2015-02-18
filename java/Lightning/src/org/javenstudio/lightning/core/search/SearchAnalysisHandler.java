package org.javenstudio.lightning.core.search;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.ISearchResponse;
import org.javenstudio.falcon.search.handler.FieldAnalysisHandler;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class SearchAnalysisHandler extends SearchHandlerBase {

	public static RequestHandler createHandler(SearchCore core) { 
		return new SearchAnalysisHandler(core);
	}
	
	private final FieldAnalysisHandler mHandler;
	
	private SearchAnalysisHandler(SearchCore core) { 
		mHandler = new FieldAnalysisHandler(core);
	}
	
	@Override
	public void handleRequestBody(Request req, Response rsp) 
			throws ErrorException { 
		checkAuth(req, IUserClient.Op.ACCESS);
		mHandler.handleRequestBody((ISearchRequest)req, (ISearchResponse)rsp);
	}
	
}
