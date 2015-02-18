package org.javenstudio.hornet.search.scorer;

import java.io.IOException;

import org.javenstudio.common.indexdb.ICollector;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.search.Collector;
import org.javenstudio.common.indexdb.search.Scorer;

/**
 * A {@link Scorer} which wraps another scorer and caches the score of the
 * current document. Successive calls to {@link #score()} will return the same
 * result and will not invoke the wrapped Scorer's score() method, unless the
 * current document has changed.<br>
 * This class might be useful due to the changes done to the {@link Collector}
 * interface, in which the score is not computed for a document by default, only
 * if the collector requests it. Some collectors may need to use the score in
 * several places, however all they have in hand is a {@link Scorer} object, and
 * might end up computing the score of a document more than once.
 */
public class ScoreCachingWrappingScorer extends Scorer {

	private final IScorer mScorer;
	private int mCurDoc = -1;
	private float mCurScore;
  
	/** Creates a new instance by wrapping the given scorer. */
	public ScoreCachingWrappingScorer(IScorer scorer) {
		super(scorer.getWeight());
		mScorer = scorer;
	}

	@Override
	public boolean score(ICollector collector, int max, int firstDocID) throws IOException {
		return mScorer.score(collector, max, firstDocID);
	}
  
	@Override
	public float getScore() throws IOException {
		int doc = mScorer.getDocID();
		if (doc != mCurDoc) {
			mCurScore = mScorer.getScore();
			mCurDoc = doc;
		}
    
		return mCurScore;
	}

	@Override
	public int getDocID() {
		return mScorer.getDocID();
	}

	@Override
	public int nextDoc() throws IOException {
		return mScorer.nextDoc();
	}
  
	@Override
	public void score(ICollector collector) throws IOException {
		mScorer.score(collector);
	}
  
	@Override
	public int advance(int target) throws IOException {
		return mScorer.advance(target);
	}
  
}
