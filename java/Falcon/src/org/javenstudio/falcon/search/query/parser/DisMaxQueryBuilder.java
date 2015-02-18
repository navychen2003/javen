package org.javenstudio.falcon.search.query.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.indexdb.IBooleanClause;
import org.javenstudio.common.indexdb.IBooleanQuery;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.params.DisMaxParams;
import org.javenstudio.falcon.search.query.FunctionQueryBuilderPlugin;
import org.javenstudio.falcon.search.query.QueryBuilder;
import org.javenstudio.falcon.search.query.QueryParsing;
import org.javenstudio.falcon.search.query.QueryUtils;
import org.javenstudio.falcon.search.schema.IndexSchema;
import org.javenstudio.falcon.util.CommonParams;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.hornet.search.query.BooleanQuery;
import org.javenstudio.panda.query.ParseException;
import org.javenstudio.panda.query.QueryParser;

/**
 * Query parser for dismax queries
 * <p>
 * <b>Note: This API is experimental and may change in non backward-compatible 
 * ways in the future</b>
 *
 */
public class DisMaxQueryBuilder extends QueryBuilder {

	/**
	 * A field we can't ever find in any schema, so we can safely tell 
	 * DisjunctionMaxQueryParser to use it as our
	 * defaultField, and map aliases from it to any field in our schema.
	 */
	private static String IMPOSSIBLE_FIELD_NAME = "\uFFFC\uFFFC\uFFFC";

	/**
	 * Applies the appropriate default rules for the "mm" param based on the 
	 * effective value of the "q.op" param
	 *
	 * @see QueryParsing#getQueryParserDefaultOperator
	 * @see QueryParsing#OP
	 * @see DisMaxParams#MM
	 */
	public static String parseMinShouldMatch(final IndexSchema schema, 
			final Params params) throws ErrorException {
		QueryParser.Operator op = QueryParsing.getQueryParserDefaultOperator(
				schema, params.get(QueryParsing.OP));
		
		return params.get(DisMaxParams.MM, 
				op.equals(QueryParser.Operator.AND) ? "100%" : "0%");
	}

	/**
	 * Uses {@link SolrPluginUtils#parseFieldBoosts(String)} with the 'qf' parameter. 
	 * Falls back to the 'df' parameter
	 * or {@link org.apache.solr.schema.IndexSchema#getDefaultSearchFieldName()}.
	 */
	public static Map<String, Float> parseQueryFields(final IndexSchema indexSchema, 
			final Params params) throws ErrorException {
		Map<String, Float> queryFields = QueryUtils.parseFieldBoosts(
				params.getParams(DisMaxParams.QF));
		
		if (queryFields.isEmpty()) {
			String df = QueryParsing.getDefaultField(indexSchema, params.get(CommonParams.DF));
			if (df == null) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Neither " + DisMaxParams.QF + ", " + CommonParams.DF + 
						", nor the default search field are present.");
			}
			
			queryFields.put(df, 1.0f);
		}
		
		return queryFields;
	}

	protected Map<String, Float> mQueryFields;
	protected IQuery mParsedUserQuery;

	protected String[] mBoostParams;
	protected List<IQuery> mBoostQueries;
	protected IQuery mAltUserQuery;
	protected QueryBuilder mAltQParser;

	public DisMaxQueryBuilder(String qstr, Params localParams, Params params, 
			ISearchRequest req) throws ErrorException {
		super(qstr, localParams, params, req);
	}
	
	@Override
	public IQuery parse() throws ErrorException {
		Params params = Params.wrapDefaults(getLocalParams(), getParams());
		mQueryFields = parseQueryFields(getRequest().getSearchCore().getSchema(), params);
    
		/** 
		 * the main query we will execute.  we disable the coord because
		 * this query is an artificial construct
		 */
		BooleanQuery query = new BooleanQuery(true);

		boolean notBlank = addMainQuery(query, params);
		if (!notBlank)
			return null;
		
		addBoostQuery(query, params);
		addBoostFunctions(query, params);

		return query;
	}

	protected void addBoostFunctions(BooleanQuery query, Params params) 
			throws ErrorException {
		String[] boostFuncs = params.getParams(DisMaxParams.BF);
		
		if (boostFuncs != null && boostFuncs.length != 0) {
			for (String boostFunc : boostFuncs) {
				if (boostFunc == null || "".equals(boostFunc)) continue;
				
				Map<String, Float> ff = QueryUtils.parseFieldBoosts(boostFunc);
				for (String f : ff.keySet()) {
					IQuery fq = subQuery(f, FunctionQueryBuilderPlugin.NAME).getQuery();
					Float b = ff.get(f);
					if (b != null) 
						fq.setBoost(b);
					
					query.add(fq, IBooleanClause.Occur.SHOULD);
				}
			}
		}
	}

	protected void addBoostQuery(BooleanQuery query, Params params) 
			throws ErrorException {
		mBoostParams = params.getParams(DisMaxParams.BQ);
		
		//List<Query> boostQueries = QueryUtils.parseQueryStrings(req, boostParams);
		mBoostQueries = null;
		
		if (mBoostParams != null && mBoostParams.length > 0) {
			mBoostQueries = new ArrayList<IQuery>();
			for (String qs : mBoostParams) {
				if (qs.trim().length() == 0) continue;
				
				IQuery q = subQuery(qs, null).getQuery();
				mBoostQueries.add(q);
			}
		}
		
		if (mBoostQueries != null) {
			if (mBoostQueries.size() == 1 && mBoostParams.length == 1) {
				// legacy logic 
				IQuery f = mBoostQueries.get(0);
				
				if (f.getBoost() == 1.0f && f instanceof BooleanQuery) {
					/* 
					 * if the default boost was used, and we've got a BooleanQuery
					 * extract the subqueries out and use them directly
					 */
					for (Object c : ((IBooleanQuery) f).getClauses()) {
						query.add((IBooleanClause) c);
					}
				} else {
					query.add(f, IBooleanClause.Occur.SHOULD);
				}
				
			} else {
				for (IQuery f : mBoostQueries) {
					query.add(f, IBooleanClause.Occur.SHOULD);
				}
			}
		}
	}

	/** Adds the main query to the query argument. If its blank then false is returned. */
	protected boolean addMainQuery(BooleanQuery query, Params params) 
			throws ErrorException {
		Map<String, Float> phraseFields = QueryUtils.parseFieldBoosts(
				params.getParams(DisMaxParams.PF));
		float tiebreaker = params.getFloat(DisMaxParams.TIE, 0.0f);

		// a parser for dealing with user input, which will convert
		// things to DisjunctionMaxQueries
		DisjunctionMaxQueryParser up = getParser(mQueryFields, DisMaxParams.QS, params, tiebreaker);

		// for parsing sloppy phrases using DisjunctionMaxQueries 
		DisjunctionMaxQueryParser pp = getParser(phraseFields, DisMaxParams.PS, params, tiebreaker);

		// Main User Query 
		mParsedUserQuery = null;
		String userQuery = getQueryString();
		mAltUserQuery = null;
		
		if (userQuery == null || userQuery.trim().length() < 1) {
			// If no query is specified, we may have an alternate
			mAltUserQuery = getAlternateUserQuery(params);
			if (mAltUserQuery == null)
				return false;
			
			query.add(mAltUserQuery, IBooleanClause.Occur.MUST);
			
		} else {
			// There is a valid query string
			userQuery = QueryUtils.partialEscape(QueryUtils.stripUnbalancedQuotes(userQuery)).toString();
			userQuery = QueryUtils.stripIllegalOperators(userQuery).toString();

			mParsedUserQuery = getUserQuery(userQuery, up, params);
			query.add(mParsedUserQuery, IBooleanClause.Occur.MUST);

			IQuery phrase = getPhraseQuery(userQuery, pp);
			if (phrase != null) 
				query.add(phrase, IBooleanClause.Occur.SHOULD);
		}
		
		return true;
	}

	protected IQuery getAlternateUserQuery(Params params) throws ErrorException {
		String altQ = params.get(DisMaxParams.ALTQ);
		if (altQ != null) {
			QueryBuilder altQParser = subQuery(altQ, null);
			return altQParser.getQuery();
		} else {
			return null;
		}
	}

	protected IQuery getPhraseQuery(String userQuery, DisjunctionMaxQueryParser pp) 
			throws ErrorException {
		// Add on Phrases for the Query 
		// build up phrase boosting queries 

		/* if the userQuery already has some quotes, strip them out.
		 * we've already done the phrases they asked for in the main
		 * part of the query, this is to boost docs that may not have
		 * matched those phrases but do match looser phrases.
		 */
		String userPhraseQuery = userQuery.replace("\"", "");
		
		try {
			return pp.parse("\"" + userPhraseQuery + "\"");
		} catch (ParseException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, ex);
		}
	}

	protected IQuery getUserQuery(String userQuery, DisjunctionMaxQueryParser up, 
			Params params) throws ErrorException {
		try {
			String minShouldMatch = parseMinShouldMatch(
					getRequest().getSearchCore().getSchema(), params);
			IQuery dis = up.parse(userQuery);
			IQuery query = dis;
	
			if (dis instanceof BooleanQuery) {
				BooleanQuery t = new BooleanQuery();
				QueryUtils.flattenBooleanQuery(t, (BooleanQuery) dis);
				QueryUtils.setMinShouldMatch(t, minShouldMatch);
				query = t;
			}
			
			return query;
		} catch (ParseException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, ex);
		}
	}

	protected DisjunctionMaxQueryParser getParser(Map<String, Float> fields, String paramName,
			Params params, float tiebreaker) throws ErrorException {
		int slop = params.getInt(paramName, 0);
		DisjunctionMaxQueryParser parser = new DisjunctionMaxQueryParser(this,
				IMPOSSIBLE_FIELD_NAME);
		parser.addAlias(IMPOSSIBLE_FIELD_NAME, tiebreaker, fields);
		parser.setPhraseSlop(slop);
		return parser;
	}

	@Override
	public String[] getDefaultHighlightFields() {
		return mQueryFields.keySet().toArray(new String[mQueryFields.keySet().size()]);
	}

	@Override
	public IQuery getHighlightQuery() throws ErrorException {
		return mParsedUserQuery == null ? mAltUserQuery : mParsedUserQuery;
	}

	@Override
	public void addDebugInfo(NamedList<Object> debugInfo) throws ErrorException {
		super.addDebugInfo(debugInfo);
		debugInfo.add("altquerystring", mAltUserQuery);
		
		if (mBoostQueries != null) {
			debugInfo.add("boost_queries", mBoostParams);
			debugInfo.add("parsed_boost_queries",QueryParsing.toString(mBoostQueries, 
					getRequest().getSearchCore().getSchema()));
		}
		
		debugInfo.add("boostfuncs", getRequest().getParams().getParams(DisMaxParams.BF));
	}
	
}
