package org.javenstudio.hornet.search.scorer;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;

import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.IWeight;
import org.javenstudio.common.indexdb.search.Scorer;
import org.javenstudio.common.indexdb.util.ArrayUtil;

/** Scorer for conjunctions, sets of queries, all of which are required. */
class ConjunctionScorer extends Scorer {
  
	private final IScorer[] mScorers;
	private final float mCoord;
	private int mLastDoc = -1;

	public ConjunctionScorer(IWeight weight, float coord, Collection<IScorer> scorers) 
			throws IOException {
		this(weight, coord, scorers.toArray(new IScorer[scorers.size()]));
	}

	public ConjunctionScorer(IWeight weight, float coord, IScorer... scorers) throws IOException {
		super(weight);
		mScorers = scorers;
		mCoord = coord;
    
		for (int i = 0; i < scorers.length; i++) {
			if (scorers[i].nextDoc() == NO_MORE_DOCS) {
				// If even one of the sub-scorers does not have any documents, this
				// scorer should not attempt to do any more work.
				mLastDoc = NO_MORE_DOCS;
				return;
			}
		}

		// Sort the array the first time...
		// We don't need to sort the array in any future calls because we know
		// it will already start off sorted (all scorers on same doc).
    
		// Note that this comparator is not consistent with equals!
		// Also we use mergeSort here to be stable (so order of Scoreres that
		// match on first document keeps preserved):
		ArrayUtil.mergeSort(scorers, new Comparator<IScorer>() { // sort the array
				public int compare(IScorer o1, IScorer o2) {
					return o1.getDocID() - o2.getDocID();
				}
			});

		// NOTE: doNext() must be called before the re-sorting of the array later on.
		// The reason is this: assume there are 5 scorers, whose first docs are 1,
		// 2, 3, 5, 5 respectively. Sorting (above) leaves the array as is. Calling
		// doNext() here advances all the first scorers to 5 (or a larger doc ID
		// they all agree on). 
		// However, if we re-sort before doNext() is called, the order will be 5, 3,
		// 2, 1, 5 and then doNext() will stop immediately, since the first scorer's
		// docs equals the last one. So the invariant that after calling doNext() 
		// all scorers are on the same doc ID is broken.
		if (doNext() == NO_MORE_DOCS) {
			// The scorers did not agree on any document.
			mLastDoc = NO_MORE_DOCS;
			return;
		}

		// If first-time skip distance is any predictor of
		// scorer sparseness, then we should always try to skip first on
		// those scorers.
		// Keep last scorer in it's last place (it will be the first
		// to be skipped on), but reverse all of the others so that
		// they will be skipped on in order of original high skip.
		int end = scorers.length - 1;
		int max = end >> 1;
		for (int i = 0; i < max; i++) {
			IScorer tmp = scorers[i];
			int idx = end - i - 1;
			scorers[i] = scorers[idx];
			scorers[idx] = tmp;
		}
	}

	private int doNext() throws IOException {
		int first = 0;
		int doc = mScorers[mScorers.length - 1].getDocID();
		IScorer firstScorer;
		while ((firstScorer = mScorers[first]).getDocID() < doc) {
			doc = firstScorer.advance(doc);
			first = first == mScorers.length - 1 ? 0 : first + 1;
		}
		return doc;
	}
  
	@Override
	public int advance(int target) throws IOException {
		if (mLastDoc == NO_MORE_DOCS) {
			return mLastDoc;
		} else if (mScorers[(mScorers.length - 1)].getDocID() < target) {
			mScorers[(mScorers.length - 1)].advance(target);
		}
		return mLastDoc = doNext();
	}

	@Override
	public int getDocID() {
		return mLastDoc;
	}
  
	@Override
	public int nextDoc() throws IOException {
		if (mLastDoc == NO_MORE_DOCS) {
			return mLastDoc;
		} else if (mLastDoc == -1) {
			return mLastDoc = mScorers[mScorers.length - 1].getDocID();
		}
		mScorers[(mScorers.length - 1)].nextDoc();
		return mLastDoc = doNext();
	}
  
	@Override
	public float getScore() throws IOException {
		float sum = 0.0f;
		for (int i = 0; i < mScorers.length; i++) {
			sum += mScorers[i].getScore();
		}
		return sum * mCoord;
	}
	
}
