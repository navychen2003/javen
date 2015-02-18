package org.javenstudio.common.indexdb.store;

import org.javenstudio.common.indexdb.Constants;
import org.javenstudio.common.indexdb.util.BytePool;

/**
 * Class to write byte streams into slices of shared
 * byte[].  This is used by DocumentsWriter to hold the
 * posting list for many terms in RAM.
 */
public class ByteSliceWriter extends DataOutput {

	private final BytePool mPool;
	private byte[] mSlice;
	private int mUpto;
	private int mOffset;

	public ByteSliceWriter(BytePool pool) {
		mPool = pool;
	}

	/**
	 * Set up the writer to write at address.
	 */
	public void init(int address) {
		mSlice = mPool.getBufferAt(address >> BytePool.BYTE_BLOCK_SHIFT);
		assert mSlice != null;
		mUpto = address & BytePool.BYTE_BLOCK_MASK;
		mOffset = address;
		assert mUpto < mSlice.length;
	}

	/** Write byte into byte slice stream */
	@Override
	public void writeByte(byte b) {
		assert mSlice != null;
		if (mSlice[mUpto] != 0) {
			mUpto = mPool.allocSlice(mSlice, mUpto);
			mSlice = mPool.getBuffer();
			mOffset = mPool.getByteOffset();
			assert mSlice != null;
		}
		mSlice[mUpto++] = b;
		assert mUpto != mSlice.length;
	}

	@Override
	public void writeBytes(final byte[] b, int offset, final int len) {
		final int offsetEnd = offset + len;
		while (offset < offsetEnd) {
			if (mSlice[mUpto] != 0) {
				// End marker
				mUpto = mPool.allocSlice(mSlice, mUpto);
				mSlice = mPool.getBuffer();
				mOffset = mPool.getByteOffset();
			}

			mSlice[mUpto++] = b[offset++];
			assert mUpto != mSlice.length;
		}
	}

	public int getAddress() {
		return mUpto + (mOffset & Constants.BYTE_BLOCK_NOT_MASK);
	}
	
}