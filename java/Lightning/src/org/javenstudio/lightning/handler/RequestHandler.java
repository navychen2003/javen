package org.javenstudio.lightning.handler;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.InfoMBean;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;
import org.javenstudio.lightning.response.ResponseOutput;

public interface RequestHandler extends InfoMBean {

	/** 
	 * <code>init</code> will be called just once, immediately after creation.
	 * <p>The args are user-level initialization parameters that
	 * may be specified when declaring a request handler in
	 * config.xml
	 */
	public void init(NamedList<?> args) throws ErrorException;

	/**
	 * Handles a query request, this method must be thread safe.
	 * <p>
	 * Information about the request may be obtained from <code>req</code> and
	 * response information may be set using <code>rsp</code>.
	 * <p>
	 * There are no mandatory actions that handleRequest must perform.
	 * An empty handleRequest implementation would fulfill
	 * all interface obligations.
	 */
	public void handleRequest(Request req, Response rsp) throws ErrorException;
	
	public boolean handleResponse(Request req, Response rsp, 
			ResponseOutput output) throws ErrorException;
	
}
