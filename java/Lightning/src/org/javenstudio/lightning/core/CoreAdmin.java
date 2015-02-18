package org.javenstudio.lightning.core;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContextLoader;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.request.RequestAcceptor;
import org.javenstudio.lightning.request.RequestInput;
import org.javenstudio.lightning.request.RequestParsers;
import org.javenstudio.lightning.response.Response;
import org.javenstudio.lightning.response.ResponseOutput;
import org.javenstudio.lightning.response.ResponseWriter;
import org.javenstudio.lightning.response.ResponseWriters;

public final class CoreAdmin implements RequestAcceptor {
	private static final Logger LOG = Logger.getLogger(CoreAdmin.class);

	private final CoreContainers mContainers;
	private final CoreFactory mFactory;
	private final ContextLoader mLoader;
	private final CoreAdminConfig mConfig;
	private final CoreAdminHandlers mHandlers;
	private final RequestParsers mParsers;
	private final ResponseWriters mWriters;
	
	public CoreAdmin(CoreContainers containers, 
			CoreFactory factory, ContextLoader loader, 
			CoreAdminConfig config) throws ErrorException { 
		mContainers = containers;
		mFactory = factory;
		mLoader = loader;
		mConfig = config;
		mHandlers = new CoreAdminHandlers(this);
		mParsers = new RequestParsers(
				factory.createRequestConfig(config.getConfig()));
		mWriters = new ResponseWriters();
	}
	
	public final CoreContainers getContainers() { return mContainers; }
	public final CoreFactory getFactory() { return mFactory; }
	
	@Override
	public final ContextLoader getContextLoader() { 
		return mLoader; 
	}
	
	public boolean checkAdminPath(RequestInput input) throws ErrorException { 
		return input.checkAdminPath(mConfig.getAdminPath());
	}
	
	@Override
	public Request parseRequest(RequestInput input) throws ErrorException { 
		return mParsers.parseRequest(this, input);
	}
	
	@Override
	public Response createResponse(Request request, ResponseOutput output) throws ErrorException { 
		return mFactory.createAdminResponse(this, request, output);
	}
	
	@Override
	public ResponseWriter getResponseWriter(Request request) throws ErrorException { 
		return mWriters.getWriter(request.getResponseWriterType());
	}
	
	@Override
	public RequestHandler getRequestHandler(Request request) throws ErrorException { 
		RequestInput input = request.getRequestInput();
    	String handlerName = input.getAdminHandlerName(request.getParams());
    	
    	if (LOG.isDebugEnabled()) {
    		LOG.debug("request admin path: " + input.getQueryPath() 
    				+ " handlerName: " + handlerName);
    	}
    	
    	return mHandlers.get(handlerName);
	}
	
}
