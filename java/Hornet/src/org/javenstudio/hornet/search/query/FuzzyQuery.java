package org.javenstudio.hornet.search.query;

import java.io.IOException;

import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.index.term.Term;
import org.javenstudio.common.indexdb.index.term.Terms;
import org.javenstudio.common.indexdb.index.term.TermsEnum;
import org.javenstudio.common.indexdb.util.StringHelper;

/** 
 * Implements the fuzzy search query. The similarity measurement
 * is based on the Damerau-Levenshtein (optimal string alignment) algorithm,
 * though you can explicitly choose classic Levenshtein by passing <code>false</code>
 * to the <code>transpositions</code> parameter.
 * 
 * <p>This query uses {@link MultiTermQuery.TopTermsScoringBooleanQueryRewrite}
 * as default. So terms will be collected and scored according to their
 * edit distance. Only the top terms are used for building the {@link BooleanQuery}.
 * It is not recommended to change the rewrite mode for fuzzy queries.
 * 
 * <p>At most, this query will match terms up to 
 * {@value org.apache.lucene.util.automaton.LevenshteinAutomata#MAXIMUM_SUPPORTED_DISTANCE} edits. 
 * Higher distances (especially with transpositions enabled), are generally not useful and 
 * will match a significant amount of the term dictionary. If you really want this, consider
 * using an n-gram indexing technique (such as the SpellChecker in the 
 * <a href="{@docRoot}/../suggest/overview-summary.html">suggest module</a>) instead.
 */
public class FuzzyQuery extends MultiTermQuery {
  
	static final int MAXIMUM_SUPPORTED_DISTANCE = 2; //LevenshteinAutomata.MAXIMUM_SUPPORTED_DISTANCE;
	
	public final static int sDefaultMaxEdits = MAXIMUM_SUPPORTED_DISTANCE;
	public final static int sDefaultPrefixLength = 0;
	public final static int sDefaultMaxExpansions = 50;
	public final static boolean sDefaultTranspositions = true;
  
	private final int mMaxEdits;
	private final int mMaxExpansions;
	private final int mPrefixLength;
	private final boolean mTranspositions;
	private final ITerm mTerm;
  
	/**
	 * Create a new FuzzyQuery that will match terms with an edit distance 
	 * of at most <code>maxEdits</code> to <code>term</code>.
	 * If a <code>prefixLength</code> &gt; 0 is specified, a common prefix
	 * of that length is also required.
	 * 
	 * @param term the term to search for
	 * @param maxEdits must be >= 0 and <= {@link LevenshteinAutomata#MAXIMUM_SUPPORTED_DISTANCE}.
	 * @param prefixLength length of common (non-fuzzy) prefix
	 * @param maxExpansions the maximum number of terms to match. If this number is
	 *  greater than {@link BooleanQuery#getMaxClauseCount} when the query is rewritten, 
	 *  then the maxClauseCount will be used instead.
	 * @param transpositions true if transpositions should be treated as a primitive
	 *        edit operation. If this is false, comparisons will implement the classic
	 *        Levenshtein algorithm.
	 */
	public FuzzyQuery(ITerm term, int maxEdits, int prefixLength, 
			int maxExpansions, boolean transpositions) {
		super(term.getField());
    
		if (maxEdits < 0 || maxEdits > MAXIMUM_SUPPORTED_DISTANCE) 
			throw new IllegalArgumentException("maxEdits must be between 0 and " + MAXIMUM_SUPPORTED_DISTANCE);
		
		if (prefixLength < 0) 
			throw new IllegalArgumentException("prefixLength cannot be negative.");
    
		if (maxExpansions < 0) 
			throw new IllegalArgumentException("maxExpansions cannot be negative.");
		
		mTerm = term;
		mMaxEdits = maxEdits;
		mPrefixLength = prefixLength;
		mTranspositions = transpositions;
		mMaxExpansions = maxExpansions;
		
		setRewriteMethod(new TopTermsScoringBooleanQueryRewrite(maxExpansions));
	}
  
	/**
	 * Calls {@link #FuzzyQuery(Term, int, int, int, boolean) 
	 * FuzzyQuery(term, minimumSimilarity, prefixLength, defaultMaxExpansions, defaultTranspositions)}.
	 */
	public FuzzyQuery(ITerm term, int maxEdits, int prefixLength) {
		this(term, maxEdits, prefixLength, sDefaultMaxExpansions, sDefaultTranspositions);
	}
  
	/**
	 * Calls {@link #FuzzyQuery(Term, int, int) FuzzyQuery(term, maxEdits, defaultPrefixLength)}.
	 */
	public FuzzyQuery(ITerm term, int maxEdits) {
		this(term, maxEdits, sDefaultPrefixLength);
	}

	/**
	 * Calls {@link #FuzzyQuery(Term, int) FuzzyQuery(term, defaultMaxEdits)}.
	 */
	public FuzzyQuery(ITerm term) {
		this(term, sDefaultMaxEdits);
	}
  
	/**
	 * @return the maximum number of edit distances allowed for this query to match.
	 */
	public int getMaxEdits() {
		return mMaxEdits;
	}
    
	/**
	 * Returns the non-fuzzy prefix length. This is the number of characters at the start
	 * of a term that must be identical (not fuzzy) to the query term if the query
	 * is to match that term. 
	 */
	public int getPrefixLength() {
		return mPrefixLength;
	}

	@Override
	protected ITermsEnum getTermsEnum(ITerms terms) throws IOException {
		if (mMaxEdits == 0 || mPrefixLength >= mTerm.getText().length()) {
			// can only match if it's exact
			return new SingleTermsEnum((TermsEnum)terms.iterator(null), 
					mTerm.getBytes());
		}
    
		return new FuzzyTermsEnum((Terms)terms, (Term)getTerm(), 
				mMaxEdits, mPrefixLength, mTranspositions);
	}
  
	/**
	 * Returns the pattern term.
	 */
	public ITerm getTerm() {
		return mTerm;
	}
    
	@Override
	public String toString(String field) {
		final StringBuilder buffer = new StringBuilder();
		if (!mTerm.getField().equals(field)) {
			buffer.append(mTerm.getField());
			buffer.append(":");
		}
		buffer.append(mTerm.getText());
		buffer.append('~');
		buffer.append(Integer.toString(mMaxEdits));
		buffer.append(StringHelper.toBoostString(getBoost()));
		return buffer.toString();
	}
  
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + mMaxEdits;
		result = prime * result + mPrefixLength;
		result = prime * result + mMaxExpansions;
		result = prime * result + (mTranspositions ? 0 : 1);
		result = prime * result + ((mTerm == null) ? 0 : mTerm.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		
		FuzzyQuery other = (FuzzyQuery) obj;
		
		if (mMaxEdits != other.mMaxEdits)
			return false;
		if (mPrefixLength != other.mPrefixLength)
			return false;
		if (mMaxExpansions != other.mMaxExpansions)
			return false;
		if (mTranspositions != other.mTranspositions)
			return false;
		
		if (mTerm == null) {
			if (other.mTerm != null)
				return false;
		} else if (!mTerm.equals(other.mTerm)) {
			return false;
		}
		
		return true;
	}
  
	/**
	 * @deprecated pass integer edit distances instead.
	 */
	@Deprecated
	public final static float sDefaultMinSimilarity = MAXIMUM_SUPPORTED_DISTANCE;

	/**
	 * Helper function to convert from deprecated "minimumSimilarity" fractions
	 * to raw edit distances.
	 * 
	 * @param minimumSimilarity scaled similarity
	 * @param termLen length (in unicode codepoints) of the term.
	 * @return equivalent number of maxEdits
	 * @deprecated pass integer edit distances instead.
	 */
	@Deprecated
	public static int floatToEdits(float minimumSimilarity, int termLen) {
		if (minimumSimilarity >= 1f) {
			return (int) Math.min(minimumSimilarity, MAXIMUM_SUPPORTED_DISTANCE);
		} else if (minimumSimilarity == 0.0f) {
			return 0; // 0 means exact, not infinite # of edits!
		} else {
			return Math.min((int) ((1D-minimumSimilarity) * termLen), 
					MAXIMUM_SUPPORTED_DISTANCE);
		}
	}
	
}
