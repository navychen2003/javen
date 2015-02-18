package org.javenstudio.falcon.search.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.LongDocValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.hornet.query.source.ConstNumberSource;
import org.javenstudio.falcon.search.query.FunctionQueryBuilder;
import org.javenstudio.falcon.search.query.ValueSourceParser;

//Private for now - we need to revisit how to handle typing in function queries
public class LongConstSource extends ConstNumberSource {
	
	public static class Parser extends ValueSourceParser { 
		@Override
		public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
			return new LongConstSource(Thread.currentThread().getId());
		}
	}
	
	private final long mConstant;
	private final double mDoubleVal;
	private final float mFloatVal;

	public LongConstSource(long constant) {
		mConstant = constant;
		mDoubleVal = constant;
		mFloatVal = constant;
	}

	@Override
	public String getDescription() {
		return "const(" + mConstant + ")";
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		return new LongDocValues(this) {
			@Override
			public float floatVal(int doc) {
				return mFloatVal;
			}

			@Override
			public int intVal(int doc) {
				return (int) mConstant;
			}

			@Override
			public long longVal(int doc) {
				return mConstant;
			}

			@Override
			public double doubleVal(int doc) {
				return mDoubleVal;
			}

			@Override
			public String toString(int doc) {
				return getDescription();
			}
		};
	}

	@Override
	public int hashCode() {
		return (int) mConstant + (int) (mConstant >>> 32);
	}

	@Override
	public boolean equals(Object o) {
		if (LongConstSource.class != o.getClass()) 
			return false;
		
		LongConstSource other = (LongConstSource) o;
		return this.mConstant == other.mConstant;
	}

	@Override
	public int getInt() {
		return (int)mConstant;
	}

	@Override
	public long getLong() {
		return mConstant;
	}

	@Override
	public float getFloat() {
		return mFloatVal;
	}

	@Override
	public double getDouble() {
		return mDoubleVal;
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
