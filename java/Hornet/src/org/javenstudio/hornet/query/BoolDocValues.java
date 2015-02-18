package org.javenstudio.hornet.query;

import org.javenstudio.hornet.util.MutableValue;
import org.javenstudio.hornet.util.MutableValueBool;

/**
 * Abstract {@link FunctionValues} implementation which supports retrieving boolean values.
 * Implementations can control how the boolean values are loaded through {@link #boolVal(int)}}
 */
public abstract class BoolDocValues extends FunctionValues {
	
	protected final ValueSource mSource;

	public BoolDocValues(ValueSource vs) {
		mSource = vs;
	}

	@Override
	public abstract boolean boolVal(int doc);

	@Override
	public byte byteVal(int doc) {
		return boolVal(doc) ? (byte)1 : (byte)0;
	}

	@Override
	public short shortVal(int doc) {
		return boolVal(doc) ? (short)1 : (short)0;
	}

	@Override
	public float floatVal(int doc) {
		return boolVal(doc) ? (float)1 : (float)0;
	}

	@Override
	public int intVal(int doc) {
		return boolVal(doc) ? 1 : 0;
	}

	@Override
	public long longVal(int doc) {
		return boolVal(doc) ? (long)1 : (long)0;
	}

	@Override
	public double doubleVal(int doc) {
		return boolVal(doc) ? (double)1 : (double)0;
	}

	@Override
	public String stringVal(int doc) {
		return Boolean.toString(boolVal(doc));
	}

	@Override
	public Object objectVal(int doc) {
		return exists(doc) ? boolVal(doc) : null;
	}

	@Override
	public String toString(int doc) {
		return mSource.getDescription() + '=' + stringVal(doc);
	}

	@Override
	public ValueFiller getValueFiller() {
		return new ValueFiller() {
			private final MutableValueBool mVal = new MutableValueBool();

			@Override
			public MutableValue getValue() {
				return mVal;
			}

			@Override
			public void fillValue(int doc) {
				mVal.set(boolVal(doc));
				mVal.setExists(exists(doc));
			}
		};
	}
	
}
