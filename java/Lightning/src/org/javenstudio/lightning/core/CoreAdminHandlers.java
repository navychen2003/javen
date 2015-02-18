package org.javenstudio.lightning.core;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.PluginInfo;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.handler.RequestHandlers;
import org.javenstudio.lightning.handler.system.CoreStatusHandler;

public class CoreAdminHandlers extends RequestHandlers {
	//private static final Logger LOG = Logger.getLogger(CoreAdminHandlers.class);

	private final CoreAdmin mAdmin;
	
	public CoreAdminHandlers(CoreAdmin admin) throws ErrorException { 
		mAdmin = admin;
		admin.getFactory().registerAdminHandlers(this, admin);
	}
	
	public static void registerHandlers(RequestHandlers handlers, CoreAdmin admin) 
			throws ErrorException {
		handlers.register("status", new CoreStatusHandler(admin));
		handlers.register("", handlers.get("status"));
	}
	
	public final CoreAdmin getAdmin() { return mAdmin; }

	@Override
	public RequestHandler createRequestHandler(PluginInfo info)
			throws ErrorException {
		return null;
	}

}
