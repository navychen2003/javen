package org.javenstudio.hornet.search.similarity;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ICollectionStatistics;
import org.javenstudio.common.indexdb.IExactSimilarityScorer;
import org.javenstudio.common.indexdb.IFieldState;
import org.javenstudio.common.indexdb.INorm;
import org.javenstudio.common.indexdb.ISimilarity;
import org.javenstudio.common.indexdb.ISimilarityWeight;
import org.javenstudio.common.indexdb.ISloppySimilarityScorer;
import org.javenstudio.common.indexdb.ITermStatistics;
import org.javenstudio.common.indexdb.search.Similarity;

/**
 * Provides the ability to use a different {@link Similarity} for different fields.
 * <p>
 * Subclasses should implement {@link #get(String)} to return an appropriate
 * Similarity (for example, using field-specific parameter values) for the field.
 * 
 */
public abstract class PerFieldSimilarityWrapper extends Similarity {
  
	/**
	 * Sole constructor. (For invocation by subclass 
	 * constructors, typically implicit.)
	 */
	public PerFieldSimilarityWrapper() {}

	@Override
	public final void computeNorm(IFieldState state, INorm norm) {
		get(state.getName()).computeNorm(state, norm);
	}

	@Override
	public final ISimilarityWeight computeWeight(float queryBoost, 
			ICollectionStatistics collectionStats, ITermStatistics... termStats) {
		PerFieldSimWeight weight = new PerFieldSimWeight();
		weight.mDelegate = get(collectionStats.getField());
		weight.mDelegateWeight = weight.mDelegate.computeWeight(
				queryBoost, collectionStats, termStats);
		return weight;
	}

	@Override
	public final IExactSimilarityScorer getExactSimilarityScorer(ISimilarityWeight weight, 
			IAtomicReaderRef context) throws IOException {
		PerFieldSimWeight perFieldWeight = (PerFieldSimWeight) weight;
		return perFieldWeight.mDelegate.getExactSimilarityScorer(
				perFieldWeight.mDelegateWeight, context);
	}

	@Override
	public final ISloppySimilarityScorer getSloppySimilarityScorer(ISimilarityWeight weight, 
			IAtomicReaderRef context) throws IOException {
		PerFieldSimWeight perFieldWeight = (PerFieldSimWeight) weight;
		return perFieldWeight.mDelegate.getSloppySimilarityScorer(
				perFieldWeight.mDelegateWeight, context);
	}
  
	/** 
	 * Returns a {@link Similarity} for scoring a field.
	 */
	public abstract Similarity get(String name);
  
	static class PerFieldSimWeight extends SimilarityWeight {
		private ISimilarity mDelegate;
		private ISimilarityWeight mDelegateWeight;
    
		@Override
		public float getValueForNormalization() {
			return mDelegateWeight.getValueForNormalization();
		}
    
		@Override
		public void normalize(float queryNorm, float topLevelBoost) {
			mDelegateWeight.normalize(queryNorm, topLevelBoost);
		}
	}
	
}
