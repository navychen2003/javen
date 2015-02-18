package org.javenstudio.lightning.request;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContextLoader;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.response.Response;
import org.javenstudio.lightning.response.ResponseOutput;
import org.javenstudio.lightning.response.ResponseWriter;

public interface RequestAcceptor {
	
	public Request parseRequest(RequestInput input) throws ErrorException;
	public Response createResponse(Request request, ResponseOutput output) throws ErrorException;
	
	public RequestHandler getRequestHandler(Request request) throws ErrorException;
	public ResponseWriter getResponseWriter(Request request) throws ErrorException;
	
	public ContextLoader getContextLoader();
	
}
