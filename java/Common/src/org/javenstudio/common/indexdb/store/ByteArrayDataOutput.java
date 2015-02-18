package org.javenstudio.common.indexdb.store;

import org.javenstudio.common.indexdb.util.BytesRef;

/**
 * DataOutput backed by a byte array.
 * <b>WARNING:</b> This class omits most low-level checks,
 * so be sure to test heavily with assertions enabled.
 * 
 */
public class ByteArrayDataOutput extends DataOutput {
	
	private byte[] mBytes;
	private int mPos;
	private int mLimit;

	public ByteArrayDataOutput(byte[] bytes) {
		reset(bytes);
	}

	public ByteArrayDataOutput(byte[] bytes, int offset, int len) {
		reset(bytes, offset, len);
	}

	public ByteArrayDataOutput() {
		reset(BytesRef.EMPTY_BYTES);
	}

	public void reset(byte[] bytes) {
		reset(bytes, 0, bytes.length);
	}
  
	public void reset(byte[] bytes, int offset, int len) {
		mBytes = bytes;
		mPos = offset;
		mLimit = offset + len;
	}
  
	public int getPosition() {
		return mPos;
	}

	@Override
	public void writeByte(byte b) {
		assert mPos < mLimit;
		mBytes[mPos++] = b;
	}

	@Override
	public void writeBytes(byte[] b, int offset, int length) {
		assert mPos + length <= mLimit;
    	System.arraycopy(b, offset, mBytes, mPos, length);
    	mPos += length;
	}
	
}
