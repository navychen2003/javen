package org.javenstudio.hornet.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.hornet.query.DoubleDocValues;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSourceContext;

/**
 * Function that returns a constant double value for every document.
 */
public class DoubleConstValueSource extends ConstNumberSource {
	
	private final double mConstant;
	private final float mFloatVal;
	private final long mLongVal;

	public DoubleConstValueSource(double constant) {
		mConstant = constant;
		mFloatVal = (float)constant;
		mLongVal = (long)constant;
	}

	@Override
	public String getDescription() {
		return "const(" + mConstant + ")";
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		return new DoubleDocValues(this) {
			@Override
			public float floatVal(int doc) {
				return mFloatVal;
			}

			@Override
			public int intVal(int doc) {
				return (int) mLongVal;
			}

			@Override
			public long longVal(int doc) {
				return mLongVal;
			}

			@Override
			public double doubleVal(int doc) {
				return mConstant;
			}

			@Override
			public String stringVal(int doc) {
				return Double.toString(mConstant);
			}

			@Override
			public Object objectVal(int doc) {
				return mConstant;
			}

			@Override
			public String toString(int doc) {
				return getDescription();
			}
		};
	}

	@Override
	public int hashCode() {
		long bits = Double.doubleToRawLongBits(mConstant);
		return (int)(bits ^ (bits >>> 32));
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null || !(o instanceof DoubleConstValueSource)) 
			return false;
		
		DoubleConstValueSource other = (DoubleConstValueSource) o;
		return this.mConstant == other.mConstant;
	}

	@Override
	public int getInt() {
		return (int)mLongVal;
	}

	@Override
  	public long getLong() {
		return mLongVal;
	}

	@Override
	public float getFloat() {
		return mFloatVal;
	}

	@Override
	public double getDouble() {
		return mConstant;
	}

	@Override
	public Number getNumber() {
		return mConstant;
	}

	@Override
	public boolean getBool() {
		return mConstant != 0;
	}
	
}
