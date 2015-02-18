package org.javenstudio.falcon.search.facet;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.search.ResponseBuilder;
import org.javenstudio.falcon.search.params.FacetParams;

/**
 * <b>This API is experimental and subject to change</b>
 */
public class FacetInfo {

	private final Map<String,QueryFacet> mQueryFacets = 
			new LinkedHashMap<String,QueryFacet>();
    private final Map<String,DistributedFieldFacet> mFacets = 
    		new LinkedHashMap<String,DistributedFieldFacet>();
    
    private final NamedMap<NamedMap<Object>> mDateFacets = 
    		new NamedMap<NamedMap<Object>>();
    private final NamedMap<NamedMap<Object>> mRangeFacets = 
    		new NamedMap<NamedMap<Object>>();

    public void parse(Params params, ResponseBuilder rb) throws ErrorException {
    	mQueryFacets.clear();
    	mFacets.clear();

    	String[] facetQs = params.getParams(FacetParams.FACET_QUERY);
    	if (facetQs != null) {
    		for (String query : facetQs) {
    			QueryFacet queryFacet = new QueryFacet(rb, query);
    			mQueryFacets.put(queryFacet.getKey(), queryFacet);
    		}
    	}

    	String[] facetFs = params.getParams(FacetParams.FACET_FIELD);
    	if (facetFs != null) {
    		for (String field : facetFs) {
    			DistributedFieldFacet ff = new DistributedFieldFacet(rb, field);
    			mFacets.put(ff.getKey(), ff);
    		}
    	}
	}
    
    public NamedMap<NamedMap<Object>> getDateFacets() { return mDateFacets; }
    public NamedMap<NamedMap<Object>> getRangeFacets() { return mRangeFacets; }
    
    public QueryFacet getQueryFacet(String key) { 
    	return mQueryFacets.get(key);
    }
    
    public Collection<QueryFacet> getQueryFacetValues() { 
    	return mQueryFacets.values();
    }
    
    public DistributedFieldFacet getFieldFacet(String key) { 
    	return mFacets.get(key);
    }
    
    public Collection<DistributedFieldFacet> getFieldFacetValues() { 
    	return mFacets.values();
    }
    
    public NamedMap<Object> getDateFacet(String fieldName) { 
    	return mDateFacets.get(fieldName);
    }
    
    public void addDateFacet(String fieldName, NamedMap<Object> value) { 
    	mDateFacets.add(fieldName, value);
    }
    
    public NamedMap<Object> getRangeFacet(String fieldName) { 
    	return mRangeFacets.get(fieldName);
    }
    
    public void addRangeFacet(String fieldName, NamedMap<Object> value) { 
    	mRangeFacets.add(fieldName, value);
    }
    
}
