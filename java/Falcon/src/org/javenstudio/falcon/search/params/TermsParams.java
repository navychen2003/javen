package org.javenstudio.falcon.search.params;

import java.util.regex.Pattern;

import org.javenstudio.falcon.util.CommonParams;

/**
 *
 */
public interface TermsParams {
	
  /**
   * The component name.  Set to true to turn on the TermsComponent
   */
  public static final String TERMS = "terms";

  /**
   * Used for building up the other terms
   */
  public static final String TERMS_PREFIX = TERMS + ".";

  /**
   * Required.  Specify the field to look up terms in.
   */
  public static final String TERMS_FIELD = TERMS_PREFIX + "fl";

  /**
   * Optional.  The lower bound term to start at. 
   *  The TermEnum will start at the next term after this term in the dictionary.
   *
   * If not specified, the empty string is used
   */
  public static final String TERMS_LOWER = TERMS_PREFIX + "lower";

  /**
   * Optional.  The term to stop at.
   *
   * @see #TERMS_UPPER_INCLUSIVE
   */
  public static final String TERMS_UPPER = TERMS_PREFIX + "upper";
  
  /**
   * Optional.  If true, include the upper bound term in the results.  False by default.
   */
  public static final String TERMS_UPPER_INCLUSIVE = TERMS_PREFIX + "upper.incl";

  /**
   * Optional.  If true, include the lower bound term in the results, 
   * otherwise skip to the next one.  True by default.
   */
  public static final String TERMS_LOWER_INCLUSIVE = TERMS_PREFIX + "lower.incl";

  /**
   * Optional.  The number of results to return.  
   * If not specified, looks for {@link CommonParams#ROWS}.  
   * If that's not specified, uses 10.
   */
  public static final String TERMS_LIMIT = TERMS_PREFIX + "limit";

  public static final String TERMS_PREFIX_STR = TERMS_PREFIX + "prefix";

  public static final String TERMS_REGEXP_STR = TERMS_PREFIX + "regex";

  public static final String TERMS_REGEXP_FLAG = TERMS_REGEXP_STR + ".flag";

  public static enum TermsRegexpFlag {
      UNIX_LINES(Pattern.UNIX_LINES),
      CASE_INSENSITIVE(Pattern.CASE_INSENSITIVE),
      COMMENTS(Pattern.COMMENTS),
      MULTILINE(Pattern.MULTILINE),
      LITERAL(Pattern.LITERAL),
      DOTALL(Pattern.DOTALL),
      UNICODE_CASE(Pattern.UNICODE_CASE),
      CANON_EQ(Pattern.CANON_EQ);

      private int mValue;

      TermsRegexpFlag(int value) {
          mValue = value;
      }

      public int getValue() {
          return mValue;
      }
  }

  /**
   * Optional.  The minimum value of docFreq to be returned.  1 by default
   */
  public static final String TERMS_MINCOUNT = TERMS_PREFIX + "mincount";
  
  /**
   * Optional.  The maximum value of docFreq to be returned. 
   * -1 by default means no boundary
   */
  public static final String TERMS_MAXCOUNT = TERMS_PREFIX + "maxcount";

  /**
   * Optional.  
   * If true, return the raw characters of the indexed term, 
   * regardless of if it is readable.
   * For instance, the index form of numeric numbers is not human readable. 
   * The default is false.
   */
  public static final String TERMS_RAW = TERMS_PREFIX + "raw";

  /**
   * Optional.  If sorting by frequency is enabled. 
   * Defaults to sorting by count.
   */
  public static final String TERMS_SORT = TERMS_PREFIX + "sort";
  
  public static final String TERMS_SORT_COUNT = "count";
  public static final String TERMS_SORT_INDEX = "index";
  
}
