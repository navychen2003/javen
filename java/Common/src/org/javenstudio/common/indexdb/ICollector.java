package org.javenstudio.common.indexdb;

import java.io.IOException;

public interface ICollector {

	/**
	 * Called before successive calls to {@link #collect(int)}. Implementations
	 * that need the score of the current document (passed-in to
	 * {@link #collect(int)}), should save the passed-in Scorer and call
	 * scorer.score() when needed.
	 */
	public void setScorer(IScorer scorer) throws IOException;
  
	/**
	 * Called once for every document matching a query, with the unbased document
	 * number.
	 * 
	 * <p>
	 * Note: This is called in an inner search loop. For good search performance,
	 * implementations of this method should not call {@link IndexSearcher#doc(int)} or
	 * {@link IndexReader#document(int)} on every hit.
	 * Doing so can slow searches by an order of magnitude or more.
	 */
	public void collect(int doc) throws IOException;

	/**
	 * Called before collecting from each {@link AtomicReaderContext}. All doc ids in
	 * {@link #collect(int)} will correspond to {@link IndexReaderContext#reader}.
	 * 
	 * Add {@link AtomicReaderContext#docBase} to the current  {@link IndexReaderContext#reader}'s
	 * internal document id to re-base ids in {@link #collect(int)}.
	 * 
	 * @param context
	 *          next atomic reader context
	 */
	public void setNextReader(IAtomicReaderRef context) throws IOException;

	/**
	 * Return <code>true</code> if this collector does not
	 * require the matching docIDs to be delivered in int sort
	 * order (smallest to largest) to {@link #collect}.
	 *
	 * <p> Most Indexdb Query implementations will visit
	 * matching docIDs in order.  However, some queries
	 * (currently limited to certain cases of {@link
	 * BooleanQuery}) can achieve faster searching if the
	 * <code>Collector</code> allows them to deliver the
	 * docIDs out of order.</p>
	 *
	 * <p> Many collectors don't mind getting docIDs out of
	 * order, so it's important to return <code>true</code>
	 * here.
	 */
	public boolean acceptsDocsOutOfOrder();
	
}
