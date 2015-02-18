package org.javenstudio.hornet.search.query;

import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.search.Query;

/**
 * An abstract {@link Query} that matches documents
 * containing a subset of terms provided by a {@link
 * FilteredTermsEnum} enumeration.
 *
 * <p>This query cannot be used directly; you must subclass
 * it and define {@link #getTermsEnum(Terms,AttributeSource)} to provide a {@link
 * FilteredTermsEnum} that iterates through the terms to be
 * matched.
 *
 * <p><b>NOTE</b>: if {@link #setRewriteMethod} is either
 * {@link #CONSTANT_SCORE_BOOLEAN_QUERY_REWRITE} or {@link
 * #SCORING_BOOLEAN_QUERY_REWRITE}, you may encounter a
 * {@link BooleanQuery.TooManyClauses} exception during
 * searching, which happens when the number of terms to be
 * searched exceeds {@link
 * BooleanQuery#getMaxClauseCount()}.  Setting {@link
 * #setRewriteMethod} to {@link #CONSTANT_SCORE_FILTER_REWRITE}
 * prevents this.
 *
 * <p>The recommended rewrite method is {@link
 * #CONSTANT_SCORE_AUTO_REWRITE_DEFAULT}: it doesn't spend CPU
 * computing unhelpful scores, and it tries to pick the most
 * performant rewrite method given the query. If you
 * need scoring (like {@link FuzzyQuery}, use
 * {@link ScoringBooleanQueryRewrite} which uses
 * a priority queue to only collect competitive terms
 * and not hit this limitation.
 *
 * Note that org.apache.lucene.queryparser.classic.QueryParser produces
 * MultiTermQueries using {@link
 * #CONSTANT_SCORE_AUTO_REWRITE_DEFAULT} by default.
 */
public abstract class MultiTermQuery extends Query {
	
	private RewriteMethod mRewriteMethod = CONSTANT_SCORE_AUTO_REWRITE_DEFAULT;
	private final String mField;

	/** 
	 * A rewrite method that first creates a private Filter,
	 *  by visiting each term in sequence and marking all docs
	 *  for that term.  Matching documents are assigned a
	 *  constant score equal to the query's boost.
	 * 
	 *  <p> This method is faster than the BooleanQuery
	 *  rewrite methods when the number of matched terms or
	 *  matched documents is non-trivial. Also, it will never
	 *  hit an errant {@link BooleanQuery.TooManyClauses}
	 *  exception.
	 *
	 *  @see #setRewriteMethod 
	 */
	public static final RewriteMethod CONSTANT_SCORE_FILTER_REWRITE = new RewriteMethod() {
		@Override
		public Query rewrite(IIndexReader reader, MultiTermQuery query) {
			Query result = new ConstantScoreQuery(new MultiTermQueryWrapperFilter<MultiTermQuery>(query));
			result.setBoost(query.getBoost());
			return result;
		}
	};

	/** 
	 * A rewrite method that first translates each term into
	 *  {@link BooleanClause.Occur#SHOULD} clause in a
	 *  BooleanQuery, and keeps the scores as computed by the
	 *  query.  Note that typically such scores are
	 *  meaningless to the user, and require non-trivial CPU
	 *  to compute, so it's almost always better to use {@link
	 *  #CONSTANT_SCORE_AUTO_REWRITE_DEFAULT} instead.
	 *
	 *  <p><b>NOTE</b>: This rewrite method will hit {@link
	 *  BooleanQuery.TooManyClauses} if the number of terms
	 *  exceeds {@link BooleanQuery#getMaxClauseCount}.
	 *
	 *  @see #setRewriteMethod 
	 */
	public final static RewriteMethod SCORING_BOOLEAN_QUERY_REWRITE = 
			ScoringRewrite.SCORING_BOOLEAN_QUERY_REWRITE;
  
	/** 
	 * Like {@link #SCORING_BOOLEAN_QUERY_REWRITE} except
	 *  scores are not computed.  Instead, each matching
	 *  document receives a constant score equal to the
	 *  query's boost.
	 * 
	 *  <p><b>NOTE</b>: This rewrite method will hit {@link
	 *  BooleanQuery.TooManyClauses} if the number of terms
	 *  exceeds {@link BooleanQuery#getMaxClauseCount}.
	 *
	 *  @see #setRewriteMethod 
	 */
	public final static RewriteMethod CONSTANT_SCORE_BOOLEAN_QUERY_REWRITE = 
			ScoringRewrite.CONSTANT_SCORE_BOOLEAN_QUERY_REWRITE;

	/** 
	 * Read-only default instance of {@link
	 *  ConstantScoreAutoRewrite}, with {@link
	 *  ConstantScoreAutoRewrite#setTermCountCutoff} set to
	 *  {@link
	 *  ConstantScoreAutoRewrite#DEFAULT_TERM_COUNT_CUTOFF}
	 *  and {@link
	 *  ConstantScoreAutoRewrite#setDocCountPercent} set to
	 *  {@link
	 *  ConstantScoreAutoRewrite#DEFAULT_DOC_COUNT_PERCENT}.
	 *  Note that you cannot alter the configuration of this
	 *  instance; you'll need to create a private instance
	 *  instead. 
	 */
	public final static RewriteMethod CONSTANT_SCORE_AUTO_REWRITE_DEFAULT = 
		new ConstantScoreAutoRewrite() {
			@Override
			public void setTermCountCutoff(int count) {
				throw new UnsupportedOperationException("Please create a private instance");
			}
	
			@Override
			public void setDocCountPercent(double percent) {
				throw new UnsupportedOperationException("Please create a private instance");
			}
		};
	
	/**
	 * Constructs a query matching terms that cannot be represented with a single
	 * Term.
	 */
	public MultiTermQuery(final String field) {
		mField = field;
		assert field != null;
	}

	/** Returns the field name for this query */
	public final String getFieldName() { return mField; }

	/** 
	 * Construct the enumeration to be used, expanding the
	 *  pattern term.  This method should only be called if
	 *  the field exists (ie, implementations can assume the
	 *  field does exist).  This method should not return null
	 *  (should instead return {@link TermsEnum#EMPTY} if no
	 *  terms match).  The TermsEnum must already be
	 *  positioned to the first matching term.
	 * The given {@link AttributeSource} is passed by the {@link RewriteMethod} to
	 * provide attributes, the rewrite method uses to inform about e.g. maximum competitive boosts.
	 * This is currently only used by {@link TopTermsRewrite}
	 */
	protected abstract ITermsEnum getTermsEnum(ITerms terms) throws IOException;

	/**
	 * To rewrite to a simpler form, instead return a simpler
	 * enum from {@link #getTermsEnum(Terms, AttributeSource)}.  For example,
	 * to rewrite to a single term, return a {@link SingleTermsEnum}
	 */
	@Override
	public final IQuery rewrite(IIndexReader reader) throws IOException {
		return mRewriteMethod.rewrite(reader, this);
	}

	/** @see #setRewriteMethod */
	public RewriteMethod getRewriteMethod() {
		return mRewriteMethod;
	}

	/**
	 * Sets the rewrite method to be used when executing the
	 * query.  You can use one of the four core methods, or
	 * implement your own subclass of {@link RewriteMethod}. 
	 */
	public void setRewriteMethod(RewriteMethod method) {
		mRewriteMethod = method;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(getBoost());
		result = prime * result + mRewriteMethod.hashCode();
		if (mField != null) 
			result = prime * result + mField.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		
		MultiTermQuery other = (MultiTermQuery) obj;
		if (Float.floatToIntBits(getBoost()) != Float.floatToIntBits(other.getBoost()))
			return false;
		
		if (!mRewriteMethod.equals(other.mRewriteMethod)) 
			return false;
		
		return (other.mField == null ? mField == null : other.mField.equals(mField));
	}
 
}
