package org.javenstudio.hornet.search.query;

import java.util.Set;
import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IExplanation;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.search.ComplexExplanation;
import org.javenstudio.common.indexdb.search.Explanation;
import org.javenstudio.common.indexdb.search.Query;
import org.javenstudio.common.indexdb.search.Scorer;
import org.javenstudio.common.indexdb.search.Weight;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.StringHelper;

/**
 * A query that matches all documents.
 */
public class MatchAllDocsQuery extends Query {

	private class MatchAllScorer extends Scorer {
		private final float mScore;
		private final int mMaxDoc;
		private final Bits mLiveDocs;
		private int mDoc = -1;

		MatchAllScorer(IIndexReader reader, Bits liveDocs, Weight w, float score) 
				throws IOException {
			super(w);
			mLiveDocs = liveDocs;
			mScore = score;
			mMaxDoc = reader.getMaxDoc();
		}

		@Override
		public int getDocID() { return mDoc; }

		@Override
		public int nextDoc() throws IOException {
			mDoc ++;
			while (mLiveDocs != null && mDoc < mMaxDoc && !mLiveDocs.get(mDoc)) {
				mDoc ++;
			}
			if (mDoc == mMaxDoc) 
				mDoc = NO_MORE_DOCS;
			
			return mDoc;
		}
    
		@Override
		public float getScore() { return mScore; }

		@Override
		public int advance(int target) throws IOException {
			mDoc = target-1;
			return nextDoc();
		}
	}

	private class MatchAllDocsWeight extends Weight {
		private float mQueryWeight;
		private float mQueryNorm;

		public MatchAllDocsWeight(ISearcher searcher) {
		}

		@Override
		public String toString() {
			return "weight(" + MatchAllDocsQuery.this + ")";
		}

		@Override
		public Query getQuery() {
			return MatchAllDocsQuery.this;
		}

		@Override
		public float getValueForNormalization() {
			mQueryWeight = getBoost();
			return mQueryWeight * mQueryWeight;
		}

		@Override
		public void normalize(float queryNorm, float topLevelBoost) {
			mQueryNorm = queryNorm * topLevelBoost;
			mQueryWeight *= mQueryNorm;
		}

		@Override
		public IScorer getScorer(IAtomicReaderRef context, boolean scoreDocsInOrder,
				boolean topScorer, Bits acceptDocs) throws IOException {
			return new MatchAllScorer(context.getReader(), acceptDocs, this, mQueryWeight);
		}

		@Override
		public IExplanation explain(IAtomicReaderRef context, int doc) {
			// explain query weight
			Explanation queryExpl = new ComplexExplanation(true, 
					mQueryWeight, "MatchAllDocsQuery, product of:");
			
			if (getBoost() != 1.0f) 
				queryExpl.addDetail(new Explanation(getBoost(),"boost"));
			
			queryExpl.addDetail(new Explanation(mQueryNorm, "queryNorm"));

			return queryExpl;
		}
	}

	@Override
	public Weight createWeight(ISearcher searcher) {
		return new MatchAllDocsWeight(searcher);
	}

	@Override
	public void extractTerms(Set<ITerm> terms) {
	}

	@Override
	public String toString(String field) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("*:*");
		buffer.append(StringHelper.toBoostString(getBoost()));
		return buffer.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof MatchAllDocsQuery))
			return false;
		MatchAllDocsQuery other = (MatchAllDocsQuery) o;
		return this.getBoost() == other.getBoost();
	}

	@Override
	public int hashCode() {
		return Float.floatToIntBits(getBoost()) ^ 0x1AA71190;
	}
	
}
