package org.javenstudio.hornet.search.collector;

import java.io.IOException;

import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.search.Scorer;
import org.javenstudio.hornet.search.cache.FieldValueHitQueue;

/**
 * Implements a TopFieldCollector over one SortField criteria, with tracking
 * document scores and maxScore.
 */
class OneComparatorScoringMaxScoreCollector extends OneComparatorNonScoringCollector {
	
	protected Scorer mScorer;
  
	public OneComparatorScoringMaxScoreCollector(
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
			if ((mReverseMul * mComparator.compareBottom(doc)) <= 0) {
				// since docs are visited in doc Id order, if compare is 0, it means
				// this document is largest than anything else in the queue, and
				// therefore not competitive.
				return;
			}
      
			// This hit is competitive - replace bottom element in queue & adjustTop
			mComparator.copy(mBottom.getSlot(), doc);
			updateBottom(doc, score);
			mComparator.setBottom(mBottom.getSlot());
			
		} else {
			// Startup transient: queue hasn't gathered numHits yet
			final int slot = mTotalHits - 1;
			// Copy hit into queue
			mComparator.copy(slot, doc);
			
			add(slot, doc, score);
			if (mQueueFull) {
				mComparator.setBottom(mBottom.getSlot());
			}
		}
	}
  
	@Override
	public void setScorer(IScorer scorer) throws IOException {
		mScorer = (Scorer)scorer;
		super.setScorer((Scorer)scorer);
	}
	
}
