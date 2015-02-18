package org.javenstudio.lightning.request;

import java.util.List;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContentStream;
import org.javenstudio.falcon.util.ContextResource;
import org.javenstudio.falcon.util.Params;

public class RequestConfig {

	public RequestConfig(ContextResource config) throws ErrorException { 
		// do nothing
	}
	
	public void initParsers(RequestParsers parsers) throws ErrorException { 
		// do nothing
	}
	
	public Request buildRequest(RequestAcceptor acceptor, RequestInput input, 
			Params params, List<ContentStream> streams) throws ErrorException { 
		throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
				"buildRequest() not implemented");
	}
	
}
