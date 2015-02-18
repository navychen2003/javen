package org.javenstudio.hornet.store.packed;

import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexInput;

final class DirectPacked64SingleBlockReader extends ReaderImpl {

	private final IIndexInput mInput;
	private final long mStartPointer;
	private final int mValuesPerBlock;
	private final long mMask;

	DirectPacked64SingleBlockReader(int bitsPerValue, int valueCount,
			IIndexInput in) {
		super(valueCount, bitsPerValue);
		mInput = in;
		mStartPointer = in.getFilePointer();
		mValuesPerBlock = 64 / bitsPerValue;
		mMask = ~(~0L << bitsPerValue);
	}

	@Override
	public long get(int index) {
		final int blockOffset = index / mValuesPerBlock;
		final long skip = ((long) blockOffset) << 3;
		try {
			mInput.seek(mStartPointer + skip);

			long block = mInput.readLong();
			final int offsetInBlock = index % mValuesPerBlock;
			return (block >>> (offsetInBlock * mBitsPerValue)) & mMask;
			
		} catch (IOException e) {
			throw new IllegalStateException("failed", e);
		}
	}

	@Override
	public long ramBytesUsed() {
		return 0;
	}
	
}
