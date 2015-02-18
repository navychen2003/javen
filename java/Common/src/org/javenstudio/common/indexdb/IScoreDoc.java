package org.javenstudio.common.indexdb;

public interface IScoreDoc {

	/** The score of this document for the query. */
	public float getScore();
	
	/** 
	 * A hit document's number.
	 * @see IndexSearcher#doc(int) 
	 */
	public int getDoc();
	
	/** Only set by {@link TopDocs#merge} */
	public int getShardIndex();
	
}
