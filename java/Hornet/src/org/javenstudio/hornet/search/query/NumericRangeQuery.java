package org.javenstudio.hornet.search.query;

import java.io.IOException;

import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.index.term.TermsEnum;
import org.javenstudio.common.indexdb.util.NumericType;
import org.javenstudio.common.indexdb.util.NumericUtil;
import org.javenstudio.common.indexdb.util.StringHelper;

/**
 * <p>A {@link Query} that matches numeric values within a
 * specified range.  To use this, you must first index the
 * numeric values using {@link IntField}, {@link
 * FloatField}, {@link LongField} or {@link DoubleField} (expert: {@link
 * NumericTokenStream}).  If your terms are instead textual,
 * you should use {@link TermRangeQuery}.  {@link
 * NumericRangeFilter} is the filter equivalent of this
 * query.</p>
 *
 * <p>You create a new NumericRangeQuery with the static
 * factory methods, eg:
 *
 * <pre class="prettyprint">
 * Query q = NumericRangeQuery.newFloatRange("weight", 0.03f, 0.10f, true, true);
 * </pre>
 *
 * matches all documents whose float valued "weight" field
 * ranges from 0.03 to 0.10, inclusive.
 *
 * <p>The performance of NumericRangeQuery is much better
 * than the corresponding {@link TermRangeQuery} because the
 * number of terms that must be searched is usually far
 * fewer, thanks to trie indexing, described below.</p>
 *
 * <p>You can optionally specify a <a
 * href="#precisionStepDesc"><code>precisionStep</code></a>
 * when creating this query.  This is necessary if you've
 * changed this configuration from its default (4) during
 * indexing.  Lower values consume more disk space but speed
 * up searching.  Suitable values are between <b>1</b> and
 * <b>8</b>. A good starting point to test is <b>4</b>,
 * which is the default value for all <code>Numeric*</code>
 * classes.  See <a href="#precisionStepDesc">below</a> for
 * details.
 *
 * <p>This query defaults to {@linkplain
 * MultiTermQuery#CONSTANT_SCORE_AUTO_REWRITE_DEFAULT} for
 * 32 bit (int/float) ranges with precisionStep &le;8 and 64
 * bit (long/double) ranges with precisionStep &le;6.
 * Otherwise it uses {@linkplain
 * MultiTermQuery#CONSTANT_SCORE_FILTER_REWRITE} as the
 * number of terms is likely to be high.  With precision
 * steps of &le;4, this query can be run with one of the
 * BooleanQuery rewrite methods without changing
 * BooleanQuery's default max clause count.
 *
 * <br><h3>How it works</h3>
 *
 * <p>See the publication about <a target="_blank" href="http://www.panfmp.org">panFMP</a>,
 * where this algorithm was described (referred to as <code>TrieRangeQuery</code>):
 *
 * <blockquote><strong>Schindler, U, Diepenbroek, M</strong>, 2008.
 * <em>Generic XML-based Framework for Metadata Portals.</em>
 * Computers &amp; Geosciences 34 (12), 1947-1955.
 * <a href="http://dx.doi.org/10.1016/j.cageo.2008.02.023"
 * target="_blank">doi:10.1016/j.cageo.2008.02.023</a></blockquote>
 *
 * <p><em>A quote from this paper:</em> Because Apache Lucene is a full-text
 * search engine and not a conventional database, it cannot handle numerical ranges
 * (e.g., field value is inside user defined bounds, even dates are numerical values).
 * We have developed an extension to Apache Lucene that stores
 * the numerical values in a special string-encoded format with variable precision
 * (all numerical values like doubles, longs, floats, and ints are converted to
 * lexicographic sortable string representations and stored with different precisions
 * (for a more detailed description of how the values are stored,
 * see {@link NumericUtil}). A range is then divided recursively into multiple intervals for searching:
 * The center of the range is searched only with the lowest possible precision in the <em>trie</em>,
 * while the boundaries are matched more exactly. This reduces the number of terms dramatically.</p>
 *
 * <p>For the variant that stores long values in 8 different precisions (each reduced by 8 bits) that
 * uses a lowest precision of 1 byte, the index contains only a maximum of 256 distinct values in the
 * lowest precision. Overall, a range could consist of a theoretical maximum of
 * <code>7*255*2 + 255 = 3825</code> distinct terms (when there is a term for every distinct value of an
 * 8-byte-number in the index and the range covers almost all of them; a maximum of 255 distinct values is used
 * because it would always be possible to reduce the full 256 values to one term with degraded precision).
 * In practice, we have seen up to 300 terms in most cases (index with 500,000 metadata records
 * and a uniform value distribution).</p>
 *
 * <a name="precisionStepDesc"><h3>Precision Step</h3>
 * <p>You can choose any <code>precisionStep</code> when encoding values.
 * Lower step values mean more precisions and so more terms in index (and index gets larger).
 * On the other hand, the maximum number of terms to match reduces, which optimized query speed.
 * The formula to calculate the maximum term count is:
 * <pre>
 *  n = [ (bitsPerValue/precisionStep - 1) * (2^precisionStep - 1 ) * 2 ] + (2^precisionStep - 1 )
 * </pre>
 * <p><em>(this formula is only correct, when <code>bitsPerValue/precisionStep</code> is an integer;
 * in other cases, the value must be rounded up and the last summand must contain the modulo of the division as
 * precision step)</em>.
 * For longs stored using a precision step of 4, <code>n = 15*15*2 + 15 = 465</code>, and for a precision
 * step of 2, <code>n = 31*3*2 + 3 = 189</code>. But the faster search speed is reduced by more seeking
 * in the term enum of the index. Because of this, the ideal <code>precisionStep</code> value can only
 * be found out by testing. <b>Important:</b> You can index with a lower precision step value and test search speed
 * using a multiple of the original step value.</p>
 *
 * <p>Good values for <code>precisionStep</code> are depending on usage and data type:
 * <ul>
 *  <li>The default for all data types is <b>4</b>, which is used, when no <code>precisionStep</code> is given.
 *  <li>Ideal value in most cases for <em>64 bit</em> data types <em>(long, double)</em> is <b>6</b> or <b>8</b>.
 *  <li>Ideal value in most cases for <em>32 bit</em> data types <em>(int, float)</em> is <b>4</b>.
 *  <li>For low cardinality fields larger precision steps are good. If the cardinality is &lt; 100, it is
 *  fair to use {@link Integer#MAX_VALUE} (see below).
 *  <li>Steps <b>&ge;64</b> for <em>long/double</em> and <b>&ge;32</b> for <em>int/float</em> produces one token
 *  per value in the index and querying is as slow as a conventional {@link TermRangeQuery}. But it can be used
 *  to produce fields, that are solely used for sorting (in this case simply use {@link Integer#MAX_VALUE} as
 *  <code>precisionStep</code>). Using {@link IntField},
 * {@link LongField}, {@link FloatField} or {@link DoubleField} for sorting
 *  is ideal, because building the field cache is much faster than with text-only numbers.
 *  These fields have one term per value and therefore also work with term enumeration for building distinct lists
 *  (e.g. facets / preselected values to search for).
 *  Sorting is also possible with range query optimized fields using one of the above <code>precisionSteps</code>.
 * </ul>
 *
 * <p>Comparisons of the different types of RangeQueries on an index with about 500,000 docs showed
 * that {@link TermRangeQuery} in boolean rewrite mode (with raised {@link BooleanQuery} clause count)
 * took about 30-40 secs to complete, {@link TermRangeQuery} in constant score filter rewrite mode took 5 secs
 * and executing this class took &lt;100ms to complete (on an Opteron64 machine, Java 1.5, 8 bit
 * precision step). This query type was developed for a geographic portal, where the performance for
 * e.g. bounding boxes or exact date/time stamps is important.</p>
 *
 * @since 2.9
 */
public final class NumericRangeQuery<T extends Number> extends MultiTermQuery {

	// used to handle float/double infinity correcty
	static final long LONG_NEGATIVE_INFINITY =
			NumericUtil.doubleToSortableLong(Double.NEGATIVE_INFINITY);
	static final long LONG_POSITIVE_INFINITY =
			NumericUtil.doubleToSortableLong(Double.POSITIVE_INFINITY);
	static final int INT_NEGATIVE_INFINITY =
			NumericUtil.floatToSortableInt(Float.NEGATIVE_INFINITY);
	static final int INT_POSITIVE_INFINITY =
			NumericUtil.floatToSortableInt(Float.POSITIVE_INFINITY);
	
	// members (package private, to be also fast accessible by NumericRangeTermEnum)
	private final boolean mMinInclusive, mMaxInclusive;
	private final int mPrecisionStep;
	private final NumericType mDataType;
	private final T mMin, mMax;
	
	private NumericRangeQuery(final String field, final int precisionStep, final NumericType dataType,
			T min, T max, final boolean minInclusive, final boolean maxInclusive) {
		super(field);
		if (precisionStep < 1)
			throw new IllegalArgumentException("precisionStep must be >=1");
		
		mPrecisionStep = precisionStep;
		mDataType = dataType;
		mMin = min;
		mMax = max;
		mMinInclusive = minInclusive;
		mMaxInclusive = maxInclusive;
	}
  
	/**
	 * Factory that creates a <code>NumericRangeQuery</code>, that queries a <code>long</code>
	 * range using the given <a href="#precisionStepDesc"><code>precisionStep</code></a>.
	 * You can have half-open ranges (which are in fact &lt;/&le; or &gt;/&ge; queries)
	 * by setting the min or max value to <code>null</code>. By setting inclusive to false, it will
	 * match all documents excluding the bounds, with inclusive on, the boundaries are hits, too.
	 */
	public static NumericRangeQuery<Long> newLongRange(final String field, final int precisionStep,
			Long min, Long max, final boolean minInclusive, final boolean maxInclusive) {
		return new NumericRangeQuery<Long>(field, precisionStep, NumericType.LONG, 
				min, max, minInclusive, maxInclusive);
	}
  
	/**
	 * Factory that creates a <code>NumericRangeQuery</code>, that queries a <code>long</code>
	 * range using the default <code>precisionStep</code> {@link NumericUtil#PRECISION_STEP_DEFAULT} (4).
	 * You can have half-open ranges (which are in fact &lt;/&le; or &gt;/&ge; queries)
	 * by setting the min or max value to <code>null</code>. By setting inclusive to false, it will
	 * match all documents excluding the bounds, with inclusive on, the boundaries are hits, too.
	 */
	public static NumericRangeQuery<Long> newLongRange(final String field,
			Long min, Long max, final boolean minInclusive, final boolean maxInclusive) {
		return new NumericRangeQuery<Long>(field, NumericUtil.PRECISION_STEP_DEFAULT, NumericType.LONG, 
				min, max, minInclusive, maxInclusive);
	}
  
	/**
	 * Factory that creates a <code>NumericRangeQuery</code>, that queries a <code>int</code>
	 * range using the given <a href="#precisionStepDesc"><code>precisionStep</code></a>.
	 * You can have half-open ranges (which are in fact &lt;/&le; or &gt;/&ge; queries)
	 * by setting the min or max value to <code>null</code>. By setting inclusive to false, it will
	 * match all documents excluding the bounds, with inclusive on, the boundaries are hits, too.
	 */
	public static NumericRangeQuery<Integer> newIntRange(final String field, final int precisionStep,
			Integer min, Integer max, final boolean minInclusive, final boolean maxInclusive) {
		return new NumericRangeQuery<Integer>(field, precisionStep, NumericType.INT, 
				min, max, minInclusive, maxInclusive);
	}
  
	/**
	 * Factory that creates a <code>NumericRangeQuery</code>, that queries a <code>int</code>
	 * range using the default <code>precisionStep</code> {@link NumericUtil#PRECISION_STEP_DEFAULT} (4).
	 * You can have half-open ranges (which are in fact &lt;/&le; or &gt;/&ge; queries)
	 * by setting the min or max value to <code>null</code>. By setting inclusive to false, it will
	 * match all documents excluding the bounds, with inclusive on, the boundaries are hits, too.
	 */
	public static NumericRangeQuery<Integer> newIntRange(final String field,
			Integer min, Integer max, final boolean minInclusive, final boolean maxInclusive) {
		return new NumericRangeQuery<Integer>(field, NumericUtil.PRECISION_STEP_DEFAULT, NumericType.INT, 
				min, max, minInclusive, maxInclusive);
	}
  
	/**
	 * Factory that creates a <code>NumericRangeQuery</code>, that queries a <code>double</code>
	 * range using the given <a href="#precisionStepDesc"><code>precisionStep</code></a>.
	 * You can have half-open ranges (which are in fact &lt;/&le; or &gt;/&ge; queries)
	 * by setting the min or max value to <code>null</code>.
	 * {@link Double#NaN} will never match a half-open range, to hit {@code NaN} use a query
	 * with {@code min == max == Double.NaN}.  By setting inclusive to false, it will
	 * match all documents excluding the bounds, with inclusive on, the boundaries are hits, too.
	 */
	public static NumericRangeQuery<Double> newDoubleRange(final String field, final int precisionStep,
			Double min, Double max, final boolean minInclusive, final boolean maxInclusive) {
		return new NumericRangeQuery<Double>(field, precisionStep, NumericType.DOUBLE, 
				min, max, minInclusive, maxInclusive);
	}
  
	/**
	 * Factory that creates a <code>NumericRangeQuery</code>, that queries a <code>double</code>
	 * range using the default <code>precisionStep</code> {@link NumericUtil#PRECISION_STEP_DEFAULT} (4).
	 * You can have half-open ranges (which are in fact &lt;/&le; or &gt;/&ge; queries)
	 * by setting the min or max value to <code>null</code>.
	 * {@link Double#NaN} will never match a half-open range, to hit {@code NaN} use a query
	 * with {@code min == max == Double.NaN}.  By setting inclusive to false, it will
	 * match all documents excluding the bounds, with inclusive on, the boundaries are hits, too.
	 */
	public static NumericRangeQuery<Double> newDoubleRange(final String field,
			Double min, Double max, final boolean minInclusive, final boolean maxInclusive) {
		return new NumericRangeQuery<Double>(field, NumericUtil.PRECISION_STEP_DEFAULT, NumericType.DOUBLE, 
				min, max, minInclusive, maxInclusive);
	}
  
	/**
	 * Factory that creates a <code>NumericRangeQuery</code>, that queries a <code>float</code>
	 * range using the given <a href="#precisionStepDesc"><code>precisionStep</code></a>.
	 * You can have half-open ranges (which are in fact &lt;/&le; or &gt;/&ge; queries)
	 * by setting the min or max value to <code>null</code>.
	 * {@link Float#NaN} will never match a half-open range, to hit {@code NaN} use a query
	 * with {@code min == max == Float.NaN}.  By setting inclusive to false, it will
	 * match all documents excluding the bounds, with inclusive on, the boundaries are hits, too.
	 */
	public static NumericRangeQuery<Float> newFloatRange(final String field, final int precisionStep,
			Float min, Float max, final boolean minInclusive, final boolean maxInclusive) {
		return new NumericRangeQuery<Float>(field, precisionStep, NumericType.FLOAT, 
				min, max, minInclusive, maxInclusive);
	}
  
	/**
	 * Factory that creates a <code>NumericRangeQuery</code>, that queries a <code>float</code>
	 * range using the default <code>precisionStep</code> {@link NumericUtil#PRECISION_STEP_DEFAULT} (4).
	 * You can have half-open ranges (which are in fact &lt;/&le; or &gt;/&ge; queries)
	 * by setting the min or max value to <code>null</code>.
	 * {@link Float#NaN} will never match a half-open range, to hit {@code NaN} use a query
	 * with {@code min == max == Float.NaN}.  By setting inclusive to false, it will
	 * match all documents excluding the bounds, with inclusive on, the boundaries are hits, too.
	 */
	public static NumericRangeQuery<Float> newFloatRange(final String field,
			Float min, Float max, final boolean minInclusive, final boolean maxInclusive) {
		return new NumericRangeQuery<Float>(field, NumericUtil.PRECISION_STEP_DEFAULT, NumericType.FLOAT, 
				min, max, minInclusive, maxInclusive);
	}

	@Override @SuppressWarnings("unchecked")
	protected TermsEnum getTermsEnum(final ITerms terms) throws IOException {
		// very strange: java.lang.Number itself is not Comparable, but all subclasses used here are
		if (mMin != null && mMax != null && ((Comparable<T>) mMin).compareTo(mMax) > 0) 
			return TermsEnum.EMPTY;
		
		return new NumericRangeTermsEnum<T>(this, (TermsEnum)terms.iterator(null));
	}

	public final NumericType getDataType() { return mDataType; }
	
	/** Returns <code>true</code> if the lower endpoint is inclusive */
	public boolean includesMin() { return mMinInclusive; }
  
	/** Returns <code>true</code> if the upper endpoint is inclusive */
	public boolean includesMax() { return mMaxInclusive; }

	/** Returns the lower value of this range query */
	public T getMin() { return mMin; }

	/** Returns the upper value of this range query */
	public T getMax() { return mMax; }
  
	/** Returns the precision step. */
	public int getPrecisionStep() { return mPrecisionStep; }
  
	@Override
	public String toString(final String field) {
		final StringBuilder sb = new StringBuilder();
		if (!getFieldName().equals(field)) 
			sb.append(getFieldName()).append(':');
		
		return sb.append(mMinInclusive ? '[' : '{')
				.append((mMin == null) ? "*" : mMin.toString())
				.append(" TO ")
				.append((mMax == null) ? "*" : mMax.toString())
				.append(mMaxInclusive ? ']' : '}')
				.append(StringHelper.toBoostString(getBoost()))
				.toString();
	}

	@Override
	@SuppressWarnings({"rawtypes"})
	public final boolean equals(final Object o) {
		if (o == this) return true;
		if (!super.equals(o))
			return false;
		
		if (o instanceof NumericRangeQuery) {
			final NumericRangeQuery q=(NumericRangeQuery)o;
			return ((q.mMin == null ? mMin == null : q.mMin.equals(mMin)) &&
					(q.mMax == null ? mMax == null : q.mMax.equals(mMax)) &&
					mMinInclusive == q.mMinInclusive &&
					mMaxInclusive == q.mMaxInclusive &&
					mPrecisionStep == q.mPrecisionStep
					);
		}
		
		return false;
	}

	@Override
	public final int hashCode() {
		int hash = super.hashCode();
		hash += mPrecisionStep^0x64365465;
		
		if (mMin != null) 
			hash += mMin.hashCode()^0x14fa55fb;
		if (mMax != null) 
			hash += mMax.hashCode()^0x733fa5fe;
		
		return hash + (Boolean.valueOf(mMinInclusive).hashCode()^0x14fa55fb)+
				(Boolean.valueOf(mMaxInclusive).hashCode()^0x733fa5fe);
	}
  
}
