package org.javenstudio.hornet.query;

import org.javenstudio.hornet.util.MutableValue;
import org.javenstudio.hornet.util.MutableValueFloat;

/**
 * Abstract {@link FunctionValues} implementation which supports retrieving float values.
 * Implementations can control how the float values are loaded through {@link #floatVal(int)}}
 */
public abstract class FloatDocValues extends FunctionValues {
	
	protected final ValueSource mSource;

	public FloatDocValues(ValueSource vs) {
		mSource = vs;
	}

	@Override
	public byte byteVal(int doc) {
		return (byte)floatVal(doc);
	}

	@Override
	public short shortVal(int doc) {
		return (short)floatVal(doc);
	}

	@Override
	public abstract float floatVal(int doc);

	@Override
	public int intVal(int doc) {
		return (int)floatVal(doc);
	}

	@Override
	public long longVal(int doc) {
		return (long)floatVal(doc);
	}

	@Override
	public double doubleVal(int doc) {
		return (double)floatVal(doc);
	}

	@Override
	public String stringVal(int doc) {
		return Float.toString(floatVal(doc));
	}

	@Override
	public Object objectVal(int doc) {
		return exists(doc) ? floatVal(doc) : null;
	}

	@Override
	public String toString(int doc) {
		return mSource.getDescription() + '=' + stringVal(doc);
	}

	@Override
	public ValueFiller getValueFiller() {
		return new ValueFiller() {
			private final MutableValueFloat mVal = new MutableValueFloat();

			@Override
			public MutableValue getValue() {
				return mVal;
			}

			@Override
			public void fillValue(int doc) {
				mVal.set(floatVal(doc));
				mVal.setExists(exists(doc));
			}
		};
	}
	
}
