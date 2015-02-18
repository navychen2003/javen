package org.javenstudio.hornet.search.comparator;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.search.FieldComparator;

/** Sorts by ascending docID */
public final class DocComparator extends FieldComparator<Integer> {
	
	private final int[] mDocIDs;
	private int mDocBase;
	private int mBottom;

	public DocComparator(int numHits) {
		mDocIDs = new int[numHits];
	}

	@Override
	public int compare(int slot1, int slot2) {
		// No overflow risk because docIDs are non-negative
		return mDocIDs[slot1] - mDocIDs[slot2];
	}

	@Override
	public int compareBottom(int doc) {
		// No overflow risk because docIDs are non-negative
		return mBottom - (mDocBase + doc);
	}

	@Override
	public void copy(int slot, int doc) {
		mDocIDs[slot] = mDocBase + doc;
	}

	@Override
	public FieldComparator<Integer> setNextReader(IAtomicReaderRef context) {
		// TODO: can we "map" our docIDs to the current
		// reader? saves having to then subtract on every
		// compare call
		mDocBase = context.getDocBase();
		return this;
	}
  
	@Override
	public void setBottom(final int bottom) {
		mBottom = mDocIDs[bottom];
	}

	@Override
	public Integer getValue(int slot) {
		return Integer.valueOf(mDocIDs[slot]);
	}

	@Override
	public int compareDocToValue(int doc, Integer valueObj) {
		final int value = valueObj.intValue();
		int docValue = mDocBase + doc;
		if (docValue < value) {
			return -1;
		} else if (docValue > value) {
			return 1;
		} else {
			return 0;
		}
	}
	
}
