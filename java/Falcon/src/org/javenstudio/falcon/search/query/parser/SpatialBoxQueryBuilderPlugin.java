package org.javenstudio.falcon.search.query.parser;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.query.QueryBuilder;
import org.javenstudio.falcon.search.query.QueryBuilderPlugin;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.Params;

public class SpatialBoxQueryBuilderPlugin extends QueryBuilderPlugin {
	public static final String NAME = "bbox";
	
	@Override
	public void init(NamedList<?> args) {
		// do nothing
	}

	@Override
	public QueryBuilder createBuilder(String qstr, Params localParams, Params params, 
			ISearchRequest req) throws ErrorException {
		return new SpatialFilterQueryBuilder(qstr, localParams, params, req, true);
	}
	
}
