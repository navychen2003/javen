package org.javenstudio.falcon.search.query.parser;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.query.QueryBuilder;
import org.javenstudio.falcon.search.query.QueryBuilderPlugin;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.Params;

/**
 * An advanced multi-field query parser based on the DisMax parser.
 * See Wiki page http://wiki.apache.org/solr/ExtendedDisMax
 */
public class ExtendedDismaxQueryBuilderPlugin extends QueryBuilderPlugin {
	public static final String NAME = "edismax";

	@Override
	public void init(NamedList<?> args) {
		// do nothing
	}

	@Override
	public QueryBuilder createBuilder(String qstr, Params localParams, Params params, 
			ISearchRequest req) throws ErrorException {
		return new ExtendedDismaxQueryBuilder(qstr, localParams, params, req);
	}
	
}
