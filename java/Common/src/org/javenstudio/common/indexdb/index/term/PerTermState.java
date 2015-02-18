package org.javenstudio.common.indexdb.index.term;

import org.javenstudio.common.indexdb.ITermState;

/**
 * Holder for per-term statistics.
 * 
 * @see TermsEnum#docFreq
 * @see TermsEnum#totalTermFreq
 */
public class PerTermState extends TermState {

	private int mDocFreq;
	private long mTotalTermFreq;

	public PerTermState(int docFreq, long totalTermFreq) {
		mDocFreq = docFreq;
		mTotalTermFreq = totalTermFreq;
	}
	
	public final int getDocFreq() { return mDocFreq; }
	public final long getTotalTermFreq() { return mTotalTermFreq; }
	
	@Override
	public void copyFrom(ITermState other) {
		assert other instanceof PerTermState : "can not copy from " + other.getClass().getName();
		mDocFreq = ((PerTermState) other).mDocFreq;
		mTotalTermFreq = ((PerTermState) other).mTotalTermFreq;
	}

	@Override
	public String toString() {
		return "PerTermState{docFreq=" + mDocFreq + " totalTermFreq=" + mTotalTermFreq + "}";
	}
	
}
