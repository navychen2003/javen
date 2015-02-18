package org.javenstudio.hornet.search.hits;

import java.io.IOException;

import org.javenstudio.common.indexdb.IScoreDoc;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.ITopDocs;
import org.javenstudio.common.indexdb.search.ScoreDoc;
import org.javenstudio.common.indexdb.search.Sort;
import org.javenstudio.common.indexdb.search.TopDocs;
import org.javenstudio.common.indexdb.search.TopFieldDocs;
import org.javenstudio.common.indexdb.util.PriorityQueue;
import org.javenstudio.hornet.search.collector.TopFieldCollector;

public class HitHelper {

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
	public static ITopDocs merge(ISort sort, int topN, ITopDocs[] shardHits) throws IOException {
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
			
			if (shard.getScoreDocsSize() > 0) {
				availHitCount += shard.getScoreDocsSize();
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
			final ScoreDoc hit = (ScoreDoc)shardHits[ref.getShardIndex()].getScoreDocAt(ref.mHitIndex++);
			hit.setShardIndex(ref.getShardIndex());
			hits[hitUpto] = hit;

			hitUpto ++;

			if (ref.getHitIndex() < shardHits[ref.getShardIndex()].getScoreDocsSize()) {
				// Not done with this these TopDocs yet:
				queue.add(ref);
			}
		}

		if (sort == null) {
			return new TopDocs(totalHitCount, hits, maxScore);
		} else {
			return new TopFieldDocs(totalHitCount, hits, sort.getSortFields(), maxScore);
		}
	}
	
}
