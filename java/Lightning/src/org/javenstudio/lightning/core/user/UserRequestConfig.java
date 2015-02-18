package org.javenstudio.lightning.core.user;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContextResource;
import org.javenstudio.falcon.util.ModifiableParams;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.lightning.request.DefaultRequestConfig;
import org.javenstudio.lightning.request.RequestAcceptor;
import org.javenstudio.lightning.request.RequestBase;
import org.javenstudio.lightning.request.RequestInput;

public class UserRequestConfig extends DefaultRequestConfig {

	public UserRequestConfig(ContextResource config) throws ErrorException { 
		super(config);
	}
	
	@Override
	protected RequestBase createRequest(RequestAcceptor acceptor, RequestInput input, 
			Params params) throws ErrorException { 
		if (params != null && params instanceof ModifiableParams) {
			@SuppressWarnings("unused")
			ModifiableParams mparams = (ModifiableParams)params;
			String queryPath = input.getQueryPath();
			if (queryPath != null) { 
			}
		}
		
		return super.createRequest(acceptor, input, params);
	}
	
}
