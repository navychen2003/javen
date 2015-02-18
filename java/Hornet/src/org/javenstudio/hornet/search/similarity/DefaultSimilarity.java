package org.javenstudio.hornet.search.similarity;

import org.javenstudio.common.indexdb.IFieldState;
import org.javenstudio.common.indexdb.INorm;
import org.javenstudio.common.indexdb.util.BytesRef;

/** Expert: Default scoring implementation. */
public class DefaultSimilarity extends TFIDFSimilarity {
  
	/** Implemented as <code>overlap / maxOverlap</code>. */
	@Override
	public float coord(int overlap, int maxOverlap) {
		return overlap / (float)maxOverlap;
	}

	/** Implemented as <code>1/sqrt(sumOfSquaredWeights)</code>. */
	@Override
	public float queryNorm(float sumOfSquaredWeights) {
		return (float)(1.0 / Math.sqrt(sumOfSquaredWeights));
	}
  
	/** 
	 * Implemented as
	 *  <code>state.getBoost()*lengthNorm(numTerms)</code>, where
	 *  <code>numTerms</code> is {@link FieldInvertState#getLength()} if {@link
	 *  #setDiscountOverlaps} is false, else it's {@link
	 *  FieldInvertState#getLength()} - {@link
	 *  FieldInvertState#getNumOverlap()}.
	 */
	@Override
	public void computeNorm(IFieldState state, INorm norm) {
		final int numTerms;
		if (mDiscountOverlaps)
			numTerms = state.getLength() - state.getNumOverlap();
		else
			numTerms = state.getLength();
		norm.setByte(encodeNormValue(state.getBoost() * ((float) (1.0 / Math.sqrt(numTerms)))));
	}

	/** Implemented as <code>sqrt(freq)</code>. */
	@Override
	public float tf(float freq) {
		return (float)Math.sqrt(freq);
	}
    
	/** Implemented as <code>1 / (distance + 1)</code>. */
	@Override
	public float sloppyFreq(int distance) {
		return 1.0f / (distance + 1);
	}
  
	/** The default implementation returns <code>1</code> */
	@Override
	public float scorePayload(int doc, int start, int end, BytesRef payload) {
		return 1;
	}

	/** Implemented as <code>log(numDocs/(docFreq+1)) + 1</code>. */
	@Override
	public float idf(long docFreq, long numDocs) {
		return (float)(Math.log(numDocs/(double)(docFreq+1)) + 1.0);
	}
    
	// Default true
	protected boolean mDiscountOverlaps = true;

	/** 
	 * Determines whether overlap tokens (Tokens with
	 *  0 position increment) are ignored when computing
	 *  norm.  By default this is true, meaning overlap
	 *  tokens do not count when computing norms.
	 *
	 *  @see #computeNorm
	 */
	public void setDiscountOverlaps(boolean v) {
		mDiscountOverlaps = v;
	}

	/** @see #setDiscountOverlaps */
	public boolean getDiscountOverlaps() {
		return mDiscountOverlaps;
	}

	@Override
	public String toString() {
		return "DefaultSimilarity";
	}
	
}
