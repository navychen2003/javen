package org.javenstudio.falcon.search;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.javenstudio.common.indexdb.IBooleanClause;
import org.javenstudio.common.indexdb.IDocument;
import org.javenstudio.common.indexdb.IExplanation;
import org.javenstudio.common.indexdb.IField;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.document.Fieldable;
import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.CommonParams;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.util.ResultItem;
import org.javenstudio.falcon.util.ResultList;
import org.javenstudio.falcon.util.StrHelper;
import org.javenstudio.hornet.search.query.BooleanQuery;
import org.javenstudio.falcon.search.cache.CacheRegenerator;
import org.javenstudio.falcon.search.cache.SearchCache;
import org.javenstudio.falcon.search.hits.DocIterator;
import org.javenstudio.falcon.search.hits.DocList;
import org.javenstudio.falcon.search.hits.DocSet;
import org.javenstudio.falcon.search.hits.FieldParams;
import org.javenstudio.falcon.search.query.QueryParsing;
import org.javenstudio.falcon.search.schema.IndexSchema;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.panda.query.QueryParser;

/**
 * <p>Utilities that may be of use to RequestHandlers.</p>
 *
 * <p>
 * Many of these functions have code that was stolen/mutated from
 * StandardRequestHandler.
 * </p>
 *
 * <p>:TODO: refactor StandardRequestHandler to use these utilities</p>
 *
 * <p>:TODO: Many "standard" functionality methods are not cognisant of
 * default parameter settings.
 */
public class RequestHelper {
	static final Logger LOG = Logger.getLogger(RequestHelper.class);

	/**
	 * Set default-ish params on a Request.
	 *
	 * RequestHandlers can use this method to ensure their defaults and
	 * overrides are visible to other components such as the response writer
	 *
	 * @param req The request whose params we are interested i
	 * @param defaults values to be used if no values are specified in the request params
	 * @param appends values to be appended to those from the request 
	 * (or defaults) when dealing with multi-val params, or treated as another layer of 
	 * defaults for singl-val params.
	 * @param invariants values which will be used instead of any request, 
	 * or default values, regardless of context.
	 */
	public static void setDefaults(ISearchRequest req, Params defaults,
			Params appends, Params invariants) {

		Params p = req.getParams();
		p = Params.wrapDefaults(p, defaults);
		p = Params.wrapAppended(p, appends);
		p = Params.wrapDefaults(invariants, p);

		req.setParams(p);
	}

	/**
	 * Searcher.numDocs(Query,Query) freaks out if the filtering
	 * query is null, so we use this workarround.
	 */
 	public static int getNumDocs(Searcher s, IQuery q, IQuery f) 
 			throws ErrorException {
 		return (f == null) ? s.getDocSet(q).size() : s.getNumDocs(q, f);
 	}

 	private final static Pattern sSplitList = Pattern.compile(",| ");

 	/** Split a value that may contain a comma, space of bar separated list. */
 	public static String[] split(String value) {
 		return sSplitList.split(value.trim(), 0);
 	}

 	/**
 	 * Pre-fetch documents into the index searcher's document cache.
 	 *
 	 * This is an entirely optional step which you might want to perform for
 	 * the following reasons:
 	 *
 	 * <ul>
 	 *     <li>Locates the document-retrieval costs in one spot, which helps
 	 *     detailed performance measurement</li>
 	 *
 	 *     <li>Determines a priori what fields will be needed to be fetched by
 	 *     various subtasks, like response writing and highlighting.  This
 	 *     minimizes the chance that many needed fields will be loaded lazily.
 	 *     (it is more efficient to load all the field we require normally).</li>
 	 * </ul>
 	 *
 	 * If lazy field loading is disabled, this method does nothing.
 	 */
 	public static void optimizePreFetchDocs(ResponseBuilder rb,
 			DocList docs, IQuery query, ISearchRequest req,
 			ISearchResponse res) throws ErrorException {
 		
 		Searcher searcher = rb.getSearcher();
 		if (!searcher.isEnableLazyFieldLoading()) {
 			// nothing to do
 			return;
 		}

 		SearchReturnFields returnFields = res.getReturnFields();
 		if (returnFields != null && returnFields.getIndexFieldNames() != null) {
 			Set<String> fieldFilter = returnFields.getIndexFieldNames();

 			if (rb.isDoHighlights()) {
 				// copy return fields list
 				fieldFilter = new HashSet<String>(fieldFilter);
 				// add highlight fields

 				//Highlighter highlighter = HighlightComponent.getHighlighter(req.getCore());
 				//for (String field: highlighter.getHighlightFields(query, req, null))
 				//  fieldFilter.add(field);

 				// fetch unique key if one exists.
 				SchemaField keyField = rb.getSearchCore().getSchema().getUniqueKeyField();
 				if (keyField != null)
 					fieldFilter.add(keyField.getName());
 			}

 			// get documents
 			DocIterator iter = docs.iterator();
 			
 			for (int i=0; i < docs.size(); i++) {
 				searcher.getDoc(iter.nextDoc(), fieldFilter);
 			}
 		}
 	}

 	public static Set<String> getDebugInterests(String[] params, ResponseBuilder rb){
 		Set<String> debugInterests = new HashSet<String>();
 		
 		if (params != null) {
 			for (int i = 0; i < params.length; i++) {
 				if (params[i].equalsIgnoreCase("all") || params[i].equalsIgnoreCase("true")){
 					rb.setDebug(true);
 					break;
 					//still might add others
 					
 				} else if (params[i].equals(CommonParams.TIMING)){
 					rb.setDebugTimings(true);
 					
 				} else if (params[i].equals(CommonParams.QUERY)){
 					rb.setDebugQuery(true);
 					
 				} else if (params[i].equals(CommonParams.RESULTS)){
 					rb.setDebugResults(true);
 				}
 			}
 		}
 		
 		return debugInterests;
 	}
  
 	/**
 	 * <p>
 	 * Returns a NamedList containing many "standard" pieces of debugging
 	 * information.
 	 * </p>
 	 *
 	 * <ul>
 	 * <li>rawquerystring - the 'q' param exactly as specified by the client
 	 * </li>
 	 * <li>querystring - the 'q' param after any preprocessing done by the plugin
 	 * </li>
 	 * <li>parsedquery - the main query executed formated by the
 	 *     QueryParsing utils class (which knows about field types)
 	 * </li>
 	 * <li>parsedquery_toString - the main query executed formated by it's
 	 *     own toString method (in case it has internal state
 	 *     doesn't know about)
 	 * </li>
 	 * <li>explain - the list of score explanations for each document in
 	 *     results against query.
 	 * </li>
 	 * <li>otherQuery - the query string specified in 'explainOther' query param.
 	 * </li>
 	 * <li>explainOther - the list of score explanations for each document in
 	 *     results against 'otherQuery'
 	 * </li>
 	 * </ul>
 	 *
 	 * @param req the request we are dealing with
 	 * @param userQuery the users query as a string, after any basic
 	 *                  preprocessing has been done
 	 * @param query the query built from the userQuery
 	 *              (and perhaps other clauses) that identifies the main
 	 *              result set of the response.
 	 * @param results the main result set of the response
 	 * @return The debug info
 	 * @throws java.io.IOException if there was an IO error
 	 */
 	public static NamedList<Object> doStandardDebug(ISearchRequest req,
 			String userQuery, IQuery query, DocList results, boolean dbgQuery, 
 			boolean dbgResults) throws ErrorException {
 		NamedList<Object> dbg = new NamedMap<Object>();
 		
 		doStandardQueryDebug(req, userQuery, query, dbgQuery, dbg);
 		doStandardResultsDebug(req, query, results, dbgResults, dbg);
 		
 		return dbg;
 	}
  
 	public static void doStandardQueryDebug(ISearchRequest req, String userQuery, 
 			IQuery query, boolean dbgQuery, NamedList<Object> dbg) 
 			throws ErrorException {
 		if (dbgQuery) {
 			/** userQuery may have been pre-processed .. expose that */
 			dbg.add("rawquerystring", req.getParams().get(CommonParams.Q));
 			dbg.add("querystring", userQuery);

 			/** 
 			 * QueryParsing.toString isn't perfect, use it to see converted
 			 * values, use regular toString to see any attributes of the
 			 * underlying Query it may have missed.
 			 */
 			dbg.add("parsedquery", QueryParsing.toString(query, 
 					req.getSearchCore().getSchema()));
 			dbg.add("parsedquery_toString", query.toString());
 		}
 	}
  
 	public static void doStandardResultsDebug(ISearchRequest req, IQuery query, 
 			DocList results, boolean dbgResults, NamedList<Object> dbg) 
 			throws ErrorException {
 		if (dbgResults) {
 			Searcher searcher = req.getSearcher();
 			IndexSchema schema = req.getSearchCore().getSchema();
 			
 			boolean explainStruct = req.getParams().getBool(CommonParams.EXPLAIN_STRUCT, false);
 			NamedList<IExplanation> explain = getExplanations(query, results, searcher, schema);
 			
 			dbg.add("explain", explainStruct
 					? explanationsToNamedLists(explain) : explanationsToStrings(explain));

 			String otherQueryS = req.getParams().get(CommonParams.EXPLAIN_OTHER);
 			
 			if (otherQueryS != null && otherQueryS.length() > 0) {
 				DocList otherResults = doSimpleQuery(otherQueryS, req, 0, 10);
 				dbg.add("otherQuery", otherQueryS);
 				
 				NamedList<IExplanation> explainO = getExplanations(query, 
 						otherResults, searcher, schema);
 				
 				dbg.add("explainOther", explainStruct
 						? explanationsToNamedLists(explainO) : explanationsToStrings(explainO));
 			}
 		}
 	}

 	public static NamedList<Object> explanationToNamedList(IExplanation e) {
 		NamedList<Object> out = new NamedMap<Object>();

 		out.add("match", e.isMatch());
 		out.add("value", e.getValue());
 		out.add("description", e.getDescription());

 		IExplanation[] details = e.getDetails();

 		// short circut out
 		if (details == null || details.length == 0) 
 			return out;

 		List<NamedList<Object>> kids = 
 				new ArrayList<NamedList<Object>>(details.length);
 		
 		for (IExplanation d : details) {
 			kids.add(explanationToNamedList(d));
 		}
 		
 		out.add("details", kids);

 		return out;
 	}

 	public static NamedList<NamedList<Object>> explanationsToNamedLists
 			(NamedList<IExplanation> explanations) {
 		NamedList<NamedList<Object>> out = new NamedMap<NamedList<Object>>();
 		
 		for (Map.Entry<String,IExplanation> entry : explanations) {
 			out.add(entry.getKey(), explanationToNamedList(entry.getValue()));
 		}
 		
 		return out;
 	}

 	/**
 	 * Generates an NamedList of Explanations for each item in a list of docs.
 	 *
 	 * @param query The Query you want explanations in the context of
 	 * @param docs The Documents you want explained relative that query
 	 */
 	public static NamedList<IExplanation> getExplanations(IQuery query,
 			DocList docs, Searcher searcher, IndexSchema schema) 
 			throws ErrorException {

 		NamedList<IExplanation> explainList = new NamedMap<IExplanation>();
 		DocIterator iterator = docs.iterator();
 		
 		for (int i=0; i < docs.size(); i++) {
 			int id = iterator.nextDoc();

 			IDocument doc = searcher.getDoc(id);
 			String strid = schema.getPrintableUniqueKey(doc);

 			explainList.add(strid, searcher.explain(query, id));
 		}
 		
 		return explainList;
 	}

 	private static NamedList<String> explanationsToStrings
 			(NamedList<IExplanation> explanations) {
 		NamedList<String> out = new NamedMap<String>();
 		
 		for (Map.Entry<String,IExplanation> entry : explanations) {
 			out.add(entry.getKey(), "\n" + entry.getValue().toString());
 		}
 		
 		return out;
 	}

 	/**
 	 * Executes a basic query
 	 */
 	public static DocList doSimpleQuery(String sreq,
 			ISearchRequest req, int start, int limit) throws ErrorException {
 		List<String> commands = StrHelper.splitSmart(sreq,';');

 		String qs = commands.size() >= 1 ? commands.get(0) : "";

		IQuery query = req.getSearchCore().getQueryFactory()
				.getQueryBuilder(qs, null, req).getQuery();

		// If the first non-query, non-filter command is a simple sort on an indexed field, then
		// we can use the sort ability.
		ISort sort = null;
		if (commands.size() >= 2) 
			sort = QueryParsing.parseSort(commands.get(1), req);

		DocList results = req.getSearcher()
				.getDocList(query,(DocSet)null, sort, start, limit);
		
		return results;
 	}
  
 	private static final Pattern sWhitespacePattern = Pattern.compile("\\s+");
 	private static final Pattern sCaratPattern = Pattern.compile("\\^");
 	private static final Pattern sTildePattern = Pattern.compile("[~]");

 	/**
 	 * Given a string containing fieldNames and boost info,
 	 * converts it to a Map from field name to boost info.
 	 *
 	 * <p>
 	 * Doesn't care if boost info is negative, you're on your own.
 	 * </p>
 	 * <p>
 	 * Doesn't care if boost info is missing, again: you're on your own.
 	 * </p>
 	 *
 	 * @param in a String like "fieldOne^2.3 fieldTwo fieldThree^-0.4"
 	 * @return Map of fieldOne =&gt; 2.3, fieldTwo =&gt; null, fieldThree =&gt; -0.4
 	 */
 	public static Map<String,Float> parseFieldBoosts(String in) {
 		return parseFieldBoosts(new String[]{in});
 	}
  
 	/**
 	 * Like <code>parseFieldBoosts(String)</code>, but parses all the strings
 	 * in the provided array (which may be null).
 	 *
 	 * @param fieldLists an array of Strings eg. <code>{"fieldOne^2.3", "fieldTwo", fieldThree^-0.4}</code>
 	 * @return Map of fieldOne =&gt; 2.3, fieldTwo =&gt; null, fieldThree =&gt; -0.4
 	 */
 	public static Map<String,Float> parseFieldBoosts(String[] fieldLists) {
 		if (fieldLists == null || fieldLists.length == 0) 
 			return new HashMap<String,Float>();
 		
 		Map<String, Float> out = new HashMap<String,Float>(7);
 		for (String in : fieldLists) {
 			if (in == null) 
 				continue;
 			
 			in = in.trim();
 			if (in.length() == 0) 
 				continue;
      
 			String[] bb = sWhitespacePattern.split(in);
 			
 			for (String s : bb) {
 				String[] bbb = sCaratPattern.split(s);
 				out.put(bbb[0], 1 == bbb.length ? null : Float.valueOf(bbb[1]));
 			}
 		}
 		
 		return out;
 	}
 	
 	/**
 	 * Like {@link #parseFieldBoosts}, but allows for an optional slop value prefixed by "~".
 	 *
 	 * @param fieldLists - an array of Strings eg. <code>{"fieldOne^2.3", "fieldTwo", fieldThree~5^-0.4}</code>
 	 * @param wordGrams - (0=all words, 2,3 = shingle size)
 	 * @param defaultSlop - the default slop for this param
 	 * @return - FieldParams containing the fieldname,boost,slop,and shingle size
 	 */
 	public static List<FieldParams> parseFieldBoostsAndSlop(String[] fieldLists, 
 			int wordGrams, int defaultSlop) {
 		if (fieldLists == null || fieldLists.length == 0) 
 			return new ArrayList<FieldParams>();
 		
 		List<FieldParams> out = new ArrayList<FieldParams>();
 		for (String in : fieldLists) {
 			if (in == null) 
 				continue;
 			
 			in = in.trim();
 			if (in.length() == 0) 
 				continue;
      
 			String[] fieldConfigs = sWhitespacePattern.split(in);
 			
 			for (String s : fieldConfigs) {
 				String[] fieldAndSlopVsBoost = sCaratPattern.split(s);
 				String[] fieldVsSlop = sTildePattern.split(fieldAndSlopVsBoost[0]);
 				
 				String field = fieldVsSlop[0];
 				int slop  = (2 == fieldVsSlop.length) ? 
 						Integer.valueOf(fieldVsSlop[1]) : defaultSlop;
 				Float boost = (1 == fieldAndSlopVsBoost.length) ? 
 						1  : Float.valueOf(fieldAndSlopVsBoost[1]);
 				
 				FieldParams fp = new FieldParams(field, wordGrams, slop, boost);
 				out.add(fp);
 			}
 		}
 		
 		return out;
 	}

 	/**
 	 * Checks the number of optional clauses in the query, and compares it
 	 * with the specification string to determine the proper value to use.
 	 *
 	 * <p>
 	 * Details about the specification format can be found
 	 * <a href="doc-files/min-should-match.html">here</a>
 	 * </p>
 	 *
 	 * <p>A few important notes...</p>
 	 * <ul>
 	 * <li>
 	 * If the calculations based on the specification determine that no
 	 * optional clauses are needed, BooleanQuerysetMinMumberShouldMatch
 	 * will never be called, but the usual rules about BooleanQueries
 	 * still apply at search time (a BooleanQuery containing no required
 	 * clauses must still match at least one optional clause)
 	 * <li>
 	 * <li>
 	 * No matter what number the calculation arrives at,
 	 * BooleanQuery.setMinShouldMatch() will never be called with a
 	 * value greater then the number of optional clauses (or less then 1)
 	 * </li>
 	 * </ul>
 	 *
 	 * <p>:TODO: should optimize the case where number is same
 	 * as clauses to just make them all "required"
 	 * </p>
 	 */
 	public static void setMinShouldMatch(BooleanQuery q, String spec) {
 		int optionalClauses = 0;
 		for (IBooleanClause c : q.clauses()) {
 			if (c.getOccur() == IBooleanClause.Occur.SHOULD) 
 				optionalClauses++;
 		}

 		int msm = calculateMinShouldMatch(optionalClauses, spec);
 		if (msm > 0) 
 			q.setMinimumNumberShouldMatch(msm);
 	}

 	// private static Pattern sSpaceAroundLessThanPattern = Pattern.compile("\\s*<\\s*");
 	private static Pattern sSpaceAroundLessThanPattern = Pattern.compile("(\\s+<\\s*)|(\\s*<\\s+)");
 	private static Pattern sSpacePattern = Pattern.compile(" ");
 	private static Pattern sLessThanPattern = Pattern.compile("<");

 	/**
 	 * helper exposed for UnitTests
 	 * @see #setMinShouldMatch
 	 */
 	static int calculateMinShouldMatch(int optionalClauseCount, String spec) {
 		int result = optionalClauseCount;
 		spec = spec.trim();

 		if (spec.indexOf("<") > -1) {
 			/** we have conditional spec(s) */
 			spec = sSpaceAroundLessThanPattern.matcher(spec).replaceAll("<");
 			
 			for (String s : sSpacePattern.split(spec)) {
 				String[] parts = sLessThanPattern.split(s,0);
 				int upperBound = Integer.parseInt(parts[0]);
 				
 				if (optionalClauseCount <= upperBound) 
 					return result;
 				else 
 					result = calculateMinShouldMatch(optionalClauseCount, parts[1]);
 			}
 			
 			return result;
 		}

 		/** otherwise, simple expresion */

 		if (spec.indexOf('%') > -1) {
 			/** percentage - assume the % was the last char.  If not, let Integer.parseInt fail. */
 			spec = spec.substring(0,spec.length()-1);
 			
 			int percent = Integer.parseInt(spec);
 			float calc = (result * percent) * (1/100f);
 			
 			result = calc < 0 ? result + (int)calc : (int)calc;
 			
 		} else {
 			int calc = Integer.parseInt(spec);
 			result = calc < 0 ? result + calc : calc;
 		}

 		return (optionalClauseCount < result ?
 				optionalClauseCount : (result < 0 ? 0 : result));
 	}

 	/**
 	 * Recursively walks the "from" query pulling out sub-queries and
 	 * adding them to the "to" query.
 	 *
 	 * <p>
 	 * Boosts are multiplied as needed.  Sub-BooleanQueryies which are not
 	 * optional will not be flattened.  From will be mangled durring the walk,
 	 * so do not attempt to reuse it.
 	 * </p>
 	 */
 	public static void flattenBooleanQuery(BooleanQuery to, BooleanQuery from) {
 		for (IBooleanClause clause : from.clauses()) {
 			IQuery cq = clause.getQuery();
 			cq.setBoost(cq.getBoost() * from.getBoost());

 			if (cq instanceof BooleanQuery && !clause.isRequired() && !clause.isProhibited()) {
 				/** we can recurse */
 				flattenBooleanQuery(to, (BooleanQuery)cq);

 			} else {
 				to.add(clause);
 			}
 		}
 	}

 	/**
 	 * Escapes all special characters except '"', '-', and '+'
 	 *
 	 * @see QueryParser#escape
 	 */
 	public static CharSequence partialEscape(CharSequence s) {
 		StringBuilder sb = new StringBuilder();
 		for (int i = 0; i < s.length(); i++) {
 			char c = s.charAt(i);
 			if (c == '\\' || c == '!' || c == '(' || c == ')' ||
 				c == ':'  || c == '^' || c == '[' || c == ']' || c == '/' ||
 				c == '{'  || c == '}' || c == '~' || c == '*' || c == '?'
 				) {
 				sb.append('\\');
 			}
 			sb.append(c);
 		}
 		return sb;
 	}

 	// Pattern to detect dangling operator(s) at end of query
 	// \s+[-+\s]+$
 	private final static Pattern DANGLING_OP_PATTERN = Pattern.compile( "\\s+[-+\\s]+$" );
  
 	// Pattern to detect consecutive + and/or - operators
 	// \s+[+-](?:\s*[+-]+)+
 	private final static Pattern CONSECUTIVE_OP_PATTERN = Pattern.compile( "\\s+[+-](?:\\s*[+-]+)+" );

 	/**
 	 * Strips operators that are used illegally, otherwise reuturns it's
 	 * input.  Some examples of illegal user queries are: "chocolate +-
 	 * chip", "chocolate - - chip", and "chocolate chip -".
 	 */
 	public static CharSequence stripIllegalOperators(CharSequence s) {
 		String temp = CONSECUTIVE_OP_PATTERN.matcher(s).replaceAll(" ");
 		return DANGLING_OP_PATTERN.matcher(temp).replaceAll("");
 	}

 	/**
 	 * Returns it's input if there is an even (ie: balanced) number of
 	 * '"' characters -- otherwise returns a String in which all '"'
 	 * characters are striped out.
 	 */
 	public static CharSequence stripUnbalancedQuotes(CharSequence s) {
 		int count = 0;
 		
 		for (int i = 0; i < s.length(); i++) {
 			if (s.charAt(i) == '\"') 
 				count++; 
 		}
 		
 		if ((count & 1) == 0) 
 			return s;
 		
 		return s.toString().replace("\"","");
 	}

 	public static NamedList<Object> removeNulls(NamedList<Object> nl) {
 		for (int i=0; i < nl.size(); i++) {
 			if (nl.getName(i) == null) {
 				NamedList<Object> newList = (nl instanceof NamedMap) ? 
 						new NamedMap<Object>() : new NamedList<Object>();
 						
 				for (int j=0; j < nl.size(); j++) {
 					String n = nl.getName(j);
 					if (n != null) 
 						newList.add(n, nl.getVal(j));
 				}
 				
 				return newList;
 			}
 		}
 		
 		return nl;
 	}

 	/**
 	 * Determines the correct Sort based on the request parameter "sort"
 	 *
 	 * @return null if no sort is specified.
 	 */
 	public static ISort getSort(ISearchRequest req) throws ErrorException {
 		String sort = req.getParams().get(CommonParams.SORT);
 		if (sort == null || sort.equals("")) 
 			return null;

 		ErrorException sortE = null;
 		ISort ss = null;
 		try {
 			ss = QueryParsing.parseSort(sort, req);
 		} catch (ErrorException e) {
 			sortE = e;
 		}

 		if ((ss == null) || (sortE != null)) {
 			/**
 			 * we definitely had some sort of sort string from the user,
 			 * but no SortSpec came out of it
 			 */
 			if (LOG.isWarnEnabled())
 				LOG.warn("Invalid sort \""+sort+"\" was specified, ignoring", sortE);
 			
 			return null;
 		}

 		return ss;
 	}

 	/** 
 	 * Turns an array of query strings into a List of Query objects.
 	 *
 	 * @return null if no queries are generated
 	 */
 	public static List<IQuery> parseQueryStrings(ISearchRequest req,
 			String[] queries) throws ErrorException {
 		if (queries == null || queries.length == 0) 
 			return null;
 		
 		List<IQuery> out = new ArrayList<IQuery>(queries.length);
 		for (String q : queries) {
 			if (q != null && q.trim().length() != 0) {
 				out.add(req.getSearchCore().getQueryFactory()
 						.getQueryBuilder(q, null, req).getQuery());
 			}
 		}
 		
 		return out;
 	}

 	/**
 	 * A CacheRegenerator that can be used whenever the items in the cache
 	 * are not dependant on the current searcher.
 	 *
 	 * <p>
 	 * Flat out copies the oldKey=&gt;oldVal pair into the newCache
 	 * </p>
 	 */
 	public static class IdentityRegenerator implements CacheRegenerator<Object,Object> {
		@Override
 		public boolean regenerateItem(Searcher newSearcher,
 				SearchCache<Object,Object> newCache, SearchCache<Object,Object> oldCache,
 				Object oldKey, Object oldVal) throws ErrorException {

 			newCache.put(oldKey, oldVal);
 			return true;
 		}
 	}

 	/**
 	 * Convert a DocList to a ResultDocumentList
 	 *
 	 * The optional param "ids" is populated with the indexdb document id
 	 * for each ResultDocument.
 	 *
 	 * @param docs The {@link DocList} to convert
 	 * @param searcher The {@link Searcher} to use to load the docs from the index
 	 * @param fields The names of the Fields to load
 	 * @param ids A map to store the ids of the docs
 	 * @return The new {@link ResultList} containing all the loaded docs
 	 * @throws java.io.IOException if there was a problem loading the docs
 	 * @since 1.4
 	 */
 	public static ResultList docListToResultDocumentList(
 			DocList docs, Searcher searcher, Set<String> fields,
 			Map<ResultItem, Integer> ids) throws ErrorException {
 		IndexSchema schema = searcher.getSchema();

 		ResultList list = new ResultList();
 		list.setNumFound(docs.matches());
 		list.setMaxScore(docs.maxScore());
 		list.setStart(docs.offset());

 		DocIterator dit = docs.iterator();

 		while (dit.hasNext()) {
 			int docid = dit.nextDoc();

 			IDocument indexdbDoc = searcher.getDoc(docid, fields);
 			ResultItem doc = new ResultItem();
      
 			for( IField field : indexdbDoc) {
 				if (fields == null || fields.contains(field.getName())) {
 					SchemaField sf = schema.getField(field.getName());
 					doc.addField( field.getName(), sf.getType().toObject((Fieldable)field));
 				}
 			}
 			
 			if (docs.hasScores() && (fields == null || fields.contains("score"))) 
 				doc.addField("score", dit.score());
      
 			list.add(doc);

 			if (ids != null) 
 				ids.put(doc, new Integer(docid));
 		}
 		
 		return list;
 	}

 	public static void invokeSetters(Object bean, NamedList<Object> initArgs) 
 			throws ErrorException {
 		if (initArgs == null) return;
 		
 		Class<?> clazz = bean.getClass();
 		Method[] methods = clazz.getMethods();
 		
 		Iterator<Map.Entry<String, Object>> iterator = initArgs.iterator();
 		
 		while (iterator.hasNext()) {
 			Map.Entry<String, Object> entry = iterator.next();
 			
 			String key = entry.getKey();
 			String setterName = "set" + String.valueOf(
 					Character.toUpperCase(key.charAt(0))) + key.substring(1);
 			
 			Method method = null;
 			try {
 				for (Method m : methods) {
 					if (m.getName().equals(setterName) && m.getParameterTypes().length == 1) {
 						method = m;
 						break;
 					}
 				}
 				
 				if (method == null) {
 					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
 							"no setter corrresponding to '" + key + "' in " + clazz.getName());
 				}
 				
 				@SuppressWarnings("unused")
				Class<?> pClazz = method.getParameterTypes()[0];
 				
 				Object val = entry.getValue();
 				method.invoke(bean, val);
 				
 			} catch (InvocationTargetException e1) {
 				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
 						"Error invoking setter " + setterName + " on class : " + clazz.getName(), e1);
 				
 			} catch (IllegalAccessException e1) {
 				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
 						"Error invoking setter " + setterName + " on class : " + clazz.getName(), e1);
 			}
 		}
 	}
  
}
