package org.javenstudio.hornet.search.collector;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IFieldComparator;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.search.Scorer;
import org.javenstudio.hornet.search.cache.FieldValueHitQueue;

/**
 * Implements a TopFieldCollector over multiple SortField criteria, without
 * tracking document scores and maxScore.
 */
class MultiComparatorNonScoringCollector extends TopFieldCollector {
	
	protected final IFieldComparator<?>[] mComparators;
	protected final int[] mReverseMul;
	protected final FieldValueHitQueue<FieldValueHitQueue.Entry> mEntryQueue;
  
	public MultiComparatorNonScoringCollector(
			FieldValueHitQueue<FieldValueHitQueue.Entry> queue,
			int numHits, boolean fillFields) throws IOException {
		super(queue, numHits, fillFields);
		mEntryQueue = queue;
		mComparators = queue.getComparators();
		mReverseMul = queue.getReverseMul();
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
	public void setNextReader(IAtomicReaderRef context) throws IOException {
		mDocBase = context.getDocBase();
		for (int i = 0; i < mComparators.length; i++) {
			mEntryQueue.setComparator(i, mComparators[i].setNextReader(context));
		}
	}

	@Override
	public void setScorer(IScorer scorer) throws IOException {
		// set the scorer on all comparators
		for (int i = 0; i < mComparators.length; i++) {
			mComparators[i].setScorer((Scorer)scorer);
		}
	}
	
}
