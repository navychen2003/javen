package org.javenstudio.falcon.search.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.javenstudio.common.indexdb.IBooleanClause;
import org.javenstudio.common.indexdb.IBooleanQuery;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.search.Query;
import org.javenstudio.falcon.search.hits.FieldParams;
import org.javenstudio.hornet.search.query.BooleanClause;
import org.javenstudio.hornet.search.query.BooleanQuery;
import org.javenstudio.hornet.search.query.MatchAllDocsQuery;

/**
 *
 */
public class QueryUtils {

	/** return true if this query has no positive components */
	public static boolean isNegative(IQuery q) {
		if (!(q instanceof BooleanQuery)) 
			return false;
		
		BooleanQuery bq = (BooleanQuery)q;
		IBooleanClause[] clauses = bq.getClauses();
		
		if (clauses == null || clauses.length == 0) 
			return false;
		
		for (IBooleanClause clause : clauses) {
			if (!clause.isProhibited()) 
				return false;
		}
		
		return true;
	}

	/** 
	 * Returns the original query if it was already a positive query, otherwise
	 * return the negative of the query (i.e., a positive query).
	 * <p>
	 * Example: both id:10 and id:-10 will return id:10
	 * <p>
	 * The caller can tell the sign of the original by a reference comparison between
	 * the original and returned query.
	 * @param q Query to create the absolute version of
	 * @return Absolute version of the Query
	 */
	public static IQuery getAbs(IQuery q) {
		if (q instanceof WrappedQuery) {
			IQuery subQ = ((WrappedQuery)q).getWrappedQuery();
			IQuery absSubQ = getAbs(subQ);
			
			if (absSubQ == subQ) 
				return q;
			
			WrappedQuery newQ = ((WrappedQuery)q).clone();
			newQ.setWrappedQuery(absSubQ);
			
			return newQ;
		}

		if (!(q instanceof BooleanQuery)) 
			return q;
		
		BooleanQuery bq = (BooleanQuery)q;
		IBooleanClause[] clauses = bq.getClauses();
		
		if (clauses == null || clauses.length == 0) 
			return q;

		for (IBooleanClause clause : clauses) {
			if (!clause.isProhibited()) 
				return q;
		}

		if (clauses.length == 1) {
			// if only one clause, dispense with the wrapping BooleanQuery
			IQuery negClause = clauses[0].getQuery();
			
			// we shouldn't need to worry about adjusting the boosts since the negative
			// clause would have never been selected in a positive query, and hence would
			// not contribute to a score.
			return negClause;
			
		} else {
			BooleanQuery newBq = new BooleanQuery(bq.isCoordDisabled());
			newBq.setBoost(bq.getBoost());
			
			// ignore minNrShouldMatch... it doesn't make sense for a negative query
			// the inverse of -a -b is a OR b
			for (IBooleanClause clause : clauses) {
				newBq.add(clause.getQuery(), BooleanClause.Occur.SHOULD);
			}
			
			return newBq;
		}
	}

	/** 
	 * Makes negative queries suitable for querying by
	 * indexdb.
	 */
	public static IQuery makeQueryable(IQuery q) {
		if (q instanceof WrappedQuery) 
			return makeQueryable(((WrappedQuery)q).getWrappedQuery());
		
		return isNegative(q) ? fixNegativeQuery(q) : q;
	}

	/** 
	 * Fixes a negative query by adding a MatchAllDocs query clause.
	 * The query passed in *must* be a negative query.
	 */
	public static Query fixNegativeQuery(IQuery q) {
		BooleanQuery newBq = ((BooleanQuery)q).clone();
		newBq.add(new MatchAllDocsQuery(), BooleanClause.Occur.MUST);
		
		return newBq;    
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
		
		for (IBooleanClause c : q.getClauses()) {
			if (c.getOccur() == IBooleanClause.Occur.SHOULD) 
				optionalClauses++;
		}

		int msm = calculateMinShouldMatch(optionalClauses, spec);
		if (msm > 0) 
			q.setMinimumNumberShouldMatch(msm);
	}
	
	// private static Pattern spaceAroundLessThanPattern = Pattern.compile("\\s*<\\s*");
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
			// we have conditional spec(s) 
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

		// otherwise, simple expresion 

		if (spec.indexOf('%') > -1) {
			// percentage - assume the % was the last char. 
			// If not, let Integer.parseInt fail. 
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
	 * @param fieldLists an array of Strings eg. 
	 * 	<code>{"fieldOne^2.3", "fieldTwo", fieldThree^-0.4}</code>
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
			if(in.length()==0) 
				continue;
      
			String[] bb = sWhitespacePattern.split(in);
			for (String s : bb) {
				String[] bbb = sCaratPattern.split(s);
				out.put(bbb[0], 1 == bbb.length ? null : Float.valueOf(bbb[1]));
			}
		}
		
		return out;
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
		String temp = CONSECUTIVE_OP_PATTERN.matcher( s ).replaceAll( " " );
		return DANGLING_OP_PATTERN.matcher( temp ).replaceAll( "" );
	}

	/**
	 * Returns it's input if there is an even (ie: balanced) number of
	 * '"' characters -- otherwise returns a String in which all '"'
	 * characters are striped out.
	 */
	public static CharSequence stripUnbalancedQuotes(CharSequence s) {
		int count = 0;
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == '\"') count++; 
		}
		
		if ((count & 1) == 0) 
			return s;
    
		return s.toString().replace("\"","");
	}
	
	/**
	 * Escapes all special characters except '"', '-', and '+'
	 */
	public static CharSequence partialEscape(CharSequence s) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '\\' || c == '!' || c == '(' || c == ')' ||
				c == ':'  || c == '^' || c == '[' || c == ']' || c == '/' ||
				c == '{'  || c == '}' || c == '~' || c == '*' || c == '?') {
				sb.append('\\');
			}
			sb.append(c);
		}
		return sb;
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
	public static void flattenBooleanQuery(IBooleanQuery to, IBooleanQuery from) {
		for (IBooleanClause clause : from.getClauses()) {

			IQuery cq = clause.getQuery();
			cq.setBoost(cq.getBoost() * from.getBoost());

			if (cq instanceof BooleanQuery
					&& !clause.isRequired()
					&& !clause.isProhibited()) {

				/* we can recurse */
				flattenBooleanQuery(to, (BooleanQuery)cq);

			} else {
				to.add(clause);
			}
		}
	}
  
	/**
	 * Like {@link #parseFieldBoosts}, but allows for an optional slop value prefixed by "~".
	 *
	 * @param fieldLists - an array of Strings eg. 
	 * 	<code>{"fieldOne^2.3", "fieldTwo", fieldThree~5^-0.4}</code>
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
			if (in == null) continue;
      
			in = in.trim();
			if (in.length() == 0) 
				continue;
      
			String[] fieldConfigs = sWhitespacePattern.split(in);
			for (String s : fieldConfigs) {
				String[] fieldAndSlopVsBoost = sCaratPattern.split(s);
				String[] fieldVsSlop = sTildePattern.split(fieldAndSlopVsBoost[0]);
				
				String field = fieldVsSlop[0];
				int slop  = (fieldVsSlop.length == 2) ? Integer.valueOf(fieldVsSlop[1]) : defaultSlop;
				Float boost = (fieldAndSlopVsBoost.length == 1) ? 1 : Float.valueOf(fieldAndSlopVsBoost[1]);
				
				FieldParams fp = new FieldParams(field,wordGrams,slop,boost);
				out.add(fp);
			}
		}
		return out;
	}
  
}
