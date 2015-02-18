package org.javenstudio.hornet.search.collector;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IFieldComparator;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.search.Scorer;
import org.javenstudio.hornet.search.cache.FieldValueHitQueue;

/**
 * Implements a TopFieldCollector over one SortField criteria, without
 * tracking document scores and maxScore.
 */
class OneComparatorNonScoringCollector extends TopFieldCollector {

	protected final int mReverseMul;
	protected final FieldValueHitQueue<FieldValueHitQueue.Entry> mEntryQueue;
	protected IFieldComparator<?> mComparator;
  
	public OneComparatorNonScoringCollector(
			FieldValueHitQueue<FieldValueHitQueue.Entry> queue,
			int numHits, boolean fillFields) throws IOException {
		super(queue, numHits, fillFields);
		mEntryQueue = queue;
		mComparator = queue.getComparators()[0];
		mReverseMul = queue.getReverseMul()[0];
	}
  
	final void updateBottom(int doc) {
		// bottom.score is already set to Float.NaN in add().
		mBottom.setDoc(mDocBase + doc);
		mBottom = mQueue.updateTop();
	}

	@Override
	public void collect(int doc) throws IOException {
		++ mTotalHits;
		if (mQueueFull) {
			if ((mReverseMul * mComparator.compareBottom(doc)) <= 0) {
				// since docs are visited in doc Id order, if compare is 0, it means
				// this document is larger than anything else in the queue, and
				// therefore not competitive.
				return;
			}
      
			// This hit is competitive - replace bottom element in queue & adjustTop
			mComparator.copy(mBottom.getSlot(), doc);
			updateBottom(doc);
			mComparator.setBottom(mBottom.getSlot());
			
		} else {
			// Startup transient: queue hasn't gathered numHits yet
			final int slot = mTotalHits - 1;
			// Copy hit into queue
			mComparator.copy(slot, doc);
			
			add(slot, doc, Float.NaN);
			if (mQueueFull) 
				mComparator.setBottom(mBottom.getSlot());
		}
	}
  
	@Override
	public void setNextReader(IAtomicReaderRef context) throws IOException {
		mDocBase = context.getDocBase();
		mEntryQueue.setComparator(0, mComparator.setNextReader(context));
		mComparator = mEntryQueue.getFirstComparator();
	}
  
	@Override
	public void setScorer(IScorer scorer) throws IOException {
		mComparator.setScorer((Scorer)scorer);
	}
  
}
