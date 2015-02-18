package org.javenstudio.hornet.query.source;

import org.javenstudio.hornet.query.DoubleDocValues;
import org.javenstudio.hornet.query.ValueSource;

public class ConstDoubleDocValues extends DoubleDocValues {
	
	private final ValueSource mParent;
	private final int mIntVal;
	private final float mFloatVal;
	private final double mDoubleVal;
	private final long mLongVal;
	private final String mStringVal;
	
	public ConstDoubleDocValues(double val, ValueSource parent) {
		super(parent);
		
		mParent = parent;
		mIntVal = (int)val;
		mFloatVal = (float)val;
		mDoubleVal = val;
		mLongVal = (long)val;
		mStringVal = Double.toString(val);
	}

	@Override
	public float floatVal(int doc) {
		return mFloatVal;
	}
	
	@Override
	public int intVal(int doc) {
		return mIntVal;
	}
	
	@Override
	public long longVal(int doc) {
		return mLongVal;
	}
	
	@Override
	public double doubleVal(int doc) {
		return mDoubleVal;
	}
	
	@Override
	public String stringVal(int doc) {
		return mStringVal;
	}
	
	@Override
	public String toString(int doc) {
		return mParent.getDescription() + '=' + mStringVal;
	}
	
}
