package org.javenstudio.hornet.search;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;

import org.javenstudio.common.indexdb.IScoreDoc;
import org.javenstudio.common.indexdb.search.IndexSearcher;
import org.javenstudio.common.indexdb.search.ScoreDoc;
import org.javenstudio.common.indexdb.search.TopDocs;
import org.javenstudio.common.indexdb.search.Weight;
import org.javenstudio.hornet.search.hits.HitQueue;

/**
 * A thread subclass for searching a single searchable 
 */
final class SearcherCallableNoSort implements Callable<TopDocs> {

	private final Lock mLock;
	private final IndexSearcher mSearcher;
	private final Weight mWeight;
	private final ScoreDoc mAfter;
	private final int mNumDocs;
	private final HitQueue mQueue;
	private final LeafSlice mSlice;

	public SearcherCallableNoSort(Lock lock, IndexSearcher searcher, 
			LeafSlice slice,  Weight weight, ScoreDoc after, int nDocs, HitQueue hq) {
		mLock = lock;
		mSearcher = searcher;
		mWeight = weight;
		mAfter = after;
		mNumDocs = nDocs;
		mQueue = hq;
		mSlice = slice;
	}

	@Override
	public TopDocs call() throws IOException {
		final TopDocs docs = (TopDocs)mSearcher.search(
				Arrays.asList(mSlice.mLeaves), mWeight, mAfter, mNumDocs);
		final IScoreDoc[] scoreDocs = docs.getScoreDocs();
		//it would be so nice if we had a thread-safe insert 
		mLock.lock();
		try {
			for (int j = 0; j < scoreDocs.length; j++) { // merge scoreDocs into hq
				final IScoreDoc scoreDoc = scoreDocs[j];
				if (scoreDoc == mQueue.insertWithOverflow(scoreDoc)) 
					break;
			}
		} finally {
			mLock.unlock();
		}
		return docs;
	}
	
}
