package org.javenstudio.lightning.request;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.response.Response;
import org.javenstudio.lightning.response.ResponseOutput;
import org.javenstudio.lightning.response.ResponseWriter;

public abstract class RequestDispatcher {
	private static final Logger LOG = Logger.getLogger(RequestDispatcher.class);
	
	public abstract boolean doFilter(RequestInput input, ResponseOutput output);
	public abstract void destroy();
	
	public boolean dispatchRequest(RequestAcceptor acceptor, 
			RequestInput input, ResponseOutput output) throws ErrorException { 
		if (acceptor == null || input == null || output == null)
			return false;
		
		Request request = null; 
		RequestHandler handler = null; 
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("request path=" + input.getQueryPath() + " params=" + input.getQueryString());
			LOG.debug("  input=" + input);
			LOG.debug("  output=" + output);
		}
		
		try {
			request = acceptor.parseRequest(input);
			if (LOG.isDebugEnabled()) 
				LOG.debug("  request=" + request);
			
			handler = acceptor.getRequestHandler(request);
			if (LOG.isDebugEnabled()) 
				LOG.debug("  handler=" + handler);
			
			if (request != null && handler != null) {
				Response response = acceptor.createResponse(request, output);
				if (LOG.isDebugEnabled()) 
					LOG.debug("  response=" + response);
				
				try {
					handler.handleRequest(request, response);
				} catch (Throwable ex) { 
					if (LOG.isDebugEnabled())
						ex.printStackTrace(System.err);
					
					if (LOG.isErrorEnabled())
						LOG.error("handleRequest error: " + ex.toString(), ex);
					
					response.setException(ex);
				}
				
				if (!handler.handleResponse(request, response, output)) { 
					ResponseWriter writer = acceptor.getResponseWriter(request);
					if (LOG.isDebugEnabled())
						LOG.debug("  writer=" + writer.getClass().getName());
					
					response.setEndTime();
					writeResponse(request, response, output, writer);
				}
				
				return true;
			}
			
			return false;
		} finally { 
			if (request != null) 
				request.close();
		}
	}
	
	public void writeResponse(Request request, Response response, 
			ResponseOutput output, ResponseWriter writer) throws ErrorException { 
		// do nothing
	}
	
}
