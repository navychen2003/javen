package org.javenstudio.common.indexdb.search;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IFieldComparator;
import org.javenstudio.common.indexdb.IScorer;

/**
 * Expert: a FieldComparator compares hits so as to determine their
 * sort order when collecting the top results with {@link
 * TopFieldCollector}.  The concrete public FieldComparator
 * classes here correspond to the SortField types.
 *
 * <p>This API is designed to achieve high performance
 * sorting, by exposing a tight interaction with {@link
 * FieldValueHitQueue} as it visits hits.  Whenever a hit is
 * competitive, it's enrolled into a virtual slot, which is
 * an int ranging from 0 to numHits-1.  The {@link
 * FieldComparator} is made aware of segment transitions
 * during searching in case any internal state it's tracking
 * needs to be recomputed during these transitions.</p>
 *
 * <p>A comparator must define these functions:</p>
 *
 * <ul>
 *
 *  <li> {@link #compare} Compare a hit at 'slot a'
 *       with hit 'slot b'.
 *
 *  <li> {@link #setBottom} This method is called by
 *       {@link FieldValueHitQueue} to notify the
 *       FieldComparator of the current weakest ("bottom")
 *       slot.  Note that this slot may not hold the weakest
 *       value according to your comparator, in cases where
 *       your comparator is not the primary one (ie, is only
 *       used to break ties from the comparators before it).
 *
 *  <li> {@link #compareBottom} Compare a new hit (docID)
 *       against the "weakest" (bottom) entry in the queue.
 *
 *  <li> {@link #copy} Installs a new hit into the
 *       priority queue.  The {@link FieldValueHitQueue}
 *       calls this method when a new hit is competitive.
 *
 *  <li> {@link #setNextReader(IAtomicReaderRef)} Invoked
 *       when the search is switching to the next segment.
 *       You may need to update internal state of the
 *       comparator, for example retrieving new values from
 *       the {@link FieldCache}.
 *
 *  <li> {@link #value} Return the sort value stored in
 *       the specified slot.  This is only called at the end
 *       of the search, in order to populate {@link
 *       FieldDoc#fields} when returning the top results.
 * </ul>
 *
 */
public abstract class FieldComparator<T> implements IFieldComparator<T> {

	/**
	 * Compare hit at slot1 with hit at slot2.
	 * 
	 * @param slot1 first slot to compare
	 * @param slot2 second slot to compare
	 * @return any N < 0 if slot2's value is sorted after
	 * slot1, any N > 0 if the slot2's value is sorted before
	 * slot1 and 0 if they are equal
	 */
	public abstract int compare(int slot1, int slot2);

	/**
	 * Set the bottom slot, ie the "weakest" (sorted last)
	 * entry in the queue.  When {@link #compareBottom} is
	 * called, you should compare against this slot.  This
	 * will always be called before {@link #compareBottom}.
	 * 
	 * @param slot the currently weakest (sorted last) slot in the queue
	 */
	public abstract void setBottom(final int slot);

	/**
	 * Compare the bottom of the queue with doc.  This will
	 * only invoked after setBottom has been called.  This
	 * should return the same result as {@link
	 * #compare(int,int)}} as if bottom were slot1 and the new
	 * document were slot 2.
	 *    
	 * <p>For a search that hits many results, this method
	 * will be the hotspot (invoked by far the most
	 * frequently).</p>
	 * 
	 * @param doc that was hit
	 * @return any N < 0 if the doc's value is sorted after
	 * the bottom entry (not competitive), any N > 0 if the
	 * doc's value is sorted before the bottom entry and 0 if
	 * they are equal.
	 */
	public abstract int compareBottom(int doc) throws IOException;

	/**
	 * This method is called when a new hit is competitive.
	 * You should copy any state associated with this document
	 * that will be required for future comparisons, into the
	 * specified slot.
	 * 
	 * @param slot which slot to copy the hit to
	 * @param doc docID relative to current reader
	 */
	public abstract void copy(int slot, int doc) throws IOException;

	/**
	 * Set a new {@link IAtomicReaderRef}. All subsequent docIDs are relative to
	 * the current reader (you must add docBase if you need to
	 * map it to a top-level docID).
	 * 
	 * @param context current reader context
	 * @return the comparator to use for this segment; most
	 *   comparators can just return "this" to reuse the same
	 *   comparator across segments
	 * @throws IOException
	 */
	public abstract IFieldComparator<T> setNextReader(IAtomicReaderRef context) throws IOException;

	/** 
	 * Sets the Scorer to use in case a document's score is
	 *  needed.
	 * 
	 * @param scorer Scorer instance that you should use to
	 * obtain the current hit's score, if necessary. 
	 */
	public void setScorer(IScorer scorer) {
		// Empty implementation since most comparators don't need the score. This
		// can be overridden by those that need it.
	}
  
	/**
	 * Return the actual value in the slot.
	 *
	 * @param slot the value
	 * @return value in this slot
	 */
	public abstract T getValue(int slot);

	/** 
	 * Returns -1 if first is less than second.  Default
	 *  impl to assume the type implements Comparable and
	 *  invoke .compareTo; be sure to override this method if
	 *  your FieldComparator's type isn't a Comparable or
	 *  if your values may sometimes be null 
	 */
	@SuppressWarnings("unchecked")
	public int compareValues(T first, T second) {
		if (first == null) {
			if (second == null) {
				return 0;
			} else {
				return -1;
			}
		} else if (second == null) {
			return 1;
		} else {
			return ((Comparable<T>) first).compareTo(second);
		}
	}

	/** 
	 * Returns negative result if the doc's value is less
	 *  than the provided value. 
	 */
	public abstract int compareDocToValue(int doc, T value) throws IOException;
	
}
