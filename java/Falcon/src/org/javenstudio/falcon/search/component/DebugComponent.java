package org.javenstudio.falcon.search.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.CommonParams;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.search.RequestHelper;
import org.javenstudio.falcon.search.ResponseBuilder;
import org.javenstudio.falcon.search.hits.ShardDoc;
import org.javenstudio.falcon.search.query.QueryParsing;
import org.javenstudio.falcon.search.shard.ShardRequest;
import org.javenstudio.falcon.search.shard.ShardResponse;

/**
 * Adds debugging information to a request.
 * 
 * @since 1.3
 */
public class DebugComponent extends SearchComponent {
	public static final String COMPONENT_NAME = "debug";
  
	private Set<String> mExcludeSet = new HashSet<String>(Arrays.asList("explain"));
	
	//public String getName() { return COMPONENT_NAME; }
	
	@Override
	public void prepare(ResponseBuilder rb) throws ErrorException {
		// do nothing
	}

	@Override
	public void process(ResponseBuilder rb) throws ErrorException {
		if (rb.isDebug()) {
			NamedList<Object> stdinfo = (NamedList<Object>)RequestHelper.doStandardDebug(
					rb.getRequest(), rb.getQueryString(), rb.getQuery(), rb.getResults().getDocList(), 
					rb.isDebugQuery(), rb.isDebugResults());
      
			NamedList<Object> info = rb.getDebugInfo();
			if (info == null) {
				rb.setDebugInfo(stdinfo);
				info = stdinfo;
			} else {
				info.addAll(stdinfo);
			}
      
			if (rb.isDebugQuery() && rb.getQueryBuilder() != null) 
				rb.getQueryBuilder().addDebugInfo(rb.getDebugInfo());

			if (rb.getDebugInfo() != null) {
				if (rb.isDebugQuery() && null != rb.getFilters() ) {
					info.add("filter_queries", rb.getRequest().getParams().getParams(CommonParams.FQ));
					
					List<String> fqs = new ArrayList<String>(rb.getFilters().size());
					for (IQuery fq : rb.getFilters()) {
						fqs.add(QueryParsing.toString(fq, rb.getSearchCore().getSchema()));
					}
					
					info.add("parsed_filter_queries", fqs);
				}
        
				// Add this directly here?
				rb.getResponse().add("debug", rb.getDebugInfo());
			}
		}
	}

	@Override
	public void modifyRequest(ResponseBuilder rb, SearchComponent who, ShardRequest sreq) {
		if (!rb.isDebug()) return;

		// Turn on debug to get explain only when retrieving fields
		if ((sreq.getPurpose() & ShardRequest.PURPOSE_GET_FIELDS) != 0) {
			sreq.setPurpose(sreq.getPurpose() | ShardRequest.PURPOSE_GET_DEBUG);
      
			if (rb.isDebugAll()) {
				sreq.getParams().set(CommonParams.DEBUG_QUERY, "true");
			} else if (rb.isDebugQuery()){
				sreq.getParams().set(CommonParams.DEBUG, CommonParams.QUERY);
			} else if (rb.isDebugTimings()){
				sreq.getParams().set(CommonParams.DEBUG, CommonParams.TIMING);
			} else if (rb.isDebugResults()){
				sreq.getParams().set(CommonParams.DEBUG, CommonParams.RESULTS);
			}
      
		} else {
			sreq.getParams().set(CommonParams.DEBUG_QUERY, "false");
		}
	}

	@Override
	public void handleResponses(ResponseBuilder rb, ShardRequest sreq) {
		// do nothing
	}

	@SuppressWarnings("unchecked")
	@Override
	public void finishStage(ResponseBuilder rb) throws ErrorException {
		if (rb.isDebug() && rb.getStage() == ResponseBuilder.STAGE_GET_FIELDS) {
			NamedList<Object> info = null;
			NamedList<Object> explain = new NamedMap<Object>();

			Map.Entry<String, Object>[]  arr = 
					new NamedList.NamedListEntry[rb.getResultIds().size()];

			for (ShardRequest sreq : rb.getFinishedRequests()) {
				if ((sreq.getPurpose() & ShardRequest.PURPOSE_GET_DEBUG) == 0) 
					continue;
				
				for (ShardResponse srsp : sreq.getResponses()) {
					NamedList<?> sdebug = (NamedList<?>)srsp.getResponse().getValue("debug");
					info = (NamedList<Object>)merge(sdebug, info, mExcludeSet);

					if (rb.isDebugResults()) {
						NamedList<?> sexplain = (NamedList<?>)sdebug.get("explain");
						
						for (int i = 0; i < sexplain.size(); i++) {
							String id = sexplain.getName(i);
							
							// TODO: lookup won't work for non-string ids... String vs Float
							ShardDoc sdoc = rb.getResultIds().get(id);
							
							int idx = sdoc.getPositionInResponse();
							arr[idx] = new NamedList.NamedListEntry<Object>(id, sexplain.getVal(i));
						}
					}
				}
			}

			if (rb.isDebugResults()) 
				explain = RequestHelper.removeNulls(new NamedMap<Object>(arr));

			if (info == null) {
				// No responses were received from shards. Show local query info.
				info = new NamedMap<Object>();
				
				RequestHelper.doStandardQueryDebug(
						rb.getRequest(), rb.getQueryString(),  rb.getQuery(), 
						rb.isDebugQuery(), info);
				
				if (rb.isDebugQuery() && rb.getQueryBuilder() != null) 
					rb.getQueryBuilder().addDebugInfo(info);
			}
			
			if (rb.isDebugResults()) {
				int idx = info.indexOf("explain", 0);
				if (idx >= 0) 
					info.setVal(idx, explain);
				else 
					info.add("explain", explain);
			}

			rb.setDebugInfo(info);
			rb.getResponse().add("debug", rb.getDebugInfo() );      
		}
	}

	@SuppressWarnings("unchecked")
	protected Object merge(Object source, Object dest, Set<String> exclude) {
		if (source == null) return dest;
		if (dest == null) {
			if (source instanceof NamedList) {
				dest = source instanceof NamedMap ? 
						new NamedMap<Object>() : new NamedList<Object>();
			} else 
				return source;
			
		} else {
			if (dest instanceof Collection) {
				if (source instanceof Collection) 
					((Collection<Object>)dest).addAll((Collection<Object>)source);
				else 
					((Collection<Object>)dest).add(source);
				
				return dest;
				
			} else if (source instanceof Number) {
				if (dest instanceof Number) {
					if (source instanceof Double || dest instanceof Double) 
						return ((Number)source).doubleValue() + ((Number)dest).doubleValue();
					
					return ((Number)source).longValue() + ((Number)dest).longValue();
				}
				// fall through
				
			} else if (source instanceof String) {
				if (source.equals(dest)) 
					return dest;
				
				// fall through
			}
		}

		if (source instanceof NamedList && dest instanceof NamedList) {
			NamedList<Object> tmp = new NamedList<Object>();
			NamedList<Object> sl = (NamedList<Object>)source;
			NamedList<Object> dl = (NamedList<Object>)dest;
			
			for (int i=0; i < sl.size(); i++) {
				String skey = sl.getName(i);
				if (exclude != null && exclude.contains(skey)) 
					continue;
				
				Object sval = sl.getVal(i);
				int didx = -1;

				// optimize case where elements are in same position
				if (i < dl.size()) {
					String dkey = dl.getName(i);
					if (skey == dkey || (skey!=null && skey.equals(dkey))) 
						didx = i;
				}

				if (didx == -1) 
					didx = dl.indexOf(skey, 0);

				if (didx == -1) 
					tmp.add(skey, merge(sval, null, null));
				else 
					dl.setVal(didx, merge(sval, dl.getVal(didx), null));
			}
			
			dl.addAll(tmp);
			
			return dl;
		}

		// merge unlike elements in a list
		List<Object> t = new ArrayList<Object>();
		t.add(dest);
		t.add(source);
		
		return t;
	}

}
