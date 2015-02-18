package org.javenstudio.hornet.search.comparator;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IFieldComparator;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.hornet.search.cache.FieldCache;

/** 
 * Parses field's values as double (using {@link
 *  FieldCache#getDoubles} and sorts by ascending value 
 */
public final class DoubleComparator extends NumericComparator<Double> {
	
	private final double[] mValues;
	private final ISortField.DoubleParser mParser;
	private double[] mCurrentReaderValues;
	private double mBottom;

	public DoubleComparator(int numHits, String field, ISortField.Parser parser, Double missingValue) {
		super(field, missingValue);
		mValues = new double[numHits];
		mParser = (ISortField.DoubleParser) parser;
	}

	@Override
	public int compare(int slot1, int slot2) {
		final double v1 = mValues[slot1];
		final double v2 = mValues[slot2];
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
		double v2 = mCurrentReaderValues[doc];
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
		double v2 = mCurrentReaderValues[doc];
		// Test for v2 == 0 to save Bits.get method call for
		// the common case (doc has value and value is non-zero):
		if (mDocsWithField != null && v2 == 0 && !mDocsWithField.get(doc)) 
			v2 = mMissingValue;

		mValues[slot] = v2;
	}

	@Override
	public IFieldComparator<Double> setNextReader(IAtomicReaderRef context) throws IOException {
		// NOTE: must do this before calling super otherwise
		// we compute the docsWithField Bits twice!
		mCurrentReaderValues = FieldCache.DEFAULT.getDoubles(
				context.getReader(), mField, mParser, mMissingValue != null);
		return super.setNextReader(context);
	}
  
	@Override
	public void setBottom(final int bottom) {
		mBottom = mValues[bottom];
	}

	@Override
	public Double getValue(int slot) {
		return Double.valueOf(mValues[slot]);
	}

	@Override
	public int compareDocToValue(int doc, Double valueObj) {
		final double value = valueObj.doubleValue();
		double docValue = mCurrentReaderValues[doc];
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
