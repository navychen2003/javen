package org.javenstudio.common.indexdb;

/** 
 * Stores the weight for a query across the indexed collection. This abstract
 * implementation is empty; descendants of {@code Similarity} should
 * subclass {@code SimWeight} and define the statistics they require in the
 * subclass. Examples include idf, average field length, etc.
 */
public interface ISimilarityWeight {
  
	/** 
	 * The value for normalization of contained query clauses (e.g. sum of squared weights).
	 * <p>
	 * NOTE: a Similarity implementation might not use any query normalization at all,
	 * its not required. However, if it wants to participate in query normalization,
	 * it can return a value here.
	 */
	public float getValueForNormalization();
  
	/** 
	 * Assigns the query normalization factor and boost from parent queries to this.
	 * <p>
	 * NOTE: a Similarity implementation might not use this normalized value at all,
	 * its not required. However, its usually a good idea to at least incorporate 
	 * the topLevelBoost (e.g. from an outer BooleanQuery) into its score.
	 */
	public void normalize(float queryNorm, float topLevelBoost);
	
}
