package org.javenstudio.falcon.search.facet;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.CommonParams;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.search.ResponseBuilder;
import org.javenstudio.falcon.search.params.FacetParams;
import org.javenstudio.falcon.search.query.QueryParsing;

/**
 * <b>This API is experimental and subject to change</b>
 */
public class FacetBase {
	
	protected String mFacetType;  	// facet.field, facet.query, etc (make enum?)
	protected String mFacetStr;   	// original parameter value of facetStr
	protected String mFacetOn;    	// the field or query, absent localParams if appropriate
	protected String mKey; 			// label in the response for the result... "foo" for {!key=foo}myfield
	protected Params mLocalParams;	// any local params for the facet

    public FacetBase(ResponseBuilder rb, String facetType, 
    		String facetStr) throws ErrorException {
    	mFacetType = facetType;
    	mFacetStr = facetStr;
    	mFacetOn = facetStr;
      	mKey = facetStr;
    	
    	mLocalParams = QueryParsing.getLocalParams(facetStr, rb.getRequest().getParams());
    	if (mLocalParams != null) {
    		// remove local params unless it's a query
    		if (!facetType.equals(FacetParams.FACET_QUERY)) {
    			mFacetOn = mLocalParams.get(CommonParams.VALUE);
    			mKey = mFacetOn;
    		}

    		mKey = mLocalParams.get(CommonParams.OUTPUT_KEY, mKey);
    	}
	}

	/** returns the key in the response that this facet will be under */
	public String getKey() { return mKey; }
	
	public String getFacetType() { return mFacetType; }
	public String getFacetString() { return mFacetStr; }
	
	public Params getLocalParams() { return mLocalParams; }
	
}
