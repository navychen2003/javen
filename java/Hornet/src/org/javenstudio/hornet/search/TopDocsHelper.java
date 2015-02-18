package org.javenstudio.hornet.search;

import java.io.IOException;

import org.javenstudio.common.indexdb.IFieldComparator;
import org.javenstudio.common.indexdb.IScoreDoc;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.ITopDocs;
import org.javenstudio.common.indexdb.search.FieldComparator;
import org.javenstudio.common.indexdb.search.FieldDoc;
import org.javenstudio.common.indexdb.search.ScoreDoc;
import org.javenstudio.common.indexdb.search.Sort;
import org.javenstudio.common.indexdb.search.TopDocs;
import org.javenstudio.common.indexdb.search.TopFieldDocs;
import org.javenstudio.common.indexdb.util.PriorityQueue;

public class TopDocsHelper {

	// Refers to one hit:
	private static class ShardRef {
		// Which shard (index into shardHits[]):
		private final int mShardIndex;

		// Which hit within the shard:
		private int mHitIndex;

		public ShardRef(int shardIndex) {
			mShardIndex = shardIndex;
		}

		@Override
		public String toString() {
			return "ShardRef(shardIndex=" + mShardIndex 
					+ " hitIndex=" + mHitIndex + ")";
		}
	};

	// Specialized MergeSortQueue that just merges by
	// relevance score, descending:
	private static class ScoreMergeSortQueue extends PriorityQueue<ShardRef> {
		private final IScoreDoc[][] mShardHits;

		public ScoreMergeSortQueue(ITopDocs[] shardHits) {
			super(shardHits.length);
			mShardHits = new IScoreDoc[shardHits.length][];
			
			for (int shardIDX=0; shardIDX < shardHits.length; shardIDX++) {
				mShardHits[shardIDX] = shardHits[shardIDX].getScoreDocs();
			}
		}

		// Returns true if first is < second
		@Override
		public boolean lessThan(ShardRef first, ShardRef second) {
			assert first != second;
			final float firstScore = mShardHits[first.mShardIndex][first.mHitIndex].getScore();
			final float secondScore = mShardHits[second.mShardIndex][second.mHitIndex].getScore();

			if (firstScore < secondScore) {
				return false;
			} else if (firstScore > secondScore) {
				return true;
				
			} else {
				// Tie break: earlier shard wins
				if (first.mShardIndex < second.mShardIndex) {
					return true;
				} else if (first.mShardIndex > second.mShardIndex) {
					return false;
				} else {
					// Tie break in same shard: resolve however the
					// shard had resolved it:
					assert first.mHitIndex != second.mHitIndex;
					return first.mHitIndex < second.mHitIndex;
				}
			}
		}
	}

	@SuppressWarnings({"rawtypes","unchecked"})
	private static class MergeSortQueue extends PriorityQueue<ShardRef> {
		// These are really FieldDoc instances:
		private final IScoreDoc[][] mShardHits;
		private final IFieldComparator<?>[] mComparators;
		private final int[] mReverseMul;

		public MergeSortQueue(ISort sort, ITopDocs[] shardHits) throws IOException {
			super(shardHits.length);
			mShardHits = new IScoreDoc[shardHits.length][];
			
			for (int shardIDX=0; shardIDX < shardHits.length; shardIDX++) {
				final IScoreDoc[] shard = shardHits[shardIDX].getScoreDocs();
				if (shard != null) {
					mShardHits[shardIDX] = shard;
					// Fail gracefully if API is misused:
					for (int hitIDX=0; hitIDX < shard.length; hitIDX++) {
						final IScoreDoc sd = shard[hitIDX];
						if (!(sd instanceof FieldDoc)) {
							throw new IllegalArgumentException("shard " + shardIDX 
									+ " was not sorted by the provided Sort (expected FieldDoc but got ScoreDoc)");
						}
						
						final FieldDoc fd = (FieldDoc) sd;
						if (fd.getFields() == null) {
							throw new IllegalArgumentException("shard " + shardIDX 
									+ " did not set sort field values (FieldDoc.fields is null);" 
									+ " you must pass fillFields=true to IndexSearcher.search on each shard");
						}
					}
				}
			}

			final ISortField[] sortFields = sort.getSortFields();
			mComparators = new FieldComparator[sortFields.length];
			mReverseMul = new int[sortFields.length];
			
			for (int compIDX=0; compIDX < sortFields.length; compIDX++) {
				final ISortField sortField = sortFields[compIDX];
				mComparators[compIDX] = sortField.getComparator(1, compIDX);
				mReverseMul[compIDX] = sortField.getReverse() ? -1 : 1;
			}
		}

		// Returns true if first is < second
		@Override
		public boolean lessThan(ShardRef first, ShardRef second) {
			assert first != second;
			final FieldDoc firstFD = (FieldDoc) mShardHits[first.mShardIndex][first.mHitIndex];
			final FieldDoc secondFD = (FieldDoc) mShardHits[second.mShardIndex][second.mHitIndex];
      
			for (int compIDX=0; compIDX < mComparators.length; compIDX++) {
				final IFieldComparator comp = mComparators[compIDX];
				final int cmp = mReverseMul[compIDX] * 
						comp.compareValues(firstFD.getFieldAt(compIDX), secondFD.getFieldAt(compIDX));
        
				if (cmp != 0) 
					return cmp < 0;
			}

			// Tie break: earlier shard wins
			if (first.mShardIndex < second.mShardIndex) {
				return true;
			} else if (first.mShardIndex > second.mShardIndex) {
				return false;
				
			} else {
				// Tie break in same shard: resolve however the
				// shard had resolved it:
				assert first.mHitIndex != second.mHitIndex;
				return first.mHitIndex < second.mHitIndex;
			}
		}
	}
	
	/** 
	 * Returns a new TopDocs, containing topN results across
	 *  the provided TopDocs, sorting by the specified {@link
	 *  Sort}.  Each of the TopDocs must have been sorted by
	 *  the same Sort, and sort field values must have been
	 *  filled (ie, <code>fillFields=true</code> must be
	 *  passed to {@link
	 *  TopFieldCollector#create}.
	 *
	 * <p>Pass sort=null to merge sort by score descending.
	 */
	public static ITopDocs merge(ISort sort, int topN, ITopDocs[] shardHits) 
			throws IOException {
		final PriorityQueue<ShardRef> queue;
		if (sort == null) 
			queue = new ScoreMergeSortQueue(shardHits);
		else 
			queue = new MergeSortQueue(sort, shardHits);
		
		int totalHitCount = 0;
		int availHitCount = 0;
		float maxScore = Float.MIN_VALUE;
		
		for (int shardIDX=0; shardIDX < shardHits.length; shardIDX++) {
			final ITopDocs shard = shardHits[shardIDX];
			// totalHits can be non-zero even if no hits were
			// collected, when searchAfter was used:
			totalHitCount += shard.getTotalHits();
			
			if (shard.getScoreDocs() != null && shard.getScoreDocs().length > 0) {
				availHitCount += shard.getScoreDocs().length;
				queue.add(new ShardRef(shardIDX));
				maxScore = Math.max(maxScore, shard.getMaxScore());
			}
		}

		if (availHitCount == 0) 
			maxScore = Float.NaN;

		final IScoreDoc[] hits = new IScoreDoc[Math.min(topN, availHitCount)];
		int hitUpto = 0;
		
		while (hitUpto < hits.length) {
			assert queue.size() > 0;
			ShardRef ref = queue.pop();
			
			final ScoreDoc hit = (ScoreDoc)shardHits[ref.mShardIndex].getScoreDocAt(ref.mHitIndex++);
			hit.setShardIndex(ref.mShardIndex);
			
			hits[hitUpto] = hit;
			hitUpto++;

			if (ref.mHitIndex < shardHits[ref.mShardIndex].getScoreDocs().length) {
				// Not done with this these TopDocs yet:
				queue.add(ref);
			}
		}

		if (sort == null) 
			return new TopDocs(totalHitCount, hits, maxScore);
		else 
			return new TopFieldDocs(totalHitCount, hits, sort.getSortFields(), maxScore);
	}
	
}
