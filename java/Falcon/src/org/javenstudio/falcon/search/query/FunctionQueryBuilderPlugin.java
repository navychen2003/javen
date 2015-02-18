package org.javenstudio.falcon.search.query;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.search.ISearchRequest;

/**
 * Create a function query from the input value.
 * <br>Other parameters: none
 * <br>Example: <code>{!func}log(foo)</code>
 */
public class FunctionQueryBuilderPlugin extends QueryBuilderPlugin {
	public static final String NAME = "func";

	@Override
	public void init(NamedList<?> args) {
		// do nothing
	}

	@Override
	public QueryBuilder createBuilder(String qstr, Params localParams, Params params, 
			ISearchRequest req) throws ErrorException {
		return new FunctionQueryBuilder(qstr, localParams, params, req);
	}
	
}
