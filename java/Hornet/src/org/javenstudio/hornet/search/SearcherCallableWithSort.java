package org.javenstudio.hornet.search;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IScoreDoc;
import org.javenstudio.common.indexdb.search.FieldDoc;
import org.javenstudio.common.indexdb.search.IndexSearcher;
import org.javenstudio.common.indexdb.search.Scorer;
import org.javenstudio.common.indexdb.search.Sort;
import org.javenstudio.common.indexdb.search.TopFieldDocs;
import org.javenstudio.common.indexdb.search.Weight;
import org.javenstudio.hornet.search.collector.TopFieldCollector;

/**
 * A thread subclass for searching a single searchable 
 */
final class SearcherCallableWithSort implements Callable<TopFieldDocs> {

	private final FakeScorer mFakeScorer = new FakeScorer();
	private final Lock mLock;
	private final IndexSearcher mSearcher;
	private final Weight mWeight;
	private final int mNumDocs;
	private final TopFieldCollector mQueue;
	private final Sort mSort;
	private final LeafSlice mSlice;
	private final FieldDoc mAfter;
	private final boolean mDoDocScores;
	private final boolean mDoMaxScore;

	public SearcherCallableWithSort(Lock lock, IndexSearcher searcher, 
			LeafSlice slice, Weight weight, FieldDoc after, int nDocs, TopFieldCollector hq, 
			Sort sort, boolean doDocScores, boolean doMaxScore) {
		mLock = lock;
		mSearcher = searcher;
		mWeight = weight;
		mNumDocs = nDocs;
		mQueue = hq;
		mSort = sort;
		mSlice = slice;
		mAfter = after;
		mDoDocScores = doDocScores;
		mDoMaxScore = doMaxScore;
	}

	private final class FakeScorer extends Scorer {
		private float mScore;
		private int mDoc;

		public FakeScorer() {
			super(null);
		}
  
		@Override
		public int advance(int target) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getDocID() {
			return mDoc;
		}

		@Override
		public float getFreq() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int nextDoc() {
			throw new UnsupportedOperationException();
		}
  
		@Override
		public float getScore() {
			return mScore;
		}
	}

	@Override
	public TopFieldDocs call() throws IOException {
		assert mSlice.mLeaves.length == 1;
		
		final TopFieldDocs docs = (TopFieldDocs)mSearcher.search(
				Arrays.asList(mSlice.mLeaves), mWeight, mAfter, mNumDocs, mSort, true, 
				mDoDocScores, mDoMaxScore);
		
		mLock.lock();
		try {
			final IAtomicReaderRef ctx = mSlice.mLeaves[0];
			final int base = ctx.getDocBase();
			
			mQueue.setNextReader(ctx);
			mQueue.setScorer(mFakeScorer);
			
			for (IScoreDoc scoreDoc : docs.getScoreDocs()) {
				mFakeScorer.mDoc = scoreDoc.getDoc() - base;
				mFakeScorer.mScore = scoreDoc.getScore();
				
				mQueue.collect(scoreDoc.getDoc()-base);
			}

			// Carry over maxScore from sub:
			if (mDoMaxScore && docs.getMaxScore() > mQueue.getMaxScore()) 
				mQueue.setMaxScore(docs.getMaxScore());
			
		} finally {
			mLock.unlock();
		}
		
		return docs;
	}
	
}
