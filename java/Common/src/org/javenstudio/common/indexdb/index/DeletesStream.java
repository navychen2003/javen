package org.javenstudio.common.indexdb.index;

import java.io.IOException;
import java.util.List;

import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ISegmentCommitInfo;
import org.javenstudio.common.indexdb.ISegmentInfos;

public abstract class DeletesStream {

	public static class QueryAndLimit {
		private final IQuery mQuery;
		private final int mLimit;
		
		public QueryAndLimit(IQuery query, int limit) {
			mQuery = query;
			mLimit = limit;
		}
		
		public IQuery getQuery() { return mQuery; }
		public int getLimit() { return mLimit; }
	}
	
	public static class ApplyResult {
		// True if any actual deletes took place:
		private final boolean mAnyDeletes;

		// Current gen, for the merged segment:
		private final long mGen;

		// If non-null, contains segments that are 100% deleted
		private final List<ISegmentCommitInfo> mAllDeleted;

		public ApplyResult(boolean anyDeletes, long gen, List<ISegmentCommitInfo> allDeleted) {
			mAnyDeletes = anyDeletes;
			mAllDeleted = allDeleted;
			mGen = gen;
		}
		
		public boolean anyDeletes() { return mAnyDeletes; }
		public long getGen() { return mGen; }
		public List<ISegmentCommitInfo> getAllDeleted() { return mAllDeleted; }
	}
	
	public abstract boolean any();
	public abstract int getNumTerms();
	public abstract long getBytesUsed();
	
	public abstract long push(FrozenDeletes packet);
	public abstract long nextGen();
	public abstract void clear();
	
	/** 
	 * Resolves the buffered deleted Term/Query/docIDs, into
	 *  actual deleted docIDs in the liveDocs MutableBits for
	 *  each SegmentReader. 
	 */
	public abstract ApplyResult applyDeletes(
			ReaderPool readerPool, List<ISegmentCommitInfo> infos) 
			throws IOException;
	
	/** 
	 * Removes any BufferedDeletes that we no longer need to
	 * store because all segments in the index have had the
	 * deletes applied. 
	 */
	public abstract void prune(ISegmentInfos segmentInfos);
	
}
