package org.javenstudio.lightning.core.search;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.ISearchResponse;
import org.javenstudio.falcon.search.SearchReturnFields;
import org.javenstudio.falcon.util.ContentStream;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.util.ReturnFields;
import org.javenstudio.lightning.response.writer.RawResponseWriter;

public class LocalResponse implements ISearchResponse {

	/**
	 * Container for user defined values
	 * @see #getValues
	 * @see #add
	 * @see #setAllValues
	 * @see <a href="#returnable_data">Note on Returnable Data</a>
	 */
	private NamedList<Object> mValues = new NamedMap<Object>();
	
	/**
	 * Container for storing information that should be logged before returning.
	 */
	private NamedList<Object> mToLog = new NamedMap<Object>();
	
	/**
	 * Should this response be tagged with HTTP caching headers?
	 */
	private boolean mHttpCaching = true;
	
	/**
	 * The endtime of the request in milliseconds.
	 * Used to calculate query time.
	 * @see #setEndTime(long)
	 * @see #getEndTime()
	 */
	protected long mEndtime = 0;
	
	protected long mElapsedTime = 0;
  
	protected ReturnFields mReturnFields = null;
	
	// error if this is set...
	protected Throwable mException = null;
	
	public LocalResponse(SearchCore core, SearchRequest req) 
			throws ErrorException { 
		mReturnFields = new SearchReturnFields(core, req);
	}
	
	@Override
	public SearchReturnFields getReturnFields() { 
		return (SearchReturnFields)mReturnFields; 
	}
	
	@Override
	public void addContent(ContentStream stream) { 
		add(RawResponseWriter.CONTENT, stream);
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
	
	public long getElapsedTime() { 
		return mElapsedTime; 
	}
	
	public boolean getPartialResults() { 
		return false;
	}
	
	/** Repsonse header to be logged */
	public NamedList<Object> getResponseHeader() { 
		return null;
	}
	
	public void omitResponseHeader() {}
	
	/**
	 * Gets data to be returned in this response
	 * @see <a href="#returnable_data">Note on Returnable Data</a>
	 */
	public NamedList<Object> getValues() { 
		return mValues; 
	}

	/**
	 * Gets data to be returned in this response
	 * @see <a href="#returnable_data">Note on Returnable Data</a>
	 */
	public Object getValue(String name) { 
		return mValues.get(name);
	}
	
	/**
	 * Sets data to be returned in this response
	 * @see <a href="#returnable_data">Note on Returnable Data</a>
	 */
	public void setAllValues(NamedList<Object> nameValuePairs) {
		mValues = nameValuePairs;
	}

	/**
	 * Appends a named value to the list of named values to be returned.
	 * @param name  the name of the value - may be null if unnamed
	 * @param val   the value to add - also may be null since null is a legal value
	 * @see <a href="#returnable_data">Note on Returnable Data</a>
	 */
	public void add(String name, Object val) {
		mValues.add(name, val);
	}

	/**
	 * Causes an error to be returned instead of the results.
	 */
	public void setException(Throwable e) {
		mException = e;
	}

	/**
	 * Returns an Exception if there was a fatal error in processing the request.
	 * Returns null if the request succeeded.
	 */
	public Throwable getException() {
		return mException;
	}
  
	/**
	 * Get the time in milliseconds when the response officially finished. 
	 */
	public synchronized long getEndTime() {
		if (mEndtime == 0) 
			setEndTime();
		
		return mEndtime;
	}

	/**
	 * Stop the timer for how long this query took.
	 * @see #setEndTime(long)
	 */
	public long setEndTime() {
		return setEndTime(System.currentTimeMillis());
	}

	/**
	 * Set the in milliseconds when the response officially finished. 
	 * @see #setEndTime()
	 */
	public synchronized long setEndTime(long endtime) {
		if (endtime != 0) 
			mEndtime = endtime;
		
		return mEndtime;
	}
	
}
