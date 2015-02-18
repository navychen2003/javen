package org.javenstudio.common.indexdb.analysis;

import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.common.indexdb.util.NumericType;

/**
 * For AbstractField implements
 */
public abstract class TokenStreamField {

	private transient NumericTokenStream mNumericTokenStream = null;
	
	protected final void setNumericValue(int value) { 
		if (mNumericTokenStream != null) 
			mNumericTokenStream.setIntValue(value);
	}
	
	protected final void setNumericValue(long value) { 
		if (mNumericTokenStream != null) 
			mNumericTokenStream.setLongValue(value);
	}
	
	protected final void setNumericValue(float value) { 
		if (mNumericTokenStream != null) 
			mNumericTokenStream.setFloatValue(value);
	}
	
	protected final void setNumericValue(double value) { 
		if (mNumericTokenStream != null) 
			mNumericTokenStream.setDoubleValue(value);
	}
	
	protected final ITokenStream getNumericTokenStream(
			NumericType numericType, Number value, int numericPrecisionStep) {
		if (mNumericTokenStream == null) {
			// lazy init the TokenStream as it is heavy to instantiate
			// (attributes,...) if not needed (stored field loading)
			mNumericTokenStream = new NumericTokenStream(numericPrecisionStep);
			// initialize value in TokenStream
			final Number val = (Number) value;
			switch (numericType) {
			case INT:
				mNumericTokenStream.setIntValue(val.intValue());
				break;
			case LONG:
				mNumericTokenStream.setLongValue(val.longValue());
				break;
			case FLOAT:
				mNumericTokenStream.setFloatValue(val.floatValue());
				break;
			case DOUBLE:
				mNumericTokenStream.setDoubleValue(val.doubleValue());
				break;
			default:
				assert false : "Should never get here";
			}
		} else {
			// OK -- previously cached and we already updated if
			// setters were called.
		}

		return mNumericTokenStream;
	}
	
	protected final ITokenStream getNotTokenizedTokenStream(String value) { 
		return new SingleTokenStream(value);
	}
	
}
