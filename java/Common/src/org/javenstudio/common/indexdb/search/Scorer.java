package org.javenstudio.common.indexdb.search;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.javenstudio.common.indexdb.ICollector;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.IWeight;

/**
 * Expert: Common scoring functionality for different types of queries.
 *
 * <p>
 * A <code>Scorer</code> iterates over documents matching a
 * query in increasing order of doc Id.
 * </p>
 * <p>
 * Document scores are computed using a given <code>Similarity</code>
 * implementation.
 * </p>
 *
 * <p><b>NOTE</b>: The values Float.Nan,
 * Float.NEGATIVE_INFINITY and Float.POSITIVE_INFINITY are
 * not valid scores.  Certain collectors (eg {@link
 * TopScoreDocCollector}) will not properly collect hits
 * with these scores.
 */
public abstract class Scorer extends DocIdSetIterator implements IScorer {
	
	protected final IWeight mWeight;

	/**
	 * Constructs a Scorer
	 * @param weight The scorers <code>Weight</code>.
	 */
	protected Scorer(IWeight weight) {
		mWeight = weight;
	}

	/** 
	 * Scores and collects all matching documents.
	 * @param collector The collector to which all matching documents are passed.
	 */
	@Override
	public void score(ICollector collector) throws IOException {
		collector.setScorer(this);
		int doc;
		while ((doc = nextDoc()) != NO_MORE_DOCS) {
			collector.collect(doc);
		}
	}

	/**
	 * Expert: Collects matching documents in a range. Hook for optimization.
	 * Note, <code>firstDocID</code> is added to ensure that {@link #nextDoc()}
	 * was called before this method.
	 * 
	 * @param collector
	 *          The collector to which all matching documents are passed.
	 * @param max
	 *          Do not score documents past this.
	 * @param firstDocID
	 *          The first document ID (ensures {@link #nextDoc()} is called before
	 *          this method.
	 * @return true if more matching documents may remain.
	 */
	@Override
	public boolean score(ICollector collector, int max, int firstDocID) throws IOException {
		collector.setScorer(this);
		int doc = firstDocID;
		while (doc < max) {
			collector.collect(doc);
			doc = nextDoc();
		}
		return doc != NO_MORE_DOCS;
	}
  
	/** 
	 * Returns the score of the current document matching the query.
	 * Initially invalid, until {@link #nextDoc()} or {@link #advance(int)}
	 * is called the first time, or when called from within
	 * {@link Collector#collect}.
	 */
	@Override
	public abstract float getScore() throws IOException;

	/** 
	 * Returns number of matches for the current document.
	 *  This returns a float (not int) because
	 *  SloppyPhraseScorer discounts its freq according to how
	 *  "sloppy" the match was.
	 */
	@Override
	public float getFreq() throws IOException {
		throw new UnsupportedOperationException(this + " does not implement freq()");
	}
  
	/** returns parent Weight */
	@Override
	public IWeight getWeight() {
		return mWeight;
	}
  
	/** Returns child sub-scorers */
	@Override
	public Collection<IChild> getChildren() {
		return Collections.emptyList();
	}
  
	/** 
	 * a child Scorer and its relationship to its parent.
	 * the meaning of the relationship depends upon the parent query. 
	 */
	public static class ChildScorer implements IChild {
		private final IScorer mChild;
		private final String mRelationShip;
    
		public ChildScorer(IScorer child, String relationship) {
			mChild = child;
			mRelationShip = relationship;
		}
		
		public IScorer getScorer() { return mChild; }
		public String getRelationShip() { return mRelationShip; }
	}
	
}
