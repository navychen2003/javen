package org.javenstudio.lightning.handler;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.FsUtils;
import org.javenstudio.falcon.util.InfoMBean;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;
import org.javenstudio.lightning.response.ResponseOutput;

public abstract class RequestHandlerBase implements RequestHandler {

	// statistics
	private final long mHandlerStart = System.currentTimeMillis();
	
	// TODO: should we bother synchronizing these, or is an off-by-one error
	// acceptable every million requests or so?
	private final AtomicLong mNumRequests = new AtomicLong();
	private final AtomicLong mNumErrors = new AtomicLong();
	private final AtomicLong mNumTimeouts = new AtomicLong();
	private final AtomicLong mTotalTime = new AtomicLong();
	
	protected NamedList<?> mInitArgs = null;
	protected Params mDefaults = null;
	protected Params mAppends = null;
	protected Params mInvariants = null;
	
	@Override
	public void init(NamedList<?> args) throws ErrorException { 
		mInitArgs = args;
		
		if (args == null) 
			throw new NullPointerException("init args is null");
		
	    if (args != null) {
	        Object o = args.get("defaults");
	        if (o != null && o instanceof NamedList) {
	        	mDefaults = Params.toParams((NamedList<?>)o);
	        }
	        
	        o = args.get("appends");
	        if (o != null && o instanceof NamedList) {
	        	mAppends = Params.toParams((NamedList<?>)o);
	        }
	        
	        o = args.get("invariants");
	        if (o != null && o instanceof NamedList) {
	        	mInvariants = Params.toParams((NamedList<?>)o);
	        }
	      }
	}
	
	public final NamedList<?> getInitArgs() { 
		return mInitArgs;
	}
	
	public final long getNumRequests() { return mNumRequests.longValue(); }
	public final long getNumErrors() { return mNumErrors.longValue(); }
	public final long getNumTimeouts() { return mNumTimeouts.longValue(); }
	public final long getTotalTime() { return mTotalTime.longValue(); }
	public final long getHandlerStart() { return mHandlerStart; }
	
	public abstract void handleRequestBody(Request req, Response rsp) 
			throws ErrorException;
	
	/**
	 * Set default-ish params on a Request.
	 *
	 * RequestHandlers can use this method to ensure their defaults and
	 * overrides are visible to other components such as the response writer
	 *
	 * @param req The request whose params we are interested i
	 * @param defaults values to be used if no values are specified in the request params
	 * @param appends values to be appended to those from the request (or defaults) 
	 * when dealing with multi-val params, or treated as another layer of defaults 
	 * for singl-val params.
	 * @param invariants values which will be used instead of any request, 
	 * or default values, regardless of context.
	 */
	public static void setDefaultParams(Request req, 
			Params defaults, Params appends, Params invariants) {

		Params p = req.getParams();
		p = Params.wrapDefaults(p, defaults);
		p = Params.wrapAppended(p, appends);
		p = Params.wrapDefaults(invariants, p);

		req.setParams(p);
	}
	
	@Override
	public final void handleRequest(Request req, Response rsp) throws ErrorException { 
		mNumRequests.incrementAndGet();
		
		try { 
			setDefaultParams(req, mDefaults, mAppends, mInvariants);
			handleRequestBody(req, rsp);
			
	        if (rsp.getPartialResults()) 
	        	mNumTimeouts.incrementAndGet();
			
		} catch (Throwable e) { 
			ErrorException se;
			if (e instanceof ErrorException) {
		        se = (ErrorException)e;
		        //if (se.getCode() == ErrorException.ErrorCode.CONFLICT.getCode()) {
		        //	// TODO: should we allow this to be counted as an error (numErrors++)?
		        //}
			} else { 
				se = new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, e);
			}
			
			rsp.setException(e);
		    mNumErrors.incrementAndGet();
		    
		    throw se;
		}
		
		mTotalTime.addAndGet(rsp.getEndTime() - req.getStartTime());
	}

	@Override
	public boolean handleResponse(Request req, Response rsp, 
			ResponseOutput output) throws ErrorException { 
		return false;
	}
	
	@Override
	public String getMBeanKey() {
		return null; // donot register at newInstance
	}

	@Override
	public String getMBeanName() {
		return getClass().getName();
	}

	@Override
	public String getMBeanVersion() {
		return "1.0";
	}

	@Override
	public String getMBeanDescription() {
		return ""; //getClass().getName();
	}

	@Override
	public String getMBeanCategory() {
		return InfoMBean.CATEGORY_QUERYHANDLER;
	}

	@Override
	public NamedList<?> getMBeanStatistics() {
	    NamedList<Object> lst = new NamedMap<Object>();
	    lst.add("handlerStart", new Date(mHandlerStart));
	    lst.add("requests", mNumRequests.longValue());
	    lst.add("errors", mNumErrors.longValue());
	    lst.add("timeouts", mNumTimeouts.longValue());
	    lst.add("defaultParams", toDisplayString(mDefaults));
	    lst.add("appendParams", toDisplayString(mAppends));
	    lst.add("invariantParams", toDisplayString(mInvariants));
	    return lst;
	}
	
	private String toDisplayString(Params params) { 
		return params != null ? params.toDisplayString() : "";
	}
	
	public static String normalizePath(String path) {
	    return FsUtils.normalizePath(path);
	}
	
}
