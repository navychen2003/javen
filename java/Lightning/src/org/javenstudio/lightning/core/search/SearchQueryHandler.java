package org.javenstudio.lightning.core.search;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.PluginHolder;
import org.javenstudio.falcon.util.PluginInfo;
import org.javenstudio.falcon.util.PluginInfoInitialized;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.ISearchResponse;
import org.javenstudio.falcon.search.handler.SearchHandler;

public class SearchQueryHandler extends SearchHandlerBase 
		implements PluginInfoInitialized { 

	public static RequestHandler createHandler(SearchCore core) { 
		return new SearchQueryHandler(core);
	}
	
	private final SearchHandler mHandler;

	public SearchQueryHandler(PluginHolder core) { 
		this((SearchCore)core);
	}
	
	private SearchQueryHandler(SearchCore core) { 
		mHandler = new SearchHandler(core);
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

	@Override
	public void init(PluginInfo info) throws ErrorException {
		super.init(info.getInitArgs());
		mHandler.init(info);
	}
	
	@Override
	public String getMBeanDescription() {
	    StringBuilder sb = new StringBuilder();
	    sb.append("Search using components: ");
	    sb.append(mHandler.getComponents().getDescription());
	    return sb.toString();
	}
	
}
