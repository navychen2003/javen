package org.javenstudio.hornet.store.packed;

import java.io.EOFException;
import java.io.IOException;

import org.javenstudio.common.indexdb.IDataInput;
import org.javenstudio.common.indexdb.util.LongsRef;

final class PackedReaderIteratorImpl extends ReaderIteratorImpl {
	
	private final Format mFormat;
	private final BulkOperation mBulkOperation;
	
	private final long[] mNextBlocks;
	private final LongsRef mNextValues;
	private final int mIterations;
	
	private int mPosition;

	public PackedReaderIteratorImpl(Format format, int valueCount, int bitsPerValue, 
			IDataInput in, int mem) {
	    super(valueCount, bitsPerValue, in);
	    
	    mFormat = format;
	    mBulkOperation = BulkOperation.of(format, bitsPerValue);
	    mIterations = mBulkOperation.computeIterations(valueCount, mem);
	    assert mIterations > 0;
	    
	    mNextBlocks = new long[mIterations * mBulkOperation.blocks()];
	    mNextValues = new LongsRef(new long[mIterations * mBulkOperation.values()], 0, 0);
	    assert mIterations * mBulkOperation.values() == mNextValues.mLongs.length;
	    assert mIterations * mBulkOperation.blocks() == mNextBlocks.length;
	    
	    mNextValues.mOffset = mNextValues.mLongs.length;
	    mPosition = -1;
	}

	@Override
	public LongsRef next(int count) throws IOException {
	    assert mNextValues.mLength >= 0;
	    assert count > 0;
	    assert mNextValues.mOffset + mNextValues.mLength <= mNextValues.mLongs.length;
	    
	    mNextValues.mOffset += mNextValues.mLength;

	    final int remaining = mValueCount - mPosition - 1;
	    if (remaining <= 0) 
	    	throw new EOFException();
	    
	    count = Math.min(remaining, count);

	    if (mNextValues.mOffset == mNextValues.mLongs.length) {
	    	final int remainingBlocks = mFormat.nblocks(mBitsPerValue, remaining);
	    	final int blocksToRead = Math.min(remainingBlocks, mNextBlocks.length);
	      
	    	for (int i = 0; i < blocksToRead; ++i) {
	    		mNextBlocks[i] = mInput.readLong();
	    	}
	    	for (int i = blocksToRead; i < mNextBlocks.length; ++i) {
	    		mNextBlocks[i] = 0L;
	    	}

	    	mBulkOperation.get(mNextBlocks, 0, mNextValues.mLongs, 0, mIterations);
	    	mNextValues.mOffset = 0;
	    }

	    mNextValues.mLength = Math.min(mNextValues.mLongs.length - mNextValues.mOffset, count);
	    mPosition += mNextValues.mLength;
	    
	    return mNextValues;
	}

	@Override
	public int ord() {
	    return mPosition;
	}
	
}
