package org.javenstudio.falcon.util;

import java.util.ArrayList;

/**
 * Represent a list of ResultDocuments returned from a search.  This includes
 * position and offset information.
 * 
 * @since 1.3
 */
public class ResultList extends ArrayList<ResultItem> {
	private static final long serialVersionUID = 1L;
	
	private long mNumFound = 0;
	private long mStart = 0;
	private Float mMaxScore = null;
  
	public Float getMaxScore() {
		return mMaxScore;
	}
  
	public void setMaxScore(Float maxScore) {
		mMaxScore = maxScore;
	}
  
	public long getNumFound() {
		return mNumFound;
	}
  
	public void setNumFound(long numFound) {
		mNumFound = numFound;
	}
  
	public long getStart() {
		return mStart;
	}
  
	public void setStart(long start) {
		mStart = start;
	}

	@Override
	public String toString() {
		return "ResultList{numFound=" + mNumFound
				+ ",start=" + mStart
				+ (mMaxScore != null ? ",maxScore=" + mMaxScore : "")
				+ ",docs=" + super.toString()
				+ "}";
	}
	
}
