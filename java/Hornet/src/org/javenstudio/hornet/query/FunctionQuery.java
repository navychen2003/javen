package org.javenstudio.hornet.query;

import java.io.IOException;
import java.util.Set;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IExplanation;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.search.ComplexExplanation;
import org.javenstudio.common.indexdb.search.Explanation;
import org.javenstudio.common.indexdb.search.Query;
import org.javenstudio.common.indexdb.search.Scorer;
import org.javenstudio.common.indexdb.search.Weight;
import org.javenstudio.common.indexdb.util.Bits;

/**
 * Returns a score for each document based on a ValueSource,
 * often some function of the value of a field.
 *
 * <b>Note: This API is experimental and may change in non backward-compatible ways in the future</b>
 *
 */
public class FunctionQuery extends Query {
	
	private final ValueSource mFunction;

	/**
	 * @param func defines the function to be used for scoring
	 */
	public FunctionQuery(ValueSource func) {
		mFunction = func;
		
		if (func == null) 
			throw new NullPointerException();
	}

	/** @return The associated ValueSource */
	public ValueSource getValueSource() {
		return mFunction;
	}

	@Override
	public Query rewrite(IIndexReader reader) throws IOException {
		return this;
	}

	@Override
	public void extractTerms(Set<ITerm> terms) { 
		// do nothing
	}

	protected class FunctionWeight extends Weight {
		
		protected final ValueSourceContext mContext;
		protected final ISearcher mSearcher;
		protected float mQueryNorm;
		protected float mQueryWeight;
		
		public FunctionWeight(ISearcher searcher) throws IOException {
			mSearcher = searcher;
			mContext = ValueSourceContext.create(searcher);
			mFunction.createWeight(mContext, searcher);
		}

		@Override
		public Query getQuery() {
			return FunctionQuery.this;
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
		public Scorer getScorer(IAtomicReaderRef context, boolean scoreDocsInOrder,
				boolean topScorer, Bits acceptDocs) throws IOException {
			return new AllScorer(context, acceptDocs, this, mQueryWeight);
		}

		@Override
		public IExplanation explain(IAtomicReaderRef context, int doc) throws IOException {
			return ((AllScorer)getScorer(context, true, true, 
					context.getReader().getLiveDocs())).explain(doc);
		}
	}

	protected class AllScorer extends Scorer {
		
		protected final IIndexReader mReader;
		protected final FunctionWeight mWeight;
		protected final int mMaxDoc;
		protected final float mQWeight;
		protected final FunctionValues mValues;
		protected final Bits mAcceptDocs;
		protected int mDoc = -1;

		public AllScorer(IAtomicReaderRef context, Bits acceptDocs, 
				FunctionWeight w, float qWeight) throws IOException {
			super(w);
			
			mWeight = w;
			mQWeight = qWeight;
			mReader = context.getReader();
			mMaxDoc = mReader.getMaxDoc();
			mAcceptDocs = acceptDocs;
			mValues = mFunction.getValues(
					mWeight.mContext, context);
		}

		@Override
		public int getDocID() {
			return mDoc;
		}

		// instead of matching all docs, we could also embed a query.
		// the score could either ignore the subscore, or boost it.
		// Containment:  floatline(foo:myTerm, "myFloatField", 1.0, 0.0f)
		// Boost:        foo:myTerm^floatline("myFloatField",1.0,0.0f)
		@Override
		public int nextDoc() throws IOException {
			for (;;) {
				++ mDoc;
				if (mDoc >= mMaxDoc) 
					return mDoc = NO_MORE_DOCS;
				
				if (mAcceptDocs != null && !mAcceptDocs.get(mDoc)) 
					continue;
				
				return mDoc;
			}
		}

		@Override
		public int advance(int target) throws IOException {
			// this will work even if target==NO_MORE_DOCS
			mDoc = target-1;
			return nextDoc();
		}

		@Override
		public float getScore() throws IOException {
			float score = mQWeight * mValues.floatVal(mDoc);

			// Current Lucene priority queues can't handle NaN and -Infinity, so
			// map to -Float.MAX_VALUE. This conditional handles both -infinity
			// and NaN since comparisons with NaN are always false.
			return score > Float.NEGATIVE_INFINITY ? score : -Float.MAX_VALUE;
		}

		@Override
		public float getFreq() throws IOException {
			return 1;
		}

		public Explanation explain(int doc) throws IOException {
			float sc = mQWeight * mValues.floatVal(doc);

			Explanation result = new ComplexExplanation(true, sc, 
					"FunctionQuery(" + mFunction + "), product of:");

			result.addDetail(mValues.explain(doc));
			result.addDetail(new Explanation(getBoost(), "boost"));
			result.addDetail(new Explanation(mWeight.mQueryNorm, "queryNorm"));
			
			return result;
		}
	}

	@Override
	public Weight createWeight(ISearcher searcher) throws IOException {
		return new FunctionQuery.FunctionWeight(searcher);
	}

	/** Returns true if <code>o</code> is equal to this. */
	@Override
	public boolean equals(Object o) {
		if (!FunctionQuery.class.isInstance(o)) 
			return false;
		
		FunctionQuery other = (FunctionQuery)o;
		
		return this.getBoost() == other.getBoost() && 
				this.mFunction.equals(other.mFunction);
	}

	/** Returns a hash code value for this object. */
	@Override
	public int hashCode() {
		return mFunction.hashCode()*31 + Float.floatToIntBits(getBoost());
	}

	/** Prints a user-readable version of this query. */
	@Override
	public String toString(String field) {
		float boost = getBoost();
		return (boost != 1.0 ? "(" : "") + mFunction.toString()
				+ (boost == 1.0 ? "" : ")^" + boost);
	}
	
}
