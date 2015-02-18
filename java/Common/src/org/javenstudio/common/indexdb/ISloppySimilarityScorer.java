package org.javenstudio.common.indexdb;

import org.javenstudio.common.indexdb.util.BytesRef;

/**
 * API for scoring "sloppy" queries such as {@link SpanQuery} and 
 * sloppy {@link PhraseQuery}.
 * <p>
 * Frequencies are floating-point values: an approximate 
 * within-document frequency adjusted for "sloppiness" by 
 * {@link SloppySimScorer#computeSlopFactor(int)}.
 */
public interface ISloppySimilarityScorer {

	/**
     * Score a single document
     * @param doc document id within the inverted index segment
     * @param freq sloppy term frequency
     * @return document's score
     */
	public float score(int doc, float freq);

    /** Computes the amount of a sloppy phrase match, based on an edit distance. */
    public float computeSlopFactor(int distance);
    
    /** Calculate a scoring factor based on the data in the payload. */
    public float computePayloadFactor(int doc, int start, int end, BytesRef payload);
	
    /**
     * Explain the score for a single document
     * @param doc document id within the inverted index segment
     * @param freq Explanation of how the sloppy term frequency was computed
     * @return document's score
     */
    public IExplanation explain(int doc, IExplanation freq);
    
}
