package org.javenstudio.lightning.request;

import org.javenstudio.falcon.util.Params;
import org.javenstudio.lightning.core.Core;

public class QueryRequest extends RequestBase {

	private final Core mCore;
	
	public QueryRequest(Core core, RequestInput input, Params params) {
		super(input, params);
		mCore = core;
	}
	
	public final Core getCore() { return mCore; }
	public final RequestAcceptor getRequestAcceptor() { return getCore(); }
	
}
