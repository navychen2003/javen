package org.javenstudio.falcon.search;

import org.javenstudio.falcon.util.ContentStream;
import org.javenstudio.falcon.util.NamedList;

public interface ISearchResponse {

	public long getElapsedTime();
	public void add(String name, Object val);
	public void addContent(ContentStream stream);
	
	public Object getValue(String name);
	public NamedList<Object> getValues();
	
	public SearchReturnFields getReturnFields();
	
	public NamedList<Object> getResponseHeader();
	public Throwable getException();
	
	public NamedList<Object> getToLog();
	
}
