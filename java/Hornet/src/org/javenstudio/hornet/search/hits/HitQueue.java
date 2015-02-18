package org.javenstudio.hornet.search.hits;

import org.javenstudio.common.indexdb.IScoreDoc;
import org.javenstudio.common.indexdb.search.ScoreDoc;
import org.javenstudio.common.indexdb.util.PriorityQueue;

public final class HitQueue extends PriorityQueue<IScoreDoc> {

	/**
	 * Creates a new instance with <code>size</code> elements. If
	 * <code>prePopulate</code> is set to true, the queue will pre-populate itself
	 * with sentinel objects and set its {@link #size()} to <code>size</code>. In
	 * that case, you should not rely on {@link #size()} to get the number of
	 * actual elements that were added to the queue, but keep track yourself.<br>
	 * <b>NOTE:</b> in case <code>prePopulate</code> is true, you should pop
	 * elements from the queue using the following code example:
	 * 
	 * <pre>
	 * PriorityQueue pq = new HitQueue(10, true); // pre-populate.
	 * ScoreDoc top = pq.top();
	 * 
	 * // Add/Update one element.
	 * top.score = 1.0f;
	 * top.doc = 0;
	 * top = (ScoreDoc) pq.updateTop();
	 * int totalHits = 1;
	 * 
	 * // Now pop only the elements that were *truly* inserted.
	 * // First, pop all the sentinel elements (there are pq.size() - totalHits).
	 * for (int i = pq.size() - totalHits; i &gt; 0; i--) pq.pop();
	 * 
	 * // Now pop the truly added elements.
	 * ScoreDoc[] results = new ScoreDoc[totalHits];
	 * for (int i = totalHits - 1; i &gt;= 0; i--) {
	 *   results[i] = (ScoreDoc) pq.pop();
	 * }
	 * </pre>
	 * 
	 * <p><b>NOTE</b>: This class pre-allocate a full array of
	 * length <code>size</code>.
	 * 
	 * @param size
	 *          the requested size of this queue.
	 * @param prePopulate
	 *          specifies whether to pre-populate the queue with sentinel values.
	 * @see #getSentinelObject()
	 */
	public HitQueue(int size, boolean prePopulate) {
		super(size, prePopulate);
	}

	@Override
	protected IScoreDoc getSentinelObject() {
		// Always set the doc Id to MAX_VALUE so that it won't be favored by
		// lessThan. This generally should not happen since if score is not NEG_INF,
		// TopScoreDocCollector will always add the object to the queue.
		return new ScoreDoc(Integer.MAX_VALUE, Float.NEGATIVE_INFINITY);
	}
  
	@Override
	protected final boolean lessThan(IScoreDoc hitA, IScoreDoc hitB) {
		if (hitA.getScore() == hitB.getScore())
			return hitA.getDoc() > hitB.getDoc(); 
		else
			return hitA.getScore() < hitB.getScore();
	}
	
}
