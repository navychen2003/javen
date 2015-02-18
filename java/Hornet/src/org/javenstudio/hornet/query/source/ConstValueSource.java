package org.javenstudio.hornet.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.hornet.query.FloatDocValues;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSourceContext;

/**
 * <code>ConstValueSource</code> returns a constant for all documents
 */
public class ConstValueSource extends ConstNumberSource {
	
	private final float mConstant;
	private final double mValue;

	public ConstValueSource(float constant) {
		mConstant = constant;
		mValue = constant;
	}

	@Override
	public String getDescription() {
		return "const(" + mConstant + ")";
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		return new FloatDocValues(this) {
			@Override
			public float floatVal(int doc) {
				return mConstant;
			}
			
			@Override
			public int intVal(int doc) {
				return (int)mConstant;
			}
			
			@Override
			public long longVal(int doc) {
				return (long)mConstant;
			}
			
			@Override
			public double doubleVal(int doc) {
				return mValue;
			}
			
			@Override
			public String toString(int doc) {
				return getDescription();
			}
			
			@Override
			public Object objectVal(int doc) {
				return mConstant;
			}
			
			@Override
			public boolean boolVal(int doc) {
				return mConstant != 0.0f;
			}
		};
	}

	@Override
	public int hashCode() {
		return Float.floatToIntBits(mConstant) * 31;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null || !(o instanceof ConstValueSource)) 
			return false;
		
		ConstValueSource other = (ConstValueSource)o;
		return  this.mConstant == other.mConstant;
	}

	@Override
	public int getInt() {
		return (int)mConstant;
	}

	@Override
	public long getLong() {
		return (long)mConstant;
	}

	@Override
	public float getFloat() {
		return mConstant;
	}

	@Override
	public double getDouble() {
		return mValue;
	}

	@Override
	public Number getNumber() {
		return mConstant;
	}

	@Override
	public boolean getBool() {
		return mConstant != 0.0f;
	}
	
}
