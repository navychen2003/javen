package org.javenstudio.falcon.search.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.CommonParams;
import org.javenstudio.falcon.util.ModifiableParams;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.util.StrHelper;
import org.javenstudio.hornet.search.OpenBitSet;
import org.javenstudio.falcon.search.ResponseBuilder;
import org.javenstudio.falcon.search.facet.DistributedFieldFacet;
import org.javenstudio.falcon.search.facet.FacetInfo;
import org.javenstudio.falcon.search.facet.QueryFacet;
import org.javenstudio.falcon.search.facet.SimpleFacets;
import org.javenstudio.falcon.search.hits.ShardFacetCount;
import org.javenstudio.falcon.search.params.FacetParams;
import org.javenstudio.falcon.search.params.ShardParams;
import org.javenstudio.falcon.search.query.QueryParsing;
import org.javenstudio.falcon.search.shard.ShardRequest;
import org.javenstudio.falcon.search.shard.ShardResponse;

/**
 * TODO!
 *
 * @since 1.3
 */
public class FacetComponent extends SearchComponent {
	static Logger LOG = Logger.getLogger(FacetComponent.class);

	public static final String COMPONENT_NAME = "facet";
	
	static final String PIVOT_KEY = "facet_pivot";
	static final String COMMAND_PREFIX = "{!" + CommonParams.TERMS + "=$";

	private PivotFacetHelper mPivotHelper;

	//public String getName() { return COMPONENT_NAME; }
	
	@Override
	public void init(NamedList<?> args) {
		mPivotHelper = new PivotFacetHelper(); // Maybe this would configurable?
	}

	@Override
	public void prepare(ResponseBuilder rb) throws ErrorException {
		if (rb.getRequest().getParams().getBool(FacetParams.FACET, false)) {
			rb.setNeedDocSet(true);
			rb.setDoFacets(true);
		}
	}

	/**
	 * Actually run the query
	 */
	@Override
	public void process(ResponseBuilder rb) throws ErrorException {
		if (rb.isDoFacets()) {
			Params params = rb.getRequest().getParams();
			SimpleFacets f = new SimpleFacets(params, rb);

			NamedList<Object> counts = f.getFacetCounts();
			String[] pivots = params.getParams(FacetParams.FACET_PIVOT);
			
			if (pivots != null && pivots.length > 0) {
				NamedList<?> v = mPivotHelper.process(rb, params, pivots);
				if (v != null) 
					counts.add(PIVOT_KEY, v);
			}
      
			// TODO ???? add this directly to the response, or to the builder?
			rb.getResponse().add("facet_counts", counts);
		}
	}

	@Override
	public int distributedProcess(ResponseBuilder rb) throws ErrorException {
		if (!rb.isDoFacets()) 
			return ResponseBuilder.STAGE_DONE;
		
		if (rb.getStage() == ResponseBuilder.STAGE_GET_FIELDS) {
			// overlap facet refinement requests (those shards that we need a count for
			// particular facet values from), where possible, with
			// the requests to get fields (because we know that is the
			// only other required phase).
			// We do this in distributedProcess so we can look at all of the
			// requests in the outgoing queue at once.

			for (int shardNum=0; shardNum < rb.getShardCount(); shardNum++) {
				List<String> refinements = null;

				for (DistributedFieldFacet dff : rb.getFacetInfo().getFieldFacetValues()) {
					if (!dff.needRefinements()) continue;
					
					List<String> refList = dff.getRefineListAt(shardNum);
					if (refList == null || refList.size() == 0) 
						continue;

					// reuse the same key that was used for the main facet
					String key = dff.getKey(); 
					String termsKey = key + "__terms";
					String termsVal = StrHelper.join(refList, ',');

					String facetCommand;
					// add terms into the original facet.field command
					// do it via parameter reference to avoid another layer of encoding.

					String termsKeyEncoded = QueryParsing.encodeLocalParamVal(termsKey);
					if (dff.getLocalParams() != null) {
						facetCommand = COMMAND_PREFIX + termsKeyEncoded 
								+ " " + dff.getFacetString().substring(2);
						
					} else {
						facetCommand = COMMAND_PREFIX + termsKeyEncoded 
								+ '}' + dff.getFieldName();
					}

					if (refinements == null) 
						refinements = new ArrayList<String>();

					refinements.add(facetCommand);
					refinements.add(termsKey);
					refinements.add(termsVal);
				}

				if (refinements == null) 
					continue;

				String shard = rb.getShardAt(shardNum);
				ShardRequest refine = null;
				boolean newRequest = false;

				// try to find a request that is already going out to that shard.
				// If nshards becomes to great, we way want to move to hashing for better
				// scalability.
				for (ShardRequest sreq : rb.getOutgoingRequests()) {
					if ((sreq.getPurpose() & ShardRequest.PURPOSE_GET_FIELDS) != 0 &&
						 sreq.getShardCount() == 1 && sreq.getShardAt(0).equals(shard)) {
						refine = sreq;
						break;
					}
				}

				if (refine == null) {
					// we didn't find any other suitable requests going out to that shard, so
					// create one ourselves.
					newRequest = true;
					
					refine = new ShardRequest();
					refine.setShards(new String[]{rb.getShardAt(shardNum)});
					refine.setParams(new ModifiableParams(rb.getRequest().getParams()));
					// don't request any documents
					refine.getParams().remove(CommonParams.START);
					refine.getParams().set(CommonParams.ROWS,"0");
				}

				refine.setPurpose(refine.getPurpose() | ShardRequest.PURPOSE_REFINE_FACETS);
				refine.getParams().set(FacetParams.FACET, "true");
				refine.getParams().remove(FacetParams.FACET_FIELD);
				refine.getParams().remove(FacetParams.FACET_QUERY);

				for (int i=0; i < refinements.size();) {
					String facetCommand = refinements.get(i++);
					String termsKey = refinements.get(i++);
					String termsVal = refinements.get(i++);

					refine.getParams().add(FacetParams.FACET_FIELD, facetCommand);
					refine.getParams().set(termsKey, termsVal);
				}

				if (newRequest) 
					rb.addRequest(this, refine);
			}
		}

		return ResponseBuilder.STAGE_DONE;
	}

	@Override
	public void modifyRequest(ResponseBuilder rb, SearchComponent who, 
			ShardRequest sreq) throws ErrorException {
		if (!rb.isDoFacets()) return;

		if ((sreq.getPurpose() & ShardRequest.PURPOSE_GET_TOP_IDS) != 0) {
			sreq.setPurpose(sreq.getPurpose() | ShardRequest.PURPOSE_GET_FACETS);

			FacetInfo fi = rb.getFacetInfo();
			if (fi == null) {
				rb.setFacetInfo(fi = new FacetInfo());
				fi.parse(rb.getRequest().getParams(), rb);
				// should already be true...
				// sreq.params.set(FacetParams.FACET, "true");
			}

			sreq.getParams().remove(FacetParams.FACET_MINCOUNT);
			sreq.getParams().remove(FacetParams.FACET_OFFSET);
			sreq.getParams().remove(FacetParams.FACET_LIMIT);

			for (DistributedFieldFacet dff : fi.getFieldFacetValues()) {
				String paramStart = "f." + dff.getFieldName() + '.';
				
				sreq.getParams().remove(paramStart + FacetParams.FACET_MINCOUNT);
				sreq.getParams().remove(paramStart + FacetParams.FACET_OFFSET);

				dff.setInitialLimit(dff.getLimit() <= 0 ? dff.getLimit() : 
						dff.getOffset() + dff.getLimit());

				if (dff.getSortParam().equals(FacetParams.FACET_SORT_COUNT)) {
					if (dff.getLimit() > 0) {
						// set the initial limit higher to increase accuracy
						dff.setInitialLimit((int)(dff.getInitialLimit() * 1.5) + 10);
						// TODO: we could change this to 1, but would then need more refinement for small facet result sets?
						dff.setInitialMinCount(0); 
						
					} else {
						// if limit==-1, then no need to artificially lower mincount to 0 if it's 1
						dff.setInitialMinCount(Math.min(dff.getMinCount(), 1));
					}
					
				} else {
					// we're sorting by index order.
					// if minCount==0, we should always be able to get accurate results 
					//   w/o over-requesting or refining
					// if minCount==1, we should be able to get accurate results 
					//   w/o over-requesting, but we'll need to refine
					// if minCount==n (>1), we can set the initialMincount to minCount/nShards, rounded up.
					// For example, we know that if minCount=10 and we have 3 shards, 
					//   then at least one shard must have a count of 4 for the term
					// For the minCount>1 case, we can generate too short of a list 
					//   (miss terms at the end of the list) unless limit==-1
					// For example: each shard could produce a list of top 10, 
					//   but some of those could fail to make it into the combined list (i.e.
					//   we needed to go beyond the top 10 to generate the top 10 combined). 
					//   Overrequesting can help a little here, but not as
					//   much as when sorting by count.
					
					if (dff.getMinCount() <= 1) {
						dff.setInitialMinCount(dff.getMinCount());
						
					} else {
						dff.setInitialMinCount((int)Math.ceil((double)dff.getMinCount() / rb.getSliceCount()));
						// dff.initialMincount = 1;
					}
				}

				if (dff.getInitialMinCount() != 0) {
					sreq.getParams().set(paramStart + FacetParams.FACET_MINCOUNT, 
							dff.getInitialMinCount());
				}

				// Currently this is for testing only and allows overriding of the
				// facet.limit set to the shards
				dff.setInitialLimit(rb.getRequest().getParams().getInt("facet.shard.limit", 
						dff.getInitialLimit()));

				sreq.getParams().set(paramStart + FacetParams.FACET_LIMIT, 
						dff.getInitialLimit());
			}
			
		} else {
			// turn off faceting on other requests
			sreq.getParams().set(FacetParams.FACET, "false");
			// we could optionally remove faceting params
		}
	}

	@Override
	public void handleResponses(ResponseBuilder rb, ShardRequest sreq) throws ErrorException {
		if (!rb.isDoFacets()) return;

		if ((sreq.getPurpose() & ShardRequest.PURPOSE_GET_FACETS) != 0) {
			countFacets(rb, sreq);
			
		} else if ((sreq.getPurpose() & ShardRequest.PURPOSE_REFINE_FACETS) != 0) {
			refineFacets(rb, sreq);
		}
	}

	private void countFacets(ResponseBuilder rb, ShardRequest sreq) throws ErrorException {
		FacetInfo fi = rb.getFacetInfo();

		for (ShardResponse srsp: sreq.getResponses()) {
			int shardNum = rb.getShardNum(srsp.getShard());
			NamedList<?> facet_counts = null;
			
			try {
				facet_counts = (NamedList<?>)srsp.getResponse().getValue("facet_counts");
				
			} catch(Exception ex) {
				if (rb.getRequest().getParams().getBool(ShardParams.SHARDS_TOLERANT, false)) 
					continue; // looks like a shard did not return anything
        
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"Unable to read facet info for shard: " + srsp.getShard(), ex);
			}

			// handle facet queries
			NamedList<?> facet_queries = (NamedList<?>)facet_counts.get("facet_queries");
			
			if (facet_queries != null) {
				for (int i=0; i < facet_queries.size(); i++) {
					String returnedKey = facet_queries.getName(i);
					long count = ((Number)facet_queries.getVal(i)).longValue();
					
					QueryFacet qf = fi.getQueryFacet(returnedKey);
					qf.setCount(qf.getCount() + count);
				}
			}

			// step through each facet.field, adding results from this shard
			NamedList<?> facet_fields = (NamedList<?>)facet_counts.get("facet_fields");
    
			if (facet_fields != null) {
				for (DistributedFieldFacet dff : fi.getFieldFacetValues()) {
					dff.add(shardNum, (NamedList<?>)facet_fields.get(dff.getKey()), 
							dff.getInitialLimit());
				}
			}

			// Distributed facet_dates
			//
			// The implementation below uses the first encountered shard's 
			// facet_dates as the basis for subsequent shards' data to be merged.
			// (the "NOW" param should ensure consistency)
			@SuppressWarnings("unchecked")
			NamedMap<NamedMap<Object>> facet_dates = (NamedMap<NamedMap<Object>>) 
					facet_counts.get("facet_dates");
      
			if (facet_dates != null) {
				// go through each facet_date
				for (Map.Entry<String,NamedMap<Object>> entry : facet_dates) {
					final String field = entry.getKey();
					
					if (fi.getDateFacet(field) == null) { 
						// first time we've seen this field, no merging
						fi.addDateFacet(field, entry.getValue());

					} else { 
						// not the first time, merge current field

						NamedMap<Object> shardFieldValues = entry.getValue();
						NamedMap<Object> existFieldValues = fi.getDateFacet(field);

						for (Map.Entry<String,Object> existPair : existFieldValues) {
							final String key = existPair.getKey();
							if (key.equals("gap") || key.equals("end") || key.equals("start")) {
								// we can skip these, must all be the same across shards
								continue; 
							}
							
							// can be null if inconsistencies in shards responses
							Integer newValue = (Integer) shardFieldValues.get(key);
							if  (newValue != null) {
								Integer oldValue = ((Integer) existPair.getValue());
								existPair.setValue(oldValue + newValue);
							}
						}
					}
				}
			}

			// Distributed facet_ranges
			//
			// The implementation below uses the first encountered shard's 
			// facet_ranges as the basis for subsequent shards' data to be merged.
			@SuppressWarnings("unchecked")
			NamedMap<NamedMap<Object>> facet_ranges = (NamedMap<NamedMap<Object>>) 
					facet_counts.get("facet_ranges");
      
			if (facet_ranges != null) {
				// go through each facet_range
				for (Map.Entry<String,NamedMap<Object>> entry : facet_ranges) {
					final String field = entry.getKey();
					
					if (fi.getRangeFacet(field) == null) { 
						// first time we've seen this field, no merging
						fi.addRangeFacet(field, entry.getValue());

					} else { 
						// not the first time, merge current field counts

						@SuppressWarnings("unchecked")
						NamedList<Integer> shardFieldValues = (NamedList<Integer>) 
								entry.getValue().get("counts");

						@SuppressWarnings("unchecked")
						NamedList<Integer> existFieldValues = (NamedList<Integer>) 
								fi.getRangeFacet(field).get("counts");

						for (Map.Entry<String,Integer> existPair : existFieldValues) {
							final String key = existPair.getKey();
							
							// can be null if inconsistencies in shards responses
							Integer newValue = shardFieldValues.get(key);
							if  (newValue != null) {
								Integer oldValue = existPair.getValue();
								existPair.setValue(oldValue + newValue);
							}
						}
					}
				}
			}
		}

		//
		// This code currently assumes that there will be only a single
		// request ((with responses from all shards) sent out to get facets...
		// otherwise we would need to wait until all facet responses were received.
		//

		for (DistributedFieldFacet dff : fi.getFieldFacetValues()) {
			// no need to check these facets for refinement
			if (dff.getInitialLimit() <= 0 && dff.getInitialMinCount() == 0) 
				continue;

			// only other case where index-sort doesn't need refinement is if minCount==0
			if (dff.getMinCount() == 0 && dff.getSortParam().equals(FacetParams.FACET_SORT_INDEX)) 
				continue;

			@SuppressWarnings("unchecked") // generic array's are annoying
			List<String>[] tmp = (List<String>[]) new List[rb.getShardCount()];
			dff.setRefineLists(tmp);

			ShardFacetCount[] counts = dff.getCountSorted();
			int ntop = Math.min(counts.length, dff.getLimit() >= 0 ? 
					dff.getOffset() + dff.getLimit() : Integer.MAX_VALUE);
			long smallestCount = (counts.length == 0) ? 0 : counts[ntop-1].getCount();

			for (int i=0; i < counts.length; i++) {
				ShardFacetCount sfc = counts[i];
				boolean needRefinement = false;

				if (i < ntop) {
					// automatically flag the top values for refinement
					// this should always be true for facet.sort=index
					needRefinement = true;
					
				} else {
					// this logic should only be invoked for facet.sort=index (for now)

					// calculate the maximum value that this term may have
					// and if it is >= smallestCount, then flag for refinement
					long maxCount = sfc.getCount();
					
					for (int shardNum=0; shardNum < rb.getShardCount(); shardNum++) {
						OpenBitSet obs = dff.getCountedBitSetAt(shardNum);
						if (obs != null && !obs.get(sfc.getTermNum())) { 
							// obs can be null if a shard request failed
							// if missing from this shard, add the max it could be
							maxCount += dff.maxPossible(sfc,shardNum);
						}
					}
					
					if (maxCount >= smallestCount) {
						// TODO: on a tie, we could check the term values
						needRefinement = true;
					}
				}

				if (needRefinement) {
					// add a query for each shard missing the term that needs refinement
					for (int shardNum=0; shardNum < rb.getShardCount(); shardNum++) {
						OpenBitSet obs = dff.getCountedBitSetAt(shardNum);
						
						if (obs != null && !obs.get(sfc.getTermNum()) && dff.maxPossible(sfc,shardNum) > 0) {
							dff.setNeedRefinements(true);
							List<String> lst = dff.getRefineListAt(shardNum);
							
							if (lst == null) {
								lst = new ArrayList<String>();
								dff.setRefineListAt(shardNum, lst);
							}
							
							lst.add(sfc.getName());
						}
					}
				}
			}
		}
	}

	private void refineFacets(ResponseBuilder rb, ShardRequest sreq) {
		FacetInfo fi = rb.getFacetInfo();

		for (ShardResponse srsp: sreq.getResponses()) {
			// int shardNum = rb.getShardNum(srsp.shard);
			NamedList<?> facet_counts = (NamedList<?>)srsp.getResponse().getValue("facet_counts");
			NamedList<?> facet_fields = (NamedList<?>)facet_counts.get("facet_fields");

			// this can happen when there's an exception 
			if (facet_fields == null) 
				continue; 

			for (int i=0; i < facet_fields.size(); i++) {
				String key = facet_fields.getName(i);
				DistributedFieldFacet dff = fi.getFieldFacet(key);
				if (dff == null) continue;

				NamedList<?> shardCounts = (NamedList<?>)facet_fields.getVal(i);

				for (int j=0; j < shardCounts.size(); j++) {
					String name = shardCounts.getName(j);
					long count = ((Number)shardCounts.getVal(j)).longValue();
					
					ShardFacetCount sfc = dff.getShardFacetCount(name);
					if (sfc == null) {
						// we got back a term we didn't ask for?
						LOG.error("Unexpected term returned for facet refining. key=" + key + " term='" + name + "'"
								+ " request params=" + sreq.getParams()
								//+ " toRefine=" + dff._toRefine
								+ " response=" + shardCounts);
						continue;
					}
					
					sfc.setCount(sfc.getCount() + count);
				}
			}
		}
	}

	@Override
	public void finishStage(ResponseBuilder rb) throws ErrorException {
		if (!rb.isDoFacets() || rb.getStage() != ResponseBuilder.STAGE_GET_FIELDS) 
			return;
		
		// wait until STAGE_GET_FIELDS
		// so that "result" is already stored in the response (for aesthetics)
		FacetInfo fi = rb.getFacetInfo();
		
		NamedList<Object> facet_counts = new NamedMap<Object>();
		NamedList<Number> facet_queries = new NamedMap<Number>();
		
		facet_counts.add("facet_queries",facet_queries);
		
		for (QueryFacet qf : fi.getQueryFacetValues()) {
			facet_queries.add(qf.getKey(), toNumber(qf.getCount()));
		}

		NamedList<Object> facet_fields = new NamedMap<Object>();
		facet_counts.add("facet_fields", facet_fields);

		for (DistributedFieldFacet dff : fi.getFieldFacetValues()) {
			// order is more important for facets
			NamedList<Object> fieldCounts = new NamedList<Object>(); 
			facet_fields.add(dff.getKey(), fieldCounts);

			ShardFacetCount[] counts;
			boolean countSorted = dff.getSortParam().equals(FacetParams.FACET_SORT_COUNT);
			
			if (countSorted) {
				counts = dff.getSortedShardFacetCount();
				if (counts == null || dff.needRefinements()) 
					counts = dff.getCountSorted();
        
			} else if (dff.getSortParam().equals(FacetParams.FACET_SORT_INDEX)) {
				counts = dff.getLexSorted();
				
			} else { // TODO: log error or throw exception?
				counts = dff.getLexSorted();
			}

			if (countSorted) {
				int end = dff.getLimit() < 0 ? counts.length : 
					Math.min(dff.getOffset() + dff.getLimit(), counts.length);
				
				for (int i = dff.getOffset(); i < end; i++) {
					if (counts[i].getCount() < dff.getMinCount()) 
						break;
          
					fieldCounts.add(counts[i].getName(), toNumber(counts[i].getCount()));
				}
				
			} else {
				int off = dff.getOffset();
				int lim = dff.getLimit() >= 0 ? dff.getLimit() : Integer.MAX_VALUE;

				// index order...
				for (int i=0; i < counts.length; i++) {
					long count = counts[i].getCount();
					if (count < dff.getMinCount()) 
						continue;
					
					if (off > 0) {
						off --;
						continue;
					}
					
					if (lim <= 0) 
						break;
          
					lim--;
					fieldCounts.add(counts[i].getName(), toNumber(count));
				}
			}

			if (dff.isMissing()) 
				fieldCounts.add(null, toNumber(dff.getMissingCount()));
		}

		facet_counts.add("facet_dates", fi.getDateFacets());
		facet_counts.add("facet_ranges", fi.getRangeFacets());

		rb.getResponse().add("facet_counts", facet_counts);
		rb.setFacetInfo(null);  // could be big, so release asap
	}

	// use <int> tags for smaller facet counts (better back compatibility)
	protected Number toNumber(long val) {
		if (val < Integer.MAX_VALUE) return (int)val;
		else return val;
	}
	
	protected Number toNumber(Long val) {
		if (val.longValue() < Integer.MAX_VALUE) return val.intValue();
		else return val;
	}

}
