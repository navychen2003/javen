package org.javenstudio.falcon.search;

import org.javenstudio.falcon.util.TextWriter;

public interface SearchTextWriter {

	public ISearchRequest getSearchRequest();
	public ISearchResponse getSearchResponse();
	
	public TextWriter getTextWriter();
	
}
