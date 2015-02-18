package org.javenstudio.hornet.store.packed;

/**
 * A simple base for Readers that keeps track of valueCount and bitsPerValue.
 * 
 */
abstract class ReaderImpl extends IntsReader {
	
	protected final int mBitsPerValue;
    protected final int mValueCount;

    protected ReaderImpl(int valueCount, int bitsPerValue) {
    	mBitsPerValue = bitsPerValue;
    	assert bitsPerValue > 0 && bitsPerValue <= 64 : "bitsPerValue=" + bitsPerValue;
    	mValueCount = valueCount;
    }

    @Override
    public int getBitsPerValue() {
    	return mBitsPerValue;
    }

    @Override
    public int size() {
    	return mValueCount;
    }

    @Override
    public Object getArray() {
    	return null;
    }

    @Override
    public boolean hasArray() {
    	return false;
    }

    @Override
    public int get(int index, long[] arr, int off, int len) {
    	assert len > 0 : "len must be > 0 (got " + len + ")";
    	assert index >= 0 && index < mValueCount;
    	assert off + len <= arr.length;

    	final int gets = Math.min(mValueCount - index, len);
    	for (int i = index, o = off, end = index + gets; i < end; ++i, ++o) {
    		arr[o] = get(i);
    	}
    	return gets;
    }
    
}
