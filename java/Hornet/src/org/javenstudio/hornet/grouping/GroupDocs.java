package org.javenstudio.hornet.grouping;

import org.javenstudio.common.indexdb.IScoreDoc;

/** 
 * Represents one group in the results.
 */
public class GroupDocs<GT> {
	
	/** 
	 * The groupField value for all docs in this group; this
	 *  may be null if hits did not have the groupField. 
	 */
	private final GT mGroupValue;

	/** Max score in this group */
	private final float mMaxScore;

	/** 
	 * Overall aggregated score of this group (currently only
	 *  set by join queries). 
	 */
	private final float mScore;

	/** 
	 * Hits; this may be {@link
	 * org.apache.lucene.search.FieldDoc} instances if the
	 * withinGroupSort sorted by fields. 
	 */
	private final IScoreDoc[] mScoreDocs;

	/** Total hits within this group */
	private final int mTotalHits;

	/** 
	 * Matches the groupSort passed to {@link
	 *  AbstractFirstPassGroupingCollector}. 
	 */
	private final Object[] mGroupSortValues;

	public GroupDocs(float score, float maxScore, int totalHits,
			IScoreDoc[] scoreDocs, GT groupValue, Object[] groupSortValues) {
		mScore = score;
		mMaxScore = maxScore;
		mTotalHits = totalHits;
		mScoreDocs = scoreDocs;
		mGroupValue = groupValue;
		mGroupSortValues = groupSortValues;
	}
	
	public final GT getGroupValue() { return mGroupValue; }
	public final IScoreDoc[] getScoreDocs() { return mScoreDocs; }
	public final Object[] getGroupSortValues() { return mGroupSortValues; }
	
	public final float getMaxScore() { return mMaxScore; }
	public final float getScore() { return mScore; }
	public final int getTotalHits() { return mTotalHits; }
	
	public final int getScoreDocsSize() { 
		return mScoreDocs != null ? mScoreDocs.length : 0; 
	}
	
	public final IScoreDoc getScoreDocAt(int index) { 
		return mScoreDocs != null ? mScoreDocs[index] : null; 
	}
	
}
