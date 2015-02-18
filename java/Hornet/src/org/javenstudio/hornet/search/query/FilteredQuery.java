package org.javenstudio.hornet.search.query;

import java.io.IOException;
import java.util.Set;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ICollector;
import org.javenstudio.common.indexdb.IDocIdSet;
import org.javenstudio.common.indexdb.IDocIdSetIterator;
import org.javenstudio.common.indexdb.IExplanation;
import org.javenstudio.common.indexdb.IFilter;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.IWeight;
import org.javenstudio.common.indexdb.search.DocIdSet;
import org.javenstudio.common.indexdb.search.DocIdSetIterator;
import org.javenstudio.common.indexdb.search.Explanation;
import org.javenstudio.common.indexdb.search.Filter;
import org.javenstudio.common.indexdb.search.Query;
import org.javenstudio.common.indexdb.search.Scorer;
import org.javenstudio.common.indexdb.search.Weight;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.StringHelper;

/**
 * A query that applies a filter to the results of another query.
 *
 * <p>Note: the bits are retrieved from the filter each time this
 * query is used in a search - use a CachingWrapperFilter to avoid
 * regenerating the bits every time.
 * @since   1.4
 * @see     CachingWrapperFilter
 */
public class FilteredQuery extends Query {

	private final IQuery mQuery;
	private final IFilter mFilter;

	final class FilteredWeight extends Weight { 
		private final IWeight mWeight;
		
		public FilteredWeight(IWeight weight) { 
			mWeight = weight;
		}
		
		@Override
		public boolean scoresDocsOutOfOrder() {
			// TODO: Support out-of-order scoring!
			// For now we return false here, as we always get the scorer in order
			return false;
		}

		@Override
		public float getValueForNormalization() throws IOException { 
			return mWeight.getValueForNormalization() * getBoost() * getBoost(); // boost sub-weight
		}

		@Override
		public void normalize (float norm, float topLevelBoost) { 
			mWeight.normalize(norm, topLevelBoost * getBoost()); // incorporate boost
		}

		@Override
		public IExplanation explain(IAtomicReaderRef ir, int i) throws IOException {
			IExplanation inner = mWeight.explain(ir, i);
			IFilter f = FilteredQuery.this.mFilter;
			
			IDocIdSet docIdSet = f.getDocIdSet(ir, ir.getReader().getLiveDocs());
			IDocIdSetIterator docIdSetIterator = docIdSet == null ? 
					DocIdSet.EMPTY_DOCIDSET.iterator() : docIdSet.iterator();
					
			if (docIdSetIterator == null) 
				docIdSetIterator = DocIdSet.EMPTY_DOCIDSET.iterator();
			
			if (docIdSetIterator.advance(i) == i) 
				return inner;
			
			Explanation result = new Explanation(0.0f, "failure to match filter: " + f.toString());
			result.addDetail(inner);
			
			return result;
		}

		// return this query
		@Override
		public Query getQuery() { return FilteredQuery.this; }

		// return a filtering scorer
		@Override
		public IScorer getScorer(IAtomicReaderRef context, boolean scoreDocsInOrder, 
				boolean topScorer, Bits acceptDocs) throws IOException {
			assert mFilter != null;

			final IDocIdSet filterDocIdSet = mFilter.getDocIdSet(context, acceptDocs);
			if (filterDocIdSet == null) {
				// this means the filter does not accept any documents.
				return null;
			}
    
			final IDocIdSetIterator filterIter = filterDocIdSet.iterator();
			if (filterIter == null) {
				// this means the filter does not accept any documents.
				return null;
			}

			final int firstFilterDoc = filterIter.nextDoc();
			if (firstFilterDoc == DocIdSetIterator.NO_MORE_DOCS) 
				return null;
    
			final Bits filterAcceptDocs = filterDocIdSet.getBits();
			final boolean useRandomAccess = (filterAcceptDocs != null && 
					FilteredQuery.this.useRandomAccess(filterAcceptDocs, firstFilterDoc));

			if (useRandomAccess) {
				// if we are using random access, we return the inner scorer, just with other acceptDocs
				// TODO, replace this by when BooleanWeight is fixed to be consistent with its scorer implementations:
				// return weight.scorer(context, scoreDocsInOrder, topScorer, filterAcceptDocs);
				return mWeight.getScorer(context, true, topScorer, filterAcceptDocs);
				
			} else {
				assert firstFilterDoc > -1;
				// we are gonna advance() this scorer, so we set inorder=true/toplevel=false
				// we pass null as acceptDocs, as our filter has already respected acceptDocs, no need to do twice
				final IScorer scorer = mWeight.getScorer(context, true, false, null);
				return (scorer == null) ? null : 
					new FilteredScorer(this, scorer, filterIter, firstFilterDoc);
			}
		}
	}
	
	final class FilteredScorer extends Scorer { 
		private int mScorerDoc = -1, mFilterDoc;
		private final int mFirstFilterDoc;
		private final IScorer mScorer;
		private final IDocIdSetIterator mFilterIter;
		
		public FilteredScorer(IWeight weight, IScorer scorer, 
				IDocIdSetIterator filterIter, int firstFilterDoc) { 
			super(weight);
			mFirstFilterDoc = firstFilterDoc;
			mFilterDoc = firstFilterDoc;
			mScorer = scorer;
			mFilterIter = filterIter;
		}
		
		// optimization: we are topScorer and collect directly using short-circuited algo
		@Override
		public void score(ICollector collector) throws IOException {
			int filterDoc = mFirstFilterDoc;
			int scorerDoc = mScorer.advance(filterDoc);
			// the normalization trick already applies the boost of this query,
			// so we can use the wrapped scorer directly:
			collector.setScorer(mScorer);
			for (;;) {
				if (scorerDoc == filterDoc) {
					// Check if scorer has exhausted, only before collecting.
					if (scorerDoc == DocIdSetIterator.NO_MORE_DOCS) 
						break;
					
					collector.collect(scorerDoc);
					filterDoc = mFilterIter.nextDoc();
					scorerDoc = mScorer.advance(filterDoc);
					
				} else if (scorerDoc > filterDoc) {
					filterDoc = mFilterIter.advance(scorerDoc);
					
				} else {
					scorerDoc = mScorer.advance(filterDoc);
				}
			}
		}

		private int advanceToNextCommonDoc() throws IOException {
			for (;;) {
				if (mScorerDoc < mFilterDoc) {
					mScorerDoc = mScorer.advance(mFilterDoc);
				} else if (mScorerDoc == mFilterDoc) {
					return mScorerDoc;
				} else {
					mFilterDoc = mFilterIter.advance(mScorerDoc);
				}
			}
		}

		@Override
		public int nextDoc() throws IOException {
			// don't go to next doc on first call
			// (because filterIter is already on first doc):
			if (mScorerDoc != -1) 
				mFilterDoc = mFilterIter.nextDoc();
			
			return advanceToNextCommonDoc();
		}

		@Override
		public int advance(int target) throws IOException {
			if (target > mFilterDoc) 
				mFilterDoc = mFilterIter.advance(target);
			
			return advanceToNextCommonDoc();
		}

		@Override
		public int getDocID() { return mScorerDoc; }

		@Override
		public float getScore() throws IOException {
			return mScorer.getScore();
		}
	}
	
	/**
	 * Constructs a new query which applies a filter to the results of the original query.
	 * {@link Filter#getDocIdSet} will be called every time this query is used in a search.
	 * @param query  Query to be filtered, cannot be <code>null</code>.
	 * @param filter Filter to apply to query results, cannot be <code>null</code>.
	 */
	public FilteredQuery(IQuery query, IFilter filter) {
		if (query == null || filter == null)
			throw new IllegalArgumentException("Query and filter cannot be null.");
		mQuery = query;
		mFilter = filter;
	}
  
	/**
	 * Expert: decides if a filter should be executed as "random-access" or not.
	 * random-access means the filter "filters" in a similar way as deleted docs are filtered
	 * in lucene. This is faster when the filter accepts many documents.
	 * However, when the filter is very sparse, it can be faster to execute the query+filter
	 * as a conjunction in some cases.
	 * 
	 * The default implementation returns true if the first document accepted by the
	 * filter is < 100.
	 */
	protected boolean useRandomAccess(Bits bits, int firstFilterDoc) {
		return firstFilterDoc < 100;
	}

	/**
	 * Returns a Weight that applies the filter to the enclosed query's Weight.
	 * This is accomplished by overriding the Scorer returned by the Weight.
	 */
	@Override
	public IWeight createWeight(final ISearcher searcher) throws IOException {
		final IWeight weight = mQuery.createWeight(searcher);
		return new FilteredWeight(weight);
	}

	/** 
	 * Rewrites the query. If the wrapped is an instance of
	 * {@link MatchAllDocsQuery} it returns a {@link ConstantScoreQuery}. Otherwise
	 * it returns a new {@code FilteredQuery} wrapping the rewritten query. 
	 */
	@Override
	public IQuery rewrite(IIndexReader reader) throws IOException {
		final IQuery queryRewritten = mQuery.rewrite(reader);
    
		if (queryRewritten instanceof MatchAllDocsQuery) {
			// Special case: If the query is a MatchAllDocsQuery, we only
			// return a CSQ(filter).
			final IQuery rewritten = new ConstantScoreQuery(mFilter);
			// Combine boost of MatchAllDocsQuery and the wrapped rewritten query:
			rewritten.setBoost(this.getBoost() * queryRewritten.getBoost());
			return rewritten;
		}
    
		if (queryRewritten != mQuery) {
			// rewrite to a new FilteredQuery wrapping the rewritten query
			final Query rewritten = new FilteredQuery(queryRewritten, mFilter);
			rewritten.setBoost(this.getBoost());
			return rewritten;
			
		} else {
			// nothing to rewrite, we are done!
			return this;
		}
	}

	public final IQuery getQuery() { return mQuery; }
	public final IFilter getFilter() { return mFilter; }

	// inherit javadoc
	@Override
	public void extractTerms(Set<ITerm> terms) {
		getQuery().extractTerms(terms);
	}

	/** Prints a user-readable version of this query. */
	@Override
	public String toString (String s) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("FilteredQuery(");
		buffer.append(mQuery.toString(s));
		buffer.append(")->");
		buffer.append(mFilter);
		buffer.append(StringHelper.toBoostString(getBoost()));
		return buffer.toString();
	}

	/** Returns true if <code>o</code> is equal to this. */
	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!super.equals(o))
			return false;
		assert o instanceof FilteredQuery;
		final FilteredQuery fq = (FilteredQuery) o;
		return fq.mQuery.equals(this.mQuery) && fq.mFilter.equals(this.mFilter);
	}

	/** Returns a hash code value for this object. */
	@Override
	public int hashCode() {
		int hash = super.hashCode();
		hash = hash * 31 + mQuery.hashCode();
		hash = hash * 31 + mFilter.hashCode();
		return hash;
	}
	
}
