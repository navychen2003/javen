package org.javenstudio.hornet.query;

import org.javenstudio.hornet.util.MutableValue;
import org.javenstudio.hornet.util.MutableValueLong;

/**
 * Abstract {@link FunctionValues} implementation which supports retrieving long values.
 * Implementations can control how the long values are loaded through {@link #longVal(int)}}
 */
public abstract class LongDocValues extends FunctionValues {
	
	protected final ValueSource mSource;

	public LongDocValues(ValueSource vs) {
		mSource = vs;
	}

	@Override
	public byte byteVal(int doc) {
		return (byte)longVal(doc);
	}

	@Override
	public short shortVal(int doc) {
		return (short)longVal(doc);
	}

	@Override
	public float floatVal(int doc) {
		return (float)longVal(doc);
	}

	@Override
	public int intVal(int doc) {
		return (int)longVal(doc);
	}

	@Override
	public abstract long longVal(int doc);

	@Override
	public double doubleVal(int doc) {
		return (double)longVal(doc);
	}

	@Override
	public boolean boolVal(int doc) {
		return longVal(doc) != 0;
	}

	@Override
	public String stringVal(int doc) {
		return Long.toString(longVal(doc));
	}

	@Override
	public Object objectVal(int doc) {
		return exists(doc) ? longVal(doc) : null;
	}

	@Override
	public String toString(int doc) {
		return mSource.getDescription() + '=' + stringVal(doc);
	}

	@Override
	public ValueFiller getValueFiller() {
		return new ValueFiller() {
			private final MutableValueLong mVal = new MutableValueLong();

			@Override
			public MutableValue getValue() {
				return mVal;
			}

			@Override
			public void fillValue(int doc) {
				mVal.set(longVal(doc));
				mVal.setExists(exists(doc));
			}
		};
	}
	
}
