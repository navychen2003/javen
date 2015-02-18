package org.javenstudio.hornet.search.comparator;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.search.FieldComparator;
import org.javenstudio.common.indexdb.search.IndexSearcher;
import org.javenstudio.common.indexdb.search.Sort;
import org.javenstudio.hornet.search.collector.TopScoreDocCollector;
import org.javenstudio.hornet.search.scorer.ScoreCachingWrappingScorer;

/** 
 * Sorts by descending relevance.  NOTE: if you are
 *  sorting only by descending relevance and then
 *  secondarily by ascending docID, performance is faster
 *  using {@link TopScoreDocCollector} directly (which {@link
 *  IndexSearcher#search} uses when no {@link Sort} is
 *  specified). 
 */
public final class RelevanceComparator extends FieldComparator<Float> {
	
	private final float[] mScores;
	private float mBottom;
	private IScorer mScorer;
  
	public RelevanceComparator(int numHits) {
		mScores = new float[numHits];
	}

	@Override
	public int compare(int slot1, int slot2) {
		final float score1 = mScores[slot1];
		final float score2 = mScores[slot2];
		return score1 > score2 ? -1 : (score1 < score2 ? 1 : 0);
	}

	@Override
	public int compareBottom(int doc) throws IOException {
		float score = mScorer.getScore();
		assert !Float.isNaN(score);
		return mBottom > score ? -1 : (mBottom < score ? 1 : 0);
	}

	@Override
	public void copy(int slot, int doc) throws IOException {
		mScores[slot] = mScorer.getScore();
		assert !Float.isNaN(mScores[slot]);
	}

	@Override
	public FieldComparator<Float> setNextReader(IAtomicReaderRef context) {
		return this;
	}
  
	@Override
	public void setBottom(final int bottom) {
		mBottom = mScores[bottom];
	}

	@Override
	public void setScorer(IScorer scorer) {
		// wrap with a ScoreCachingWrappingScorer so that successive calls to
		// score() will not incur score computation over and
		// over again.
		if (!(scorer instanceof ScoreCachingWrappingScorer)) {
			mScorer = new ScoreCachingWrappingScorer(scorer);
		} else {
			mScorer = scorer;
		}
	}
  
	@Override
	public Float getValue(int slot) {
		return Float.valueOf(mScores[slot]);
	}

	// Override because we sort reverse of natural Float order:
	@Override
	public int compareValues(Float first, Float second) {
		// Reversed intentionally because relevance by default
		// sorts descending:
		return second.compareTo(first);
	}

	@Override
	public int compareDocToValue(int doc, Float valueObj) throws IOException {
		final float value = valueObj.floatValue();
		float docValue = mScorer.getScore();
		assert !Float.isNaN(docValue);
		if (docValue < value) {
			// reverse of FloatComparator
			return 1;
		} else if (docValue > value) {
			// reverse of FloatComparator
			return -1;
		} else {
			return 0;
		}
	}
	
}
