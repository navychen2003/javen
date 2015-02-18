package org.javenstudio.hornet.search.collector;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IFieldComparator;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.search.FieldDoc;
import org.javenstudio.common.indexdb.search.Scorer;
import org.javenstudio.hornet.search.cache.FieldValueHitQueue;

/**
 * Implements a TopFieldCollector when after != null.
 */
final class PagingFieldCollector extends TopFieldCollector {

	private final IFieldComparator<?>[] mComparators;
	private final int[] mReverseMul;
	private final FieldValueHitQueue<FieldValueHitQueue.Entry> mEntryQueue;
	private final boolean mTrackDocScores;
	private final boolean mTrackMaxScore;
	private final FieldDoc mAfter;
	private Scorer mScorer;
	private int mCollectedHits;
	private int mAfterDoc;
  
	public PagingFieldCollector(
			FieldValueHitQueue<FieldValueHitQueue.Entry> queue, FieldDoc after, int numHits, boolean fillFields,
			boolean trackDocScores, boolean trackMaxScore) throws IOException {
		super(queue, numHits, fillFields);
		mEntryQueue = queue;
		mTrackDocScores = trackDocScores;
		mTrackMaxScore = trackMaxScore;
		mAfter = after;
		mComparators = queue.getComparators();
		mReverseMul = queue.getReverseMul();

		// Must set maxScore to NEG_INF, or otherwise Math.max always returns NaN.
		mMaxScore = Float.NEGATIVE_INFINITY;
	}
  
	protected void updateBottom(int doc, float score) {
		mBottom.setDoc(mDocBase + doc);
		mBottom.setScore(score);
		mBottom = mQueue.updateTop();
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public void collect(int doc) throws IOException {
		mTotalHits ++;

		// Check if this hit was already collected on a
		// previous page:
		boolean sameValues = true;
		for (int compIDX=0; compIDX < mComparators.length; compIDX++) {
			final IFieldComparator comp = mComparators[compIDX];

			final int cmp = mReverseMul[compIDX] * comp.compareDocToValue(doc, mAfter.getFieldAt(compIDX));
			if (cmp < 0) {
				// Already collected on a previous page
				return;
			} else if (cmp > 0) {
				// Not yet collected
				sameValues = false;
				break;
			}
		}

		// Tie-break by docID:
		if (sameValues && doc <= mAfterDoc) {
			// Already collected on a previous page
			return;
		}

		mCollectedHits ++;

		float score = Float.NaN;
		if (mTrackMaxScore) {
			score = mScorer.getScore();
			if (score > mMaxScore) 
				mMaxScore = score;
		}

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

			// Compute score only if it is competitive.
			if (mTrackDocScores && !mTrackMaxScore) 
				score = mScorer.getScore();
			
			updateBottom(doc, score);

			for (int i = 0; i < mComparators.length; i++) {
				mComparators[i].setBottom(mBottom.getSlot());
			}
		} else {
			// Startup transient: queue hasn't gathered numHits yet
			final int slot = mCollectedHits - 1;
			
			// Copy hit into queue
			for (int i = 0; i < mComparators.length; i++) {
				mComparators[i].copy(slot, doc);
			}

			// Compute score only if it is competitive.
			if (mTrackDocScores && !mTrackMaxScore) 
				score = mScorer.getScore();
			
			mBottom = mQueue.add(new FieldValueHitQueue.Entry(slot, mDocBase + doc, score));
			mQueueFull = mCollectedHits == mNumHits;
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
		for (int i = 0; i < mComparators.length; i++) {
			mComparators[i].setScorer((Scorer)scorer);
		}
	}
  
	@Override
	public boolean acceptsDocsOutOfOrder() {
		return true;
	}

	@Override
	public void setNextReader(IAtomicReaderRef context) throws IOException {
		mDocBase = context.getDocBase();
		mAfterDoc = mAfter.getDoc() - mDocBase;
		for (int i = 0; i < mComparators.length; i++) {
			mEntryQueue.setComparator(i, mComparators[i].setNextReader(context));
		}
	}
	
}
