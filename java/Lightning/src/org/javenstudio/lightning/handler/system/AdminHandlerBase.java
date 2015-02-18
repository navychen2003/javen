package org.javenstudio.lightning.handler.system;

import java.util.Date;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.lightning.core.Core;
import org.javenstudio.lightning.core.CoreContainers;
import org.javenstudio.lightning.handler.RequestHandlerBase;
import org.javenstudio.lightning.request.Request;

public abstract class AdminHandlerBase extends RequestHandlerBase {
	
	protected void checkAuth(Request req, IUserClient.Op op) throws ErrorException { 
		UserHelper.checkAdmin(req, op);
	}
	
	protected final NamedList<Object> getCoreStatus(CoreContainers cores, String cname, 
			Params params) throws ErrorException {
		Core core = cores.getCore(cname);
		if (core != null) {
			try {
				return getCoreStatus(core, params);
			} finally {
				core.close();
			}
		}
		
		return null;
	}
	
	protected NamedList<Object> getCoreStatus(Core core, Params params) 
			throws ErrorException { 
		NamedList<Object> info = new NamedMap<Object>();
		
		info.add("name", core.getName());
		info.add("isDefaultCore", core.isDefault());
		info.add("instanceDir", normalizePath(core.getContextLoader().getInstanceDir()));
		info.add("dataDir", normalizePath(core.getDataDir()));
		info.add("config", core.getConfigResourceName());
		info.add("startTime", new Date(core.getStartTime()));
		info.add("uptime", System.currentTimeMillis() - core.getStartTime());
		
		core.getCoreStatus(info, params);
		
		return info;
	}
	
}
