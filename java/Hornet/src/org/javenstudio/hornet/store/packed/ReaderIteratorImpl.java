package org.javenstudio.hornet.store.packed;

import java.io.Closeable;
import java.io.IOException;

import org.javenstudio.common.indexdb.IDataInput;
import org.javenstudio.common.indexdb.util.LongsRef;

abstract class ReaderIteratorImpl implements ReaderIterator {

    protected final IDataInput mInput;
    protected final int mBitsPerValue;
    protected final int mValueCount;

    protected ReaderIteratorImpl(int valueCount, int bitsPerValue, IDataInput in) {
    	mInput = in;
    	mBitsPerValue = bitsPerValue;
    	mValueCount = valueCount;
    }

    @Override
    public long next() throws IOException {
    	LongsRef nextValues = next(1);
    	assert nextValues.mLength > 0;
    	
    	final long result = nextValues.mLongs[nextValues.mOffset];
    	++ nextValues.mOffset;
    	-- nextValues.mLength;
    	
    	return result;
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
    public void close() throws IOException {
    	if (mInput instanceof Closeable) 
    		((Closeable) mInput).close();
    }
    
}
