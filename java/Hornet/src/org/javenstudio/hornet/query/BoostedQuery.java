package org.javenstudio.hornet.query;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
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
import org.javenstudio.common.indexdb.search.Explanation;
import org.javenstudio.common.indexdb.search.Query;
import org.javenstudio.common.indexdb.search.Scorer;
import org.javenstudio.common.indexdb.search.Weight;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.StringHelper;

/**
 * Query that is boosted by a ValueSource
 * 
 * TODO: BoostedQuery and BoostingQuery in the same module? 
 * something has to give
 */
public class BoostedQuery extends Query {
	
	protected final ValueSource mBoostVal; // optional, can be null
	protected IQuery mQuery;

	public BoostedQuery(IQuery subQuery, ValueSource boostVal) {
		mQuery = subQuery;
		mBoostVal = boostVal;
	}

	public IQuery getQuery() { return mQuery; }
	public ValueSource getValueSource() { return mBoostVal; }

	@Override
	public Query rewrite(IIndexReader reader) throws IOException {
		IQuery newQ = mQuery.rewrite(reader);
		if (newQ == mQuery) 
			return this;
		
		BoostedQuery bq = (BoostedQuery)this.clone();
		bq.mQuery = newQ;
		
		return bq;
	}

	@Override
	public void extractTerms(Set<ITerm> terms) {
		mQuery.extractTerms(terms);
	}

	@Override
	public IWeight createWeight(ISearcher searcher) throws IOException {
		return new BoostedQuery.BoostedWeight(searcher);
	}

	private class BoostedWeight extends Weight {
		
		@SuppressWarnings("unused")
		private final ISearcher mSearcher;
		private IWeight mWeight;
		private ValueSourceContext mContext;

		public BoostedWeight(ISearcher searcher) throws IOException {
			mSearcher = searcher;
			mWeight = mQuery.createWeight(searcher);
			mContext = ValueSourceContext.create(searcher);
			mBoostVal.createWeight(mContext, searcher);
		}

		@Override
		public Query getQuery() {
			return BoostedQuery.this;
		}

		@Override
		public float getValueForNormalization() throws IOException {
			float sum = mWeight.getValueForNormalization();
			sum *= getBoost() * getBoost();
			return sum ;
		}

		@Override
		public void normalize(float norm, float topLevelBoost) {
			topLevelBoost *= getBoost();
			mWeight.normalize(norm, topLevelBoost);
		}

		@Override
		public IScorer getScorer(IAtomicReaderRef context, boolean scoreDocsInOrder,
				boolean topScorer, Bits acceptDocs) throws IOException {
			// we are gonna advance() the subscorer
			IScorer subQueryScorer = mWeight.getScorer(context, true, false, acceptDocs);
			if (subQueryScorer == null) 
				return null;
      
			return new BoostedQuery.CustomScorer(context, this, 
					getBoost(), subQueryScorer, mBoostVal);
		}

		@Override
		public IExplanation explain(IAtomicReaderRef readerContext, int doc) throws IOException {
			IExplanation subQueryExpl = mWeight.explain(readerContext,doc);
			if (!subQueryExpl.isMatch()) 
				return subQueryExpl;
      
			FunctionValues vals = mBoostVal.getValues(mContext, readerContext);
			float sc = subQueryExpl.getValue() * vals.floatVal(doc);
			
			Explanation res = new ComplexExplanation(true, sc, 
					BoostedQuery.this.toString() + ", product of:");
			
			res.addDetail(subQueryExpl);
			res.addDetail(vals.explain(doc));
			
			return res;
		}
	}

	private class CustomScorer extends Scorer {
		
		private final IAtomicReaderRef mReaderContext;
		private final BoostedQuery.BoostedWeight mBoostedWeight;
		private final float mWeight;
		private final IScorer mScorer;
		private final FunctionValues mValues;
		
		private CustomScorer(IAtomicReaderRef readerContext, 
				BoostedQuery.BoostedWeight w, float qWeight,
				IScorer scorer, ValueSource vs) throws IOException {
			super(w);
			
			mBoostedWeight = w;
			mWeight = qWeight;
			mScorer = scorer;
			mReaderContext = readerContext;
			
			mValues = vs.getValues(
					mBoostedWeight.mContext, readerContext);
		}

	    @Override
	    public int getDocID() {
	    	return mScorer.getDocID();
	    }
	
	    @Override
	    public int advance(int target) throws IOException {
	    	return mScorer.advance(target);
	    }
	
	    @Override
	    public int nextDoc() throws IOException {
	    	return mScorer.nextDoc();
	    }
	
	    @Override   
	    public float getScore() throws IOException {
	    	float score = mWeight * mScorer.getScore() * mValues.floatVal(mScorer.getDocID());
	
	    	// Current Lucene priority queues can't handle NaN and -Infinity, so
	    	// map to -Float.MAX_VALUE. This conditional handles both -infinity
	    	// and NaN since comparisons with NaN are always false.
	    	return score>Float.NEGATIVE_INFINITY ? score : -Float.MAX_VALUE;
	    }
	
	    @Override
	    public float getFreq() throws IOException {
	    	return mScorer.getFreq();
	    }
	
	    @Override
	    public Collection<IScorer.IChild> getChildren() {
	    	return Collections.singleton((IScorer.IChild)
	    			new Scorer.ChildScorer(mScorer, "CUSTOM"));
	    }
	
	    @SuppressWarnings("unused")
		public IExplanation explain(int doc) throws IOException {
	    	IExplanation subQueryExpl = mBoostedWeight.mWeight.explain(mReaderContext, doc);
	    	if (!subQueryExpl.isMatch()) 
	    		return subQueryExpl;
	      
	    	float sc = subQueryExpl.getValue() * mValues.floatVal(doc);
	    	
	    	Explanation res = new ComplexExplanation(true, sc, 
	    			BoostedQuery.this.toString() + ", product of:");
	    	
	    	res.addDetail(subQueryExpl);
	    	res.addDetail(mValues.explain(doc));
	    	return res;
		}
	}

	@Override
	public String toString(String field) {
		StringBuilder sb = new StringBuilder();
		sb.append("boost(").append(mQuery.toString(field)).append(',').append(mBoostVal).append(')');
		sb.append(StringHelper.toBoostString(getBoost()));
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null || !super.equals(o)) 
			return false;
		
		BoostedQuery other = (BoostedQuery)o;
		return this.mQuery.equals(other.mQuery)
				&& this.mBoostVal.equals(other.mBoostVal);
	}

	@Override
	public int hashCode() {
		int h = mQuery.hashCode();
		h ^= (h << 17) | (h >>> 16);
		h += mBoostVal.hashCode();
		h ^= (h << 8) | (h >>> 25);
		h += Float.floatToIntBits(getBoost());
		return h;
	}

}
