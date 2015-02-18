package org.javenstudio.hornet.search.collector;

import java.io.IOException;

import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.search.Scorer;
import org.javenstudio.hornet.search.cache.FieldValueHitQueue;

/**
 * Implements a TopFieldCollector over multiple SortField criteria, with
 * tracking document scores and maxScore.
 */
class MultiComparatorScoringMaxScoreCollector extends MultiComparatorNonScoringCollector {
	
	protected Scorer mScorer;
  
	public MultiComparatorScoringMaxScoreCollector(
			FieldValueHitQueue<FieldValueHitQueue.Entry> queue,
			int numHits, boolean fillFields) throws IOException {
		super(queue, numHits, fillFields);
		// Must set maxScore to NEG_INF, or otherwise Math.max always returns NaN.
		mMaxScore = Float.NEGATIVE_INFINITY;
	}
  
	final void updateBottom(int doc, float score) {
		mBottom.setDoc(mDocBase + doc);
		mBottom.setScore(score);
		mBottom =  mQueue.updateTop();
	}

	@Override
	public void collect(int doc) throws IOException {
		final float score = mScorer.getScore();
		if (score > mMaxScore) 
			mMaxScore = score;
		
		++ mTotalHits;
		if (mQueueFull) {
			// Fastmatch: return if this hit is not competitive
			for (int i = 0;; i++) {
				final int c = mReverseMul[i] * mComparators[i].compareBottom(doc);
				if (c < 0) {
					// Definitely not competitive.
					return;
				} else if (c > 0) {
					// Definitely competitive.
					break;
				} else if (i == mComparators.length - 1) {
					// Here c=0. If we're at the last comparator, this doc is not
					// competitive, since docs are visited in doc Id order, which means
					// this doc cannot compete with any other document in the queue.
					return;
				}
			}

			// This hit is competitive - replace bottom element in queue & adjustTop
			for (int i = 0; i < mComparators.length; i++) {
				mComparators[i].copy(mBottom.getSlot(), doc);
			}

			updateBottom(doc, score);

			for (int i = 0; i < mComparators.length; i++) {
				mComparators[i].setBottom(mBottom.getSlot());
			}
			
		} else {
			// Startup transient: queue hasn't gathered numHits yet
			final int slot = mTotalHits - 1;
			// Copy hit into queue
			for (int i = 0; i < mComparators.length; i++) {
				mComparators[i].copy(slot, doc);
			}
			
			add(slot, doc, score);
			if (mQueueFull) {
				for (int i = 0; i < mComparators.length; i++) {
					mComparators[i].setBottom(mBottom.getSlot());
				}
			}
		}
	}

	@Override
	public void setScorer(IScorer scorer) throws IOException {
		mScorer = (Scorer)scorer;
		super.setScorer((Scorer)scorer);
	}
	
}
