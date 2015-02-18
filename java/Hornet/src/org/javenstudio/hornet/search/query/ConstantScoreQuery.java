package org.javenstudio.hornet.search.query;

import java.io.IOException;
import java.util.Set;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ICollector;
import org.javenstudio.common.indexdb.IDocIdSet;
import org.javenstudio.common.indexdb.IDocIdSetIterator;
import org.javenstudio.common.indexdb.IFilter;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.common.indexdb.IWeight;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.search.Collector;
import org.javenstudio.common.indexdb.search.ComplexExplanation;
import org.javenstudio.common.indexdb.search.Explanation;
import org.javenstudio.common.indexdb.search.Query;
import org.javenstudio.common.indexdb.search.Scorer;
import org.javenstudio.common.indexdb.search.Weight;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.StringHelper;

/**
 * A query that wraps another query or a filter and simply returns a constant score equal to the
 * query boost for every document that matches the filter or query.
 * For queries it therefore simply strips of all scores and returns a constant one.
 */
public class ConstantScoreQuery extends Query {
	
	protected final IFilter mFilter;
	protected final IQuery mQuery;

	private class ConstantWeight extends Weight {
		private final IWeight mInnerWeight;
		private float mQueryNorm;
		private float mQueryWeight;
    
		public ConstantWeight(ISearcher searcher) throws IOException {
			mInnerWeight = (mQuery == null) ? null : mQuery.createWeight(searcher);
		}

		@Override
		public Query getQuery() {
			return ConstantScoreQuery.this;
		}

		@Override
		public float getValueForNormalization() throws IOException {
			// we calculate sumOfSquaredWeights of the inner weight, but ignore it (just to initialize everything)
			if (mInnerWeight != null) mInnerWeight.getValueForNormalization();
			mQueryWeight = getBoost();
			return mQueryWeight * mQueryWeight;
		}

		@Override
		public void normalize(float norm, float topLevelBoost) {
			mQueryNorm = norm * topLevelBoost;
			mQueryWeight *= mQueryNorm;
			// we normalize the inner weight, but ignore it (just to initialize everything)
			if (mInnerWeight != null) 
				mInnerWeight.normalize(norm, topLevelBoost);
		}

		@Override
		public IScorer getScorer(IAtomicReaderRef context, boolean scoreDocsInOrder,
				boolean topScorer, final Bits acceptDocs) throws IOException {
			final IDocIdSetIterator disi;
			if (mFilter != null) {
				assert mQuery == null;
				final IDocIdSet dis = mFilter.getDocIdSet(context, acceptDocs);
				if (dis == null) 
					return null;
				
				disi = dis.iterator();
			} else {
				assert mQuery != null && mInnerWeight != null;
				disi = mInnerWeight.getScorer(context, scoreDocsInOrder, topScorer, acceptDocs);
			}

			if (disi == null) 
				return null;
			
			return new ConstantScorer(disi, this, mQueryWeight);
		}
    
		@Override
		public boolean scoresDocsOutOfOrder() {
			return (mInnerWeight != null) ? mInnerWeight.scoresDocsOutOfOrder() : false;
		}

		@Override
		public Explanation explain(IAtomicReaderRef context, int doc) throws IOException {
			final IScorer cs = getScorer(context, true, false, context.getReader().getLiveDocs());
			final boolean exists = (cs != null && cs.advance(doc) == doc);

			final ComplexExplanation result = new ComplexExplanation();
			if (exists) {
				result.setDescription(ConstantScoreQuery.this.toString() + ", product of:");
				result.setValue(mQueryWeight);
				result.setMatch(Boolean.TRUE);
				result.addDetail(new Explanation(getBoost(), "boost"));
				result.addDetail(new Explanation(mQueryNorm, "queryNorm"));
				
			} else {
				result.setDescription(ConstantScoreQuery.this.toString() + " doesn't match id " + doc);
				result.setValue(0);
				result.setMatch(Boolean.FALSE);
			}
			
			return result;
		}
	}

	private class ConstantScorer extends Scorer {
		final IDocIdSetIterator mDocIdSetIterator;
		final float mTheScore;

		public ConstantScorer(IDocIdSetIterator docIdSetIterator, IWeight w, float theScore) 
				throws IOException {
			super(w);
			mTheScore = theScore;
			mDocIdSetIterator = docIdSetIterator;
		}

		@Override
		public int nextDoc() throws IOException {
			return mDocIdSetIterator.nextDoc();
		}
    
		@Override
		public int getDocID() {
			return mDocIdSetIterator.getDocID();
		}

		@Override
		public float getScore() throws IOException {
			return mTheScore;
		}

		@Override
		public int advance(int target) throws IOException {
			return mDocIdSetIterator.advance(target);
		}
    
		private Collector wrapCollector(final ICollector collector) {
			return new Collector() {
				@Override
				public void setScorer(IScorer scorer) throws IOException {
					// we must wrap again here, but using the scorer passed in as parameter:
					collector.setScorer(new ConstantScorer(scorer, 
							ConstantScorer.this.mWeight, ConstantScorer.this.mTheScore));
				}
        
				@Override
				public void collect(int doc) throws IOException {
					collector.collect(doc);
				}
        
				@Override
				public void setNextReader(IAtomicReaderRef context) throws IOException {
					collector.setNextReader(context);
				}
        
				@Override
				public boolean acceptsDocsOutOfOrder() {
					return collector.acceptsDocsOutOfOrder();
				}
			};
		}

		// this optimization allows out of order scoring as top scorer!
		@Override
		public void score(ICollector collector) throws IOException {
			if (mDocIdSetIterator instanceof Scorer) {
				((Scorer) mDocIdSetIterator).score(wrapCollector(collector));
			} else {
				super.score(collector);
			}
		}

		// this optimization allows out of order scoring as top scorer,
		@Override
		public boolean score(ICollector collector, int max, int firstDocID) throws IOException {
			if (mDocIdSetIterator instanceof Scorer) {
				return ((Scorer) mDocIdSetIterator).score(wrapCollector(collector), max, firstDocID);
			} else {
				return super.score(collector, max, firstDocID);
			}
		}
	}
	
	/** 
	 * Strips off scores from the passed in Query. The hits will get a constant score
	 * dependent on the boost factor of this query. 
	 */
	public ConstantScoreQuery(IQuery query) {
		if (query == null)
			throw new NullPointerException("Query may not be null");
		mFilter = null;
		mQuery = query;
	}

	/** 
	 * Wraps a Filter as a Query. The hits will get a constant score
	 * dependent on the boost factor of this query.
	 * If you simply want to strip off scores from a Query, no longer use
	 * {@code new ConstantScoreQuery(new QueryWrapperFilter(query))}, instead
	 * use {@link #ConstantScoreQuery(Query)}!
	 */
	public ConstantScoreQuery(IFilter filter) {
		if (filter == null)
			throw new NullPointerException("Filter may not be null");
		mFilter = filter;
		mQuery = null;
	}

	/** Returns the encapsulated filter, returns {@code null} if a query is wrapped. */
	public IFilter getFilter() {
		return mFilter;
	}

	/** Returns the encapsulated query, returns {@code null} if a filter is wrapped. */
	public IQuery getQuery() {
		return mQuery;
	}

	@Override
	public IQuery rewrite(IIndexReader reader) throws IOException {
		if (mQuery != null) {
			IQuery rewritten = mQuery.rewrite(reader);
			if (rewritten != mQuery) {
				rewritten = new ConstantScoreQuery(rewritten);
				rewritten.setBoost(this.getBoost());
				return rewritten;
			}
		}
		return this;
	}

	@Override
	public void extractTerms(Set<ITerm> terms) {
		// TODO: OK to not add any terms when wrapped a filter
		// and used with MultiSearcher, but may not be OK for
		// highlighting.
		// If a query was wrapped, we delegate to query.
		if (mQuery != null)
			mQuery.extractTerms(terms);
	}

	@Override
	public Weight createWeight(ISearcher searcher) throws IOException {
		return new ConstantScoreQuery.ConstantWeight(searcher);
	}

	@Override
	public String toString(String field) {
		return new StringBuilder("ConstantScore(")
			.append((mQuery == null) ? mFilter.toString() : mQuery.toString(field))
			.append(')')
			.append(StringHelper.toBoostString(getBoost()))
			.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!super.equals(o)) return false;
		if (o instanceof ConstantScoreQuery) {
			final ConstantScoreQuery other = (ConstantScoreQuery) o;
			return ((this.mFilter == null) ? (other.mFilter == null) : this.mFilter.equals(other.mFilter)) &&
				   ((this.mQuery == null) ? (other.mQuery == null) : this.mQuery.equals(other.mQuery));
		}
		return false;
	}

	@Override
	public int hashCode() {
		return 31 * super.hashCode() + ((mQuery == null) ? mFilter : mQuery).hashCode();
	}

}
