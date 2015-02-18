package org.javenstudio.hornet.search.comparator;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDocTerms;
import org.javenstudio.common.indexdb.IFieldComparator;
import org.javenstudio.common.indexdb.search.FieldComparator;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.search.cache.FieldCache;

/** 
 * Sorts by field's natural Term sort order.  All
 *  comparisons are done using BytesRef.compareTo, which is
 *  slow for medium to large result sets but possibly
 *  very fast for very small results sets. 
 */
public final class TermValComparator extends FieldComparator<BytesRef> {

	private final BytesRef mTempBR = new BytesRef();
	private final String mField;
	private BytesRef[] mValues;
	private IDocTerms mDocTerms;
	private BytesRef mBottom;
	
	public TermValComparator(int numHits, String field) {
		mValues = new BytesRef[numHits];
		mField = field;
	}

	@Override
	public int compare(int slot1, int slot2) {
		final BytesRef val1 = mValues[slot1];
		final BytesRef val2 = mValues[slot2];
		if (val1 == null) {
			if (val2 == null) 
				return 0;
			return -1;
		} else if (val2 == null) {
			return 1;
		}
		return val1.compareTo(val2);
	}

	@Override
	public int compareBottom(int doc) {
		BytesRef val2 = mDocTerms.getTerm(doc, mTempBR);
		if (mBottom == null) {
			if (val2 == null) 
				return 0;
			return -1;
		} else if (val2 == null) {
			return 1;
		}
		return mBottom.compareTo(val2);
	}

	@Override
	public void copy(int slot, int doc) {
		if (mValues[slot] == null) 
			mValues[slot] = new BytesRef();
		mDocTerms.getTerm(doc, mValues[slot]);
	}

	@Override
	public IFieldComparator<BytesRef> setNextReader(IAtomicReaderRef context) throws IOException {
		mDocTerms = FieldCache.DEFAULT.getTerms(context.getReader(), mField);
		return this;
	}
  
	@Override
	public void setBottom(final int bottom) {
		mBottom = mValues[bottom];
	}

	@Override
	public BytesRef getValue(int slot) {
		return mValues[slot];
	}

	@Override
	public int compareValues(BytesRef val1, BytesRef val2) {
		if (val1 == null) {
			if (val2 == null) 
				return 0;
			return -1;
		} else if (val2 == null) {
			return 1;
		}
		return val1.compareTo(val2);
	}

	@Override
	public int compareDocToValue(int doc, BytesRef value) {
		return mDocTerms.getTerm(doc, mTempBR).compareTo(value);
	}
	
}
