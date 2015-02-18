package org.javenstudio.hornet.grouping;

import java.io.IOException;

import org.javenstudio.common.indexdb.IScoreDoc;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.ITopDocs;
import org.javenstudio.common.indexdb.search.ScoreDoc;
import org.javenstudio.common.indexdb.search.TopDocs;
import org.javenstudio.hornet.search.TopDocsHelper;

/** 
 * Represents result returned by a grouping search.
 */
public class TopGroups<GT> {
	
	/** Number of documents matching the search */
	private final int mTotalHitCount;

	/** Number of documents grouped into the topN groups */
	private final int mTotalGroupedHitCount;

	/** The total number of unique groups. If <code>null</code> this value is not computed. */
	private final Integer mTotalGroupCount;

	/** Group results in groupSort order */
	private final GroupDocs<GT>[] mGroups;

	/** How groups are sorted against each other */
	private final ISortField[] mGroupSort;

	/** How docs are sorted within each group */
	private final ISortField[] mWithinGroupSort;

	/** 
	 * Highest score across all hits, or
	 *  <code>Float.NaN</code> if scores were not computed. 
	 */
	private final float mMaxScore;

	public final int getTotalHitCount() { return mTotalHitCount; }
	public final int getTotalGroupedHitCount() { return mTotalGroupedHitCount; }
	public final Integer getTotalGroupCount() { return mTotalGroupCount; }
	
	public final GroupDocs<GT>[] getGroupDocs() { return mGroups; }
	public final ISortField[] getGroupSort() { return mGroupSort; }
	public final ISortField[] getWithinGroupSort() { return mWithinGroupSort; }
	
	public TopGroups(ISortField[] groupSort, ISortField[] withinGroupSort, 
			int totalHitCount, int totalGroupedHitCount, GroupDocs<GT>[] groups, float maxScore) {
		mGroupSort = groupSort;
		mWithinGroupSort = withinGroupSort;
		mTotalHitCount = totalHitCount;
		mTotalGroupedHitCount = totalGroupedHitCount;
		mGroups = groups;
		mTotalGroupCount = null;
		mMaxScore = maxScore;
	}

	public TopGroups(TopGroups<GT> oldTopGroups, Integer totalGroupCount) {
		mGroupSort = oldTopGroups.mGroupSort;
		mWithinGroupSort = oldTopGroups.mWithinGroupSort;
		mTotalHitCount = oldTopGroups.mTotalHitCount;
		mTotalGroupedHitCount = oldTopGroups.mTotalGroupedHitCount;
		mGroups = oldTopGroups.mGroups;
		mMaxScore = oldTopGroups.mMaxScore;
		mTotalGroupCount = totalGroupCount;
	}

	/** 
	 * Merges an array of TopGroups, for example obtained
	 *  from the second-pass collector across multiple
	 *  shards.  Each TopGroups must have been sorted by the
	 *  same groupSort and docSort, and the top groups passed
	 *  to all second-pass collectors must be the same.
	 *
	 * <b>NOTE</b>: We can't always compute an exact totalGroupCount.
	 * Documents belonging to a group may occur on more than
	 * one shard and thus the merged totalGroupCount can be
	 * higher than the actual totalGroupCount. In this case the
	 * totalGroupCount represents a upper bound. If the documents
	 * of one group do only reside in one shard then the
	 * totalGroupCount is exact.
	 *
	 * <b>NOTE</b>: the topDocs in each GroupDocs is actually
	 * an instance of TopDocsAndShards
	 */
	@SuppressWarnings("unchecked")
	public static <T> TopGroups<T> merge(TopGroups<T>[] shardGroups, 
			ISort groupSort, ISort docSort, int docOffset, int docTopN, ScoreMergeMode scoreMergeMode)
			throws IOException {
		if (shardGroups.length == 0) 
			return null;

		int totalHitCount = 0;
		int totalGroupedHitCount = 0;
		
		// Optionally merge the totalGroupCount.
		Integer totalGroupCount = null;

		final int numGroups = shardGroups[0].mGroups.length;
    
		for (TopGroups<T> shard : shardGroups) {
			if (numGroups != shard.mGroups.length) {
				throw new IllegalArgumentException("number of groups differs across shards; " 
						+ "you must pass same top groups to all shards' second-pass collector");
			}
			
			totalHitCount += shard.mTotalHitCount;
			totalGroupedHitCount += shard.mTotalGroupedHitCount;
			
			if (shard.mTotalGroupCount != null) {
				if (totalGroupCount == null) 
					totalGroupCount = 0;

				totalGroupCount += shard.mTotalGroupCount;
			}
		}

		final GroupDocs<T>[] mergedGroupDocs = new GroupDocs[numGroups];
		final TopDocs[] shardTopDocs = new TopDocs[shardGroups.length];
		float totalMaxScore = Float.MIN_VALUE;

		for (int groupIDX=0; groupIDX < numGroups; groupIDX++) {
			final T groupValue = shardGroups[0].mGroups[groupIDX].getGroupValue();
      
			float maxScore = Float.MIN_VALUE;
			int totalHits = 0;
			double scoreSum = 0.0;
			
			for (int shardIDX=0; shardIDX < shardGroups.length; shardIDX++) {
				final TopGroups<T> shard = shardGroups[shardIDX];
				final GroupDocs<?> shardGroupDocs = shard.mGroups[groupIDX];
				
				if (groupValue == null) {
					if (shardGroupDocs.getGroupValue() != null) {
						throw new IllegalArgumentException("group values differ across shards; " 
								+ "you must pass same top groups to all shards' second-pass collector");
					}
				} else if (!groupValue.equals(shardGroupDocs.getGroupValue())) {
					throw new IllegalArgumentException("group values differ across shards; " 
							+ "you must pass same top groups to all shards' second-pass collector");
				}

				shardTopDocs[shardIDX] = new TopDocs(shardGroupDocs.getTotalHits(),
						shardGroupDocs.getScoreDocs(), shardGroupDocs.getMaxScore());
				
				maxScore = Math.max(maxScore, shardGroupDocs.getMaxScore());
				totalHits += shardGroupDocs.getTotalHits();
				scoreSum += shardGroupDocs.getScore();
			}

			final ITopDocs mergedTopDocs = TopDocsHelper.merge(docSort, 
					docOffset + docTopN, shardTopDocs);

			// Slice;
			final IScoreDoc[] mergedScoreDocs;
			if (docOffset == 0) {
				mergedScoreDocs = mergedTopDocs.getScoreDocs();
				
			} else if (docOffset >= mergedTopDocs.getScoreDocs().length) {
				mergedScoreDocs = new ScoreDoc[0];
				
			} else {
				mergedScoreDocs = new ScoreDoc[mergedTopDocs.getScoreDocs().length - docOffset];
				System.arraycopy(mergedTopDocs.getScoreDocs(),
						docOffset,
						mergedScoreDocs,
						0,
						mergedTopDocs.getScoreDocs().length - docOffset);
			}

			final float groupScore;
			
			switch(scoreMergeMode) {
			case None:
				groupScore = Float.NaN;
				break;
				
			case Avg:
				if (totalHits > 0) 
					groupScore = (float) (scoreSum / totalHits);
				else 
					groupScore = Float.NaN;
				break;
				
			case Total:
				groupScore = (float) scoreSum;
				break;
				
			default:
				throw new IllegalArgumentException("can't handle ScoreMergeMode " 
						+ scoreMergeMode);
			}
        
			mergedGroupDocs[groupIDX] = new GroupDocs<T>(groupScore,
					maxScore, totalHits, mergedScoreDocs, groupValue,
					shardGroups[0].mGroups[groupIDX].getGroupSortValues());
			
			totalMaxScore = Math.max(totalMaxScore, maxScore);
		}

		if (totalGroupCount != null) {
			TopGroups<T> result = new TopGroups<T>(groupSort.getSortFields(),
					docSort == null ? null : docSort.getSortFields(),
					totalHitCount,
					totalGroupedHitCount,
					mergedGroupDocs,
					totalMaxScore);
			
			return new TopGroups<T>(result, totalGroupCount);
			
		} else {
			return new TopGroups<T>(groupSort.getSortFields(),
					docSort == null ? null : docSort.getSortFields(),
					totalHitCount,
					totalGroupedHitCount,
					mergedGroupDocs,
					totalMaxScore);
		}
	}
	
}
