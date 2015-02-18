package org.javenstudio.hornet.query;

import org.javenstudio.hornet.util.MutableValue;
import org.javenstudio.hornet.util.MutableValueInt;

/**
 * Abstract {@link FunctionValues} implementation which supports retrieving int values.
 * Implementations can control how the int values are loaded through {@link #intVal(int)}
 */
public abstract class IntDocValues extends FunctionValues {
	
	protected final ValueSource mSource;

	public IntDocValues(ValueSource vs) {
		mSource = vs;
	}

	@Override
	public byte byteVal(int doc) {
		return (byte)intVal(doc);
	}

	@Override
	public short shortVal(int doc) {
		return (short)intVal(doc);
	}

	@Override
	public float floatVal(int doc) {
		return (float)intVal(doc);
	}

	@Override
	public abstract int intVal(int doc);

	@Override
	public long longVal(int doc) {
		return (long)intVal(doc);
	}

	@Override
	public double doubleVal(int doc) {
		return (double)intVal(doc);
	}

	@Override
	public String stringVal(int doc) {
		return Integer.toString(intVal(doc));
	}

	@Override
	public Object objectVal(int doc) {
		return exists(doc) ? intVal(doc) : null;
	}

	@Override
	public String toString(int doc) {
		return mSource.getDescription() + '=' + stringVal(doc);
	}

	@Override
	public ValueFiller getValueFiller() {
		return new ValueFiller() {
			private final MutableValueInt mVal = new MutableValueInt();

			@Override
			public MutableValue getValue() {
				return mVal;
			}

			@Override
			public void fillValue(int doc) {
				mVal.set(intVal(doc));
				mVal.setExists(exists(doc));
			}
		};
	}
	
}
