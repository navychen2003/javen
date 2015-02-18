package org.javenstudio.lightning.core.search;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.lightning.handler.RequestHandlerBase;
import org.javenstudio.lightning.request.Request;

public abstract class SearchHandlerBase extends RequestHandlerBase {

	protected void checkAuth(Request req, IUserClient.Op op) throws ErrorException { 
		UserHelper.checkAdmin(req, op);
	}
	
}
