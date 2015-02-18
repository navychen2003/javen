package org.javenstudio.panda.query;

import java.util.Locale;
import java.util.TimeZone;
import java.util.TooManyListenersException;

import org.javenstudio.common.indexdb.IAnalyzer;
import org.javenstudio.hornet.search.query.MultiTermQuery;
import org.javenstudio.hornet.search.query.RewriteMethod;
import org.javenstudio.hornet.util.DateTools;

/**
 * Configuration options common across queryparser implementations.
 */
public interface QueryParserConfiguration {
  
	/**
	 * Set to <code>true</code> to allow leading wildcard characters.
	 * <p>
	 * When set, <code>*</code> or <code>?</code> are allowed as the first
	 * character of a PrefixQuery and WildcardQuery. Note that this can produce
	 * very slow queries on big indexes.
	 * <p>
	 * Default: false.
	 */
	public void setLowercaseExpandedTerms(boolean lowercaseExpandedTerms);
  
	/**
	 * @see #setLowercaseExpandedTerms(boolean)
	 */
	public boolean getLowercaseExpandedTerms();
  
	/**
	 * Set to <code>true</code> to allow leading wildcard characters.
	 * <p>
	 * When set, <code>*</code> or <code>?</code> are allowed as the first
	 * character of a PrefixQuery and WildcardQuery. Note that this can produce
	 * very slow queries on big indexes.
	 * <p>
	 * Default: false.
	 */
	public void setAllowLeadingWildcard(boolean allowLeadingWildcard);
  
	/**
	 * Set to <code>true</code> to enable position increments in result query.
	 * <p>
	 * When set, result phrase and multi-phrase queries will be aware of position
	 * increments. Useful when e.g. a StopFilter increases the position increment
	 * of the token that follows an omitted token.
	 * <p>
	 * Default: false.
	 */
	public void setEnablePositionIncrements(boolean enabled);
  
	/**
	 * @see #setEnablePositionIncrements(boolean)
	 */
	public boolean getEnablePositionIncrements();
  
	/**
	 * By default, it uses
	 * {@link MultiTermQuery#CONSTANT_SCORE_AUTO_REWRITE_DEFAULT} when creating a
	 * prefix, wildcard and range queries. This implementation is generally
	 * preferable because it a) Runs faster b) Does not have the scarcity of terms
	 * unduly influence score c) avoids any {@link TooManyListenersException}
	 * exception. However, if your application really needs to use the
	 * old-fashioned boolean queries expansion rewriting and the above points are
	 * not relevant then use this change the rewrite method.
	 */
	public void setMultiTermRewriteMethod(RewriteMethod method);
  
	/**
	 * @see #setMultiTermRewriteMethod(MultiTermQuery.RewriteMethod)
	 */
	public RewriteMethod getMultiTermRewriteMethod();
  
	/**
	 * Set the prefix length for fuzzy queries. Default is 0.
	 * 
	 * @param fuzzyPrefixLength
	 *          The fuzzyPrefixLength to set.
	 */
	public void setFuzzyPrefixLength(int fuzzyPrefixLength);
  
	/**
	 * Set locale used by date range parsing.
	 */
	public void setLocale(Locale locale);
  
	/**
	 * Returns current locale, allowing access by subclasses.
	 */
	public Locale getLocale();
  
	public void setTimeZone(TimeZone timeZone);
  
	public TimeZone getTimeZone();
  
	/**
	 * Sets the default slop for phrases. If zero, then exact phrase matches are
	 * required. Default value is zero.
	 */
	public void setPhraseSlop(int defaultPhraseSlop);
  
	public IAnalyzer getAnalyzer();
  
	/**
	 * @see #setAllowLeadingWildcard(boolean)
	 */
	public boolean getAllowLeadingWildcard();
  
	/**
	 * Get the minimal similarity for fuzzy queries.
	 */
	public float getFuzzyMinSimilarity();
  
	/**
	 * Get the prefix length for fuzzy queries.
	 * 
	 * @return Returns the fuzzyPrefixLength.
	 */
	public int getFuzzyPrefixLength();
  
	/**
	 * Gets the default slop for phrases.
	 */
	public int getPhraseSlop();
  
	/**
	 * Set the minimum similarity for fuzzy queries. Default is defined on
	 * {@link FuzzyQuery#defaultMinSimilarity}.
	 */
	public void setFuzzyMinSimilarity(float fuzzyMinSim);
  
	/**
	 * Sets the default {@link Resolution} used for certain field when
	 * no {@link Resolution} is defined for this field.
	 * 
	 * @param dateResolution the default {@link Resolution}
	 */
	public void setDateResolution(DateTools.Resolution dateResolution);
  
}
