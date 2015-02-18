package org.javenstudio.lightning.request;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContentStream;
import org.javenstudio.falcon.util.IParams;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.lightning.response.Response;

public abstract class Request implements IParams {
	private static final Logger LOG = Logger.getLogger(Request.class);

	protected final long mStartTime = System.currentTimeMillis();
	protected final RequestInput mInput;
	protected final Params mOrigParams;
	
	protected Map<Object,Object> mContextMap = null;
	protected Iterable<ContentStream> mStreams = null;
	protected Params mParams = null;
	
	public Request(RequestInput input, Params params) { 
		if (input == null) throw new NullPointerException();
		mInput = input;
		mParams = mOrigParams = params;
	}
	
	public abstract RequestAcceptor getRequestAcceptor();
	public abstract HttpMethod getRequestMethod();
	
	public final RequestInput getRequestInput() { 
		return mInput;
	}
	
	public String getRemoteAddr() { 
		return mInput.getRemoteAddr();
	}
	
	public String getUserAgent() { 
		return mInput.getUserAgent();
	}
	
	/** returns the current request parameters */
	public Params getParams() { 
		return mParams;
	}
  
	/** 
	 * Change the parameters for this request.  This does not affect
	 *  the original parameters returned by getOriginalParams()
	 */
	public void setParams(Params params) { 
		if (params == null) 
			throw new NullPointerException("Params is null");
		
		mParams = params;
	}
	
	/** A Collection of ContentStreams passed to the request */
	public Iterable<ContentStream> getContentStreams() { 
		return mStreams;
	}
	
	/** 
	 * Returns the original request parameters.  As this
	 * does not normally include configured defaults
	 * it's more suitable for logging.
	 */
	public Params getOriginalParams() { 
		return mOrigParams;
	}

	/**
	 * Generic information associated with this request 
	 * that may be both read and updated.
	 */
	public Map<Object,Object> getContextMap() { 
		// QueryRequest as a whole isn't thread safe, and this isn't either.
		if (mContextMap == null) 
			mContextMap = new HashMap<Object,Object>();
		
		return mContextMap;
	}

	/** The start time of this request in milliseconds */
	public long getStartTime() { 
		return mStartTime;
	}

	/**
	 * Returns a string representing all the important parameters.
	 * Suitable for logging.
	 */
	public String getParamString() { 
		return mOrigParams != null ? mOrigParams.toString() : null;
	}
	
	/**
	 * This method should be called when all uses of this request are
	 * finished, so that resources can be freed.
	 */
	public void close() throws ErrorException { 
		Iterable<ContentStream> streams = getContentStreams();
		if (streams != null) { 
			Iterator<ContentStream> it = streams.iterator();
			while (it.hasNext()) { 
				ContentStream stream = it.next();
				try {
					if (stream != null) stream.close();
				} catch (Throwable e) { 
					if (LOG.isWarnEnabled())
						LOG.warn("close: " + stream + " error: " + e, e);
				}
			}
		}
	}
	
	public String getResponseWriterType() throws ErrorException { 
		return null;
	}
	
	public void setResponseWriterType(String type) throws ErrorException { 
		throw new UnsupportedOperationException();
	}
	
	public NamedList<Object> getParsedResponse(Response rsp) throws ErrorException { 
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String getParam(String name) throws ErrorException { 
		return getParams().get(name);
	}
	
	@Override
	public String getParam(String name, String def) throws ErrorException { 
		return getParams().get(name, def);
	}
	
	@Override
	public String[] getParams(String name) throws ErrorException { 
		return getParams().getParams(name);
	}
	
}
