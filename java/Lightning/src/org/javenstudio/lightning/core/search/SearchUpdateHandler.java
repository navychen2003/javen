package org.javenstudio.lightning.core.search;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.ISearchResponse;
import org.javenstudio.falcon.search.handler.UpdateHandler;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class SearchUpdateHandler extends SearchHandlerBase {

	public static RequestHandler createHandler(SearchCore core) { 
		return new SearchUpdateHandler(core);
	}
	
	private final UpdateHandler mHandler;

	private SearchUpdateHandler(SearchCore core) { 
		mHandler = new UpdateHandler(core);
	}
	
	@Override
	public void init(NamedList<?> args) throws ErrorException {
		super.init(args);
		mHandler.init(args);
	}
  
	@Override
	public void handleRequestBody(Request req, Response rsp) 
			throws ErrorException { 
		checkAuth(req, IUserClient.Op.ACCESS);
		mHandler.handleRequestBody((ISearchRequest)req, (ISearchResponse)rsp);
	}
  
}
