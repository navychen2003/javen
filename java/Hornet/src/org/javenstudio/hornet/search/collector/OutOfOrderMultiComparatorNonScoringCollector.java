package org.javenstudio.hornet.search.collector;

import java.io.IOException;

import org.javenstudio.hornet.search.cache.FieldValueHitQueue;

/**
 * Implements a TopFieldCollector over multiple SortField criteria, without
 * tracking document scores and maxScore, and assumes out of orderness in doc
 * Ids collection.
 */
final class OutOfOrderMultiComparatorNonScoringCollector extends 
		MultiComparatorNonScoringCollector {
  
	public OutOfOrderMultiComparatorNonScoringCollector(
			FieldValueHitQueue<FieldValueHitQueue.Entry> queue,
			int numHits, boolean fillFields) throws IOException {
		super(queue, numHits, fillFields);
	}
  
	@Override
	public void collect(int doc) throws IOException {
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
					// This is the equals case.
					if (doc + mDocBase > mBottom.getDoc()) {
						// Definitely not competitive
						return;
					}
					break;
				}
			}

			// This hit is competitive - replace bottom element in queue & adjustTop
			for (int i = 0; i < mComparators.length; i++) {
				mComparators[i].copy(mBottom.getSlot(), doc);
			}

			updateBottom(doc);

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
			
			add(slot, doc, Float.NaN);
			if (mQueueFull) {
				for (int i = 0; i < mComparators.length; i++) {
					mComparators[i].setBottom(mBottom.getSlot());
				}
			}
		}
	}
  
	@Override
	public boolean acceptsDocsOutOfOrder() {
		return true;
	}

}
