package org.javenstudio.lightning.handler.system;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.lightning.core.Core;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class PluginInfoHandler extends AdminHandlerBase {

	@SuppressWarnings("unused")
	private final Core mCore;
	
	public PluginInfoHandler(Core core) { 
		mCore = core;
		
		if (core == null) 
			throw new NullPointerException();
	}

	@Override
	public void handleRequestBody(Request req, Response rsp)
			throws ErrorException {
		checkAuth(req, IUserClient.Op.ACCESS);
		
	    Params params = req.getParams();
	    
	    boolean stats = params.getBool("stats", false);
	    rsp.add("plugins", getInfoBeans(stats));
	    
	    //rsp.setHttpCaching(false);
	}
	
	protected NamedMap<Object> getInfoBeans(boolean stats) 
			throws ErrorException { 
		NamedMap<Object> list = new NamedMap<Object>();
		
		return list;
	}
	
}
