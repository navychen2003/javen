package org.javenstudio.common.indexdb.search;

import org.javenstudio.common.indexdb.IScoreDoc;
import org.javenstudio.common.indexdb.ITopDocs;

/** 
 * Represents hits returned by {@link
 * IndexSearcher#search(Query,Filter,int)} and {@link
 * IndexSearcher#search(Query,int)}. 
 */
public class TopDocs implements ITopDocs {

	/** The total number of hits for the query. */
	private int mTotalHits;

	/** The top hits for the query. */
	private IScoreDoc[] mScoreDocs;

	/** Stores the maximum score value encountered, needed for normalizing. */
	private float mMaxScore;

	/** Constructs a TopDocs with a default maxScore=Float.NaN. */
	public TopDocs(int totalHits, IScoreDoc[] scoreDocs) {
		this(totalHits, scoreDocs, Float.NaN);
	}

	public TopDocs(int totalHits, IScoreDoc[] scoreDocs, float maxScore) {
		mTotalHits = totalHits;
		mScoreDocs = scoreDocs;
		mMaxScore = maxScore;
	}

	/**
	 * Returns the maximum score value encountered. Note that in case
	 * scores are not tracked, this returns {@link Float#NaN}.
	 */
	public float getMaxScore() {
		return mMaxScore;
	}
  
	/** Sets the maximum score value encountered. */
	public void setMaxScore(float maxScore) {
		mMaxScore = maxScore;
	}
	
	public int getScoreDocsSize() { 
		return mScoreDocs != null ? mScoreDocs.length : 0;
	}
	
	public IScoreDoc getScoreDocAt(int index) { 
		return mScoreDocs[index];
	}
	
	/** @return The top hits for the query. */
	public IScoreDoc[] getScoreDocs() { 
		return mScoreDocs;
	}
	
	/** The total number of hits for the query. */
	public int getTotalHits() { 
		return mTotalHits;
	}
	
}
