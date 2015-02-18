package org.javenstudio.lightning.core.user;

import org.javenstudio.falcon.util.Params;
import org.javenstudio.lightning.request.QueryRequest;
import org.javenstudio.lightning.request.RequestInput;

public class UserRequest extends QueryRequest {

	public UserRequest(UserCore core, RequestInput input, Params params) {
		super(core, input, params);
	}
	
}
