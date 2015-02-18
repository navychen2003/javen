package org.javenstudio.lightning.core;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.InfoMBean;
import org.javenstudio.falcon.util.PluginInfo;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.handler.RequestHandlers;
import org.javenstudio.lightning.handler.system.CorePingHandler;
import org.javenstudio.lightning.handler.system.InfoMBeanHandler;
import org.javenstudio.lightning.handler.system.LoggingHandler;
import org.javenstudio.lightning.handler.system.PluginInfoHandler;
import org.javenstudio.lightning.handler.system.PropertiesHandler;
import org.javenstudio.lightning.handler.system.ShowFileHandler;
import org.javenstudio.lightning.handler.system.SystemInfoHandler;
import org.javenstudio.lightning.handler.system.ThreadDumpHandler;

public class CoreRequestHandlers extends RequestHandlers {
	private static final Logger LOG = Logger.getLogger(CoreRequestHandlers.class);
	
	private final Core mCore;
	
	public CoreRequestHandlers(Core core) throws ErrorException { 
		mCore = core;
		core.getFactory().registerCoreHandlers(this, core);
	}
	
	public static void registerHandlers(RequestHandlers handlers, Core core) 
			throws ErrorException {
		handlers.register("/admin/system", new SystemInfoHandler(core));
		handlers.register("/admin/properties", new PropertiesHandler());
		handlers.register("/admin/threads", new ThreadDumpHandler());
		handlers.register("/admin/logging", new LoggingHandler(core.getLogWatcher()));
		handlers.register("/admin/file", new ShowFileHandler(core));
		handlers.register("/admin/ping", new CorePingHandler(core));
		handlers.register("/admin/plugins", new PluginInfoHandler(core));
		handlers.register("/admin/mbeans", new InfoMBeanHandler(core));
	}
	
	public void initHandlers(CoreConfig config) throws ErrorException {
		Core core = mCore;
		CoreFactory factory = core.getFactory();
		
		initHandlers(config.getPluginInfos(RequestHandler.class.getName()));
		factory.onCoreHandlersInited(this, core);
	}
	
	@Override
	public RequestHandler createRequestHandler(PluginInfo info) throws ErrorException { 
		RequestHandler requestHandler;
		
		String startup = info.getAttribute("startup") ;
		if (startup != null) {
			if ("lazy".equals(startup)) {
				if (LOG.isInfoEnabled())
					LOG.info("adding lazy requestHandler: " + info.getClassName());
				
				requestHandler = new LazyRequestHandler(mCore, 
						info.getClassName(), info.getInitArgs());
				
			} else {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"Unknown startup value: '" + startup + "' for: " + info.getClassName());
			}
		} else {
			if (LOG.isDebugEnabled())
				LOG.debug("create requestHandler: " + info.getClassName());
			
			requestHandler = mCore.createRequestHandler(info.getClassName());
		}
		
		return requestHandler;
	}
	
	@Override
	protected void onRegistered(String handlerName, RequestHandler handler, 
			RequestHandler old) throws ErrorException { 
		if (handlerName == null || handler == null) 
			return;
		
		if (handler instanceof InfoMBean) {
			mCore.getInfoRegistry().register(handlerName, 
					(InfoMBean)handler, true);
		}
	}
	
}
