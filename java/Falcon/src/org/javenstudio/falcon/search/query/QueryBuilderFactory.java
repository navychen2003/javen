package org.javenstudio.falcon.search.query;

import java.util.HashMap;
import java.util.Map;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.MapParams;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.search.ISearchCore;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.query.parser.DefaultQueryBuilderPlugin;
import org.javenstudio.falcon.search.query.parser.ExtendedDismaxQueryBuilderPlugin;
import org.javenstudio.falcon.search.query.parser.SpatialBoxQueryBuilderPlugin;

public class QueryBuilderFactory {

	/** internal use - name of the default parser */
	public static final String DEFAULT_QTYPE = DefaultQueryBuilderPlugin.NAME;
	
	/** internal use - name to class mappings of builtin parsers */
	private static final Object[] sStandardPlugins = {
		DefaultQueryBuilderPlugin.NAME, DefaultQueryBuilderPlugin.class,
		//OldLuceneQParserPlugin.NAME, OldLuceneQParserPlugin.class,
		FunctionQueryBuilderPlugin.NAME, FunctionQueryBuilderPlugin.class,
		//PrefixQParserPlugin.NAME, PrefixQParserPlugin.class,
		//BoostQParserPlugin.NAME, BoostQParserPlugin.class,
		//DisMaxQParserPlugin.NAME, DisMaxQParserPlugin.class,
		ExtendedDismaxQueryBuilderPlugin.NAME, ExtendedDismaxQueryBuilderPlugin.class,
		//FieldQParserPlugin.NAME, FieldQParserPlugin.class,
		//RawQParserPlugin.NAME, RawQParserPlugin.class,
		//TermQParserPlugin.NAME, TermQParserPlugin.class,
		//NestedQParserPlugin.NAME, NestedQParserPlugin.class,
		//FunctionRangeQParserPlugin.NAME, FunctionRangeQParserPlugin.class,
		//SpatialFilterQParserPlugin.NAME, SpatialFilterQParserPlugin.class,
		SpatialBoxQueryBuilderPlugin.NAME, SpatialBoxQueryBuilderPlugin.class,
		//JoinQParserPlugin.NAME, JoinQParserPlugin.class,
		//SurroundQParserPlugin.NAME, SurroundQParserPlugin.class,
	};
	
	private final ISearchCore mCore; 
	private final Map<String,QueryBuilderPlugin> mPlugins;
	
	public QueryBuilderFactory(ISearchCore core) throws ErrorException { 
		mCore = core;
		mPlugins = new HashMap<String,QueryBuilderPlugin>();
		
		initQueryBuilders();
	}
	
	public final ISearchCore getSearchCore() { 
		return mCore;
	}
	
	/** Configure the query parsers. */
	@SuppressWarnings("unchecked")
	private void initQueryBuilders() throws ErrorException { 
		mCore.initPlugins(mPlugins, QueryBuilderPlugin.class);
		
		// default parsers
		for (int i=0; i < sStandardPlugins.length; i += 2) { 
			try {
				String name = (String)sStandardPlugins[i];
				if (!mPlugins.containsKey(name)) { 
					Class<QueryBuilderPlugin> clazz = (Class<QueryBuilderPlugin>)sStandardPlugins[i+1];
					
					QueryBuilderPlugin plugin = clazz.newInstance();
					mPlugins.put(name, plugin);
					
					plugin.init(NamedList.EMPTY);
				}
			} catch (Exception ex) { 
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
			}
		}
	}
	
	public QueryBuilderPlugin getQueryPlugin(String parserName) throws ErrorException { 
		QueryBuilderPlugin plugin = mPlugins.get(parserName);
		if (plugin != null) 
			return plugin;
		
		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
				"Unknown query parser '" + parserName + "'");
	}
	
	/** 
	 * Create a <code>QueryParser</code> to parse <code>qstr</code>,
	 * assuming that the default query parser is <code>defaultParser</code>.
	 * The query parser may be overridden by local parameters in the query
	 * string itself.  For example if defaultParser=<code>"dismax"</code>
	 * and qstr=<code>foo</code>, then the dismax query parser will be used
	 * to parse and construct the query object.  However
	 * if qstr=<code>{!prefix f=myfield}foo</code>
	 * then the prefix query parser will be used.
	 */
	public QueryBuilder getQueryBuilder(String qstr, String defaultParser, 
			ISearchRequest req) throws ErrorException {
		String stringIncludingLocalParams = qstr;
		Params localParams = null;
		Params globalParams = req.getParams();
		
		boolean valFollowedParams = true;
		int localParamsEnd = -1;

		if (qstr != null && qstr.startsWith(QueryParsing.LOCALPARAM_START)) {
			Map<String, String> localMap = new HashMap<String, String>();
			localParamsEnd = QueryParsing.parseLocalParams(qstr, 0, localMap, globalParams);

			String val = localMap.get(QueryParsing.V);
			if (val != null) {
				// val was directly specified in localParams via v=<something> or v=$arg
				valFollowedParams = false;
				
			} else {
				// use the remainder of the string as the value
				valFollowedParams = true;
				val = qstr.substring(localParamsEnd);
				
				localMap.put(QueryParsing.V, val);
			}
			
			localParams = new MapParams(localMap);
		}

		String parserName;
		if (localParams == null) {
			parserName = defaultParser;
		} else {
			parserName = localParams.get(QueryParsing.TYPE, defaultParser);
			qstr = localParams.get("v");
		}

		parserName = (parserName == null) ? DEFAULT_QTYPE : parserName;

		QueryBuilderPlugin qplug = getQueryPlugin(parserName);
		QueryBuilder parser = qplug.createBuilder(qstr, localParams, req.getParams(), req);

		parser.mFactory = this;
		parser.mStringIncludingLocalParams = stringIncludingLocalParams;
		parser.mValFollowedParams = valFollowedParams;
		parser.mLocalParamsEnd = localParamsEnd;
		
		return parser;
	}
	
}
