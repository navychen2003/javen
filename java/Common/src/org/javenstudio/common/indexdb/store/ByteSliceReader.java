package org.javenstudio.common.indexdb.store;

import java.io.IOException;

import org.javenstudio.common.indexdb.util.BytePool;

/** 
 * IndexInput that knows how to read the byte slices written
 * by Posting and PostingVector.  We read the bytes in
 * each slice until we hit the end of that slice at which
 * point we read the forwarding address of the next slice
 * and then jump to it.
 */
public class ByteSliceReader extends DataInput {
	
	private BytePool mPool;
	private int mBufferUpto;
	private byte[] mBuffer;
	private int mUpto;
	private int mLimit;
	private int mLevel;
	private int mBufferOffset;
	private int mEndIndex;

	public ByteSliceReader() {}
	
	public void init(BytePool pool, int startIndex, int endIndex) {
		assert endIndex-startIndex >= 0;
		assert startIndex >= 0;
		assert endIndex >= 0;

		mPool = pool;
		mEndIndex = endIndex;

		mLevel = 0;
		mBufferUpto = startIndex / BytePool.BYTE_BLOCK_SIZE;
		mBufferOffset = mBufferUpto * BytePool.BYTE_BLOCK_SIZE;
		mBuffer = pool.getBufferAt(mBufferUpto);
		mUpto = startIndex & BytePool.BYTE_BLOCK_MASK;

		final int firstSize = BytePool.levelSizeArray[0];

		if (startIndex+firstSize >= endIndex) {
			// There is only this one slice to read
			mLimit = endIndex & BytePool.BYTE_BLOCK_MASK;
		} else
			mLimit = mUpto + firstSize - 4;
	}

	public boolean eof() {
		assert mUpto + mBufferOffset <= mEndIndex;
		return mUpto + mBufferOffset == mEndIndex;
	}

	@Override
	public byte readByte() {
		assert !eof();
		assert mUpto <= mLimit;
		if (mUpto == mLimit)
			nextSlice();
		return mBuffer[mUpto++];
	}

	public long writeTo(DataOutput out) throws IOException {
		long size = 0;
		while (true) {
			if (mLimit + mBufferOffset == mEndIndex) {
				assert mEndIndex - mBufferOffset >= mUpto;
				out.writeBytes(mBuffer, mUpto, mLimit-mUpto);
				size += mLimit-mUpto;
				break;
			} else {
				out.writeBytes(mBuffer, mUpto, mLimit-mUpto);
				size += mLimit-mUpto;
				nextSlice();
			}
		}

		return size;
	}

	public void nextSlice() {
		// Skip to our next slice
		final int nextIndex = 
				((mBuffer[mLimit]&0xff)<<24) + ((mBuffer[1+mLimit]&0xff)<<16) + 
				((mBuffer[2+mLimit]&0xff)<<8) + (mBuffer[3+mLimit]&0xff);

		mLevel = BytePool.nextLevelArray[mLevel];
		final int newSize = BytePool.levelSizeArray[mLevel];

		mBufferUpto = nextIndex / BytePool.BYTE_BLOCK_SIZE;
		mBufferOffset = mBufferUpto * BytePool.BYTE_BLOCK_SIZE;

		mBuffer = mPool.getBufferAt(mBufferUpto);
		mUpto = nextIndex & BytePool.BYTE_BLOCK_MASK;

		if (nextIndex + newSize >= mEndIndex) {
			// We are advancing to the final slice
			assert mEndIndex - nextIndex > 0;
			mLimit = mEndIndex - mBufferOffset;
		} else {
			// This is not the final slice (subtract 4 for the
			// forwarding address at the end of this new slice)
			mLimit = mUpto+newSize-4;
		}
	}

	@Override
	public void readBytes(byte[] b, int offset, int len) {
		while (len > 0) {
			final int numLeft = mLimit-mUpto;
			if (numLeft < len) {
				// Read entire slice
				System.arraycopy(mBuffer, mUpto, b, offset, numLeft);
				offset += numLeft;
				len -= numLeft;
				nextSlice();
			} else {
				// This slice is the last one
				System.arraycopy(mBuffer, mUpto, b, offset, len);
				mUpto += len;
				break;
			}
		}
	}
	
}