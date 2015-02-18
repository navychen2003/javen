package org.javenstudio.hornet.search.cache;

import java.io.IOException;

import org.javenstudio.common.indexdb.IFieldComparator;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.search.FieldDoc;
import org.javenstudio.common.indexdb.search.Filter;
import org.javenstudio.common.indexdb.search.IndexSearcher;
import org.javenstudio.common.indexdb.search.ScoreDoc;
import org.javenstudio.common.indexdb.util.PriorityQueue;

/**
 * Expert: A hit queue for sorting by hits by terms in more than one field.
 * Uses <code>FieldCache.DEFAULT</code> for maintaining
 * internal term lookup tables.
 * 
 * @see IndexSearcher#search(Query,Filter,int,Sort)
 * @see FieldCache
 */
public abstract class FieldValueHitQueue<T extends FieldValueHitQueue.Entry> 
		extends PriorityQueue<T> {

	public static class Entry extends ScoreDoc {
		private int mSlot;

		public Entry(int slot, int doc, float score) {
			super(doc, score);
			mSlot = slot;
		}
    
		public int getSlot() { return mSlot; }
		
		@Override
		public String toString() {
			return "slot:" + mSlot + " " + super.toString();
		}
	}

	/**
	 * An implementation of {@link FieldValueHitQueue} which is optimized in case
	 * there is just one comparator.
	 */
	private static final class OneComparatorFieldValueHitQueue<T extends FieldValueHitQueue.Entry> 
			extends FieldValueHitQueue<T> {
		private final int mOneReverseMul;
    
		public OneComparatorFieldValueHitQueue(ISortField[] fields, int size)
				throws IOException {
			super(fields, size);

			ISortField field = fields[0];
			setComparator(0,field.getComparator(size, 0));
			mOneReverseMul = field.getReverse() ? -1 : 1;
			mReverseMul[0] = mOneReverseMul;
		}

		/**
		 * Returns whether <code>hitA</code> is less relevant than <code>hitB</code>.
		 * @param hitA Entry
		 * @param hitB Entry
		 * @return <code>true</code> if document <code>hitA</code> should be sorted 
		 * after document <code>hitB</code>.
		 */
		@Override
		protected boolean lessThan(final Entry hitA, final Entry hitB) {
			assert hitA != hitB;
			assert hitA.mSlot != hitB.mSlot;

			final int c = mOneReverseMul * mFirstComparator.compare(hitA.mSlot, hitB.mSlot);
			if (c != 0) 
				return c > 0;

				// avoid random sort order that could lead to duplicates (bug #31241):
				return hitA.getDoc() > hitB.getDoc();
		}
	}
  
	/**
	 * An implementation of {@link FieldValueHitQueue} which is optimized in case
	 * there is more than one comparator.
	 */
	private static final class MultiComparatorsFieldValueHitQueue<T extends FieldValueHitQueue.Entry> 
			extends FieldValueHitQueue<T> {

		public MultiComparatorsFieldValueHitQueue(ISortField[] fields, int size)
				throws IOException {
			super(fields, size);

			int numComparators = mComparators.length;
			for (int i = 0; i < numComparators; ++i) {
				ISortField field = fields[i];

				mReverseMul[i] = field.getReverse() ? -1 : 1;
				setComparator(i, field.getComparator(size, i));
			}
		}
  
		@Override
		protected boolean lessThan(final Entry hitA, final Entry hitB) {
			assert hitA != hitB;
			assert hitA.mSlot != hitB.mSlot;

			int numComparators = mComparators.length;
			for (int i = 0; i < numComparators; ++i) {
				final int c = mReverseMul[i] * mComparators[i].compare(hitA.mSlot, hitB.mSlot);
				if (c != 0) {
					// Short circuit
					return c > 0;
				}
			}

			// avoid random sort order that could lead to duplicates (bug #31241):
			return hitA.getDoc() > hitB.getDoc();
		}
	}
  
	
	/** Stores the sort criteria being used. */
	protected final ISortField[] mFields;
	protected final IFieldComparator<?>[] mComparators;  // use setComparator to change this array
	protected IFieldComparator<?> mFirstComparator;      // this must always be equal to comparators[0]
	protected final int[] mReverseMul;
	
	// prevent instantiation and extension.
	private FieldValueHitQueue(ISortField[] fields, int size) {
		super(size);
		// When we get here, fields.length is guaranteed to be > 0, therefore no
		// need to check it again.
    
		// All these are required by this class's API - need to return arrays.
		// Therefore even in the case of a single comparator, create an array
		// anyway.
		mFields = fields;
		int numComparators = fields.length;
		mComparators = new IFieldComparator[numComparators];
		mReverseMul = new int[numComparators];
	}

	/**
	 * Creates a hit queue sorted by the given list of fields.
	 * 
	 * <p><b>NOTE</b>: The instances returned by this method
	 * pre-allocate a full array of length <code>numHits</code>.
	 * 
	 * @param fields
	 *          SortField array we are sorting by in priority order (highest
	 *          priority first); cannot be <code>null</code> or empty
	 * @param size
	 *          The number of hits to retain. Must be greater than zero.
	 * @throws IOException
	 */
	public static <T extends FieldValueHitQueue.Entry> 
  		FieldValueHitQueue<T> create(ISortField[] fields, int size) throws IOException {

		if (fields.length == 0) 
			throw new IllegalArgumentException("Sort must contain at least one field");

		if (fields.length == 1) 
			return new OneComparatorFieldValueHitQueue<T>(fields, size);
		else 
			return new MultiComparatorsFieldValueHitQueue<T>(fields, size);
	}
  
	public IFieldComparator<?>[] getComparators() {
		return mComparators;
	}

	public int[] getReverseMul() {
		return mReverseMul;
	}

	public void setComparator(int pos, IFieldComparator<?> comparator) {
		if (pos == 0) mFirstComparator = comparator;
		mComparators[pos] = comparator;
	}

	public final IFieldComparator<?> getFirstComparator() { 
		return mFirstComparator;
	}
  
	@Override
	protected abstract boolean lessThan(final Entry a, final Entry b);

	/**
	 * Given a queue Entry, creates a corresponding FieldDoc
	 * that contains the values used to sort the given document.
	 * These values are not the raw values out of the index, but the internal
	 * representation of them. This is so the given search hit can be collated by
	 * a MultiSearcher with other search hits.
	 * 
	 * @param entry The Entry used to create a FieldDoc
	 * @return The newly created FieldDoc
	 * @see IndexSearcher#search(Query,Filter,int,Sort)
	 */
	public FieldDoc fillFields(final Entry entry) {
		final int n = mComparators.length;
		final Object[] fields = new Object[n];
		for (int i = 0; i < n; ++i) {
			fields[i] = mComparators[i].getValue(entry.mSlot);
		}
		//if (maxscore > 1.0f) doc.score /= maxscore;   // normalize scores
		return new FieldDoc(entry.getDoc(), entry.getScore(), fields);
	}

	/** Returns the SortFields being used by this hit queue. */
	public ISortField[] getSortFields() {
		return mFields;
	}
	
}
