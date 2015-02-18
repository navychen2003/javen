package org.javenstudio.lightning.response;

import org.javenstudio.falcon.util.INamedValues;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.util.ReturnFields;

public abstract class Response implements INamedValues<Object> {

	/**
	 * Container for user defined values
	 * @see #getValues
	 * @see #add
	 * @see #setAllValues
	 * @see <a href="#returnable_data">Note on Returnable Data</a>
	 */
	private NamedList<Object> mValues = new NamedMap<Object>();
	
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
	
	private final ResponseOutput mOutput;
	
	public Response(ResponseOutput output) { 
		if (output == null) throw new NullPointerException();
		mOutput = output;
	}
	
	public final ResponseOutput getResponseOutput() { 
		return mOutput;
	}
	
	/** 
	 * Get loggable items.
	 * 
	 * @return things to log
	 */
	public abstract NamedList<Object> getToLog();
	
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
	 * Gets the document field names of fields to return by default when
	 * returning DocLists
	 */
	public ReturnFields getReturnFields() { 
		if (mReturnFields == null) 
			mReturnFields = new ReturnFields(); // by default return everything
		
		return mReturnFields;
	}
	
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
