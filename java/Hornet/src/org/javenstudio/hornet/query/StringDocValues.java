package org.javenstudio.hornet.query;

import org.javenstudio.hornet.util.MutableValue;
import org.javenstudio.hornet.util.MutableValueStr;

/**
 * Abstract {@link FunctionValues} implementation which supports retrieving String values.
 * Implementations can control how the String values are loaded through {@link #strVal(int)}}
 */
public abstract class StringDocValues extends FunctionValues {
	
	protected final ValueSource mSource;

	public StringDocValues(ValueSource vs) {
		mSource = vs;
	}

	@Override
	public abstract String stringVal(int doc);

	@Override
	public Object objectVal(int doc) {
		return exists(doc) ? stringVal(doc) : null;
	}

	@Override
	public boolean boolVal(int doc) {
		return exists(doc);
	}

	@Override
	public String toString(int doc) {
		return mSource.getDescription() + "='" + stringVal(doc) + "'";
	}

	@Override
	public ValueFiller getValueFiller() {
		return new ValueFiller() {
				private final MutableValueStr mVal = new MutableValueStr();
	
				@Override
				public MutableValue getValue() {
					return mVal;
				}
	
				@Override
				public void fillValue(int doc) {
					mVal.setExists(bytesVal(doc, mVal.get()));
				}
			};
	}
	
}
