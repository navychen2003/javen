package org.javenstudio.hornet.search.hits;

import java.io.IOException;

import org.javenstudio.common.indexdb.IFieldComparator;
import org.javenstudio.common.indexdb.IScoreDoc;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.ITopDocs;
import org.javenstudio.common.indexdb.search.FieldComparator;
import org.javenstudio.common.indexdb.search.FieldDoc;
import org.javenstudio.common.indexdb.util.PriorityQueue;

final class MergeSortQueue extends PriorityQueue<ShardRef> {

	// These are really FieldDoc instances:
	final IScoreDoc[][] mShardHits;
	final IFieldComparator<?>[] mComparators;
	final int[] mReverseMul;

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
						throw new IllegalArgumentException("shard " + shardIDX + 
								" was not sorted by the provided Sort (expected FieldDoc but got ScoreDoc)");
					}
					final FieldDoc fd = (FieldDoc) sd;
					if (fd.getFields() == null) {
						throw new IllegalArgumentException("shard " + shardIDX + 
								" did not set sort field values (FieldDoc.fields is null); " + 
								"you must pass fillFields=true to IndexSearcher.search on each shard");
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
	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	public boolean lessThan(ShardRef first, ShardRef second) {
		assert first != second;
		
		final FieldDoc firstFD = (FieldDoc) mShardHits[first.getShardIndex()][first.getHitIndex()];
		final FieldDoc secondFD = (FieldDoc) mShardHits[second.getShardIndex()][second.getHitIndex()];
		
		for (int compIDX=0; compIDX < mComparators.length; compIDX++) {
			final IFieldComparator comp = mComparators[compIDX];
			
			final int cmp = mReverseMul[compIDX] * comp.compareValues(
					firstFD.getFieldAt(compIDX), secondFD.getFieldAt(compIDX));
    
			if (cmp != 0) 
				return cmp < 0;
		}

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
