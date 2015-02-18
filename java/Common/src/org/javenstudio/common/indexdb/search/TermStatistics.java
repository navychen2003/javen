package org.javenstudio.common.indexdb.search;

import org.javenstudio.common.indexdb.ITermStatistics;
import org.javenstudio.common.indexdb.util.BytesRef;

/**
 * Contains statistics for a specific term
 */
public class TermStatistics implements ITermStatistics {
	
	private final BytesRef mTerm;
	private final long mDocFreq;
	private final long mTotalTermFreq;
  
	public TermStatistics(BytesRef term, long docFreq, long totalTermFreq) {
		assert docFreq >= 0;
		assert totalTermFreq == -1 || totalTermFreq >= docFreq; // #positions must be >= #postings
		
		mTerm = term;
		mDocFreq = docFreq;
		mTotalTermFreq = totalTermFreq;
	}
  
	/** returns the term text */
	public final BytesRef getTerm() {
		return mTerm;
	}
  
	/** 
	 * returns the number of documents this term occurs in 
	 * @see AtomicReader#docFreq(String, BytesRef) 
	 */
	public final long getDocFreq() {
		return mDocFreq;
	}
  
	/** 
	 * returns the total number of occurrences of this term
	 * @see AtomicReader#totalTermFreq(String, BytesRef) 
	 */
	public final long getTotalTermFreq() {
		return mTotalTermFreq;
	}
	
}
