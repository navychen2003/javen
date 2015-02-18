package org.javenstudio.falcon.search.stats;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.search.ResponseBuilder;
import org.javenstudio.falcon.search.params.StatsParams;
import org.javenstudio.falcon.search.schema.SchemaField;

public class StatsInfo {
	
	private final Map<String, StatsValues> mStatsFields = 
			new HashMap<String, StatsValues>();

	public final Set<String> getFieldNameSet() { 
		return mStatsFields.keySet();
	}
	
	public StatsValues getFieldStatsValues(String fieldName) { 
		return mStatsFields.get(fieldName);
	}
	
	public void parse(Params params, ResponseBuilder rb) throws ErrorException {
		mStatsFields.clear();

		String[] statsFs = params.getParams(StatsParams.STATS_FIELD);
		if (statsFs != null) {
			for (String field : statsFs) {
				SchemaField sf = rb.getSearchCore().getSchema().getField(field);
				mStatsFields.put(field, StatsValuesFactory.createStatsValues(sf));
			}
		}
	}
	
}
