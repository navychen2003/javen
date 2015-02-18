package org.javenstudio.hornet.search.comparator;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IFieldComparator;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.hornet.search.cache.FieldCache;

/** 
 * Parses field's values as long (using {@link
 *  FieldCache#getLongs} and sorts by ascending value 
 */
public final class LongComparator extends NumericComparator<Long> {
	
	private final long[] mValues;
	private final ISortField.LongParser mParser;
	private long[] mCurrentReaderValues;
	private long mBottom;

	public LongComparator(int numHits, String field, ISortField.Parser parser, Long missingValue) {
		super(field, missingValue);
		mValues = new long[numHits];
		mParser = (ISortField.LongParser) parser;
	}

	@Override
	public int compare(int slot1, int slot2) {
		// TODO: there are sneaky non-branch ways to compute
		// -1/+1/0 sign
		final long v1 = mValues[slot1];
		final long v2 = mValues[slot2];
		if (v1 > v2) {
			return 1;
		} else if (v1 < v2) {
			return -1;
		} else {
			return 0;
		}
	}

	@Override
	public int compareBottom(int doc) {
		// TODO: there are sneaky non-branch ways to compute
		// -1/+1/0 sign
		long v2 = mCurrentReaderValues[doc];
		// Test for v2 == 0 to save Bits.get method call for
		// the common case (doc has value and value is non-zero):
		if (mDocsWithField != null && v2 == 0 && !mDocsWithField.get(doc)) 
			v2 = mMissingValue;

		if (mBottom > v2) {
			return 1;
		} else if (mBottom < v2) {
			return -1;
		} else {
			return 0;
		}
	}

	@Override
	public void copy(int slot, int doc) {
		long v2 = mCurrentReaderValues[doc];
		// Test for v2 == 0 to save Bits.get method call for
		// the common case (doc has value and value is non-zero):
		if (mDocsWithField != null && v2 == 0 && !mDocsWithField.get(doc)) 
			v2 = mMissingValue;

		mValues[slot] = v2;
	}

	@Override
	public IFieldComparator<Long> setNextReader(IAtomicReaderRef context) throws IOException {
		// NOTE: must do this before calling super otherwise
		// we compute the docsWithField Bits twice!
		mCurrentReaderValues = FieldCache.DEFAULT.getLongs(
				context.getReader(), mField, mParser, mMissingValue != null);
		return super.setNextReader(context);
	}
  
	@Override
	public void setBottom(final int bottom) {
		mBottom = mValues[bottom];
	}

	@Override
	public Long getValue(int slot) {
		return Long.valueOf(mValues[slot]);
	}

	@Override
	public int compareDocToValue(int doc, Long valueObj) {
		final long value = valueObj.longValue();
		long docValue = mCurrentReaderValues[doc];
		// Test for docValue == 0 to save Bits.get method call for
		// the common case (doc has value and value is non-zero):
		if (mDocsWithField != null && docValue == 0 && !mDocsWithField.get(doc)) 
			docValue = mMissingValue;
		
		if (docValue < value) {
			return -1;
		} else if (docValue > value) {
			return 1;
		} else {
			return 0;
		}
	}
	
}
