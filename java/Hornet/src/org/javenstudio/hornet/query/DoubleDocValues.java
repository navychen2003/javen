package org.javenstudio.hornet.query;

import org.javenstudio.hornet.util.MutableValue;
import org.javenstudio.hornet.util.MutableValueDouble;

/**
 * Abstract {@link FunctionValues} implementation which supports retrieving double values.
 * Implementations can control how the double values are loaded through {@link #doubleVal(int)}}
 */
public abstract class DoubleDocValues extends FunctionValues {
	
	protected final ValueSource mSource;

	public DoubleDocValues(ValueSource vs) {
		mSource = vs;
	}

	@Override
	public byte byteVal(int doc) {
		return (byte)doubleVal(doc);
	}

	@Override
	public short shortVal(int doc) {
		return (short)doubleVal(doc);
	}

	@Override
	public float floatVal(int doc) {
		return (float)doubleVal(doc);
	}

	@Override
	public int intVal(int doc) {
		return (int)doubleVal(doc);
	}

	@Override
	public long longVal(int doc) {
		return (long)doubleVal(doc);
	}

	@Override
	public boolean boolVal(int doc) {
		return doubleVal(doc) != 0;
	}

	@Override
	public abstract double doubleVal(int doc);

	@Override
	public String stringVal(int doc) {
		return Double.toString(doubleVal(doc));
	}

	@Override
	public Object objectVal(int doc) {
		return exists(doc) ? doubleVal(doc) : null;
	}

	@Override
	public String toString(int doc) {
		return mSource.getDescription() + '=' + stringVal(doc);
	}

	@Override
	public ValueFiller getValueFiller() {
		return new ValueFiller() {
			private final MutableValueDouble mVal = new MutableValueDouble();

			@Override
			public MutableValue getValue() {
				return mVal;
			}

			@Override
			public void fillValue(int doc) {
				mVal.set(doubleVal(doc));
				mVal.setExists(exists(doc));
			}
		};
	}

}
