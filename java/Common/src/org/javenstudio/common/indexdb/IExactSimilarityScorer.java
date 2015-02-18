package org.javenstudio.common.indexdb;

/**
 * API for scoring exact queries such as {@link TermQuery} and 
 * exact {@link PhraseQuery}.
 * <p>
 * Frequencies are integers (the term or phrase frequency within the document)
 */
public interface IExactSimilarityScorer {

    /**
     * Score a single document
     * @param doc document id
     * @param freq term frequency
     * @return document's score
     */
    public float score(int doc, int freq);
    
    /**
     * Explain the score for a single document
     * @param doc document id
     * @param freq Explanation of how the term frequency was computed
     * @return document's score
     */
    public IExplanation explain(int doc, IExplanation freq);
	
}
