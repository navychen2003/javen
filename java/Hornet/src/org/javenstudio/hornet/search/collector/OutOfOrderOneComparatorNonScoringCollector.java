package org.javenstudio.hornet.search.collector;

import java.io.IOException;

import org.javenstudio.hornet.search.cache.FieldValueHitQueue;

/**
 * Implements a TopFieldCollector over one SortField criteria, without
 * tracking document scores and maxScore, and assumes out of orderness in doc
 * Ids collection.
 */
final class OutOfOrderOneComparatorNonScoringCollector extends
		OneComparatorNonScoringCollector {

	public OutOfOrderOneComparatorNonScoringCollector(
			FieldValueHitQueue<FieldValueHitQueue.Entry> queue,
			int numHits, boolean fillFields) throws IOException {
		super(queue, numHits, fillFields);
	}
  
	@Override
	public void collect(int doc) throws IOException {
		++ mTotalHits;
		if (mQueueFull) {
			// Fastmatch: return if this hit is not competitive
			final int cmp = mReverseMul * mComparator.compareBottom(doc);
			if (cmp < 0 || (cmp == 0 && doc + mDocBase > mBottom.getDoc())) 
				return;
      
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
	public boolean acceptsDocsOutOfOrder() {
		return true;
	}

}
