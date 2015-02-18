package org.javenstudio.hornet.search.comparator;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IFieldComparator;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.hornet.search.cache.FieldCache;

/** 
 * Parses field's values as short (using {@link
 *  FieldCache#getShorts} and sorts by ascending value 
 */
public final class ShortComparator extends NumericComparator<Short> {
	
	private final short[] mValues;
	private final ISortField.ShortParser mParser;
	private short[] mCurrentReaderValues;
	private short mBottom;

	public ShortComparator(int numHits, String field, ISortField.Parser parser, Short missingValue) {
		super(field, missingValue);
		mValues = new short[numHits];
		mParser = (ISortField.ShortParser) parser;
	}

	@Override
	public int compare(int slot1, int slot2) {
		return mValues[slot1] - mValues[slot2];
	}

	@Override
	public int compareBottom(int doc) {
		short v2 = mCurrentReaderValues[doc];
		// Test for v2 == 0 to save Bits.get method call for
		// the common case (doc has value and value is non-zero):
		if (mDocsWithField != null && v2 == 0 && !mDocsWithField.get(doc)) 
			v2 = mMissingValue;

		return mBottom - v2;
	}

	@Override
	public void copy(int slot, int doc) {
		short v2 = mCurrentReaderValues[doc];
		// Test for v2 == 0 to save Bits.get method call for
		// the common case (doc has value and value is non-zero):
		if (mDocsWithField != null && v2 == 0 && !mDocsWithField.get(doc)) 
			v2 = mMissingValue;

		mValues[slot] = v2;
	}

	@Override
	public IFieldComparator<Short> setNextReader(IAtomicReaderRef context) throws IOException {
		// NOTE: must do this before calling super otherwise
		// we compute the docsWithField Bits twice!
		mCurrentReaderValues = FieldCache.DEFAULT.getShorts(
				context.getReader(), mField, mParser, mMissingValue != null);
		return super.setNextReader(context);
	}

	@Override
	public void setBottom(final int bottom) {
		mBottom = mValues[bottom];
	}

	@Override
	public Short getValue(int slot) {
		return Short.valueOf(mValues[slot]);
	}

	@Override
	public int compareDocToValue(int doc, Short valueObj) {
		final short value = valueObj.shortValue();
		short docValue = mCurrentReaderValues[doc];
		// Test for docValue == 0 to save Bits.get method call for
		// the common case (doc has value and value is non-zero):
		if (mDocsWithField != null && docValue == 0 && !mDocsWithField.get(doc)) 
			docValue = mMissingValue;
		
		return docValue - value;
	}
	
}
