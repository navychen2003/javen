package org.javenstudio.lightning.response;

import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;

public abstract class ResponseBase extends Response {

	/**
	 * Container for storing information that should be logged before returning.
	 */
	private NamedList<Object> mToLog = new NamedMap<Object>();
	
	/**
	 * Should this response be tagged with HTTP caching headers?
	 */
	private boolean mHttpCaching = true;
	
	public ResponseBase(ResponseOutput output) { 
		super(output);
	}
	
	/** 
	 * Add a value to be logged.
	 * 
	 * @param name name of the thing to log
	 * @param val value of the thing to log
	 */
	public void addToLog(String name, Object val) {
		mToLog.add(name, val);
	}
  
	/** 
	 * Get loggable items.
	 * 
	 * @return things to log
	 */
	@Override
	public NamedList<Object> getToLog() {
		return mToLog;
	}
	
	public void setElapsedTime(long time) { 
		mElapsedTime = time;
	}
	
	/**
	 * Enables or disables the emission of HTTP caching headers for this response.
	 * @param httpCaching true=emit caching headers, false otherwise
	 */
	public void setHttpCaching(boolean httpCaching) {
		mHttpCaching = httpCaching;
	}
  
	/**
	 * Should this response emit HTTP caching headers?
	 * @return true=yes emit headers, false otherwise
	 */
	public boolean isHttpCaching() {
		return mHttpCaching;
	}
	
}
