package org.javenstudio.lightning.core;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;
import org.javenstudio.lightning.response.ResponseOutput;

/**
 * The <code>LazyRequestHandlerWrapper</core> wraps any {@link RequestHandler}.  
 * Rather then instanciate and initalize the handler on startup, this wrapper waits
 * until it is actually called.  This should only be used for handlers that are
 * unlikely to be used in the normal lifecycle.
 * 
 * You can enable lazy loading in config.xml using:
 * 
 * <pre>
 *  &lt;requestHandler name="..." class="..." startup="lazy"&gt;
 *    ...
 *  &lt;/requestHandler&gt;
 * </pre>
 * 
 * This is a private class - if there is a real need for it to be public, it could
 * move
 */
public class LazyRequestHandler implements RequestHandler {
	
	private final Core mCore;
	private String mClassName;
	private NamedList<?> mArgs;
	private RequestHandler mHandler;

	public LazyRequestHandler(Core core, String className, NamedList<?> args) {
		mCore = core;
		mClassName = className;
		mArgs = args;
		mHandler = null; // don't initialize
	}

	/**
	 * In normal use, this function will not be called
	 */
	@Override
	public void init(NamedList<?> args) {
		// do nothing
	}

	/**
	 * Wait for the first request before initializing the wrapped handler 
	 */
	@Override
	public void handleRequest(Request req, Response rsp) throws ErrorException {
		RequestHandler handler = mHandler;
		if (handler == null) 
			handler = getWrappedHandler();
		
		handler.handleRequest(req, rsp);
	}

	@Override
	public boolean handleResponse(Request req, Response rsp, 
			ResponseOutput output) throws ErrorException { 
		return false;
	}
	
	public synchronized RequestHandler getWrappedHandler() throws ErrorException {
		if (mHandler == null) {
			try {
				RequestHandler handler = mCore.createRequestHandler(mClassName);
				handler.init(mArgs);

				if (handler instanceof CoreAware) 
					((CoreAware)handler).inform(mCore);
				
				mHandler = handler;
			} catch (Exception ex) {
				if (ex instanceof ErrorException) 
					throw (ErrorException)ex;
				else 
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
			}
		}
		return mHandler;
	}

	public final String getHandlerClassName() {
		return mClassName;
	}

	@Override
	public String getMBeanKey() {
		return getClass().getName();
	}

	@Override
	public String getMBeanName() {
		return getClass().getName();
	}

	@Override
	public String getMBeanVersion() {
		return "1.0";
	}

	@Override
	public String getMBeanDescription() {
		return getClass().getName();
	}

	@Override
	public String getMBeanCategory() {
		return "QUERYHANDLER";
	}

	@Override
	public NamedList<?> getMBeanStatistics() {
		return null;
	}
	
}
