package org.javenstudio.common.indexdb;

public interface ITopDocs {

	/**
	 * Returns the maximum score value encountered. Note that in case
	 * scores are not tracked, this returns {@link Float#NaN}.
	 */
	public float getMaxScore();
	
	/** @return The top hits for the query. */
	public IScoreDoc[] getScoreDocs();
	
	/** The total number of hits for the query. */
	public int getTotalHits();
	
	/** @return The top hits size for the query. */
	public int getScoreDocsSize();
	
	/** @return The top hit at index for the query. */
	public IScoreDoc getScoreDocAt(int index);
	
}
