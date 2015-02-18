package org.javenstudio.lightning.request;

import org.javenstudio.falcon.util.Params;
import org.javenstudio.lightning.core.CoreAdmin;

public class AdminRequest extends RequestBase {

	private final CoreAdmin mAdmin;
	
	public AdminRequest(CoreAdmin admin, RequestInput input, Params params) {
		super(input, params);
		mAdmin = admin;
	}
	
	public final CoreAdmin getAdmin() { return mAdmin; }
	public final RequestAcceptor getRequestAcceptor() { return getAdmin(); }
	
}
