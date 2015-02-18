package org.javenstudio.common.indexdb;

import java.io.IOException;
import java.util.Collection;

public interface IScorer extends IDocIdSetIterator {

	public static interface IChild { 
		public IScorer getScorer();
		public String getRelationShip();
	}
	
	/** Returns child sub-scorers */
	public Collection<IChild> getChildren();
	
	/** 
	 * Scores and collects all matching documents.
	 * @param collector The collector to which all matching documents are passed.
	 */
	public void score(ICollector collector) throws IOException;
	
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
	public boolean score(ICollector collector, int max, int firstDocID) throws IOException;
	
	/** 
	 * Returns the score of the current document matching the query.
	 * Initially invalid, until {@link #nextDoc()} or {@link #advance(int)}
	 * is called the first time, or when called from within
	 * {@link Collector#collect}.
	 */
	public float getScore() throws IOException;
	
	/** 
	 * Returns number of matches for the current document.
	 *  This returns a float (not int) because
	 *  SloppyPhraseScorer discounts its freq according to how
	 *  "sloppy" the match was.
	 */
	public float getFreq() throws IOException;
	
	/** returns parent Weight */
	public IWeight getWeight();
	
}
