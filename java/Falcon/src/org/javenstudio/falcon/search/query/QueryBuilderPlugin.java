package org.javenstudio.falcon.search.query;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedListPlugin;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.search.ISearchRequest;

public abstract class QueryBuilderPlugin implements NamedListPlugin {

	/** return a {@link QParser} */
	public abstract QueryBuilder createBuilder(String qstr, 
			Params localParams, Params params, ISearchRequest req) 
			throws ErrorException;
	
}
