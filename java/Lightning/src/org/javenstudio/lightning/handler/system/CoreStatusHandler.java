package org.javenstudio.lightning.handler.system;

import java.util.Collections;
import java.util.Map;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.util.AdminParams;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.lightning.core.CoreAdmin;
import org.javenstudio.lightning.core.CoreContainers;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public final class CoreStatusHandler extends AdminHandlerBase {

	private final CoreAdmin mAdmin;
	
	public CoreStatusHandler(CoreAdmin admin) { 
		mAdmin = admin;
		
		if (admin == null) 
			throw new NullPointerException();
	}
	
	@Override
	public void handleRequestBody(Request req, Response rsp)
			throws ErrorException {
		checkAuth(req, IUserClient.Op.ACCESS);
		
		final CoreContainers cores = mAdmin.getContainers();
		
		final Params params = req.getParams();
	    final String cname = params.get(AdminParams.CORE);
	    
	    NamedList<Object> status = new NamedMap<Object>();
	    Map<String,Throwable> allFailures = cores.getInitFailures();
	    
    	if (cname == null) {
    		rsp.add("defaultCoreName", cores.getDefaultCoreName());
    		for (String name : cores.getCoreNames()) {
    			status.add(name, getCoreStatus(cores, name, params));
    		}
    		rsp.add("initFailures", allFailures);
    		
    	} else {
    		Map<?,?> failures = allFailures.containsKey(cname)
    				? Collections.singletonMap(cname, allFailures.get(cname))
    						: Collections.emptyMap();
    		rsp.add("initFailures", failures);
    		status.add(cname, getCoreStatus(cores, cname, params));
    	}
    	
    	rsp.add("status", status);
	}

}
