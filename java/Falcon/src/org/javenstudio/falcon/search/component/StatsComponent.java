package org.javenstudio.falcon.search.component;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.search.ResponseBuilder;
import org.javenstudio.falcon.search.params.StatsParams;
import org.javenstudio.falcon.search.shard.ShardRequest;
import org.javenstudio.falcon.search.shard.ShardResponse;
import org.javenstudio.falcon.search.stats.SimpleStats;
import org.javenstudio.falcon.search.stats.StatsInfo;
import org.javenstudio.falcon.search.stats.StatsValues;

/**
 * Stats component calculates simple statistics on numeric field values
 * 
 * @since 1.4
 */
public class StatsComponent extends SearchComponent {
	public static final String COMPONENT_NAME = "stats";
  
	//public String getName() { return COMPONENT_NAME; }
	
	@Override
	public void prepare(ResponseBuilder rb) throws ErrorException {
		if (rb.getRequest().getParams().getBool(StatsParams.STATS, false)) {
			rb.setNeedDocSet(true);
			rb.setDoStats(true);
		}
	}

	@Override
	public void process(ResponseBuilder rb) throws ErrorException {
		if (rb.isDoStats()) {
			Params params = rb.getRequest().getParams();
			SimpleStats s = new SimpleStats(rb.getRequest(),
					rb.getResults().getDocSet(),
					params);

			// TODO ???? add this directly to the response, or to the builder?
			rb.getResponse().add("stats", s.getStatsCounts());
		}
	}

	@Override
	public int distributedProcess(ResponseBuilder rb) throws ErrorException {
		return ResponseBuilder.STAGE_DONE;
	}

	@Override
	public void modifyRequest(ResponseBuilder rb, SearchComponent who, 
			ShardRequest sreq) throws ErrorException {
		if (!rb.isDoStats()) return;

		if ((sreq.getPurpose() & ShardRequest.PURPOSE_GET_TOP_IDS) != 0) {
			sreq.setPurpose(sreq.getPurpose() | ShardRequest.PURPOSE_GET_STATS);

			StatsInfo si = rb.getStatsInfo();
			if (si == null) {
				rb.setStatsInfo(si = new StatsInfo());
				si.parse(rb.getRequest().getParams(), rb);
				// should already be true...
				// sreq.params.set(StatsParams.STATS, "true");
			}
			
		} else {
			// turn off stats on other requests
			sreq.getParams().set(StatsParams.STATS, "false");
			// we could optionally remove stats params
		}
	}

	@Override
	public void handleResponses(ResponseBuilder rb, ShardRequest sreq) throws ErrorException {
		if (!rb.isDoStats() || (sreq.getPurpose() & ShardRequest.PURPOSE_GET_STATS) == 0) 
			return;

		StatsInfo si = rb.getStatsInfo();

		for (ShardResponse srsp : sreq.getResponses()) {
			NamedList<?> stats = (NamedList<?>) srsp.getResponse().getValue("stats");

			NamedList<?> stats_fields = (NamedList<?>) stats.get("stats_fields");
			if (stats_fields != null) {
				for (int i = 0; i < stats_fields.size(); i++) {
					String field = stats_fields.getName(i);
					StatsValues stv = si.getFieldStatsValues(field);
					
					NamedList<?> shardStv = (NamedList<?>) stats_fields.get(field);
					stv.accumulate(shardStv);
				}
			}
		}
	}

	@Override
	public void finishStage(ResponseBuilder rb) throws ErrorException {
		if (!rb.isDoStats() || rb.getStage() != ResponseBuilder.STAGE_GET_FIELDS) 
			return;
		
		// wait until STAGE_GET_FIELDS
		// so that "result" is already stored in the response (for aesthetics)
		StatsInfo si = rb.getStatsInfo();

		NamedList<NamedList<Object>> stats = new NamedMap<NamedList<Object>>();
		NamedList<Object> stats_fields = new NamedMap<Object>();
		
		stats.add("stats_fields", stats_fields);
		
		for (String field : si.getFieldNameSet()) {
			NamedList<?> stv = si.getFieldStatsValues(field).getStatsValues();
			if ((Long) stv.get("count") != 0) 
				stats_fields.add(field, stv);
			else 
				stats_fields.add(field, null);
		}

		rb.getResponse().add("stats", stats);
		rb.setStatsInfo(null);
	}

}
