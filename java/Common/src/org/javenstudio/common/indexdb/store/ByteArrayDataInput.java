package org.javenstudio.common.indexdb.store;

import java.io.IOException;

import org.javenstudio.common.indexdb.util.BytesRef;

/** 
 * DataInput backed by a byte array.
 * <b>WARNING:</b> This class omits all low-level checks.
 * 
 */
public final class ByteArrayDataInput extends DataInput {

	private byte[] mBytes;
	private int mPos;
	private int mLimit;

	public ByteArrayDataInput(byte[] bytes) {
		reset(bytes);
	}

	public ByteArrayDataInput(byte[] bytes, int offset, int len) {
		reset(bytes, offset, len);
	}

	public ByteArrayDataInput() {
		reset(BytesRef.EMPTY_BYTES);
	}

	public void reset(byte[] bytes) {
		reset(bytes, 0, bytes.length);
	}

	// NOTE: sets pos to 0, which is not right if you had
	// called reset w/ non-zero offset!!
	public void rewind() {
		mPos = 0;
	}

	public int getPosition() {
		return mPos;
	}
  
	public void setPosition(int pos) {
		mPos = pos;
	}

	public void reset(byte[] bytes, int offset, int len) {
		mBytes = bytes;
		mPos = offset;
		mLimit = offset + len;
	}

	public int length() {
		return mLimit;
	}

	public boolean eof() {
		return mPos == mLimit;
	}

	public void skipBytes(int count) {
		mPos += count;
	}

	@Override
	public short readShort() {
		return (short) (((mBytes[mPos++] & 0xFF) <<  8) | (mBytes[mPos++] & 0xFF));
	}
 
	@Override
	public int readInt() {
		return ((mBytes[mPos++] & 0xFF) << 24) | ((mBytes[mPos++] & 0xFF) << 16)
			 | ((mBytes[mPos++] & 0xFF) <<  8) |  (mBytes[mPos++] & 0xFF);
	}
 
	@Override
	public long readLong() {
		final int i1 = ((mBytes[mPos++] & 0xff) << 24) | ((mBytes[mPos++] & 0xff) << 16) |
				((mBytes[mPos++] & 0xff) << 8) | (mBytes[mPos++] & 0xff);
		final int i2 = ((mBytes[mPos++] & 0xff) << 24) | ((mBytes[mPos++] & 0xff) << 16) |
				((mBytes[mPos++] & 0xff) << 8) | (mBytes[mPos++] & 0xff);
		return (((long)i1) << 32) | (i2 & 0xFFFFFFFFL);
	}

	@Override
	public int readVInt() throws IOException {
		return super.readVInt();
	}
 
	@Override
	public long readVLong() throws IOException {
		return super.readVLong();
	}

	// NOTE: AIOOBE not EOF if you read too much
	@Override
	public byte readByte() {
		return mBytes[mPos++];
	}

	// NOTE: AIOOBE not EOF if you read too much
	@Override
	public void readBytes(byte[] b, int offset, int len) {
		System.arraycopy(mBytes, mPos, b, offset, len);
		mPos += len;
	}
	
}
