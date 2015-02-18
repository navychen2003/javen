package org.javenstudio.common.indexdb;

import java.io.IOException;

public interface ISimilarity {
	
	/** 
	 * Hook to integrate coordinate-level matching.
	 * <p>
	 * By default this is disabled (returns <code>1</code>), as with
	 * most modern models this will only skew performance, but some
	 * implementations such as {@link TFIDFSimilarity} override this.
	 *
	 * @param overlap the number of query terms matched in the document
	 * @param maxOverlap the total number of terms in the query
	 * @return a score factor based on term overlap with the query
	 */
	public float coord(int overlap, int maxOverlap);
  
	/** 
	 * Computes the normalization value for a query given the sum of the
	 * normalized weights {@link SimWeight#getValueForNormalization()} of 
	 * each of the query terms.  This value is passed back to the 
	 * weight ({@link SimWeight#normalize(float, float)} of each query 
	 * term, to provide a hook to attempt to make scores from different
	 * queries comparable.
	 * <p>
	 * By default this is disabled (returns <code>1</code>), but some
	 * implementations such as {@link TFIDFSimilarity} override this.
	 * 
	 * @param valueForNormalization the sum of the term normalization values
	 * @return a normalization factor for query weights
	 */
	public float queryNorm(float valueForNormalization);
  
	/**
	 * Computes the normalization value for a field, given the accumulated
	 * state of term processing for this field (see {@link FieldInvertState}).
	 * 
	 * <p>Implementations should calculate a norm value based on the field
	 * state and set that value to the given {@link Norm}.
	 *
	 * <p>Matches in longer fields are less precise, so implementations of this
	 * method usually set smaller values when <code>state.getLength()</code> is large,
	 * and larger values when <code>state.getLength()</code> is small.
	 * 
	 * @param state current processing state for this field
	 * @param norm holds the computed norm value when this method returns
	 */
	public void computeNorm(IFieldState state, INorm norm);
  
	/**
	 * Compute any collection-level weight (e.g. IDF, average document length, etc) 
	 * needed for scoring a query.
	 *
	 * @param queryBoost the query-time boost.
	 * @param collectionStats collection-level statistics, such as the number of 
	 * 		tokens in the collection.
	 * @param termStats term-level statistics, such as the document frequency of 
	 * 		a term across the collection.
	 * @return SimWeight object with the information this Similarity needs to score a query.
	 */
	public ISimilarityWeight computeWeight(float queryBoost, 
			ICollectionStatistics collectionStats, ITermStatistics... termStats);
  
	/**
	 * Creates a new {@link Similarity.ExactSimScorer} to score matching documents 
	 * from a segment of the inverted index.
	 * @param weight collection information from 
	 * 		{@link #computeWeight(float, CollectionStatistics, TermStatistics...)}
	 * @param context segment of the inverted index to be scored.
	 * @return ExactSimScorer for scoring documents across <code>context</code>
	 * @throws IOException
	 */
	public IExactSimilarityScorer getExactSimilarityScorer(ISimilarityWeight weight, 
			IAtomicReaderRef context) throws IOException;
  
	/**
	 * Creates a new {@link Similarity.SloppySimScorer} to score matching documents 
	 * from a segment of the inverted index.
	 * @param weight collection information from 
	 * 		{@link #computeWeight(float, CollectionStatistics, TermStatistics...)}
	 * @param context segment of the inverted index to be scored.
	 * @return SloppySimScorer for scoring documents across <code>context</code>
	 * @throws IOException
	 */
	public ISloppySimilarityScorer getSloppySimilarityScorer(ISimilarityWeight weight, 
			IAtomicReaderRef context) throws IOException;
	
}
