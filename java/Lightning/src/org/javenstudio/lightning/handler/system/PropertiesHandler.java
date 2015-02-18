package org.javenstudio.lightning.handler.system;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class PropertiesHandler extends AdminHandlerBase {
	//private static final Logger LOG = Logger.getLogger(PropertiesHandler.class);

	@Override
	public void handleRequestBody(Request req, Response rsp)
			throws ErrorException {
		checkAuth(req, IUserClient.Op.ACCESS);
		
	    Object props = null;
	    
	    String name = req.getParam("name");
	    if (name != null) {
	    	NamedList<String> p = new NamedMap<String>();
	    	p.add(name, System.getProperty(name));
	    	props = p;
	    	
	    } else {
	    	props = System.getProperties();
	    }
	    
	    rsp.add("system.properties", props);
	    //rsp.setHttpCaching(false);
	}
	
}
