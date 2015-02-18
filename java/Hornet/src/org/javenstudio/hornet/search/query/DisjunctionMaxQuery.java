package org.javenstudio.hornet.search.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IExplanation;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.IWeight;
import org.javenstudio.common.indexdb.search.ComplexExplanation;
import org.javenstudio.common.indexdb.search.Query;
import org.javenstudio.common.indexdb.search.Weight;
import org.javenstudio.common.indexdb.util.Bits;

/**
 * A query that generates the union of documents produced by its subqueries, 
 * and that scores each document with the maximum
 * score for that document as produced by any subquery, plus a tie breaking 
 * increment for any additional matching subqueries.
 * This is useful when searching for a word in multiple fields with different 
 * boost factors (so that the fields cannot be
 * combined equivalently into a single search field).  We want the primary score 
 * to be the one associated with the highest boost,
 * not the sum of the field scores (as BooleanQuery would give).
 * If the query is "albino elephant" this ensures that "albino" matching 
 * one field and "elephant" matching
 * another gets a higher score than "albino" matching both fields.
 * To get this result, use both BooleanQuery and DisjunctionMaxQuery:  
 * for each term a DisjunctionMaxQuery searches for it in
 * each field, while the set of these DisjunctionMaxQuery's is combined into a BooleanQuery.
 * The tie breaker capability allows results that include the same term in 
 * multiple fields to be judged better than results that
 * include this term in only the best of those multiple fields, without confusing 
 * this with the better case of two different terms
 * in the multiple fields.
 */
public class DisjunctionMaxQuery extends Query implements Iterable<IQuery> {

	/** The subqueries */
	private List<IQuery> mDisjuncts = new ArrayList<IQuery>();

	/** 
	 * Multiple of the non-max disjunct scores added into our final score. 
	 * Non-zero values support tie-breaking. 
	 */
	private float mTieBreakerMultiplier = 0.0f;

	/** 
	 * Creates a new empty DisjunctionMaxQuery.  Use add() to add the subqueries.
	 * @param tieBreakerMultiplier the score of each non-maximum disjunct 
	 * for a document is multiplied by this weight
	 * and added into the final score.  If non-zero, the value should be small, 
	 * on the order of 0.1, which says that
	 * 10 occurrences of word in a lower-scored field that is also in a higher scored field 
	 * is just as good as a unique
	 * word in the lower scored field (i.e., one that is not in any higher scored field.
	 */
	public DisjunctionMaxQuery(float tieBreakerMultiplier) {
		mTieBreakerMultiplier = tieBreakerMultiplier;
	}

	/**
	 * Creates a new DisjunctionMaxQuery
	 * @param disjuncts a {@code Collection<Query>} of all the disjuncts to add
	 * @param tieBreakerMultiplier   the weight to give to each matching non-maximum disjunct
	 */
	public DisjunctionMaxQuery(Collection<IQuery> disjuncts, float tieBreakerMultiplier) {
		mTieBreakerMultiplier = tieBreakerMultiplier;
		add(disjuncts);
	}

	/** 
	 * Add a subquery to this disjunction
	 * @param query the disjunct added
	 */
	public void add(IQuery query) {
		mDisjuncts.add(query);
	}

	/** 
	 * Add a collection of disjuncts to this disjunction
	 * via {@code Iterable<Query>}
	 * @param disjuncts a collection of queries to add as disjuncts.
	 */
	public void add(Collection<IQuery> disjuncts) {
		mDisjuncts.addAll(disjuncts);
	}

	/** @return An {@code Iterator<Query>} over the disjuncts */
	public Iterator<IQuery> iterator() {
		return mDisjuncts.iterator();
	}
  
	/**
	 * @return the disjuncts.
	 */
	public List<IQuery> getDisjuncts() {
		return mDisjuncts;
	}

	/**
	 * @return tie breaker value for multiple matches.
	 */
	public float getTieBreakerMultiplier() {
		return mTieBreakerMultiplier;
	}

	/**
	 * Expert: the Weight for DisjunctionMaxQuery, used to
	 * normalize, score and explain these queries.
	 *
	 * <p>NOTE: this API and implementation is subject to
	 * change suddenly in the next release.</p>
	 */
	protected class DisjunctionMaxWeight extends Weight {

		/** The Weights for our subqueries, in 1-1 correspondence with disjuncts */
		protected ArrayList<IWeight> mWeights = new ArrayList<IWeight>(); 

		/** 
		 * Construct the Weight for this Query searched by searcher. 
		 * Recursively construct subquery weights. 
		 */
		public DisjunctionMaxWeight(ISearcher searcher) throws IOException {
			for (IQuery disjunctQuery : mDisjuncts) {
				mWeights.add(disjunctQuery.createWeight(searcher));
			}
		}

		/** Return our associated DisjunctionMaxQuery */
		@Override
		public Query getQuery() { 
			return DisjunctionMaxQuery.this; 
		}

		/** 
		 * Compute the sub of squared weights of us applied to our subqueries. 
		 * Used for normalization. 
		 */
		@Override
		public float getValueForNormalization() throws IOException {
			float max = 0.0f, sum = 0.0f;
			
			for (IWeight currentWeight : mWeights) {
				float sub = currentWeight.getValueForNormalization();
				
				sum += sub;
				max = Math.max(max, sub);
			}
			
			float boost = getBoost();
			
			return (((sum - max) * mTieBreakerMultiplier * mTieBreakerMultiplier) + max) * boost * boost;
		}

		/** Apply the computed normalization factor to our subqueries */
		@Override
    	public void normalize(float norm, float topLevelBoost) {
			topLevelBoost *= getBoost();  // Incorporate our boost
			
			for (IWeight wt : mWeights) {
				wt.normalize(norm, topLevelBoost);
			}
		}

		/** Create the scorer used to score our associated DisjunctionMaxQuery */
		@Override
		public IScorer getScorer(IAtomicReaderRef context, boolean scoreDocsInOrder,
				boolean topScorer, Bits acceptDocs) throws IOException {
			IScorer[] scorers = new IScorer[mWeights.size()];
			int idx = 0;
			
			for (IWeight w : mWeights) {
				// we will advance() subscorers
				IScorer subScorer = w.getScorer(context, true, false, acceptDocs);
				if (subScorer != null) 
					scorers[idx++] = subScorer;
			}
			
			// all scorers did not have documents
			if (idx == 0) return null;
			
			DisjunctionMaxScorer result = new DisjunctionMaxScorer(this, 
					mTieBreakerMultiplier, scorers, idx);
			
			return result;
		}

		/** Explain the score we computed for doc */
		@Override
		public IExplanation explain(IAtomicReaderRef context, int doc) throws IOException {
			if (mDisjuncts.size() == 1) 
				return mWeights.get(0).explain(context, doc);
			
			ComplexExplanation result = new ComplexExplanation();
			float max = 0.0f, sum = 0.0f;
			
			result.setDescription(mTieBreakerMultiplier == 0.0f ? 
					"max of:" : "max plus " + mTieBreakerMultiplier + " times others of:");
			
			for (IWeight wt : mWeights) {
				IExplanation e = wt.explain(context, doc);
				if (e.isMatch()) {
					result.setMatch(Boolean.TRUE);
					result.addDetail(e);
					
					sum += e.getValue();
					max = Math.max(max, e.getValue());
				}
			}
			
			result.setValue(max + (sum - max) * mTieBreakerMultiplier);
			return result;
		}
    
	}

	/** Create the Weight used to score us */
	@Override
	public Weight createWeight(ISearcher searcher) throws IOException {
		return new DisjunctionMaxWeight(searcher);
	}

	/** 
	 * Optimize our representation and our subqueries representations
	 * @param reader the IndexReader we query
	 * @return an optimized copy of us (which may not be a copy if there is nothing to optimize) 
	 */
	@Override
	public IQuery rewrite(IIndexReader reader) throws IOException {
		int numDisjunctions = mDisjuncts.size();
		
		if (numDisjunctions == 1) {
			IQuery singleton = mDisjuncts.get(0);
			IQuery result = singleton.rewrite(reader);
			
			if (getBoost() != 1.0f) {
				if (result == singleton) 
					result = ((Query)result).clone();
				
				result.setBoost(getBoost() * result.getBoost());
			}
			
			return result;
		}
		
		DisjunctionMaxQuery clone = null;
		for (int i = 0 ; i < numDisjunctions; i++) {
			IQuery clause = mDisjuncts.get(i);
			IQuery rewrite = clause.rewrite(reader);
			
			if (rewrite != clause) {
				if (clone == null) 
					clone = this.clone();
				
				clone.mDisjuncts.set(i, rewrite);
			}
		}
		
		if (clone != null) 
			return clone;
		else 
			return this;
	}

	/** 
	 * Create a shallow copy of us -- used in rewriting if necessary
	 * @return a copy of us (but reuse, don't copy, our subqueries) 
	 */
	@Override 
	@SuppressWarnings("unchecked")
	public DisjunctionMaxQuery clone() {
		DisjunctionMaxQuery clone = (DisjunctionMaxQuery)super.clone();
		clone.mDisjuncts = (List<IQuery>) ((ArrayList<IQuery>)this.mDisjuncts).clone();
		return clone;
	}

	// inherit javadoc
	@Override
	public void extractTerms(Set<ITerm> terms) {
		for (IQuery query : mDisjuncts) {
			query.extractTerms(terms);
		}
	}

	/** 
	 * Prettyprint us.
	 * @param field the field to which we are applied
	 * @return a string that shows what we do, 
	 * of the form "(disjunct1 | disjunct2 | ... | disjunctn)^boost"
	 */
	@Override
	public String toString(String field) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("(");
		
		int numDisjunctions = mDisjuncts.size();
		
		for (int i = 0 ; i < numDisjunctions; i++) {
			IQuery subquery = mDisjuncts.get(i);
			
			// wrap sub-bools in parens
			if (subquery instanceof BooleanQuery) { 
				buffer.append("(");
				buffer.append(subquery.toString(field));
				buffer.append(")");
				
			} else {
				buffer.append(subquery.toString(field));
			}
			
			if (i != numDisjunctions-1) 
				buffer.append(" | ");
		}
		
		buffer.append(")");
		
		if (mTieBreakerMultiplier != 0.0f) {
			buffer.append("~");
			buffer.append(mTieBreakerMultiplier);
		}
		
		if (getBoost() != 1.0) {
			buffer.append("^");
			buffer.append(getBoost());
		}
		
		return buffer.toString();
	}

	/** 
	 * Return true iff we represent the same query as o
	 * @param o another object
	 * @return true iff o is a DisjunctionMaxQuery with the same boost 
	 * and the same subqueries, in the same order, as us
	 */
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof DisjunctionMaxQuery)) 
			return false;
		
		DisjunctionMaxQuery other = (DisjunctionMaxQuery)o;
		return this.getBoost() == other.getBoost()
				&& this.mTieBreakerMultiplier == other.mTieBreakerMultiplier
				&& this.mDisjuncts.equals(other.mDisjuncts);
	}

	/** 
	 * Compute a hash code for hashing us
	 * @return the hash code
	 */
	@Override
	public int hashCode() {
		return Float.floatToIntBits(getBoost())
				+ Float.floatToIntBits(mTieBreakerMultiplier)
				+ mDisjuncts.hashCode();
	}

}
