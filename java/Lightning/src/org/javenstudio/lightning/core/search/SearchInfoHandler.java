package org.javenstudio.lightning.core.search;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.ISearchResponse;
import org.javenstudio.falcon.search.handler.LukeRequestHandler;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class SearchInfoHandler extends SearchHandlerBase {

	public static RequestHandler createHandler(SearchCore core) { 
		return new SearchInfoHandler(core);
	}
	
	private final LukeRequestHandler mHandler;
	
	private SearchInfoHandler(SearchCore core) { 
		mHandler = new LukeRequestHandler(core);
	}
	
	@Override
	public void handleRequestBody(Request req, Response rsp) 
			throws ErrorException { 
		checkAuth(req, IUserClient.Op.ACCESS);
		mHandler.handleRequestBody((ISearchRequest)req, (ISearchResponse)rsp);
	}
	
}
