package org.javenstudio.hornet.store.packed;

import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexInput;

/** Reads directly from disk on each get */
final class DirectPackedReader extends ReaderImpl {
	private static final int BLOCK_BITS = Packed64.BLOCK_BITS;
	private static final int MOD_MASK = Packed64.MOD_MASK;
	
	private final IIndexInput mInput;
	private final long mStartPointer;

	// masks[n-1] masks for bottom n bits
	private final long[] mMasks;

	public DirectPackedReader(int bitsPerValue, int valueCount, IIndexInput in) {
		super(valueCount, bitsPerValue);
		mInput = in;

		long v = 1;
		mMasks = new long[bitsPerValue];
		for (int i = 0; i < bitsPerValue; i++) {
			v *= 2;
			mMasks[i] = v - 1;
		}

		mStartPointer = in.getFilePointer();
	}

	@Override
	public long get(int index) {
		final long majorBitPos = (long)index * mBitsPerValue;
		final int elementPos = (int)(majorBitPos >>> BLOCK_BITS); // / BLOCK_SIZE
		final int bitPos =     (int)(majorBitPos & MOD_MASK); // % BLOCK_SIZE);

		final long result;
		try {
			mInput.seek(mStartPointer + (elementPos << 3));
			
			final long l1 = mInput.readLong();
			final int bits1 = 64 - bitPos;
			if (bits1 >= mBitsPerValue) { // not split
				result = l1 >> (bits1-mBitsPerValue) & mMasks[mBitsPerValue-1];
			
			} else {
				final int bits2 = mBitsPerValue - bits1;
				final long result1 = (l1 & mMasks[bits1-1]) << bits2;
				final long l2 = mInput.readLong();
				final long result2 = l2 >> (64 - bits2) & mMasks[bits2-1];
				result = result1 | result2;
			}

			return result;
		} catch (IOException ioe) {
			throw new IllegalStateException("failed", ioe);
		}
	}

	@Override
	public long ramBytesUsed() {
		return 0;
	}
	
}
