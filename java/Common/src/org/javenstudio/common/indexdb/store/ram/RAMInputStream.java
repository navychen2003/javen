package org.javenstudio.common.indexdb.store.ram;

import java.io.IOException;
import java.io.EOFException;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IIndexOutput;
import org.javenstudio.common.indexdb.store.IndexInput;

/** 
 * A memory-resident {@link IndexInput} implementation. 
 */
public class RAMInputStream extends IndexInput implements Cloneable {
	static final int BUFFER_SIZE = RAMOutputStream.BUFFER_SIZE;

	private RAMFile mFile;
	private long mLength;

	private byte[] mCurrentBuffer;
	private int mCurrentBufferIndex;
  
	private int mBufferPosition;
	private long mBufferStart;
	private int mBufferLength;

	public RAMInputStream(IIndexContext context, RAMFile f) 
			throws IOException {
		super(context);
		mFile = f;
		mLength = mFile.mLength;
		if (mLength/BUFFER_SIZE >= Integer.MAX_VALUE) 
			throw new IOException("RAMInputStream too large length=" + mLength); 

		// make sure that we switch to the
		// first needed buffer lazily
		mCurrentBufferIndex = -1;
		mCurrentBuffer = null;
	}

	@Override
	public void close() {
		// nothing to do here
	}

	@Override
	public long length() {
		return mLength;
	}

	@Override
	public byte readByte() throws IOException {
		if (mBufferPosition >= mBufferLength) {
			mCurrentBufferIndex ++;
			switchCurrentBuffer(true);
		}
		return mCurrentBuffer[mBufferPosition++];
	}

	@Override
	public void readBytes(byte[] b, int offset, int len) throws IOException {
		while (len > 0) {
			if (mBufferPosition >= mBufferLength) {
				mCurrentBufferIndex ++;
				switchCurrentBuffer(true);
			}

			int remainInBuffer = mBufferLength - mBufferPosition;
			int bytesToCopy = len < remainInBuffer ? len : remainInBuffer;
			System.arraycopy(mCurrentBuffer, mBufferPosition, b, offset, bytesToCopy);
			offset += bytesToCopy;
			len -= bytesToCopy;
			mBufferPosition += bytesToCopy;
		}
	}

	private final void switchCurrentBuffer(boolean enforceEOF) throws IOException {
		mBufferStart = (long) BUFFER_SIZE * (long)mCurrentBufferIndex;
		if (mCurrentBufferIndex >= mFile.numBuffers()) {
			// end of file reached, no more buffers left
			if (enforceEOF) {
				throw new EOFException("read past EOF: " + this);
			} else {
				// Force EOF if a read takes place at this position
				mCurrentBufferIndex --;
				mBufferPosition = BUFFER_SIZE;
			}
		} else {
			mCurrentBuffer = mFile.getBuffer(mCurrentBufferIndex);
			mBufferPosition = 0;
			long buflen = mLength - mBufferStart;
			mBufferLength = buflen > BUFFER_SIZE ? BUFFER_SIZE : (int) buflen;
		}
	}

	@Override
	public void copyBytes(IIndexOutput out, long numBytes) throws IOException {
		assert numBytes >= 0: "numBytes=" + numBytes;
    
		long left = numBytes;
		while (left > 0) {
			if (mBufferPosition == mBufferLength) {
				++ mCurrentBufferIndex;
				switchCurrentBuffer(true);
			}
      
			final int bytesInBuffer = mBufferLength - mBufferPosition;
			final int toCopy = (int) (bytesInBuffer < left ? bytesInBuffer : left);
			out.writeBytes(mCurrentBuffer, mBufferPosition, toCopy);
			mBufferPosition += toCopy;
			left -= toCopy;
		}
    
		assert left == 0: "Insufficient bytes to copy: numBytes=" + numBytes + 
				" copied=" + (numBytes - left);
	}
  
	@Override
	public long getFilePointer() {
		return mCurrentBufferIndex < 0 ? 0 : mBufferStart + mBufferPosition;
	}

	@Override
	public void seek(long pos) throws IOException {
		if (mCurrentBuffer == null || pos < mBufferStart || pos >= mBufferStart + BUFFER_SIZE) {
			mCurrentBufferIndex = (int) (pos / BUFFER_SIZE);
			switchCurrentBuffer(false);
		}
		mBufferPosition = (int) (pos % BUFFER_SIZE);
	}
	
}
