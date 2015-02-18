package org.javenstudio.common.indexdb;

/**
 * Contains statistics for a collection (field)
 */
public interface ICollectionStatistics {

	/** returns the field name */
	public String getField();
	
	/** 
	 * returns the total number of documents, regardless of 
	 * whether they all contain values for this field. 
	 * @see IndexReader#maxDoc() 
	 */
	public long getMaxDoc();
	
	/** 
	 * returns the total number of documents that
	 * have at least one term for this field. 
	 * @see Terms#getDocCount() 
	 */
	public long getDocCount();
	
	/** 
	 * returns the total number of tokens for this field
	 * @see Terms#getSumTotalTermFreq() 
	 */
	public long getSumTotalTermFreq();
	
	/** 
	 * returns the total number of postings for this field 
	 * @see Terms#getSumDocFreq() 
	 */
	public long getSumDocFreq();
	
}
