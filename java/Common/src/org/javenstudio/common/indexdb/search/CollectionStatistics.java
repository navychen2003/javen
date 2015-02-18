package org.javenstudio.common.indexdb.search;

import org.javenstudio.common.indexdb.ICollectionStatistics;

/**
 * Contains statistics for a collection (field)
 */
public class CollectionStatistics implements ICollectionStatistics {
	
	private final String mField;
	private final long mMaxDoc;
	private final long mDocCount;
	private final long mSumTotalTermFreq;
	private final long mSumDocFreq;
  
	public CollectionStatistics(String field, long maxDoc, long docCount, 
			long sumTotalTermFreq, long sumDocFreq) {
		assert maxDoc >= 0;
		assert docCount >= -1 && docCount <= maxDoc; // #docs with field must be <= #docs
		assert sumDocFreq >= -1;
		assert sumTotalTermFreq == -1 || sumTotalTermFreq >= sumDocFreq; // #positions must be >= #postings
		
		mField = field;
		mMaxDoc = maxDoc;
		mDocCount = docCount;
		mSumTotalTermFreq = sumTotalTermFreq;
		mSumDocFreq = sumDocFreq;
	}
  
	/** returns the field name */
	public final String getField() {
		return mField;
	}
  
	/** 
	 * returns the total number of documents, regardless of 
	 * whether they all contain values for this field. 
	 * @see IndexReader#maxDoc() 
	 */
	public final long getMaxDoc() {
		return mMaxDoc;
	}
  
	/** 
	 * returns the total number of documents that
	 * have at least one term for this field. 
	 * @see Terms#getDocCount() 
	 */
	public final long getDocCount() {
		return mDocCount;
	}
  
	/** 
	 * returns the total number of tokens for this field
	 * @see Terms#getSumTotalTermFreq() 
	 */
	public final long getSumTotalTermFreq() {
		return mSumTotalTermFreq;
	}
  
	/** 
	 * returns the total number of postings for this field 
	 * @see Terms#getSumDocFreq() 
	 */
	public final long getSumDocFreq() {
		return mSumDocFreq;
	}
	
}
