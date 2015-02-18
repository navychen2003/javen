package org.javenstudio.falcon.search.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.hornet.query.BoolDocValues;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.hornet.query.source.ConstNumberSource;

public class BoolConstSource extends ConstNumberSource {
	
	private final boolean mConstant;

	public BoolConstSource(boolean constant) {
		mConstant = constant;
	}

	@Override
	public String getDescription() {
		return "const(" + mConstant + ")";
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		return new BoolDocValues(this) {
				@Override
				public boolean boolVal(int doc) {
					return mConstant;
				}
			};
	}

	@Override
	public int hashCode() {
		return mConstant ? 0x12345678 : 0x87654321;
	}

	@Override
	public boolean equals(Object o) {
		if (BoolConstSource.class != o.getClass()) 
			return false;
		
		BoolConstSource other = (BoolConstSource) o;
		return mConstant == other.mConstant;
	}

    @Override
    public int getInt() {
    	return mConstant ? 1 : 0;
    }

    @Override
    public long getLong() {
    	return mConstant ? 1 : 0;
    }

    @Override
    public float getFloat() {
    	return mConstant ? 1 : 0;
    }

    @Override
    public double getDouble() {
    	return mConstant ? 1 : 0;
    }

    @Override
    public Number getNumber() {
    	return mConstant ? 1 : 0;
    }

    @Override
    public boolean getBool() {
    	return mConstant;
    }
    
}
