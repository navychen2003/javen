package org.javenstudio.common.indexdb.index.term;

import org.javenstudio.common.indexdb.ITermState;

/**
 * Holder for merge-term total statistics.
 * 
 * @see TermsEnum#docFreq
 * @see TermsEnum#totalTermFreq
 */
public class MergeTermState extends TermState {

	private long mTotalTerms;
	private long mTotalDocFreq;
	private long mTotalTermFreq;
	
	public MergeTermState() { 
		this(0, 0, 0);
	}
	
	public MergeTermState(long terms, long docFreq, long termFreqs) { 
		mTotalTerms = terms;
		mTotalDocFreq = docFreq;
		mTotalTermFreq = termFreqs;
	}
	
	public final long getTotalTerms() { return mTotalTerms; }
	public final long getTotalDocFreq() { return mTotalDocFreq; }
	public final long getTotalTermFreq() { return mTotalTermFreq; }
	
	@Override
	public void copyFrom(ITermState other) {
		assert other instanceof MergeTermState : "can not copy from " + other.getClass().getName();
		mTotalTerms = ((MergeTermState) other).mTotalTerms;
		mTotalDocFreq = ((MergeTermState) other).mTotalDocFreq;
		mTotalTermFreq = ((MergeTermState) other).mTotalTermFreq;
	}

	public void mergeFrom(ITermState other) { 
		assert other instanceof MergeTermState : "can not merge from " + other.getClass().getName();
		mTotalTerms += ((MergeTermState) other).mTotalTerms;
		mTotalDocFreq += ((MergeTermState) other).mTotalDocFreq;
		mTotalTermFreq += ((MergeTermState) other).mTotalTermFreq;
	}
	
	@Override
	public String toString() {
		return "MergeTermState{totalTerms=" + mTotalTerms + " totalDocFreq=" 
				+ mTotalDocFreq + " totalTermFreq=" + mTotalTermFreq + "}";
	}
	
}
