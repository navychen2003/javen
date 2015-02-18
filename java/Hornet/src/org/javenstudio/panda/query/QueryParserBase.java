package org.javenstudio.panda.query;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.javenstudio.common.indexdb.IAnalyzer;
import org.javenstudio.common.indexdb.IBooleanClause;
import org.javenstudio.common.indexdb.IBooleanQuery;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.TooManyClauses;
import org.javenstudio.common.indexdb.index.term.Term;
import org.javenstudio.common.indexdb.search.Query;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.search.query.BooleanClause;
import org.javenstudio.hornet.search.query.BooleanQuery;
import org.javenstudio.hornet.search.query.FuzzyQuery;
import org.javenstudio.hornet.search.query.MatchAllDocsQuery;
import org.javenstudio.hornet.search.query.MultiPhraseQuery;
import org.javenstudio.hornet.search.query.MultiTermQuery;
import org.javenstudio.hornet.search.query.PhraseQuery;
import org.javenstudio.hornet.search.query.PrefixQuery;
import org.javenstudio.hornet.search.query.RewriteMethod;
import org.javenstudio.hornet.search.query.TermQuery;
import org.javenstudio.hornet.search.query.TermRangeQuery;
import org.javenstudio.hornet.util.DateTools;

/** 
 * This class is overridden by QueryParser in QueryParser.jj
 * and acts to separate the majority of the Java code from the .jj grammar file. 
 */
public abstract class QueryParserBase implements QueryParserConfiguration {

	/** 
	 * Do not catch this exception in your code, it means you are using methods that 
	 * you should no longer use. 
	 */
	public static class MethodRemovedUseAnother extends Throwable {
		private static final long serialVersionUID = 1L; 
	}

	public static final int CONJ_NONE   = 0;
	public static final int CONJ_AND    = 1;
	public static final int CONJ_OR     = 2;

	public static final int MOD_NONE    = 0;
	public static final int MOD_NOT     = 10;
	public static final int MOD_REQ     = 11;

	// make it possible to call setDefaultOperator() without accessing
	// the nested class:
	/** Alternative form of QueryParser.Operator.AND */
	public static final QueryParser.Operator AND_OPERATOR = QueryParser.Operator.AND;
	/** Alternative form of QueryParser.Operator.OR */
	public static final QueryParser.Operator OR_OPERATOR = QueryParser.Operator.OR;

	/** The actual operator that parser uses to combine query terms */
	protected QueryParser.Operator mOperator = OR_OPERATOR;

	protected RewriteMethod mMultiTermRewriteMethod = 
			MultiTermQuery.CONSTANT_SCORE_AUTO_REWRITE_DEFAULT;
	
	protected boolean mLowercaseExpandedTerms = true;
	protected boolean mAllowLeadingWildcard = false;
	protected boolean mEnablePositionIncrements = true;

	protected IAnalyzer mAnalyzer;
	protected String mField;
	protected int mPhraseSlop = 0;
	
	@SuppressWarnings("deprecation")
	protected float mFuzzyMinSim = FuzzyQuery.sDefaultMinSimilarity;
	protected int mFuzzyPrefixLength = FuzzyQuery.sDefaultPrefixLength;
	
	protected Locale mLocale = Locale.getDefault();
	protected TimeZone mTimeZone = TimeZone.getDefault();

	// the default date resolution
	protected DateTools.Resolution mDateResolution = null;
	// maps field names to date resolutions
	protected Map<String,DateTools.Resolution> mFieldToDateResolution = null;

	// Whether or not to analyze range terms when constructing RangeQuerys
	// (For example, analyzing terms into collation keys for locale-sensitive RangeQuery)
	protected boolean mAnalyzeRangeTerms = false;

	protected boolean mAutoGeneratePhraseQueries;

	// So the generated QueryParser(CharStream) won't error out
	protected QueryParserBase() {}

	/** 
	 * Initializes a query parser.  Called by the QueryParser constructor
	 *  @param matchVersion  Lucene version to match. See <a href="QueryParser.html#version">here</a>.
	 *  @param f  the default field for query terms.
	 *  @param a   used to find terms in the query text.
	 */
	public void init(String f, IAnalyzer a) {
		mAnalyzer = a;
		mField = f;
		
		setAutoGeneratePhraseQueries(false);
	}

	// the generated parser will create these in QueryParser
	public abstract void reInit(CharStream stream);
	public abstract IQuery getTopLevelQuery(String field) throws ParseException;

	/** 
	 * Parses a query string, returning a {@link org.apache.lucene.search.Query}.
	 *  @param query  the query string to be parsed.
	 *  @throws ParseException if the parsing fails
	 */
	public IQuery parse(String query) throws ParseException {
		reInit(new FastCharStream(new StringReader(query)));
		
		try {
			// TopLevelQuery is a Query followed by the end-of-input (EOF)
			IQuery res = getTopLevelQuery(mField);
			return res != null ? res : newBooleanQuery(false);
			
		} catch (ParseException tme) {
			// rethrow to include the original query:
			ParseException e = new ParseException("Cannot parse '" 
					+ query + "': " + tme.getMessage());
			e.initCause(tme);
			throw e;
			
		} catch (TokenMgrError tme) {
		  ParseException e = new ParseException("Cannot parse '" 
				  + query + "': " + tme.getMessage());
		  e.initCause(tme);
		  throw e;
		  
		} catch (TooManyClauses tmc) {
			ParseException e = new ParseException("Cannot parse '" 
					+ query + "': too many boolean clauses");
			e.initCause(tmc);
			throw e;
		}
	}

	/** @return Returns the analyzer. */
	public IAnalyzer getAnalyzer() {
		return mAnalyzer;
	}

	/** @return Returns the default field. */
	public String getFieldName() {
		return mField;
	}

	/** @see #setAutoGeneratePhraseQueries(boolean) */
	public final boolean getAutoGeneratePhraseQueries() {
		return mAutoGeneratePhraseQueries;
	}

	/**
	 * Set to true if phrase queries will be automatically generated
	 * when the analyzer returns more than one term from whitespace
	 * delimited text.
	 * NOTE: this behavior may not be suitable for all languages.
	 * <p>
	 * Set to false if phrase queries should only be generated when
	 * surrounded by double quotes.
	 */
	public final void setAutoGeneratePhraseQueries(boolean value) {
		mAutoGeneratePhraseQueries = value;
	}

	/**
	 * Get the minimal similarity for fuzzy queries.
	 */
	public float getFuzzyMinSimilarity() {
		return mFuzzyMinSim;
	}

	/**
	 * Set the minimum similarity for fuzzy queries.
	 * Default is 2f.
	 */
	public void setFuzzyMinSimilarity(float fuzzyMinSim) {
		mFuzzyMinSim = fuzzyMinSim;
	}

	/**
	 * Get the prefix length for fuzzy queries.
	 * @return Returns the fuzzyPrefixLength.
	 */
	public int getFuzzyPrefixLength() {
		return mFuzzyPrefixLength;
	}

	/**
	 * Set the prefix length for fuzzy queries. Default is 0.
	 * @param fuzzyPrefixLength The fuzzyPrefixLength to set.
	 */
	public void setFuzzyPrefixLength(int fuzzyPrefixLength) {
		mFuzzyPrefixLength = fuzzyPrefixLength;
	}

	/**
	 * Sets the default slop for phrases.  If zero, then exact phrase matches
	 * are required.  Default value is zero.
	 */
	public void setPhraseSlop(int phraseSlop) {
		mPhraseSlop = phraseSlop;
	}

	/**
	 * Gets the default slop for phrases.
	 */
	public int getPhraseSlop() {
		return mPhraseSlop;
	}

	/**
	 * Set to <code>true</code> to allow leading wildcard characters.
	 * <p>
	 * When set, <code>*</code> or <code>?</code> are allowed as
	 * the first character of a PrefixQuery and WildcardQuery.
	 * Note that this can produce very slow
	 * queries on big indexes.
	 * <p>
	 * Default: false.
	 */
	public void setAllowLeadingWildcard(boolean allowLeadingWildcard) {
		mAllowLeadingWildcard = allowLeadingWildcard;
	}

	/**
	 * @see #setAllowLeadingWildcard(boolean)
	 */
	public boolean getAllowLeadingWildcard() {
		return mAllowLeadingWildcard;
	}

	/**
	 * Set to <code>true</code> to enable position increments in result query.
	 * <p>
	 * When set, result phrase and multi-phrase queries will
	 * be aware of position increments.
	 * Useful when e.g. a StopFilter increases the position increment of
	 * the token that follows an omitted token.
	 * <p>
	 * Default: true.
	 */
	public void setEnablePositionIncrements(boolean enable) {
		mEnablePositionIncrements = enable;
	}

	/**
	 * @see #setEnablePositionIncrements(boolean)
	 */
	public boolean getEnablePositionIncrements() {
		return mEnablePositionIncrements;
	}

	/**
	 * Sets the boolean operator of the QueryParser.
	 * In default mode (<code>OR_OPERATOR</code>) terms without any modifiers
	 * are considered optional: for example <code>capital of Hungary</code> is equal to
	 * <code>capital OR of OR Hungary</code>.<br/>
	 * In <code>AND_OPERATOR</code> mode terms are considered to be in conjunction: the
	 * above mentioned query is parsed as <code>capital AND of AND Hungary</code>
	 */
	public void setDefaultOperator(QueryParser.Operator op) {
		mOperator = op;
	}

	/**
	 * Gets implicit operator setting, which will be either AND_OPERATOR
	 * or OR_OPERATOR.
	 */
	public QueryParser.Operator getDefaultOperator() {
		return mOperator;
	}

	/**
	 * Whether terms of wildcard, prefix, fuzzy and range queries are to be automatically
	 * lower-cased or not.  Default is <code>true</code>.
	 */
	public void setLowercaseExpandedTerms(boolean lowercaseExpandedTerms) {
		mLowercaseExpandedTerms = lowercaseExpandedTerms;
	}

	/**
	 * @see #setLowercaseExpandedTerms(boolean)
	 */
	public boolean getLowercaseExpandedTerms() {
		return mLowercaseExpandedTerms;
	}

	/**
	 * By default QueryParser uses {@link MultiTermQuery#CONSTANT_SCORE_AUTO_REWRITE_DEFAULT}
	 * when creating a PrefixQuery, WildcardQuery or RangeQuery. 
	 * This implementation is generally preferable because it
	 * a) Runs faster b) Does not have the scarcity of terms unduly influence score
	 * c) avoids any "TooManyBooleanClauses" exception.
	 * However, if your application really needs to use the
	 * old-fashioned BooleanQuery expansion rewriting and the above
	 * points are not relevant then use this to change
	 * the rewrite method.
	 */
	public void setMultiTermRewriteMethod(RewriteMethod method) {
		mMultiTermRewriteMethod = method;
	}

	/**
	 * @see #setMultiTermRewriteMethod
	 */
	public RewriteMethod getMultiTermRewriteMethod() {
		return mMultiTermRewriteMethod;
	}

	/**
	 * Set locale used by date range parsing, lowercasing, and other
	 * locale-sensitive operations.
	 */
	public void setLocale(Locale locale) {
		mLocale = locale;
	}

	/**
	 * Returns current locale, allowing access by subclasses.
	 */
	public Locale getLocale() {
		return mLocale;
	}
  
	public void setTimeZone(TimeZone timeZone) {
		mTimeZone = timeZone;
	}
  
	public TimeZone getTimeZone() {
		return mTimeZone;
	}

	/**
	 * Sets the default date resolution used by RangeQueries for fields for which no
	 * specific date resolutions has been set. Field specific resolutions can be set
	 * with {@link #setDateResolution(String, DateTools.Resolution)}.
	 *
	 * @param dateResolution the default date resolution to set
	 */
	public void setDateResolution(DateTools.Resolution dateResolution) {
		mDateResolution = dateResolution;
	}

	/**
	 * Sets the date resolution used by RangeQueries for a specific field.
	 *
	 * @param fieldName field for which the date resolution is to be set
	 * @param dateResolution date resolution to set
	 */
	public void setDateResolution(String fieldName, DateTools.Resolution dateResolution) {
		if (fieldName == null) 
			throw new IllegalArgumentException("Field cannot be null.");

		if (mFieldToDateResolution == null) {
			// lazily initialize HashMap
			mFieldToDateResolution = new HashMap<String,DateTools.Resolution>();
		}

		mFieldToDateResolution.put(fieldName, dateResolution);
	}

	/**
	 * Returns the date resolution that is used by RangeQueries for the given field.
	 * Returns null, if no default or field specific date resolution has been set
	 * for the given field.
	 */
	public DateTools.Resolution getDateResolution(String fieldName) {
		if (fieldName == null) 
			throw new IllegalArgumentException("Field cannot be null.");

		if (mFieldToDateResolution == null) {
			// no field specific date resolutions set; return default date resolution instead
			return mDateResolution;
		}

		DateTools.Resolution resolution = mFieldToDateResolution.get(fieldName);
		if (resolution == null) {
			// no date resolutions set for the given field; return default date resolution instead
			resolution = mDateResolution;
		}

		return resolution;
	}

	/**
	 * Set whether or not to analyze range terms when constructing RangeQuerys.
	 * For example, setting this to true can enable analyzing terms into 
	 * collation keys for locale-sensitive RangeQuery.
	 * 
	 * @param analyzeRangeTerms whether or not terms should be analyzed for RangeQuerys
	 */
	public void setAnalyzeRangeTerms(boolean analyzeRangeTerms) {
		mAnalyzeRangeTerms = analyzeRangeTerms;
	}

	/**
	 * @return whether or not to analyze range terms when constructing RangeQuerys.
	 */
	public boolean getAnalyzeRangeTerms() {
		return mAnalyzeRangeTerms;
	}

	protected void addClause(List<IBooleanClause> clauses, int conj, int mods, IQuery q) {
		QueryParserHelper.addClause(this, clauses, conj, mods, q);
	}

	/**
	 * @exception ParseException throw in overridden method to disallow
	 */
	protected IQuery getFieldQuery(String field, String queryText, 
			boolean quoted) throws ParseException {
		return newFieldQuery(mAnalyzer, field, queryText, quoted);
	}
  
	/**
	 * @exception ParseException throw in overridden method to disallow
	 */
	protected IQuery newFieldQuery(IAnalyzer analyzer, String field, String queryText, 
			boolean quoted) throws ParseException {
		return QueryParserHelper.newFieldQuery(this, analyzer, field, queryText, quoted);
	}

	/**
	 * Base implementation delegates to {@link #getFieldQuery(String,String,boolean)}.
	 * This method may be overridden, for example, to return
	 * a SpanNearQuery instead of a PhraseQuery.
	 *
	 * @exception ParseException throw in overridden method to disallow
	 */
	protected IQuery getFieldQuery(String field, String queryText, int slop)
			throws ParseException {
		IQuery query = getFieldQuery(field, queryText, true);

		if (query instanceof PhraseQuery) 
			((PhraseQuery) query).setSlop(slop);
		
		if (query instanceof MultiPhraseQuery) 
			((MultiPhraseQuery) query).setSlop(slop);

		return query;
	}

	protected IQuery getRangeQuery(String field, String part1, String part2,
			boolean startInclusive, boolean endInclusive) throws ParseException {
		return QueryParserHelper.getRangeQuery(this, 
				field, part1, part2, startInclusive, endInclusive);
	}

	/**
	 * Builds a new BooleanQuery instance
	 * @param disableCoord disable coord
	 * @return new BooleanQuery instance
	 */
	protected IBooleanQuery newBooleanQuery(boolean disableCoord) {
		return new BooleanQuery(disableCoord);
	}

	/**
	 * Builds a new BooleanClause instance
	 * @param q sub query
	 * @param occur how this clause should occur when matching documents
	 * @return new BooleanClause instance
	 */
	protected IBooleanClause newBooleanClause(IQuery q, BooleanClause.Occur occur) {
		return new BooleanClause(q, occur);
	}

	/**
	 * Builds a new TermQuery instance
	 * @param term term
	 * @return new TermQuery instance
	 */
	protected IQuery newTermQuery(Term term){
		return new TermQuery(term);
	}

	/**
	 * Builds a new PhraseQuery instance
	 * @return new PhraseQuery instance
	 */
	protected PhraseQuery newPhraseQuery(){
		return new PhraseQuery();
	}

	/**
	 * Builds a new MultiPhraseQuery instance
	 * @return new MultiPhraseQuery instance
	 */
	protected MultiPhraseQuery newMultiPhraseQuery(){
		return new MultiPhraseQuery();
	}

	/**
	 * Builds a new PrefixQuery instance
	 * @param prefix Prefix term
	 * @return new PrefixQuery instance
	 */
	protected IQuery newPrefixQuery(ITerm prefix){
		PrefixQuery query = new PrefixQuery(prefix);
		query.setRewriteMethod(mMultiTermRewriteMethod);
		return query;
	}

	/**
	 * Builds a new RegexpQuery instance
	 * @param regexp Regexp term
	 * @return new RegexpQuery instance
	 */
	protected IQuery newRegexpQuery(ITerm regexp) {
		//RegexpQuery query = new RegexpQuery(regexp);
		//query.setRewriteMethod(multiTermRewriteMethod);
		//return query;
		
		throw new UnsupportedOperationException("RegexpQuery not supported yet");
	}

	/**
	 * Builds a new FuzzyQuery instance
	 * @param term Term
	 * @param minimumSimilarity minimum similarity
	 * @param prefixLength prefix length
	 * @return new FuzzyQuery Instance
	 */
	@SuppressWarnings("deprecation")
	protected IQuery newFuzzyQuery(Term term, float minimumSimilarity, int prefixLength) {
		// FuzzyQuery doesn't yet allow constant score rewrite
		String text = term.getText();
		int numEdits = FuzzyQuery.floatToEdits(minimumSimilarity, 
				text.codePointCount(0, text.length()));
		
		return new FuzzyQuery(term,numEdits,prefixLength);
	}

	// TODO: Should this be protected instead?
	private BytesRef analyzeMultitermTerm(String field, String part) {
		return analyzeMultitermTerm(field, part, mAnalyzer);
	}

	protected BytesRef analyzeMultitermTerm(String field, String part, IAnalyzer analyzerIn) {
		return QueryParserHelper.analyzeMultitermTerm(this, field, part, analyzerIn);
	}

	/**
	 * Builds a new TermRangeQuery instance
	 * @param field Field
	 * @param part1 min
	 * @param part2 max
	 * @param startInclusive true if the start of the range is inclusive
	 * @param endInclusive true if the end of the range is inclusive
	 * @return new TermRangeQuery instance
	 */
	protected IQuery newRangeQuery(String field, String part1, String part2, 
			boolean startInclusive, boolean endInclusive) {
		final BytesRef start;
		final BytesRef end;
     
		if (part1 == null) {
			start = null;
		} else {
			start = mAnalyzeRangeTerms ? 
					analyzeMultitermTerm(field, part1) : new BytesRef(part1);
		}
     
		if (part2 == null) {
			end = null;
		} else {
			end = mAnalyzeRangeTerms ? 
					analyzeMultitermTerm(field, part2) : new BytesRef(part2);
		}
      
		final TermRangeQuery query = new TermRangeQuery(field, start, end, 
				startInclusive, endInclusive);

		query.setRewriteMethod(mMultiTermRewriteMethod);
		
		return query;
	}

	/**
	 * Builds a new MatchAllDocsQuery instance
	 * @return new MatchAllDocsQuery instance
	 */
	protected IQuery newMatchAllDocsQuery() {
		return new MatchAllDocsQuery();
	}

	/**
	 * Builds a new WildcardQuery instance
	 * @param t wildcard term
	 * @return new WildcardQuery instance
	 */
	protected IQuery newWildcardQuery(ITerm t) {
		//  WildcardQuery query = new WildcardQuery(t);
		//  query.setRewriteMethod(mMultiTermRewriteMethod);
		//  return query;
		
		throw new UnsupportedOperationException("WildcardQuery not supported yet");
	}

	/**
	 * Factory method for generating query, given a set of clauses.
	 * By default creates a boolean query composed of clauses passed in.
	 *
	 * Can be overridden by extending classes, to modify query being
	 * returned.
	 *
	 * @param clauses List that contains {@link BooleanClause} instances to join.
	 *
	 * @return Resulting {@link Query} object.
	 * @exception ParseException throw in overridden method to disallow
	 */
	protected IQuery getBooleanQuery(List<IBooleanClause> clauses) throws ParseException {
		return getBooleanQuery(clauses, false);
	}

	/**
	 * Factory method for generating query, given a set of clauses.
	 * By default creates a boolean query composed of clauses passed in.
	 *
	 * Can be overridden by extending classes, to modify query being
	 * returned.
	 *
	 * @param clauses List that contains {@link BooleanClause} instances
	 *    to join.
	 * @param disableCoord true if coord scoring should be disabled.
	 *
	 * @return Resulting {@link Query} object.
	 * @exception ParseException throw in overridden method to disallow
	 */
	protected IQuery getBooleanQuery(List<IBooleanClause> clauses, boolean disableCoord)
			throws ParseException {
		if (clauses.size() == 0) 
			return null; // all clause words were filtered away by the analyzer.
    
		IBooleanQuery query = newBooleanQuery(disableCoord);
		for (final IBooleanClause clause: clauses) {
			query.add(clause);
		}
		
		return query;
	}

	/**
	 * Factory method for generating a query. Called when parser
	 * parses an input term token that contains one or more wildcard
	 * characters (? and *), but is not a prefix term token (one
	 * that has just a single * character at the end)
	 *<p>
	 * Depending on settings, prefix term may be lower-cased
	 * automatically. It will not go through the default Analyzer,
	 * however, since normal Analyzers are unlikely to work properly
	 * with wildcard templates.
	 *<p>
	 * Can be overridden by extending classes, to provide custom handling for
	 * wildcard queries, which may be necessary due to missing analyzer calls.
	 *
	 * @param field Name of the field query will use.
	 * @param termStr Term token that contains one or more wild card
	 *   characters (? or *), but is not simple prefix term
	 *
	 * @return Resulting {@link Query} built for the term
	 * @exception ParseException throw in overridden method to disallow
	 */
	protected IQuery getWildcardQuery(String field, String termStr) throws ParseException {
		if ("*".equals(field)) {
			if ("*".equals(termStr)) 
				return newMatchAllDocsQuery();
		}
		
		if (!mAllowLeadingWildcard && (termStr.startsWith("*") || termStr.startsWith("?")))
			throw new ParseException("'*' or '?' not allowed as first character in WildcardQuery");
		
		if (mLowercaseExpandedTerms) 
			termStr = termStr.toLowerCase(mLocale);
    
		Term t = new Term(field, termStr);
		
		return newWildcardQuery(t);
	}

	/**
	 * Factory method for generating a query. Called when parser
	 * parses an input term token that contains a regular expression
	 * query.
	 *<p>
	 * Depending on settings, pattern term may be lower-cased
	 * automatically. It will not go through the default Analyzer,
	 * however, since normal Analyzers are unlikely to work properly
	 * with regular expression templates.
	 *<p>
	 * Can be overridden by extending classes, to provide custom handling for
	 * regular expression queries, which may be necessary due to missing analyzer
	 * calls.
	 *
	 * @param field Name of the field query will use.
	 * @param termStr Term token that contains a regular expression
	 *
	 * @return Resulting {@link Query} built for the term
	 * @exception ParseException throw in overridden method to disallow
	 */
	protected IQuery getRegexpQuery(String field, String termStr) throws ParseException {
		if (mLowercaseExpandedTerms) 
			termStr = termStr.toLowerCase(mLocale);
    
		Term t = new Term(field, termStr);
		
		return newRegexpQuery(t);
	}

	/**
	 * Factory method for generating a query (similar to
	 * {@link #getWildcardQuery}). Called when parser parses an input term
	 * token that uses prefix notation; that is, contains a single '*' wildcard
	 * character as its last character. Since this is a special case
	 * of generic wildcard term, and such a query can be optimized easily,
	 * this usually results in a different query object.
	 *<p>
	 * Depending on settings, a prefix term may be lower-cased
	 * automatically. It will not go through the default Analyzer,
	 * however, since normal Analyzers are unlikely to work properly
	 * with wildcard templates.
	 *<p>
	 * Can be overridden by extending classes, to provide custom handling for
	 * wild card queries, which may be necessary due to missing analyzer calls.
	 *
	 * @param field Name of the field query will use.
	 * @param termStr Term token to use for building term for the query
	 *    (<b>without</b> trailing '*' character!)
	 *
	 * @return Resulting {@link Query} built for the term
	 * @exception ParseException throw in overridden method to disallow
	 */
	protected IQuery getPrefixQuery(String field, String termStr) throws ParseException {
		if (!mAllowLeadingWildcard && termStr.startsWith("*"))
			throw new ParseException("'*' not allowed as first character in PrefixQuery");
		
		if (mLowercaseExpandedTerms) 
			termStr = termStr.toLowerCase(mLocale);
    
		Term t = new Term(field, termStr);
		
		return newPrefixQuery(t);
	}

	/**
	 * Factory method for generating a query (similar to
	 * {@link #getWildcardQuery}). Called when parser parses
	 * an input term token that has the fuzzy suffix (~) appended.
	 *
	 * @param field Name of the field query will use.
	 * @param termStr Term token to use for building term for the query
	 *
	 * @return Resulting {@link Query} built for the term
	 * @exception ParseException throw in overridden method to disallow
	 */
	protected IQuery getFuzzyQuery(String field, String termStr, 
			float minSimilarity) throws ParseException {
		if (mLowercaseExpandedTerms) 
			termStr = termStr.toLowerCase(mLocale);
    
		Term t = new Term(field, termStr);
		
		return newFuzzyQuery(t, minSimilarity, mFuzzyPrefixLength);
	}

	/**
	 * Returns a String where the escape char has been
	 * removed, or kept only once if there was a double escape.
	 *
	 * Supports escaped unicode characters, e. g. translates
	 * <code>\\u0041</code> to <code>A</code>.
	 */
	protected String discardEscapeChar(String input) throws ParseException {
		return QueryParserHelper.discardEscapeChar(input);
	}

	// extracted from the .jj grammar
	protected IQuery handleBareTokenQuery(String qfield, ParseToken term, ParseToken fuzzySlop, 
			boolean prefix, boolean wildcard, boolean fuzzy, boolean regexp) throws ParseException {
		return QueryParserHelper.handleBareTokenQuery(this, qfield, term, fuzzySlop, 
				prefix, wildcard, fuzzy, regexp);
	}
	
	// extracted from the .jj grammar
	protected IQuery handleQuotedTerm(String qfield, ParseToken term, ParseToken fuzzySlop) 
			throws ParseException {
		return QueryParserHelper.handleQuotedTerm(this, qfield, term, fuzzySlop);
	}
	
	// extracted from the .jj grammar
	protected IQuery handleBoost(IQuery q, ParseToken boost) {
		return QueryParserHelper.handleBoost(q, boost);
	}
	
}
