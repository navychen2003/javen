package org.javenstudio.lightning.response;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;

public class QueryResponse extends ResponseBase {
	static final Logger LOG = Logger.getLogger(QueryResponse.class);

	public QueryResponse(ResponseOutput output) { 
		super(output);
		// responseHeader should be first
		add("responseHeader", new NamedMap<Object>());
	}
	
	/** Repsonse header to be logged */
	@Override
	public synchronized NamedList<Object> getResponseHeader() {
		@SuppressWarnings("unchecked")
		NamedMap<Object> header = (NamedMap<Object>)getValue("responseHeader");
		if (header == null) { 
			header = new NamedMap<Object>();
			add("responseHeader", header);
		}
		return header;
	}
	
	@Override
	public void omitResponseHeader() { 
		if (LOG.isDebugEnabled())
			LOG.debug("remove responseHeader values: " + getValue("responseHeader"));
		
		getValues().remove("responseHeader");
	}
	
	@Override
	public boolean getPartialResults() { 
		NamedList<?> header = getResponseHeader();
		if (header != null) { 
			Object partialResults = header.get("partialResults");
	        boolean timedOut = (partialResults == null) ? false : (Boolean)partialResults;
	        return timedOut;
		}
		return false;
	}
	
}
