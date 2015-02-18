package org.javenstudio.hornet.search.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.javenstudio.common.indexdb.Constants;
import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IBooleanClause;
import org.javenstudio.common.indexdb.IBooleanQuery;
import org.javenstudio.common.indexdb.IBooleanWeight;
import org.javenstudio.common.indexdb.IDocsEnum;
import org.javenstudio.common.indexdb.IExactSimilarityScorer;
import org.javenstudio.common.indexdb.IExplanation;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.common.indexdb.ISimilarity;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.IWeight;
import org.javenstudio.common.indexdb.TooManyClauses;
import org.javenstudio.common.indexdb.search.ComplexExplanation;
import org.javenstudio.common.indexdb.search.DocsAndFreqs;
import org.javenstudio.common.indexdb.search.Explanation;
import org.javenstudio.common.indexdb.search.Query;
import org.javenstudio.common.indexdb.search.Weight;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.StringHelper;
import org.javenstudio.hornet.search.query.TermQuery.TermWeight;
import org.javenstudio.hornet.search.scorer.BooleanScorer;
import org.javenstudio.hornet.search.scorer.BooleanScorer2;
import org.javenstudio.hornet.search.scorer.ConjunctionTermScorer;
import org.javenstudio.hornet.search.scorer.MatchOnlyConjunctionTermScorer;

/** 
 * A Query that matches documents matching boolean combinations of other
  * queries, e.g. {@link TermQuery}s, {@link PhraseQuery}s or other
  * BooleanQuerys.
  */
public class BooleanQuery extends Query implements IBooleanQuery {

	private static int sMaxClauseCount = Constants.MAX_CLAUSE_COUNT;

	/** 
	 * Return the maximum number of clauses permitted, 1024 by default.
	 * Attempts to add more than the permitted number of clauses cause {@link
	 * TooManyClauses} to be thrown.
	 * @see #setMaxClauseCount(int)
	 */
	public static int getMaxClauseCount() { return sMaxClauseCount; }

	/** 
	 * Set the maximum number of clauses permitted per BooleanQuery.
	 * Default value is 1024.
	 */
	public static void setMaxClauseCount(int maxClauseCount) {
		if (maxClauseCount < 1)
			throw new IllegalArgumentException("maxClauseCount must be >= 1");
		BooleanQuery.sMaxClauseCount = maxClauseCount;
	}

	private ArrayList<IBooleanClause> mClauses = new ArrayList<IBooleanClause>();
	private final boolean mDisableCoord;
	private int mMinNrShouldMatch = 0;

	/** Constructs an empty boolean query. */
	public BooleanQuery() {
		mDisableCoord = false;
	}

	/** 
	 * Constructs an empty boolean query.
	 *
	 * {@link Similarity#coord(int,int)} may be disabled in scoring, as
	 * appropriate. For example, this score factor does not make sense for most
	 * automatically generated queries, like {@link WildcardQuery} and {@link
	 * FuzzyQuery}.
	 *
	 * @param disableCoord disables {@link Similarity#coord(int,int)} in scoring.
	 */
	public BooleanQuery(boolean disableCoord) {
		mDisableCoord = disableCoord;
	}

	/** 
	 * Returns true if {@link Similarity#coord(int,int)} is disabled in
	 * scoring for this query instance.
	 * @see #BooleanQuery(boolean)
	 */
	public boolean isCoordDisabled() { return mDisableCoord; }

	/**
	 * Specifies a minimum number of the optional BooleanClauses
	 * which must be satisfied.
	 *
	 * <p>
	 * By default no optional clauses are necessary for a match
	 * (unless there are no required clauses).  If this method is used,
	 * then the specified number of clauses is required.
	 * </p>
	 * <p>
	 * Use of this method is totally independent of specifying that
	 * any specific clauses are required (or prohibited).  This number will
	 * only be compared against the number of matching optional clauses.
	 * </p>
	 *
	 * @param min the number of optional clauses that must match
	 */
	public void setMinimumNumberShouldMatch(int min) {
		mMinNrShouldMatch = min;
	}
  
	/**
	 * Gets the minimum number of the optional BooleanClauses
	 * which must be satisfied.
	 */
	public int getMinimumNumberShouldMatch() {
		return mMinNrShouldMatch;
	}

	/** 
	 * Adds a clause to a boolean query.
	 *
	 * @throws TooManyClauses if the new number of clauses exceeds the maximum clause number
	 * @see #getMaxClauseCount()
	 */
	@Override
	public void add(IQuery query, IBooleanClause.Occur occur) {
		add(new BooleanClause(query, occur));
	}

	/** 
	 * Adds a clause to a boolean query.
	 * @throws TooManyClauses if the new number of clauses exceeds the maximum clause number
	 * @see #getMaxClauseCount()
	 */
	@Override
	public void add(IBooleanClause clause) {
		if (mClauses.size() >= sMaxClauseCount)
			throw new TooManyClauses(sMaxClauseCount);

		mClauses.add(clause);
	}

	/** Returns the set of clauses in this query. */
	@Override
	public IBooleanClause[] getClauses() {
		return mClauses.toArray(new BooleanClause[mClauses.size()]);
	}

	/** Returns the list of clauses in this query. */
	public List<IBooleanClause> clauses() { return mClauses; }

	/** 
	 * Returns an iterator on the clauses in this query. It implements the {@link Iterable} interface to
	 * make it possible to do:
	 * <pre>for (BooleanClause clause : booleanQuery) {}</pre>
	 */
	@Override
	public final Iterator<IBooleanClause> iterator() { 
		return clauses().iterator(); 
	}

	/**
	 * Expert: the Weight for BooleanQuery, used to
	 * normalize, score and explain these queries.
	 *
	 * <p>NOTE: this API and implementation is subject to
	 * change suddenly in the next release.</p>
	 */
	public class BooleanWeight extends Weight implements IBooleanWeight {
		/** The Similarity implementation. */
		protected ISimilarity mSimilarity;
		protected ArrayList<IWeight> mWeights;
		protected int mMaxCoord;  // num optional + num required
		private final boolean mDisableCoord;
		private final boolean mTermConjunction;

		public BooleanWeight(ISearcher searcher, boolean disableCoord)
				throws IOException {
			mSimilarity = searcher.getSimilarity();
			mDisableCoord = disableCoord;
			mWeights = new ArrayList<IWeight>(mClauses.size());
			
			boolean termConjunction = mClauses.isEmpty() || mMinNrShouldMatch != 0 ? false : true;
			for (int i = 0 ; i < mClauses.size(); i++) {
				IBooleanClause c = mClauses.get(i);
				IWeight w = c.getQuery().createWeight(searcher);
				if (!(c.isRequired() && (w instanceof TermWeight))) 
					termConjunction = false;
				
				mWeights.add(w);
				if (!c.isProhibited()) 
					mMaxCoord++;
			}
			mTermConjunction = termConjunction;
		}

		@Override
		public Query getQuery() { return BooleanQuery.this; }

		@Override
		public float getValueForNormalization() throws IOException {
			float sum = 0.0f;
			for (int i = 0 ; i < mWeights.size(); i++) {
				// call sumOfSquaredWeights for all clauses in case of side effects
				float s = mWeights.get(i).getValueForNormalization(); 	// sum sub weights
				if (!mClauses.get(i).isProhibited()) {
					// only add to sum for non-prohibited clauses
					sum += s;
				}
			}

			sum *= getBoost() * getBoost();	// boost each sub-weight

			return sum ;
		}

		@Override
		public float coord(int overlap, int maxOverlap) {
			return mSimilarity.coord(overlap, maxOverlap);
		}

		@Override
		public void normalize(float norm, float topLevelBoost) {
			topLevelBoost *= getBoost(); 	// incorporate boost
			for (IWeight w : mWeights) {
				// normalize all clauses, (even if prohibited in case of side affects)
				w.normalize(norm, topLevelBoost);
			}
		}

		@Override
		public IExplanation explain(IAtomicReaderRef context, int doc)
				throws IOException {
			final int minShouldMatch = BooleanQuery.this.getMinimumNumberShouldMatch();
			ComplexExplanation sumExpl = new ComplexExplanation();
			sumExpl.setDescription("sum of:");
			
			int coord = 0;
			float sum = 0.0f;
			boolean fail = false;
			int shouldMatchCount = 0;
			
			Iterator<IBooleanClause> cIter = mClauses.iterator();
			for (Iterator<IWeight> wIter = mWeights.iterator(); wIter.hasNext();) {
				IWeight w = wIter.next();
				IBooleanClause c = cIter.next();
				if (w.getScorer(context, true, true, context.getReader().getLiveDocs()) == null) {
					if (c.isRequired()) {
						fail = true;
						Explanation r = new Explanation(0.0f, 
								"no match on required clause (" + c.getQuery().toString() + ")");
						sumExpl.addDetail(r);
					}
					continue;
				}
				
				IExplanation e = w.explain(context, doc);
				if (e.isMatch()) {
					if (!c.isProhibited()) {
						sumExpl.addDetail(e);
						sum += e.getValue();
						coord++;
					} else {
						Explanation r = new Explanation(0.0f, 
								"match on prohibited clause (" + c.getQuery().toString() + ")");
						r.addDetail(e);
						sumExpl.addDetail(r);
						fail = true;
					}
					if (c.getOccur() == IBooleanClause.Occur.SHOULD)
						shouldMatchCount++;
					
				} else if (c.isRequired()) {
					Explanation r = new Explanation(0.0f, 
							"no match on required clause (" + c.getQuery().toString() + ")");
					r.addDetail(e);
					sumExpl.addDetail(r);
					fail = true;
				}
			}
			if (fail) {
				sumExpl.setMatch(Boolean.FALSE);
				sumExpl.setValue(0.0f);
				sumExpl.setDescription("Failure to meet condition(s) of required/prohibited clause(s)");
				return sumExpl;
				
			} else if (shouldMatchCount < minShouldMatch) {
				sumExpl.setMatch(Boolean.FALSE);
				sumExpl.setValue(0.0f);
				sumExpl.setDescription("Failure to match minimum number " +
						"of optional clauses: " + minShouldMatch);
				return sumExpl;
			}
      
			sumExpl.setMatch(0 < coord ? Boolean.TRUE : Boolean.FALSE);
			sumExpl.setValue(sum);
      
			final float coordFactor = mDisableCoord ? 1.0f : coord(coord, mMaxCoord);
			if (coordFactor == 1.0f) {
				return sumExpl;  	// eliminate wrapper
				
			} else {
				ComplexExplanation result = new ComplexExplanation(
						sumExpl.isMatch(), sum*coordFactor, "product of:");
				result.addDetail(sumExpl);
				result.addDetail(new Explanation(coordFactor, "coord("+coord+"/"+mMaxCoord+")"));
				return result;
			}
		}

		@Override
		public IScorer getScorer(IAtomicReaderRef context, boolean scoreDocsInOrder,
				boolean topScorer, Bits acceptDocs) throws IOException {
			if (mTermConjunction) {
				// specialized scorer for term conjunctions
				return createConjunctionTermScorer(context, acceptDocs);
			}
			
			List<IScorer> required = new ArrayList<IScorer>();
			List<IScorer> prohibited = new ArrayList<IScorer>();
			List<IScorer> optional = new ArrayList<IScorer>();
			
			Iterator<IBooleanClause> cIter = mClauses.iterator();
			for (IWeight w  : mWeights) {
				IBooleanClause c =  cIter.next();
				IScorer subScorer = w.getScorer(context, true, false, acceptDocs);
				if (subScorer == null) {
					if (c.isRequired()) 
						return null;
				} else if (c.isRequired()) {
					required.add(subScorer);
				} else if (c.isProhibited()) {
					prohibited.add(subScorer);
				} else {
					optional.add(subScorer);
				}
			}
      
			// Check if we can return a BooleanScorer
			if (!scoreDocsInOrder && topScorer && required.size() == 0) {
				return new BooleanScorer(this, mDisableCoord, mMinNrShouldMatch, 
						optional, prohibited, mMaxCoord);
			}
      
			if (required.size() == 0 && optional.size() == 0) {
				// no required and optional clauses.
				return null;
			} else if (optional.size() < mMinNrShouldMatch) {
				// either >1 req scorer, or there are 0 req scorers and at least 1
				// optional scorer. Therefore if there are not enough optional scorers
				// no documents will be matched by the query
				return null;
			}
      
			// Return a BooleanScorer2
			return new BooleanScorer2(this, mDisableCoord, mMinNrShouldMatch, 
					required, prohibited, optional, mMaxCoord);
		}

		private IScorer createConjunctionTermScorer(IAtomicReaderRef context, Bits acceptDocs)
				throws IOException {
			// TODO: fix scorer API to specify "needsScores" up
			// front, so we can do match-only if caller doesn't
			// needs scores
			final DocsAndFreqs[] docsAndFreqs = new DocsAndFreqs[mWeights.size()];
			
			for (int i = 0; i < docsAndFreqs.length; i++) {
				final TermWeight weight = (TermWeight) mWeights.get(i);
				final ITermsEnum termsEnum = weight.getTermsEnum(context);
				if (termsEnum == null) 
					return null;
				
				final IExactSimilarityScorer docScorer = weight.createDocScorer(context);
				final IDocsEnum docsAndFreqsEnum = termsEnum.getDocs(acceptDocs, null);
				if (docsAndFreqsEnum == null) {
					// TODO: we could carry over TermState from the
					// terms we already seek'd to, to save re-seeking
					// to make the match-only scorer, but it's likely
					// rare that BQ mixes terms from omitTf and
					// non-omitTF fields:

					// At least one sub cannot provide freqs; abort
					// and fallback to full match-only scorer:
					return createMatchOnlyConjunctionTermScorer(context, acceptDocs);
				}

				docsAndFreqs[i] = new DocsAndFreqs(
						docsAndFreqsEnum, docsAndFreqsEnum, termsEnum.getDocFreq(), docScorer);
			}
			
			return new ConjunctionTermScorer(this, mDisableCoord ? 1.0f : coord(
					docsAndFreqs.length, docsAndFreqs.length), docsAndFreqs);
		}

		private IScorer createMatchOnlyConjunctionTermScorer(IAtomicReaderRef context, Bits acceptDocs)
				throws IOException {
			final DocsAndFreqs[] docsAndFreqs = new DocsAndFreqs[mWeights.size()];
			
			for (int i = 0; i < docsAndFreqs.length; i++) {
				final TermWeight weight = (TermWeight) mWeights.get(i);
				final ITermsEnum termsEnum = weight.getTermsEnum(context);
				if (termsEnum == null) 
					return null;
				
				final IExactSimilarityScorer docScorer = weight.createDocScorer(context);
				docsAndFreqs[i] = new DocsAndFreqs(null,
						termsEnum.getDocs(acceptDocs, null, 0),
						termsEnum.getDocFreq(), docScorer);
			}

			return new MatchOnlyConjunctionTermScorer(this, mDisableCoord ? 1.0f : coord(
					docsAndFreqs.length, docsAndFreqs.length), docsAndFreqs);
		}
    
		@Override
		public boolean scoresDocsOutOfOrder() {
			for (IBooleanClause c : mClauses) {
				if (c.isRequired()) 
					return false; // BS2 (in-order) will be used by scorer()
			}
      
			// scorer() will return an out-of-order scorer if requested.
			return true;
		}
    
	}

	@Override
	public IWeight createWeight(ISearcher searcher) throws IOException {
		return new BooleanWeight(searcher, mDisableCoord);
	}

	@Override
	public IQuery rewrite(IIndexReader reader) throws IOException {
		if (mMinNrShouldMatch == 0 && mClauses.size() == 1) {	// optimize 1-clause queries
			IBooleanClause c = mClauses.get(0);
			if (!c.isProhibited()) {				// just return clause
				Query query = (Query)c.getQuery().rewrite(reader);	// rewrite first

				if (getBoost() != 1.0f) { 			// incorporate boost
					if (query == c.getQuery()) 		// if rewrite was no-op
						query = query.clone();   	// then clone before boost
					
					// Since the BooleanQuery only has 1 clause, the BooleanQuery will be
					// written out. Therefore the rewritten Query's boost must incorporate both
					// the clause's boost, and the boost of the BooleanQuery itself
					query.setBoost(getBoost() * query.getBoost());
				}

				return query;
			}
		}

		BooleanQuery clone = null;					// recursively rewrite
		for (int i = 0 ; i < mClauses.size(); i++) {
			IBooleanClause c = mClauses.get(i);
			Query query = (Query)c.getQuery().rewrite(reader);
			if (query != c.getQuery()) {      		// clause rewrote: must clone
				if (clone == null) {
					// The BooleanQuery clone is lazily initialized so only initialize
					// it if a rewritten clause differs from the original clause (and hasn't been
					// initialized already).  If nothing differs, the clone isn't needlessly created
					clone = this.clone();
				}
				clone.mClauses.set(i, new BooleanClause(query, c.getOccur()));
			}
		}
		
		if (clone != null) 
			return clone;		// some clauses rewrote
		
		return this;   			// no clauses rewrote
	}

	// inherit javadoc
	@Override
	public void extractTerms(Set<ITerm> terms) {
		for (IBooleanClause clause : mClauses) {
			clause.getQuery().extractTerms(terms);
		}
	}

	@Override @SuppressWarnings("unchecked")
	public BooleanQuery clone() {
		BooleanQuery clone = (BooleanQuery)super.clone();
		clone.mClauses = (ArrayList<IBooleanClause>) this.mClauses.clone();
		return clone;
	}

	/** Prints a user-readable version of this query. */
	@Override
	public String toString(String field) {
		StringBuilder buffer = new StringBuilder();
		boolean needParens=(getBoost() != 1.0) || (getMinimumNumberShouldMatch()>0) ;
		if (needParens) 
			buffer.append("(");

		for (int i = 0 ; i < mClauses.size(); i++) {
			IBooleanClause c = mClauses.get(i);
			if (c.isProhibited())
				buffer.append("-");
			else if (c.isRequired())
				buffer.append("+");

			IQuery subQuery = c.getQuery();
			if (subQuery != null) {
				if (subQuery instanceof BooleanQuery) {	// wrap sub-bools in parens
					buffer.append("(");
					buffer.append(subQuery.toString(field));
					buffer.append(")");
				} else 
					buffer.append(subQuery.toString(field));
			} else 
				buffer.append("null");

			if (i != mClauses.size()-1)
				buffer.append(" ");
		}

		if (needParens) 
			buffer.append(")");

		if (getMinimumNumberShouldMatch()>0) {
			buffer.append('~');
			buffer.append(getMinimumNumberShouldMatch());
		}

		if (getBoost() != 1.0f) 
			buffer.append(StringHelper.toBoostString(getBoost()));

		return buffer.toString();
	}

	/** Returns true if <code>o</code> is equal to this. */
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof BooleanQuery))
			return false;
		BooleanQuery other = (BooleanQuery)o;
		return (this.getBoost() == other.getBoost())
				&& this.mClauses.equals(other.mClauses)
				&& this.getMinimumNumberShouldMatch() == other.getMinimumNumberShouldMatch()
				&& this.mDisableCoord == other.mDisableCoord;
	}

	/** Returns a hash code value for this object.*/
	@Override
	public int hashCode() {
		return Float.floatToIntBits(getBoost()) ^ mClauses.hashCode()
				+ getMinimumNumberShouldMatch() + (mDisableCoord ? 17:0);
	}
  
}
