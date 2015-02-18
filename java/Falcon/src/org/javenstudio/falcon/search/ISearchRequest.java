package org.javenstudio.falcon.search;

import java.util.Map;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContentStream;
import org.javenstudio.falcon.util.Params;

public interface ISearchRequest {

	public ISearchCore getSearchCore();
	public Searcher getSearcher() throws ErrorException;
	
	public Params getParams();
	public Params getOriginalParams();
	public void setParams(Params params);
	
	public Map<Object,Object> getContextMap();
	public Iterable<ContentStream> getContentStreams();
	
	public long getStartTime();
	public void close() throws ErrorException;
	
}
