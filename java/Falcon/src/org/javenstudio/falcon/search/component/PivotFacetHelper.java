package org.javenstudio.falcon.search.component;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.indexdb.index.term.Term;
import org.javenstudio.common.indexdb.search.Query;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.hornet.search.query.TermQuery;
import org.javenstudio.hornet.search.query.TermRangeQuery;
import org.javenstudio.falcon.search.Searcher;
import org.javenstudio.falcon.search.ResponseBuilder;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.facet.SimpleFacets;
import org.javenstudio.falcon.search.hits.DocSet;
import org.javenstudio.falcon.search.params.FacetParams;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.search.schema.SchemaFieldType;

/**
 * This is thread safe
 * @since 4.0
 */
public class PivotFacetHelper {
	
	/**
	 * Designed to be overridden by subclasses that provide different faceting implementations.
	 * TODO: Currently this is returning a SimpleFacets object, but those capabilities would
	 *       be better as an extracted abstract class or interface.
	 */
	@Deprecated
	protected SimpleFacets getFacetImplementation(ISearchRequest req, DocSet docs, 
			Params params) throws ErrorException {
		return new SimpleFacets(req, docs, params);
	}

	public NamedMap<List<NamedList<Object>>> process(ResponseBuilder rb, 
			Params params, String[] pivots) throws ErrorException {
		if (!rb.isDoFacets() || pivots == null) 
			return null;
    
		int minMatch = params.getInt(FacetParams.FACET_PIVOT_MINCOUNT, 1);
    
		NamedMap<List<NamedList<Object>>> pivotResponse = 
				new NamedMap<List<NamedList<Object>>>();
		
		for (String pivot : pivots) {
			String[] fields = pivot.split(",");  // only support two levels for now
      
			if (fields.length < 2) {
				throw new ErrorException( ErrorException.ErrorCode.BAD_REQUEST, 
						"Pivot Facet needs at least two fields: "+pivot );
			}
      
			DocSet docs = rb.getResults().getDocSet();
			
			String field = fields[0];
			String subField = fields[1];
			Deque<String> fnames = new LinkedList<String>();
			
			for (int i = fields.length-1; i > 1; i--) {
				fnames.push(fields[i]);
			}
      
			SimpleFacets sf = getFacetImplementation(rb.getRequest(), 
					rb.getResults().getDocSet(), rb.getRequest().getParams());
			NamedList<Integer> superFacets = sf.getTermCounts(field);
      
			pivotResponse.add(pivot, 
					doPivots(superFacets, field, subField, fnames, rb, docs, minMatch));
		}
		
		return pivotResponse;
	}
  
	/**
	 * Recursive function to do all the pivots
	 */
	protected List<NamedList<Object>> doPivots(NamedList<Integer> superFacets, 
			String field, String subField, Deque<String> fnames, ResponseBuilder rb, 
			DocSet docs, int minMatch ) throws ErrorException {
		Searcher searcher = rb.getSearcher();
		
		// TODO: optimize to avoid converting to an external string 
		// and then having to convert back to internal below
		SchemaField sfield = searcher.getSchema().getField(field);
		SchemaFieldType ftype = sfield.getType();

		String nextField = fnames.poll();

		List<NamedList<Object>> values = 
				new ArrayList<NamedList<Object>>(superFacets.size());
		
		for (Map.Entry<String, Integer> kv : superFacets) {
			// Only sub-facet if parent facet has positive count 
			// - still may not be any values for the sub-field though
			if (kv.getValue() >= minMatch ) {

				// may be null when using facet.missing
				final String fieldValue = kv.getKey(); 

				// don't reuse the same BytesRef each time since we will be 
				// constructing Term objects used in TermQueries that may be cached.
				BytesRef termval = null;

				NamedMap<Object> pivot = new NamedMap<Object>();
				pivot.add("field", field);
				
				if (fieldValue == null) {
					pivot.add("value", null);
					
				} else {
					termval = new BytesRef();
					ftype.readableToIndexed(fieldValue, termval);
					
					pivot.add("value", ftype.toObject(sfield, termval));
				}
				
				pivot.add("count", kv.getValue());
        
				if (subField == null) {
					values.add(pivot);
					
				} else {
					DocSet subset = null;
					if (termval == null) {
						DocSet hasVal = searcher.getDocSet(
								new TermRangeQuery(field, null, null, false, false));
						
						subset = docs.andNot(hasVal);
						
					} else {
						Query query = new TermQuery(new Term(field, termval));
						
						subset = searcher.getDocSet(query, docs);
					}
					
					SimpleFacets sf = getFacetImplementation(rb.getRequest(), 
							subset, rb.getRequest().getParams());
          
					NamedList<Integer> nl = sf.getTermCounts(subField);
					
					if (nl.size() >= minMatch) {
						pivot.add("pivot", doPivots(nl, subField, nextField, fnames, rb, subset, minMatch));
						values.add(pivot); // only add response if there are some counts
					}
				}
			}
		}
    
		// put the field back on the list
		fnames.push(nextField);
		
		return values;
	}

}
