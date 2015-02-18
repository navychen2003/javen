package org.javenstudio.falcon.search.facet;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.ResponseBuilder;
import org.javenstudio.falcon.search.params.FacetParams;

/**
 * <b>This API is experimental and subject to change</b>
 */
public class QueryFacet extends FacetBase {
	
    private long mCount;

    public QueryFacet(ResponseBuilder rb, String facetStr) 
    		throws ErrorException {
      super(rb, FacetParams.FACET_QUERY, facetStr);
    }
    
    public void setCount(long count) { mCount = count; }
    public long getCount() { return mCount; }
    
}
