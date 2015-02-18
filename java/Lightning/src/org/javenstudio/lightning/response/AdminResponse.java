package org.javenstudio.lightning.response;

import org.javenstudio.lightning.request.Request;

public class AdminResponse extends QueryResponse {

	public AdminResponse(ResponseOutput output, Request request) { 
		super(output);
		addToLog("webapp", request.getRequestInput().getContextPath());
		addToLog("path", request.getRequestInput().getQueryPath());
		addToLog("params", "{" + request.getParamString() + "}");
	}
	
}
