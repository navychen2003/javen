package org.javenstudio.falcon.search.query.parser;

import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.query.QueryBuilder;
import org.javenstudio.falcon.search.query.QueryBuilderPlugin;
import org.javenstudio.falcon.search.query.QueryParsing;
import org.javenstudio.falcon.util.CommonParams;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.panda.query.ParseException;

/**
 * Parse variant on the QueryParser syntax.
 * <br>Other parameters:<ul>
 * <li>q.op - the default operator "OR" or "AND"</li>
 * <li>df - the default field name</li>
 * </ul>
 * <br>Example: <code>{!text q.op=AND df=text sort='price asc'}myfield:foo +bar -baz</code>
 */
public class DefaultQueryBuilderPlugin extends QueryBuilderPlugin {
	static final Logger LOG = Logger.getLogger(DefaultQueryBuilderPlugin.class);

	public static final String NAME = "default";
	
	@Override
	public void init(NamedList<?> args) throws ErrorException {
		// do nothing
	}
	
	@Override
	public QueryBuilder createBuilder(String qstr, 
			Params localParams, Params params, ISearchRequest req) 
			throws ErrorException { 
		return new DefaultQueryBuilder(qstr, localParams, params, req);
	}
	
	static class DefaultQueryBuilder extends QueryBuilder {
		private DefaultQueryParser mParser;

		public DefaultQueryBuilder(String qstr, Params localParams, 
				Params params, ISearchRequest req) throws ErrorException {
			super(qstr, localParams, params, req);
		}

		@Override
		public IQuery parse() throws ErrorException {
			String qstr = getQueryString();
			if (qstr == null || qstr.length() == 0) 
				return null;

			String defaultField = getParam(CommonParams.DF);
			if (defaultField == null) 
				defaultField = getSearchCore().getSchema().getDefaultSearchFieldName();
			
			if (LOG.isDebugEnabled())
				LOG.debug("parseQuery: query=" + qstr + " defaultField=" + defaultField);
			
			try {
		    	mParser = new DefaultQueryParser(this, defaultField);
	
		    	mParser.setDefaultOperator(QueryParsing.getQueryParserDefaultOperator(
		    			getSearchCore().getSchema(), getParam(QueryParsing.OP)));
	
		    	return mParser.parse(qstr);
			} catch (ParseException ex) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, ex);
			}
		}

		@Override
		public String[] getDefaultHighlightFields() {
			return mParser == null ? new String[]{} : new String[]{mParser.getFieldName()};
		}
	}
	
}
