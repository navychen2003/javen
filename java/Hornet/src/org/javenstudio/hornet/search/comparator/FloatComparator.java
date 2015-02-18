package org.javenstudio.hornet.search.comparator;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IFieldComparator;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.hornet.search.cache.FieldCache;

/** 
 * Parses field's values as float (using {@link
 *  FieldCache#getFloats} and sorts by ascending value 
 */
public final class FloatComparator extends NumericComparator<Float> {
	
	private final float[] mValues;
	private final ISortField.FloatParser mParser;
	private float[] mCurrentReaderValues;
	private float mBottom;

	public FloatComparator(int numHits, String field, ISortField.Parser parser, Float missingValue) {
		super(field, missingValue);
		mValues = new float[numHits];
		mParser = (ISortField.FloatParser) parser;
	}
  
	@Override
	public int compare(int slot1, int slot2) {
		// TODO: are there sneaky non-branch ways to compute
		// sign of float?
		final float v1 = mValues[slot1];
		final float v2 = mValues[slot2];
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
		// TODO: are there sneaky non-branch ways to compute sign of float?
		float v2 = mCurrentReaderValues[doc];
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
		float v2 = mCurrentReaderValues[doc];
		// Test for v2 == 0 to save Bits.get method call for
		// the common case (doc has value and value is non-zero):
		if (mDocsWithField != null && v2 == 0 && !mDocsWithField.get(doc)) 
			v2 = mMissingValue;

		mValues[slot] = v2;
	}

	@Override
	public IFieldComparator<Float> setNextReader(IAtomicReaderRef context) throws IOException {
		// NOTE: must do this before calling super otherwise
		// we compute the docsWithField Bits twice!
		mCurrentReaderValues = FieldCache.DEFAULT.getFloats(
				context.getReader(), mField, mParser, mMissingValue != null);
		return super.setNextReader(context);
	}
  
	@Override
	public void setBottom(final int bottom) {
		mBottom = mValues[bottom];
	}

	@Override
	public Float getValue(int slot) {
		return Float.valueOf(mValues[slot]);
	}

	@Override
	public int compareDocToValue(int doc, Float valueObj) {
		final float value = valueObj.floatValue();
		float docValue = mCurrentReaderValues[doc];
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
