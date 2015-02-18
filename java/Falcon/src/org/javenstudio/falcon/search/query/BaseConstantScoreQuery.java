package org.javenstudio.falcon.search.query;

import java.io.IOException;
import java.util.Set;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDocIdSet;
import org.javenstudio.common.indexdb.IDocIdSetIterator;
import org.javenstudio.common.indexdb.IExplanation;
import org.javenstudio.common.indexdb.IFilter;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.search.ComplexExplanation;
import org.javenstudio.common.indexdb.search.DocIdSet;
import org.javenstudio.common.indexdb.search.Explanation;
import org.javenstudio.common.indexdb.search.Query;
import org.javenstudio.common.indexdb.search.Scorer;
import org.javenstudio.common.indexdb.search.Weight;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.hornet.search.query.ConstantScoreQuery;
import org.javenstudio.falcon.search.filter.SearchFilter;

/**
 * A query that wraps a filter and simply returns a constant score equal to the
 * query boost for every document in the filter.   This extension also supports
 * weighting of a SearchFilter.
 *
 * Experimental and subject to change.
 */
public class BaseConstantScoreQuery extends ConstantScoreQuery 
		implements ExtendedQuery {
	
	protected boolean mCache = true;  // cache by default
	protected int mCost;

	public BaseConstantScoreQuery(IFilter filter) {
		super(filter);
	}

	@Override
	public void setCache(boolean cache) { mCache = cache; }

	@Override
	public boolean getCache() { return mCache; }

	@Override
	public void setCacheSep(boolean cacheSep) { }

	@Override
	public boolean getCacheSep() { return false; }

	@Override
	public void setCost(int cost) { mCost = cost; }

	@Override
	public int getCost() { return mCost; }

	@Override
	public Query rewrite(IIndexReader reader) throws IOException {
		return this;
	}

	@Override
	public void extractTerms(Set<ITerm> terms) {
		// OK to not add any terms when used for MultiSearcher,
		// but may not be OK for highlighting
	}

	protected class ConstantWeight extends Weight {
		protected ValueSourceContext mContext;
		protected float mQueryNorm;
		protected float mQueryWeight;
		
		public ConstantWeight(ISearcher searcher) throws IOException {
			mContext = ValueSourceContext.create(searcher);
			if (mFilter instanceof SearchFilter)
				((SearchFilter)mFilter).createWeight(mContext, searcher);
		}

		@Override
		public Query getQuery() {
			return BaseConstantScoreQuery.this;
		}

		@Override
		public float getValueForNormalization() throws IOException {
			mQueryWeight = getBoost();
			return mQueryWeight * mQueryWeight;
		}

		@Override
		public void normalize(float norm, float topLevelBoost) {
			mQueryNorm = norm * topLevelBoost;
			mQueryWeight *= mQueryNorm;
		}

		@Override
		public IScorer getScorer(IAtomicReaderRef context, boolean scoreDocsInOrder,
				boolean topScorer, Bits acceptDocs) throws IOException {
			return new ConstantScorer(context, this, mQueryWeight, acceptDocs);
		}

		@Override
		public IExplanation explain(IAtomicReaderRef context, int doc) throws IOException {
			ConstantScorer cs = new ConstantScorer(context, this, 
					mQueryWeight, context.getReader().getLiveDocs());
			
			boolean exists = (cs.mDocIdSetIterator.advance(doc) == doc);
			ComplexExplanation result = new ComplexExplanation();

			if (exists) {
				result.setDescription("ConstantScoreQuery(" + mFilter + "), product of:");
				result.setValue(mQueryWeight);
				result.setMatch(Boolean.TRUE);
				result.addDetail(new Explanation(getBoost(), "boost"));
				result.addDetail(new Explanation(mQueryNorm,"queryNorm"));
				
			} else {
				result.setDescription("ConstantScoreQuery(" + mFilter + ") doesn't match id " + doc);
				result.setValue(0);
				result.setMatch(Boolean.FALSE);
			}
			
			return result;
		}
	}

	protected class ConstantScorer extends Scorer {
		protected final IDocIdSetIterator mDocIdSetIterator;
		protected final float mTheScore;
		protected final Bits mAcceptDocs;
		protected int mDoc = -1;

		public ConstantScorer(IAtomicReaderRef context, ConstantWeight w, 
				float theScore, Bits acceptDocs) throws IOException {
			super(w);
			mTheScore = theScore;
			mAcceptDocs = acceptDocs;
			
			IDocIdSet docIdSet = (mFilter instanceof SearchFilter) ? 
					((SearchFilter)mFilter).getDocIdSet(w.mContext, context, acceptDocs) : 
					mFilter.getDocIdSet(context, acceptDocs);
					
			if (docIdSet == null) {
				mDocIdSetIterator = DocIdSet.EMPTY_DOCIDSET.iterator();
				
			} else {
				IDocIdSetIterator iter = docIdSet.iterator();
				if (iter == null) 
					mDocIdSetIterator = DocIdSet.EMPTY_DOCIDSET.iterator();
				else 
					mDocIdSetIterator = iter;
			}
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
		public float getFreq() throws IOException {
			return 1;
		}

		@Override
		public int advance(int target) throws IOException {
			return mDocIdSetIterator.advance(target);
		}
	}

	@Override
	public Weight createWeight(ISearcher searcher) throws IOException {
		return new BaseConstantScoreQuery.ConstantWeight(searcher);
	}

	/** Prints a user-readable version of this query. */
	@Override
	public String toString(String field) {
		return "ConstantScore(" + mFilter.toString()
				+ (getBoost() == 1.0 ? ")" : "^" + getBoost());
	}

	/** Returns true if <code>o</code> is equal to this. */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof BaseConstantScoreQuery)) 
			return false;
		
		BaseConstantScoreQuery other = (BaseConstantScoreQuery)o;
		return this.getBoost()==other.getBoost() && mFilter.equals(other.mFilter);
	}

	/** Returns a hash code value for this object. */
	@Override
	public int hashCode() {
		// Simple add is OK since no existing filter hashcode has a float component.
		return mFilter.hashCode() + Float.floatToIntBits(getBoost());
	}
	
}
