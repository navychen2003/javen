package org.javenstudio.common.indexdb;

import org.javenstudio.common.indexdb.util.BytesRef;

/**
 * Contains statistics for a specific term
 */
public interface ITermStatistics {

	/** returns the term text */
	public BytesRef getTerm();
	
	/** 
	 * returns the number of documents this term occurs in 
	 * @see AtomicReader#docFreq(String, BytesRef) 
	 */
	public long getDocFreq();
	
	/** 
	 * returns the total number of occurrences of this term
	 * @see AtomicReader#totalTermFreq(String, BytesRef) 
	 */
	public long getTotalTermFreq();
	
}
