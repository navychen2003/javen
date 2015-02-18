package org.javenstudio.falcon.search.query.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.indexdb.IBooleanClause;
import org.javenstudio.common.indexdb.IBooleanQuery;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.hits.FieldParams;
import org.javenstudio.falcon.search.params.DisMaxParams;
import org.javenstudio.falcon.search.query.FunctionQueryBuilderPlugin;
import org.javenstudio.falcon.search.query.QueryBuilder;
import org.javenstudio.falcon.search.query.QueryParsing;
import org.javenstudio.falcon.search.query.QueryUtils;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.hornet.query.BoostedQuery;
import org.javenstudio.hornet.query.FunctionQuery;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.source.ProductFloatFunction;
import org.javenstudio.hornet.query.source.QueryValueSource;
import org.javenstudio.hornet.search.query.BooleanQuery;
import org.javenstudio.panda.query.ParseException;

/**
 * Query parser that generates DisjunctionMaxQueries based on user configuration.
 * See Wiki page http://wiki.apache.org/solr/ExtendedDisMax
 */
public class ExtendedDismaxQueryBuilder extends QueryBuilder {

	/**
	 * A field we can't ever find in any schema, so we can safely tell
	 * DisjunctionMaxQueryParser to use it as our defaultField, and
	 * map aliases from it to any field in our schema.
	 */
	private static String IMPOSSIBLE_FIELD_NAME = "\uFFFC\uFFFC\uFFFC";
	
	protected static class ExtendedClause {
		protected String mField;
		protected String mRawField;	// if the clause is +(foo:bar) then rawField=(foo
		protected boolean mIsPhrase;
		protected boolean mHasWhitespace;
		protected boolean mHasSpecialSyntax;
		protected boolean mSyntaxError;
		protected char mMust;   	// + or -
		protected String mValue;  	// the field value (minus the field name, +/-, quotes)
		protected String mRaw;  	// the raw clause w/o leading/trailing whitespace
		
		public boolean isBareWord() {
			return mMust == 0 && !mIsPhrase;
		}
	}
  
	private ExtendedDismaxConfiguration mConfig;
	private IQuery mParsedUserQuery;
	private IQuery mAltUserQuery;
	private List<IQuery> mBoostQueries;
  
	public ExtendedDismaxQueryBuilder(String qstr, Params localParams, Params params, 
			ISearchRequest req) throws ErrorException {
		super(qstr, localParams, params, req);
		mConfig = this.createConfiguration(qstr, localParams, params, req);
	}
  
	@Override
	public IQuery parse() throws ErrorException {
		// the main query we will execute.  we disable the coord because
		// this query is an artificial construct
		BooleanQuery query = new BooleanQuery(true);
    
    	// Main User Query
		mParsedUserQuery = null;
		
		String userQuery = getQueryString();
		mAltUserQuery = null;
		
		if (userQuery == null || userQuery.trim().length() == 0) {
			// If no query is specified, we may have an alternate
			if (mConfig.mAltQ != null) {
				QueryBuilder altQParser = subQuery(mConfig.mAltQ, null);
				mAltUserQuery = altQParser.getQuery();
				query.add(mAltUserQuery, IBooleanClause.Occur.MUST);
				
			} else {
				return null;
				// throw new ErrorException("missing query string" );
			}
			
		} else {
			// There is a valid query string
			ExtendedQueryParser up = createEdismaxQueryParser(this, IMPOSSIBLE_FIELD_NAME);
			up.addAlias(IMPOSSIBLE_FIELD_NAME, mConfig.mTiebreaker, mConfig.mQueryFields);
			addAliasesFromRequest(up, mConfig.mTiebreaker);
			
			up.setPhraseSlop(mConfig.mQuerySlop); // slop for explicit user phrase queries
			up.setAllowLeadingWildcard(true);
      
			// defer escaping and only do if lucene parsing fails, or we need phrases
			// parsing fails.  Need to sloppy phrase queries anyway though.
			List<ExtendedClause> clauses = splitIntoClauses(userQuery, false);
      
			// Always rebuild mainUserQuery from clauses to catch modifications from splitIntoClauses
			// This was necessary for userFields modifications to get propagated into the query.
			// Convert lower or mixed case operators to uppercase if we saw them.
			// only do this for the lucene query part and not for phrase query boosting
			// since some fields might not be case insensitive.
			// We don't use a regex for this because it might change and AND or OR in
			// a phrase query in a case sensitive field.
			String mainUserQuery = rebuildUserQuery(clauses, mConfig.mLowercaseOperators);
      
			// but always for unstructured implicit bqs created by getFieldQuery
			up.mMinShouldMatch = mConfig.mMinShouldMatch;
      
			mParsedUserQuery = parseOriginalQuery(up, mainUserQuery, clauses, mConfig);
			if (mParsedUserQuery == null) 
				mParsedUserQuery = parseEscapedQuery(up, escapeUserQuery(clauses), mConfig);
      
			query.add(mParsedUserQuery, IBooleanClause.Occur.MUST);
      
			addPhraseFieldQueries(query, clauses, mConfig);
		}
    
    	// Boosting Query 
		mBoostQueries = getBoostQueries();
		for (IQuery f : mBoostQueries) {
			query.add(f, IBooleanClause.Occur.SHOULD);
		}
    
    	// Boosting Functions 
		List<IQuery> boostFunctions = getBoostFunctions();
		for (IQuery f : boostFunctions) {
			query.add(f, IBooleanClause.Occur.SHOULD);
		}
    
		// create a boosted query (scores multiplied by boosts)
		IQuery topQuery = query;
		List<ValueSource> boosts = getMultiplicativeBoosts();
		
		if (boosts.size() > 1) {
			ValueSource prod = new ProductFloatFunction(
					boosts.toArray(new ValueSource[boosts.size()]));
			topQuery = new BoostedQuery(query, prod);
			
		} else if (boosts.size() == 1) {
			topQuery = new BoostedQuery(query, boosts.get(0));
		}
    
		return topQuery;
	}
  
	/**
	 * Adds shingled phrase queries to all the fields specified 
	 * in the pf, pf2 anf pf3 parameters
	 */
	protected void addPhraseFieldQueries(BooleanQuery query, List<ExtendedClause> clauses,
			ExtendedDismaxConfiguration config) throws ErrorException {
		// sloppy phrase queries for proximity
		List<FieldParams> allPhraseFields = config.getAllPhraseFields();
    
		if (allPhraseFields.size() > 0) {
			// find non-field clauses
			List<ExtendedClause> normalClauses = new ArrayList<ExtendedClause>(clauses.size());
			
			for (ExtendedClause clause : clauses) {
				if (clause.mField != null || clause.mIsPhrase) 
					continue;
				
				// check for keywords "AND,OR,TO"
				if (clause.isBareWord()) {
					String s = clause.mValue.toString();
					// avoid putting explicit operators in the phrase query
					if ("OR".equals(s) || "AND".equals(s) || "NOT".equals(s) || "TO".equals(s)) 
						continue;
				}
				
				normalClauses.add(clause);
			}
      
			// full phrase and shingles
			for (FieldParams phraseField: allPhraseFields) {
				Map<String,Float> pf = new HashMap<String,Float>(1);
				pf.put(phraseField.getField(),phraseField.getBoost());
				
				addShingledPhraseQueries(query, normalClauses, pf,   
						phraseField.getWordGrams(), config.mTiebreaker, phraseField.getSlop());
			}
		}
	}

	/**
	 * Creates an instance of ExtendedDismaxConfiguration. It will contain all
	 * the necessary parameters to parse the query
	 */
	protected ExtendedDismaxConfiguration createConfiguration(String qstr,
			Params localParams, Params params, ISearchRequest req) throws ErrorException {
		return new ExtendedDismaxConfiguration(localParams,params,req);
	}
  
	/**
	 * Creates an instance of ExtendedQueryParser, the query parser that's going to be used
	 * to parse the query.
	 */
	protected ExtendedQueryParser createEdismaxQueryParser(QueryBuilder qParser, String field) {
		return new ExtendedQueryParser(qParser, field);
	}
  
	/**
	 * Parses an escaped version of the user's query.  This method is called 
	 * in the event that the original query encounters exceptions during parsing.
	 *
	 * @param up parser used
	 * @param escapedUserQuery query that is parsed, should already be escaped 
	 * so that no trivial parse errors are encountered
	 * @param config Configuration options for this parse request
	 * @return the resulting query (flattened if needed) with "min should match" 
	 * rules applied as specified in the config.
	 * @see #parseOriginalQuery
	 * @see SolrPluginUtils#flattenBooleanQuery
	 */
	protected IQuery parseEscapedQuery(ExtendedQueryParser up,
			String escapedUserQuery, ExtendedDismaxConfiguration config) throws ErrorException {
		try {
			IQuery query = up.parse(escapedUserQuery);
	    
			if (query instanceof IBooleanQuery) {
				BooleanQuery t = new BooleanQuery();
				QueryUtils.flattenBooleanQuery(t, (IBooleanQuery)query);
				QueryUtils.setMinShouldMatch(t, config.mMinShouldMatch);
				query = t;
			}
			
			return query;
		} catch (ParseException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, ex);
		}
	}
  
	/**
	 * Parses the user's original query.  
	 * This method attempts to cleanly parse the specified query string using 
	 * the specified parser, any Exceptions are ignored resulting in null being returned.
	 *
	 * @param up parser used
	 * @param mainUserQuery query string that is parsed
	 * @param clauses used to dictate "min should match" logic
	 * @param config Configuration options for this parse request
	 * @return the resulting query with "min should match" rules applied 
	 * 	as specified in the config.
	 * @see #parseEscapedQuery
	 */
	protected IQuery parseOriginalQuery(ExtendedQueryParser up, String mainUserQuery, 
			List<ExtendedClause> clauses, ExtendedDismaxConfiguration config) {
		IQuery query = null;
		try {
			up.setRemoveStopFilter(!config.mStopwords);
			up.mExceptions = true;
			query = up.parse(mainUserQuery);
      
			if (shouldRemoveStopFilter(config, query)) {
				// if the query was all stop words, remove none of them
				up.setRemoveStopFilter(true);
				query = up.parse(mainUserQuery);          
			}
		} catch (ParseException e) {
			// ignore failure and reparse later after escaping reserved chars
			up.mExceptions = false;
		}
    
		if (query == null) 
			return null;
    
		// For correct queries, turn off mm processing if there
		// were explicit operators (except for AND).
		boolean doMinMatched = doMinMatched(clauses, config.mLowercaseOperators);
		if (doMinMatched && query instanceof BooleanQuery) 
			QueryUtils.setMinShouldMatch((BooleanQuery)query, config.mMinShouldMatch);
		
		return query;
	}

	/**
	 * Determines if query should be re-parsed removing the stop filter.
	 * @return true if there are stopwords configured and the parsed query was empty
	 *         false in any other case.
	 */
	protected boolean shouldRemoveStopFilter(ExtendedDismaxConfiguration config, IQuery query) {
		return config.mStopwords && isEmpty(query);
	}
  
	static boolean isEmpty(IQuery q) {
		if (q == null) return true;
		if (q instanceof BooleanQuery && ((BooleanQuery)q).clauses().size() == 0) 
			return true;
		return false;
	}
  
	private String escapeUserQuery(List<ExtendedClause> clauses) {
		StringBuilder sb = new StringBuilder();
		for (ExtendedClause clause : clauses) {
			boolean doQuote = clause.mIsPhrase;
      
			String s = clause.mValue;
			if (!clause.mIsPhrase && ("OR".equals(s) || "AND".equals(s) || "NOT".equals(s))) 
				doQuote = true;
      
			if (clause.mMust != 0) 
				sb.append(clause.mMust);
      
			if (clause.mField != null) {
				sb.append(clause.mField);
				sb.append(':');
			}
			
			if (doQuote) 
				sb.append('"');
      
			sb.append(clause.mValue);
			if (doQuote) 
				sb.append('"');
      
			if (clause.mField != null) {
				// Add the default user field boost, if any
				Float boost = mConfig.mUserFields.getBoost(clause.mField);
				if (boost != null)
					sb.append("^").append(boost);
			}
			
			sb.append(' ');
		}
		
		return sb.toString();
	}
  
	/**
	 * Returns false if at least one of the clauses is an explicit operator 
	 * (except for AND)
	 */
	private boolean doMinMatched(List<ExtendedClause> clauses, 
			boolean lowercaseOperators) {
		for (ExtendedClause clause : clauses) {
			if (clause.mMust == '+') return false;
			if (clause.mMust == '-') return false;
			
			if (clause.isBareWord()) {
				String s = clause.mValue;
				if ("OR".equals(s)) {
					return false;
				} else if ("NOT".equals(s)) {
					return false;
				} else if (lowercaseOperators && "or".equals(s)) {
					return false;
				}
			}
		}
		return true;
	}
  
	/**
	 * Generates a query string from the raw clauses, uppercasing 
	 * 'and' and 'or' as needed.
	 * @param clauses the clauses of the query string to be rebuilt
	 * @param lowercaseOperators if true, lowercase 'and' and 'or' clauses will 
	 *        be recognized as operators and uppercased in the final query string.
	 * @return the generated query string.
	 */
	protected String rebuildUserQuery(List<ExtendedClause> clauses, 
			boolean lowercaseOperators) {
		StringBuilder sb = new StringBuilder();
		
		for (int i=0; i<clauses.size(); i++) {
			ExtendedClause clause = clauses.get(i);
			String s = clause.mRaw;
			
			// and and or won't be operators at the start or end
			if (lowercaseOperators && i>0 && i+1<clauses.size()) {
				if ("AND".equalsIgnoreCase(s)) 
					s = "AND";
				else if ("OR".equalsIgnoreCase(s)) 
					s = "OR";
			}
			sb.append(s);
			sb.append(' ');
		}
		
		return sb.toString();
	}
  
	/**
	 * Parses all multiplicative boosts
	 */
	protected List<ValueSource> getMultiplicativeBoosts() throws ErrorException {
		List<ValueSource> boosts = new ArrayList<ValueSource>();
		
		if (mConfig.hasMultiplicativeBoosts()) {
			for (String boostStr : mConfig.mMultBoosts) {
				if (boostStr == null || boostStr.length() == 0) 
					continue;
				
				IQuery boost = subQuery(boostStr, FunctionQueryBuilderPlugin.NAME).getQuery();
				ValueSource vs;
				
				if (boost instanceof FunctionQuery) 
					vs = ((FunctionQuery)boost).getValueSource();
				else 
					vs = new QueryValueSource(boost, 1.0f);
				
				boosts.add(vs);
			}
		}
		
		return boosts;
	}
  
	/**
	 * Parses all function queries
	 */
	protected List<IQuery> getBoostFunctions() throws ErrorException {
		List<IQuery> boostFunctions = new LinkedList<IQuery>();
		
		if (mConfig.hasBoostFunctions()) {
			for (String boostFunc : mConfig.mBoostFuncs) {
				if (boostFunc == null || "".equals(boostFunc)) 
					continue;
				
				Map<String,Float> ff = QueryUtils.parseFieldBoosts(boostFunc);
				for (String f : ff.keySet()) {
					IQuery fq = subQuery(f, FunctionQueryBuilderPlugin.NAME).getQuery();
					Float b = ff.get(f);
					if (b != null) 
						fq.setBoost(b);
					
					boostFunctions.add(fq);
				}
			}
		}
		
		return boostFunctions;
	}
  
	/**
	 * Parses all boost queries
	 */
	protected List<IQuery> getBoostQueries() throws ErrorException {
		List<IQuery> boostQueries = new LinkedList<IQuery>();
		
		if (mConfig.hasBoostParams()) {
			for (String qs : mConfig.mBoostParams) {
				if (qs.trim().length() == 0) 
					continue;
				
				IQuery q = subQuery(qs, null).getQuery();
				boostQueries.add(q);
			}
		}
		
		return boostQueries;
	}
  
	/**
	 * Extracts all the aliased fields from the requests and adds them to up
	 */
	private void addAliasesFromRequest(ExtendedQueryParser up, 
			float tiebreaker) throws ErrorException {
		Iterator<String> it = mConfig.mParams.getParameterNamesIterator();
		
		while (it.hasNext()) {
			String param = it.next();
			if (param.startsWith("f.") && param.endsWith(".qf")) {
				// Add the alias
				String fname = param.substring(2, param.length()-3);
				String qfReplacement = mConfig.mParams.get(param);
				
				Map<String,Float> parsedQf = QueryUtils.parseFieldBoosts(qfReplacement);
				if (parsedQf.size() == 0)
					return;
				
				up.addAlias(fname, tiebreaker, parsedQf);
			}
		}
	}
  
	/**
	 * Modifies the main query by adding a new optional Query consisting
	 * of shingled phrase queries across the specified clauses using the 
	 * specified field =&gt; boost mappings.
	 *
	 * @param mainQuery Where the phrase boosting queries will be added
	 * @param clauses Clauses that will be used to construct the phrases
	 * @param fields Field =&gt; boost mappings for the phrase queries
	 * @param shingleSize how big the phrases should be, 0 means a single phrase
	 * @param tiebreaker tie breaker value for the DisjunctionMaxQueries
	 * @param slop slop value for the constructed phrases
	 */
	protected void addShingledPhraseQueries(final BooleanQuery mainQuery, 
			final List<ExtendedClause> clauses, final Map<String,Float> fields,
			int shingleSize, final float tiebreaker, final int slop) throws ErrorException {
    
		if (fields == null || fields.isEmpty() || 
			clauses == null || clauses.size() < shingleSize) 
			return;
    
		if (shingleSize == 0) 
			shingleSize = clauses.size();
    
		final int goat = shingleSize-1; // :TODO: better name for var?
		StringBuilder userPhraseQuery = new StringBuilder();
		
		for (int i=0; i < clauses.size() - goat; i++) {
			userPhraseQuery.append('"');
			
			for (int j=0; j <= goat; j++) {
				userPhraseQuery.append(clauses.get(i + j).mValue);
				userPhraseQuery.append(' ');
			}
			
			userPhraseQuery.append('"');
			userPhraseQuery.append(' ');
		}
    
		// for parsing sloppy phrases using DisjunctionMaxQueries 
		ExtendedQueryParser pp = createEdismaxQueryParser(this, IMPOSSIBLE_FIELD_NAME);
    
		pp.addAlias(IMPOSSIBLE_FIELD_NAME, tiebreaker, fields);
		pp.setPhraseSlop(slop);
		pp.setRemoveStopFilter(true);  // remove stop filter and keep stopwords
    
		/* :TODO: reevaluate using makeDismax=true vs false...
		 * 
		 * The DismaxQueryParser always used DisjunctionMaxQueries for the 
		 * pf boost, for the same reasons it used them for the qf fields.
		 * When Yonik first wrote the ExtendedDismaxQParserPlugin, he added
		 * the "makeDismax=false" property to use BooleanQueries instead, but 
		 * when asked why his response was "I honestly don't recall" ...
		 *
		 * https://issues.apache.org/jira/browse/SOLR-1553?focusedCommentId=12793813#action_12793813
		 *
		 * so for now, we continue to use dismax style queries because it 
		 * seems the most logical and is back compatible, but we should 
		 * try to figure out what Yonik was thinking at the time (because he 
		 * rarely does things for no reason)
		 */
		pp.mMakeDismax = true; 
    
    
		// minClauseSize is independent of the shingleSize because of stop words
		// (if they are removed from the middle, so be it, but we need at least 
		// two or there shouldn't be a boost)
		pp.mMinClauseSize = 2;  
    
		// TODO: perhaps we shouldn't use synonyms either...
		try {
			IQuery phrase = pp.parse(userPhraseQuery.toString());
			if (phrase != null) 
				mainQuery.add(phrase, IBooleanClause.Occur.SHOULD);
	    
		} catch (ParseException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, ex);
		}
	}
  
	@Override
	public String[] getDefaultHighlightFields() {
		return mConfig.mQueryFields.keySet().toArray(new String[0]);
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
			debugInfo.add("boost_queries", mConfig.mBoostParams);
			debugInfo.add("parsed_boost_queries", QueryParsing.toString(mBoostQueries, 
					getRequest().getSearchCore().getSchema()));
		}
		
		debugInfo.add("boostfuncs", getRequest().getParams().getParams(DisMaxParams.BF));
	}
  
	public List<ExtendedClause> splitIntoClauses(String s, boolean ignoreQuote) 
			throws ErrorException {
		ArrayList<ExtendedClause> lst = new ArrayList<ExtendedClause>(4);
		ExtendedClause clause;
    
		int pos = 0;
		int end = s.length();
		int start;
		char ch = 0;
		boolean disallowUserField;
		
		while (pos < end) {
			clause = new ExtendedClause();
			disallowUserField = true;
      
			ch = s.charAt(pos);
      
			while (Character.isWhitespace(ch)) {
				if (++pos >= end) break;
				ch = s.charAt(pos);
			}
      
			start = pos;      
      
			if (ch == '+' || ch == '-') {
				clause.mMust = ch;
				pos ++;
			}
      
			clause.mField = getFieldName(s, pos, end);
			if (clause.mField != null && !mConfig.mUserFields.isAllowed(clause.mField)) 
				clause.mField = null;
      
			if (clause.mField != null) {
				disallowUserField = false;
				
				int colon = s.indexOf(':',pos);
				clause.mRawField = s.substring(pos, colon);
				
				pos += colon - pos; // skip the field name
				pos++;  // skip the ':'
			}
      
			if (pos >= end) break;
      
			char inString = 0;
			ch = s.charAt(pos);
			
			if (!ignoreQuote && ch == '"') {
				clause.mIsPhrase = true;
				inString = '"';
				pos ++;
			}
      
			StringBuilder sb = new StringBuilder();
			while (pos < end) {
				ch = s.charAt(pos++);
				if (ch == '\\') {    // skip escaped chars, but leave escaped
					sb.append(ch);
					if (pos >= end) {
						sb.append(ch); // double backslash if we are at the end of the string
						break;
					}
					
					ch = s.charAt(pos++);
					sb.append(ch);
					continue;
					
				} else if (inString != 0 && ch == inString) {
					inString = 0;
					break;
					
				} else if (Character.isWhitespace(ch)) {
					clause.mHasWhitespace = true;
					
					if (inString == 0) {
						// end of the token if we aren't in a string, backing
						// up the position.
						pos --;
						break;
					}
				}
        
				if (inString == 0) {
					switch (ch) {
					case '!':
					case '(':
					case ')':
					case ':':
					case '^':
					case '[':
					case ']':
					case '{':
					case '}':
					case '~':
					case '*':
					case '?':
					case '"':
					case '+':
					case '-':
					case '\\':
					case '|':
					case '&':
					case '/':
						clause.mHasSpecialSyntax = true;
						sb.append('\\');
					}
				} else if (ch == '"') {
					// only char we need to escape in a string is double quote
					sb.append('\\');
				}
				sb.append(ch);
			}
			
			clause.mValue = sb.toString();
      
			if (clause.mIsPhrase) {
				if (inString != 0) {
					// detected bad quote balancing... retry
					// parsing with quotes like any other char
					return splitIntoClauses(s, true);
				}
        
				// special syntax in a string isn't special
				clause.mHasSpecialSyntax = false; 
				
			} else {
				// an empty clause... must be just a + or - on it's own
				if (clause.mValue.length() == 0) {
					clause.mSyntaxError = true;
					
					if (clause.mMust != 0) {
						clause.mValue = "\\" + clause.mMust;
						clause.mMust = 0;
						clause.mHasSpecialSyntax = true;
						
					} else {
						// uh.. this shouldn't happen.
						clause = null;
					}
				}
			}
      
			if (clause != null) {
				if (disallowUserField) {
					clause.mRaw = s.substring(start, pos);
					
					// escape colons, except for "match all" query
					if (!"*:*".equals(clause.mRaw)) 
						clause.mRaw = clause.mRaw.replaceAll(":", "\\\\:");
          
				} else {
					clause.mRaw = s.substring(start, pos);
					
					// Add default userField boost if no explicit boost exists
					if (mConfig.mUserFields.isAllowed(clause.mField) && !clause.mRaw.contains("^")) {
						Float boost = mConfig.mUserFields.getBoost(clause.mField);
						if (boost != null)
							clause.mRaw += "^" + boost;
					}
				}
				
				lst.add(clause);
			}
		}
    
		return lst;
	}
  
	/** 
	 * returns a field name or legal field alias from the current 
	 * position of the string 
	 */
	public String getFieldName(String s, int pos, int end) throws ErrorException {
		if (pos >= end) return null;
		
		int p = pos;
		int colon = s.indexOf(':',pos);
		
		// make sure there is space after the colon, but not whitespace
		if (colon <= pos || colon+1 >= end || Character.isWhitespace(s.charAt(colon+1))) 
			return null;
		
		char ch = s.charAt(p++);
		
		while ((ch == '(' || ch == '+' || ch == '-') && (pos < end)) {
			ch = s.charAt(p++);
			pos ++;
		}
		
		if (!Character.isJavaIdentifierPart(ch)) 
			return null;
		
		while (p < colon) {
			ch = s.charAt(p++);
			if (!(Character.isJavaIdentifierPart(ch) || ch == '-' || ch == '.')) 
				return null;
		}
		
		String fname = s.substring(pos, p);
		boolean isInSchema = getRequest().getSearchCore().getSchema().getFieldTypeNoEx(fname) != null;
		boolean isAlias = mConfig.mParams.get("f." + fname + ".qf") != null;
		boolean isMagic = (MagicFieldName.get(fname) != null);
    
		return (isInSchema || isAlias || isMagic) ? fname : null;
	}
  
}
