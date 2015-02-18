package org.javenstudio.hornet.search.hits;

import org.javenstudio.common.indexdb.IScoreDoc;
import org.javenstudio.common.indexdb.ITopDocs;
import org.javenstudio.common.indexdb.search.ScoreDoc;
import org.javenstudio.common.indexdb.util.PriorityQueue;

// Specialized MergeSortQueue that just merges by
// relevance score, descending:
final class ScoreMergeSortQueue extends PriorityQueue<ShardRef> {
	
	final IScoreDoc[][] mShardHits;

	public ScoreMergeSortQueue(ITopDocs[] shardHits) {
		super(shardHits.length);
		mShardHits = new ScoreDoc[shardHits.length][];
		for (int shardIDX=0; shardIDX < shardHits.length; shardIDX++) {
			mShardHits[shardIDX] = shardHits[shardIDX].getScoreDocs();
		}
	}

	// Returns true if first is < second
	@Override
	public boolean lessThan(ShardRef first, ShardRef second) {
		assert first != second;
		final float firstScore = mShardHits[first.getShardIndex()][first.getHitIndex()].getScore();
		final float secondScore = mShardHits[second.getShardIndex()][second.getHitIndex()].getScore();

		if (firstScore < secondScore) {
			return false;
		} else if (firstScore > secondScore) {
			return true;
		} else {
			// Tie break: earlier shard wins
			if (first.getShardIndex() < second.getShardIndex()) {
				return true;
			} else if (first.getShardIndex() > second.getShardIndex()) {
				return false;
			} else {
				// Tie break in same shard: resolve however the
				// shard had resolved it:
				assert first.getHitIndex() != second.getHitIndex();
				return first.getHitIndex() < second.getHitIndex();
			}
		}
	}
	
}
