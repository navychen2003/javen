package org.javenstudio.common.indexdb;

import java.io.IOException;

public interface IFieldComparator<T> {

	/**
	 * Compare hit at slot1 with hit at slot2.
	 * 
	 * @param slot1 first slot to compare
	 * @param slot2 second slot to compare
	 * @return any N < 0 if slot2's value is sorted after
	 * slot1, any N > 0 if the slot2's value is sorted before
	 * slot1 and 0 if they are equal
	 */
	public int compare(int slot1, int slot2);

	/**
	 * Set the bottom slot, ie the "weakest" (sorted last)
	 * entry in the queue.  When {@link #compareBottom} is
	 * called, you should compare against this slot.  This
	 * will always be called before {@link #compareBottom}.
	 * 
	 * @param slot the currently weakest (sorted last) slot in the queue
	 */
	public void setBottom(final int slot);

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
	public int compareBottom(int doc) throws IOException;

	/**
	 * This method is called when a new hit is competitive.
	 * You should copy any state associated with this document
	 * that will be required for future comparisons, into the
	 * specified slot.
	 * 
	 * @param slot which slot to copy the hit to
	 * @param doc docID relative to current reader
	 */
	public void copy(int slot, int doc) throws IOException;

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
	public IFieldComparator<T> setNextReader(IAtomicReaderRef context) throws IOException;

	/** 
	 * Sets the Scorer to use in case a document's score is
	 *  needed.
	 * 
	 * @param scorer Scorer instance that you should use to
	 * obtain the current hit's score, if necessary. 
	 */
	public void setScorer(IScorer scorer);
  
	/**
	 * Return the actual value in the slot.
	 *
	 * @param slot the value
	 * @return value in this slot
	 */
	public T getValue(int slot);

	/** 
	 * Returns -1 if first is less than second.  Default
	 *  impl to assume the type implements Comparable and
	 *  invoke .compareTo; be sure to override this method if
	 *  your FieldComparator's type isn't a Comparable or
	 *  if your values may sometimes be null 
	 */
	public int compareValues(T first, T second);

	/** Returns negative result if the doc's value is less
	 *  than the provided value. */
	public int compareDocToValue(int doc, T value) throws IOException;
	
}
