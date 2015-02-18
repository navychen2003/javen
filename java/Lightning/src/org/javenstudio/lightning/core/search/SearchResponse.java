package org.javenstudio.lightning.core.search;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.ISearchResponse;
import org.javenstudio.falcon.search.SearchReturnFields;
import org.javenstudio.falcon.util.ContentStream;
import org.javenstudio.lightning.response.QueryResponse;
import org.javenstudio.lightning.response.ResponseOutput;
import org.javenstudio.lightning.response.writer.RawResponseWriter;

public class SearchResponse extends QueryResponse implements ISearchResponse {

	public SearchResponse(SearchCore core, SearchRequest req, 
			ResponseOutput output) throws ErrorException { 
		super(output);
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
	
}
