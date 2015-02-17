package org.javenstudio.cocoka.net.http.fetch;

public interface FetchSpans {

	public void clearFetchSpans(); 
	public void addFetchSpan(FetchSpan span); 
	public FetchSpan[] getFetchSpans(String source); 
	public String[] getFetchSources(); 
	
}
