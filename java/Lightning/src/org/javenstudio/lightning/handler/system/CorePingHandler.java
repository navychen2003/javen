package org.javenstudio.lightning.handler.system;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.util.FileUtils;
import org.javenstudio.falcon.util.ISO8601CanonicalDateFormat;
import org.javenstudio.falcon.util.ModifiableParams;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.util.ThreadLocalDateFormat;
import org.javenstudio.lightning.core.Core;
import org.javenstudio.lightning.core.CoreAware;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

/**
 * Ping Request Handler for reporting Core health to a Load Balancer.
 *
 * <p>
 * This handler is designed to be used as the endpoint for an HTTP 
 * Load-Balancer to use when checking the "health" or "up status" of a 
 * server.
 * </p>
 * 
 * <p> 
 * In it's simplest form, the PingHandler should be
 * configured with some defaults indicating a request that should be
 * executed.  If the request succeeds, then the PingHandler
 * will respond back with a simple "OK" status.  If the request fails,
 * then the PingHandler will respond back with the
 * corrisponding HTTP Error code.  Clients (such as load balancers)
 * can be configured to poll the PingHandler monitoring for
 * these types of responses (or for a simple connection failure) to
 * know if there is a problem with the server.
 * </p>
 *
 * <pre class="prettyprint">
 * &lt;requestHandler name="/admin/ping" class="lightning.PingHandler"&gt;
 *   &lt;lst name="invariants"&gt;
 *     &lt;str name="qt"&gt;/search&lt;/str&gt;&lt;!-- handler to delegate to --&gt;
 *     &lt;str name="q"&gt;some test query&lt;/str&gt;
 *   &lt;/lst&gt;
 * &lt;/requestHandler&gt;
 * </pre>
 *
 * <p>
 * A more advanced option available, is to configure the handler with a 
 * "healthcheckFile" which can be used to enable/disable the PingHandler.
 * </p>
 *
 * <pre class="prettyprint">
 * &lt;requestHandler name="/admin/ping" class="lightning.PingHandler"&gt;
 *   &lt;!-- relative paths are resolved against the data dir --&gt;
 *   &lt;str name="healthcheckFile"&gt;server-enabled.txt&lt;/str&gt;
 *   &lt;lst name="invariants"&gt;
 *     &lt;str name="qt"&gt;/search&lt;/str&gt;&lt;!-- handler to delegate to --&gt;
 *     &lt;str name="q"&gt;some test query&lt;/str&gt;
 *   &lt;/lst&gt;
 * &lt;/requestHandler&gt;
 * </pre>
 *
 * <ul>
 *   <li>If the health check file exists, the handler will execute the 
 *       delegated query and return status as described above.
 *   </li>
 *   <li>If the health check file does not exist, the handler will return 
 *       an HTTP error even if the server is working fine and the delegated 
 *       query would have succeeded
 *   </li>
 * </ul>
 *
 * <p> 
 * This health check file feature can be used as a way to indicate
 * to some Load Balancers that the server should be "removed from
 * rotation" for maintenance, or upgrades, or whatever reason you may
 * wish.  
 * </p>
 *
 * <p> 
 * The health check file may be created/deleted by any external
 * system, or the PingHandler itself can be used to
 * create/delete the file by specifying an "action" param in a
 * request: 
 * </p>
 *
 * <ul>
 *   <li><code>http://.../ping?action=enable</code>
 *       - creates the health check file if it does not already exist
 *   </li>
 *   <li><code>http://.../ping?action=disable</code>
 *       - deletes the health check file if it exists
 *   </li>
 *   <li><code>http://.../ping?action=status</code>
 *       - returns a status code indicating if the healthcheck file exists 
 *       ("<code>enabled</code>") or not ("<code>disabled<code>")
 *   </li>
 * </ul>
 *
 * @since 1.3
 */
public class CorePingHandler extends AdminHandlerBase implements CoreAware {
	static final Logger LOG = Logger.getLogger(CorePingHandler.class);

	static final String HEALTHCHECK_FILE_PARAM = "healthcheckFile";
	
	static enum ACTIONS {STATUS, ENABLE, DISABLE, PING};
	
	private final Core mCore;
	private String mHealthFileName = null;
	private File mHealthcheck = null;

	public CorePingHandler(Core core) { 
		mCore = core;
		
		if (core == null) 
			throw new NullPointerException();
	}
	
	public Core getCore() { return mCore; }
	
	@Override
	public void init(NamedList<?> args) throws ErrorException {
		super.init(args);
		Object tmp = args.get(HEALTHCHECK_FILE_PARAM);
		mHealthFileName = (tmp == null ? null : tmp.toString());
	}
	
	@Override
	public void inform(Core core) throws ErrorException { 
		if (mCore != core) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"inform wrong core: " + core + " not equals " + mCore);
		}
		
	    if (mHealthFileName != null) {
	        mHealthcheck = new File(mHealthFileName);
	        
	        if (!mHealthcheck.isAbsolute()) {
	        	mHealthcheck = new File(core.getDataDir(), mHealthFileName);
	        	mHealthcheck = mHealthcheck.getAbsoluteFile();
	        }

	        if (!mHealthcheck.getParentFile().canWrite()) {
	        	// this is not fatal, users may not care about enable/disable via 
	        	// request, file might be touched/deleted by an external system
	        	if (LOG.isWarnEnabled()) {
	        		LOG.warn("Directory for configured healthcheck file is not writable, " 
	        				+ "PingRequestHandler will not be able to control enable/disable: " 
	        				+ mHealthcheck.getParentFile().getAbsolutePath());
	        	}
	        }
	    }
	}
	
	/**
	 * Returns true if the healthcheck flag-file is enabled but does not exist, 
	 * otherwise (no file configured, or file configured and exists) 
	 * returns false. 
	 */
	public boolean isPingDisabled() {
		return (mHealthcheck != null && !mHealthcheck.exists());
	}

	@Override
	public void handleRequestBody(Request req, Response rsp)
			throws ErrorException {
		checkAuth(req, IUserClient.Op.ACCESS);
		
	    Params params = req.getParams();
	    
	    // in this case, we want to default distrib to false so
	    // we only ping the single node
	    Boolean distrib = params.getBool("distrib");
	    if (distrib == null)   {
	    	ModifiableParams mparams = new ModifiableParams(params);
	    	mparams.set("distrib", false);
	    	req.setParams(mparams);
	    }
	    
	    String actionParam = params.get("action");
	    ACTIONS action = null;
	    if (actionParam == null){
	    	action = ACTIONS.PING;
	    } else {
	    	try {
	    		action = ACTIONS.valueOf(actionParam.toUpperCase(Locale.ROOT));
	    	} catch (IllegalArgumentException e) {
	    		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
	    				"Unknown action: " + actionParam);
	    	}
	    }
	    
	    switch (action) {
	    case PING:
	        handlePing(req, rsp);
	        break;
	    case ENABLE:
	        handleEnable(true);
	        break;
	    case DISABLE:
	        handleEnable(false);
	        break;
	    case STATUS:
	        handleStatus(req, rsp);
	    }
	}
	
	protected void handleStatus(Request req, Response rsp) throws ErrorException {
		if (mHealthcheck == null) { 
			rsp.setException(new ErrorException(
					ErrorException.ErrorCode.SERVICE_UNAVAILABLE, 
					"healthcheck not configured"));
			
			return;
		}
		
		rsp.add("status", isPingDisabled() ? "disabled" : "enabled");
	}
	
	protected void handlePing(Request req, Response rsp) throws ErrorException {
		//Params params = req.getParams();
		Core core = mCore;
    
		// Get the RequestHandler
		// optional; you get the default otherwise
		//String qt = params.get(CommonParams.QT); 
		
		//RequestHandler handler = core.getRequestHandler(qt);
		//if (handler == null) {
		//	throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
		//			"Unknown RequestHandler (qt): " + qt);
		//}
    
		//if (handler instanceof CorePingHandler) {
		//	throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
		//			"Cannot execute the PingHandler recursively");
		//}
    
		//if (LOG.isDebugEnabled())
		//	LOG.debug("handlePing: handler=" + handler);
		
		// Execute the ping query and catch any possible exception
		Throwable ex = null;
		try {
			Response pingrsp = core.createResponse(req, rsp.getResponseOutput());
			//core.execute(handler, req, pingrsp);
			ex = pingrsp.getException();
		} catch (Throwable th) {
			ex = th;
		}
    
		// Send an error or an 'OK' message (response code will be 200)
		if (ex != null) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Ping query caused exception: " + ex.getMessage(), ex);
		}
		
		rsp.add("status", "OK");
	}
  
	protected void handleEnable(boolean enable) throws ErrorException {
		if (mHealthcheck == null) {
			if (LOG.isWarnEnabled()) 
				LOG.warn("No healthcheck file defined.");
			
			return;
		}
		
		if (enable) {
			try {
				// write out when the file was created
				FileUtils.write(mHealthcheck, 
						formatExternal(new Date()), "UTF-8");
				
			} catch (IOException e) {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"Unable to write healthcheck flag file", e);
			}
		} else {
			if (mHealthcheck.exists() && !mHealthcheck.delete()) {
				throw new ErrorException(ErrorException.ErrorCode.NOT_FOUND,
						"Did not successfully delete healthcheck file: "
						+ mHealthcheck.getAbsolutePath());
			}
		}
	}
	
	/**
	 * Thread safe DateFormat that can <b>format</b> in the canonical
	 * ISO8601 date format, not including the trailing "Z" (since it is
	 * left off in the internal indexed values)
	 */
	protected static final ThreadLocalDateFormat sFmtThreadLocal = 
			new ThreadLocalDateFormat(new ISO8601CanonicalDateFormat());
	
	public static String formatExternal(Date d) {
		return sFmtThreadLocal.get().format(d) + 'Z';
	}
	
}
