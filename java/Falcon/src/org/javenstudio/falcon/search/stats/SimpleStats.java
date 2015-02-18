package org.javenstudio.falcon.search.stats;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.javenstudio.common.indexdb.IDocTermsIndex;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.hornet.search.cache.FieldCache;
import org.javenstudio.falcon.search.Searcher;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.facet.FieldFacetStats;
import org.javenstudio.falcon.search.hits.DocIterator;
import org.javenstudio.falcon.search.hits.DocSet;
import org.javenstudio.falcon.search.hits.UnInvertedField;
import org.javenstudio.falcon.search.params.ShardParams;
import org.javenstudio.falcon.search.params.StatsParams;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.search.schema.SchemaFieldType;
import org.javenstudio.falcon.search.schema.TrieFieldType;

public class SimpleStats {
	
	/** Searcher to use for all calculations */
	protected Searcher mSearcher;
	
	/** The main set of documents */
	protected DocSet mDocs;
	
	/** Configuration params behavior should be driven by */
	protected Params mParams;

	protected ISearchRequest mRequest;

	public SimpleStats(ISearchRequest req, DocSet docs, 
			Params params) throws ErrorException {
		mRequest = req;
		mSearcher = req.getSearcher();
		mDocs = docs;
		mParams = params;
	}

	public NamedList<Object> getStatsCounts() throws ErrorException {
		NamedList<Object> res = new NamedMap<Object>();
		res.add("stats_fields", getStatsFields());
		return res;
	}

	public NamedList<Object> getStatsFields() throws ErrorException {
		NamedList<Object> res = new NamedMap<Object>();
		
		String[] statsFs = mParams.getParams(StatsParams.STATS_FIELD);
		boolean isShard = mParams.getBool(ShardParams.IS_SHARD, false);
		
		if (statsFs != null) {
			for (String fieldName : statsFs) {
				String[] facets = mParams.getFieldParams(fieldName, StatsParams.STATS_FACET);
				if (facets == null) 
					facets = new String[0]; // make sure it is something...
				
				SchemaField sf = mSearcher.getSchema().getField(fieldName);
				SchemaFieldType ft = sf.getType();
				NamedList<?> stv;

				// Currently, only UnInvertedField can deal with multi-part trie fields
				String prefix = TrieFieldType.getMainValuePrefix(ft);

				if (sf.isMultiValued() || ft.isMultiValuedFieldCache() || prefix != null) {
					//use UnInvertedField for multivalued fields
					UnInvertedField uif = UnInvertedField.getUnInvertedField(fieldName, mSearcher);
					stv = uif.getStats(mSearcher, mDocs, facets).getStatsValues();
					
				} else {
					stv = getFieldCacheStats(fieldName, facets);
				}
				
				if (isShard == true || (Long) stv.get("count") > 0) 
					res.add(fieldName, stv);
				else 
					res.add(fieldName, null);
			}
		}
		
		return res;
	}
  
	// why does this use a top-level field cache?
	public NamedList<?> getFieldCacheStats(String fieldName, 
			String[] facet) throws ErrorException {
		SchemaField sf = mSearcher.getSchema().getField(fieldName);
    
		IDocTermsIndex si;
		try {
			si = FieldCache.DEFAULT.getTermsIndex(
					mSearcher.getAtomicReader(), fieldName);
		} catch (IOException e) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"failed to open field cache for: " + fieldName, e);
		}
		
		StatsValues allstats = StatsValuesFactory.createStatsValues(sf);
		
		final int nTerms = si.getNumOrd();
		if (nTerms <= 0 || mDocs.size() <= 0) 
			return allstats.getStatsValues();

		// don't worry about faceting if no documents match...
		List<FieldFacetStats> facetStats = new ArrayList<FieldFacetStats>();
		IDocTermsIndex facetTermsIndex;
		
		for (String facetField : facet) {
			SchemaField fsf = mSearcher.getSchema().getField(facetField);

			if (fsf.isMultiValued()) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
						"Stats can only facet on single-valued fields, not: " + facetField);
			}

			try {
				facetTermsIndex = FieldCache.DEFAULT.getTermsIndex(
						mSearcher.getAtomicReader(), facetField);
			} catch (IOException e) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"failed to open field cache for: " + facetField, e);
			}
			
			facetStats.add(new FieldFacetStats(
					facetField, facetTermsIndex, sf, fsf, nTerms));
		}
    
		final BytesRef tempBR = new BytesRef();
		DocIterator iter = mDocs.iterator();
		
		while (iter.hasNext()) {
			int docID = iter.nextDoc();
			BytesRef raw = si.lookup(si.getOrd(docID), tempBR);
			
			if (raw.getLength() > 0) 
				allstats.accumulate(raw);
			else 
				allstats.missing();
			
			// now update the facets
			for (FieldFacetStats f : facetStats) {
				f.facet(docID, raw);
			}
		}

		for (FieldFacetStats f : facetStats) {
			allstats.addFacet(f.getName(), f.getFacetStatsValues());
		}
		
		return allstats.getStatsValues();
	}
	
}
