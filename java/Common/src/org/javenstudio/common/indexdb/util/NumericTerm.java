package org.javenstudio.common.indexdb.util;

/**
 * The term numeric of a Token.
 */
public class NumericTerm {

	private long mValue = 0L;
    private int mValueSize = 0; 
    private int mShift = 0; 
    private int mPrecisionStep = 0;
    
    public NumericTerm() {}
    
    public NumericTerm(NumericTerm t) { 
    	copyFrom(t);
    }
    
    public void copyFrom(NumericTerm t) { 
    	if (t != null) { 
    		mValue = t.mValue;
    		mValueSize = t.mValueSize;
    		mShift = t.mShift;
    		mPrecisionStep = t.mPrecisionStep;
    	}
    }
    
    public int fillBytesRef(BytesRef bytes) {
    	try {
    		assert mValueSize == 64 || mValueSize == 32;
    		return (mValueSize == 64) ? 
    				NumericUtil.longToPrefixCoded(mValue, mShift, bytes) :
    				NumericUtil.intToPrefixCoded((int) mValue, mShift, bytes);
    	} catch (IllegalArgumentException iae) {
    		// return empty token before first or after last
    		bytes.mLength = 0;
    		return 0;
    	}
    }

    /** Returns current shift value, undefined before first token */
    public int getShift() { return mShift; }
    
    public void setShift(int shift) { 
    	mShift = shift; 
    }
    
    public int increaseShift() {
    	return (mShift += mPrecisionStep);
    }

    /** 
     * Returns current token's raw value as {@code long} with all {@link #getShift} applied, 
     * undefined before first token 
     */
    public long getRawValue() { 
    	return mValue  & ~((1L << mShift) - 1L); 
    }
    
    /** 
     * Returns value size in bits (32 for {@code float}, {@code int}; 
     * 64 for {@code double}, {@code long}) 
     */
    public int getValueSize() { return mValueSize; }

    public void init(long value, int valueSize, int precisionStep, int shift) {
    	mValue = value;
    	mValueSize = valueSize;
    	mPrecisionStep = precisionStep;
    	mShift = shift;
    }

    public void clear() {
    	// this attribute has no contents to clear!
    	// we keep it untouched as it's fully controlled by outer class.
    }
    
}
