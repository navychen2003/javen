package org.javenstudio.lightning.core.user;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.QueryResponse;
import org.javenstudio.lightning.response.ResponseOutput;

public class UserResponse extends QueryResponse {

	public UserResponse(UserCore core, Request req, 
			ResponseOutput output) throws ErrorException { 
		super(output);
	}
	
}
