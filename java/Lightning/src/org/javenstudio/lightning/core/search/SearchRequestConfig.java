package org.javenstudio.lightning.core.search;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContextResource;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.lightning.request.DefaultRequestConfig;
import org.javenstudio.lightning.request.RequestAcceptor;
import org.javenstudio.lightning.request.RequestBase;
import org.javenstudio.lightning.request.RequestInput;

public class SearchRequestConfig extends DefaultRequestConfig {

	public SearchRequestConfig(ContextResource config) throws ErrorException { 
		super(config);
	}
	
	@Override
	protected RequestBase createRequest(RequestAcceptor acceptor, RequestInput input, 
			Params params) throws ErrorException { 
		if (acceptor instanceof SearchCore) {
			return new SearchRequest((SearchCore)acceptor, input, params);
			
		} else { 
			return super.createRequest(acceptor, input, params);
		}
	}
	
}
