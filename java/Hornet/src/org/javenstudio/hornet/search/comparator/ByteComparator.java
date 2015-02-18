package org.javenstudio.hornet.search.comparator;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IFieldComparator;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.hornet.search.cache.FieldCache;

/** 
 * Parses field's values as byte (using {@link
 *  FieldCache#getBytes} and sorts by ascending value 
 */
public final class ByteComparator extends NumericComparator<Byte> {
	
	private final byte[] mValues;
	private final ISortField.ByteParser mParser;
	private byte[] mCurrentReaderValues;
	private byte mBottom;

	public ByteComparator(int numHits, String field, ISortField.Parser parser, Byte missingValue) {
		super(field, missingValue);
		mValues = new byte[numHits];
		mParser = (ISortField.ByteParser) parser;
	}

	@Override
	public int compare(int slot1, int slot2) {
		return mValues[slot1] - mValues[slot2];
	}

	@Override
	public int compareBottom(int doc) {
		byte v2 = mCurrentReaderValues[doc];
		// Test for v2 == 0 to save Bits.get method call for
		// the common case (doc has value and value is non-zero):
		if (mDocsWithField != null && v2 == 0 && !mDocsWithField.get(doc)) 
			v2 = mMissingValue;

		return mBottom - v2;
	}

	@Override
	public void copy(int slot, int doc) {
		byte v2 = mCurrentReaderValues[doc];
		// Test for v2 == 0 to save Bits.get method call for
		// the common case (doc has value and value is non-zero):
		if (mDocsWithField != null && v2 == 0 && !mDocsWithField.get(doc)) 
			v2 = mMissingValue;
		
		mValues[slot] = v2;
	}

	@Override
	public IFieldComparator<Byte> setNextReader(IAtomicReaderRef context) throws IOException {
		// NOTE: must do this before calling super otherwise
		// we compute the docsWithField Bits twice!
		mCurrentReaderValues = FieldCache.DEFAULT.getBytes(
				context.getReader(), mField, mParser, mMissingValue != null);
		return super.setNextReader(context);
	}
  
	@Override
	public void setBottom(final int bottom) {
		mBottom = mValues[bottom];
	}

	@Override
	public Byte getValue(int slot) {
		return Byte.valueOf(mValues[slot]);
	}

	@Override
	public int compareDocToValue(int doc, Byte value) {
		byte docValue = mCurrentReaderValues[doc];
		// Test for docValue == 0 to save Bits.get method call for
		// the common case (doc has value and value is non-zero):
		if (mDocsWithField != null && docValue == 0 && !mDocsWithField.get(doc)) 
			docValue = mMissingValue;
		
		return docValue - value.byteValue();
	}
	
}
