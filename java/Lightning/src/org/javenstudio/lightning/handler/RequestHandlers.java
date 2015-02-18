package org.javenstudio.lightning.handler;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.PluginInfo;
import org.javenstudio.falcon.util.PluginInfoInitialized;

public abstract class RequestHandlers {
	private static final Logger LOG = Logger.getLogger(RequestHandlers.class);
	
	// Use a synchronized map - since the handlers can be changed at runtime, 
	// the map implementation should be thread safe
	private final Map<String, RequestHandler> mHandlers =
			new ConcurrentHashMap<String,RequestHandler>() ;

	public RequestHandlers() {}
	
	/**
	 * Trim the trailing '/' if its there, and convert null to empty string.
	 * 
	 * we want:
	 *  /update/csv   and
	 *  /update/csv/
	 * to map to the same handler 
	 * 
	 */
	private static String normalize(String p) {
		if (p == null) return "";
		if (p.endsWith( "/" ) && p.length() > 1)
			return p.substring(0, p.length()-1);
		
		return p;
	}
	
	/**
	 * @return the RequestHandler registered at the given name 
	 */
	public RequestHandler get(String handlerName) {
		return mHandlers.get(normalize(handlerName));
	}

	/**
	 * @return a Map of all registered handlers of the specified type.
	 */
	public Map<String,RequestHandler> getAll(Class<?> clazz) {
		Map<String,RequestHandler> result = new HashMap<String,RequestHandler>(7);
		for (Map.Entry<String,RequestHandler> e : mHandlers.entrySet()) {
			if (clazz.isInstance(e.getValue())) 
				result.put(e.getKey(), e.getValue());
		}
		return result;
	}
	
	/**
	 * Handlers must be initialized before calling this function.  As soon as this is
	 * called, the handler can immediately accept requests.
	 * 
	 * This call is thread safe.
	 * 
	 * @return the previous handler at the given path or null
	 */
	public final RequestHandler register(String handlerName, RequestHandler handler) 
			throws ErrorException {
		String norm = normalize(handlerName);
		if (handler == null) 
			return mHandlers.remove(norm);
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("register handler: " + handlerName + " -> " 
					+ handler.getClass().getName());
		}
		
		RequestHandler old = mHandlers.put(norm, handler);
		onRegistered(norm, handler, old);
		
		return old;
	}

	protected void onRegistered(String handlerName, RequestHandler handler, 
			RequestHandler old) throws ErrorException { 
		// do nothing
	}
	
	/**
	 * Returns an unmodifiable Map containing the registered handlers
	 */
	public Map<String,RequestHandler> getRequestHandlers() {
		return Collections.unmodifiableMap(mHandlers);
	}
  
	public abstract RequestHandler createRequestHandler(PluginInfo info) 
			throws ErrorException;
	
	/**
	 * Read config.xml and register the appropriate handlers
	 * 
	 * This function should <b>only</b> be called from the Core constructor.  It is
	 * not intended as a public API.
	 * 
	 * While the normal runtime registration contract is that handlers MUST be initialized
	 * before they are registered, this function does not do that exactly.
	 *
	 * This function registers all handlers first and then calls init() for each one.
	 *
	 * This is OK because this function is only called at startup and there is no chance that
	 * a handler could be asked to handle a request before it is initialized.
	 * 
	 * The advantage to this approach is that handlers can know what path they are registered
	 * to and what other handlers are available at startup.
	 * 
	 * Handlers will be registered and initialized in the order they appear in config.xml
	 */
	public void initHandlers(List<PluginInfo> pluginInfos) throws ErrorException {
		// use link map so we iterate in the same order
		Map<String, PluginInfo> handlerInfos = 
				new LinkedHashMap<String, PluginInfo>();
		
		for (PluginInfo info : pluginInfos) {
			try {
				RequestHandler requestHandler = createRequestHandler(info);
				if (requestHandler == null) 
					continue;
				
				handlerInfos.put(info.getName(), info);
				RequestHandler old = register(info.getName(), requestHandler);
				if (old != null && LOG.isWarnEnabled()) {
					LOG.warn("Multiple requestHandler registered to the same name: " + info.getName() 
							+ " ignoring: " + old.getClass().getName());
				}
				
				if (info.isDefault()) {
					old = register("", requestHandler);
					if (old != null && LOG.isWarnEnabled()) {
						LOG.warn("Multiple default requestHandler registered ignoring: " 
								+ old.getClass().getName()); 
					}
				}
				
				if (LOG.isInfoEnabled())
					LOG.info("created " + info.getName() + ": " + info.getClassName());
				
			} catch (Exception ex) {
				if (ex instanceof ErrorException) {
					throw (ErrorException)ex; 
				} else {
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							"RequestHandler init failure", ex);
				}
			}
		}

		// we've now registered all handlers, time to init them in the same order
		for (Map.Entry<String,RequestHandler> entry : mHandlers.entrySet()) {
			String name = entry.getKey();
			RequestHandler handler = entry.getValue();
			PluginInfo info = handlerInfos.get(name);
			
			if (info != null) { 
				if (LOG.isDebugEnabled()) {
					LOG.debug("initHandler: name=" + name + " handler=" + handler.getClass().getName() 
							+ " with PluginInfo: " + info);
				}
				
				if (handler instanceof PluginInfoInitialized) 
					((PluginInfoInitialized) handler).init(info);
				else
					handler.init(info.getInitArgs());
				
			} else { 
				if (LOG.isDebugEnabled()) {
					LOG.debug("initHandler: name=" + name + " handler=" + handler.getClass().getName() 
							+ " with empty args");
				}
				
				handler.init(new NamedList<Object>());
			}
		}
	}
	
}
