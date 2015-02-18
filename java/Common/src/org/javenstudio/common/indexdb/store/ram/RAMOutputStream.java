package org.javenstudio.common.indexdb.store.ram;

import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IIndexInput;
import org.javenstudio.common.indexdb.IIndexOutput;
import org.javenstudio.common.indexdb.store.IndexOutput;

/**
 * A memory-resident {@link IndexOutput} implementation.
 */
public class RAMOutputStream extends IndexOutput {
	static final int BUFFER_SIZE = 1024;

	private RAMFile mFile;

	private byte[] mCurrentBuffer;
	private int mCurrentBufferIndex;
  
	private int mBufferPosition;
	private long mBufferStart;
	private int mBufferLength;

	/** Construct an empty output buffer. */
	public RAMOutputStream(IIndexContext context) {
		this(context, new RAMFile());
	}

	public RAMOutputStream(IIndexContext context, RAMFile f) {
		super(context);
		mFile = f;

		// make sure that we switch to the
		// first needed buffer lazily
		mCurrentBufferIndex = -1;
		mCurrentBuffer = null;
	}

	/** Copy the current contents of this buffer to the named output. */
	public void writeTo(IIndexOutput out) throws IOException {
		flush();
		
		final long end = mFile.mLength;
		long pos = 0;
		int buffer = 0;
		
		while (pos < end) {
			int length = BUFFER_SIZE;
			long nextPos = pos + length;
			if (nextPos > end)   	// at the last buffer
				length = (int)(end - pos);
			
			out.writeBytes(mFile.getBuffer(buffer++), length);
			pos = nextPos;
		}
	}

	/** 
	 * Copy the current contents of this buffer to output
	 *  byte array 
	 */
	public void writeTo(byte[] bytes, int offset) throws IOException {
		flush();
		
		final long end = mFile.mLength;
		long pos = 0;
		int buffer = 0;
		int bytesUpto = offset;
		
		while (pos < end) {
			int length = BUFFER_SIZE;
			long nextPos = pos + length;
			if (nextPos > end) 		// at the last buffer
				length = (int)(end - pos);
			
			System.arraycopy(mFile.getBuffer(buffer++), 0, bytes, bytesUpto, length);
			bytesUpto += length;
			pos = nextPos;
		}
	}

	/** Resets this to an empty file. */
	public void reset() {
		mCurrentBuffer = null;
		mCurrentBufferIndex = -1;
		mBufferPosition = 0;
		mBufferStart = 0;
		mBufferLength = 0;
		mFile.setLength(0);
	}

	@Override
	public void close() throws IOException {
		flush();
	}

	@Override
	public void seek(long pos) throws IOException {
		// set the file length in case we seek back
		// and flush() has not been called yet
		setFileLength();
		if (pos < mBufferStart || pos >= mBufferStart + mBufferLength) {
			mCurrentBufferIndex = (int) (pos / BUFFER_SIZE);
			switchCurrentBuffer();
		}

		mBufferPosition = (int) (pos % BUFFER_SIZE);
	}

	@Override
	public long length() {
		return mFile.mLength;
	}

	@Override
	public void writeByte(byte b) throws IOException {
		if (mBufferPosition == mBufferLength) {
			mCurrentBufferIndex ++;
			switchCurrentBuffer();
		}
		mCurrentBuffer[mBufferPosition++] = b;
	}

	@Override
	public void writeBytes(byte[] b, int offset, int len) throws IOException {
		assert b != null;
		
		while (len > 0) {
			if (mBufferPosition == mBufferLength) {
				mCurrentBufferIndex ++;
				switchCurrentBuffer();
			}

			int remainInBuffer = mCurrentBuffer.length - mBufferPosition;
			int bytesToCopy = len < remainInBuffer ? len : remainInBuffer;
			
			System.arraycopy(b, offset, mCurrentBuffer, mBufferPosition, bytesToCopy);
			
			offset += bytesToCopy;
			len -= bytesToCopy;
			mBufferPosition += bytesToCopy;
		}
	}

	private final void switchCurrentBuffer() throws IOException {
		if (mCurrentBufferIndex == mFile.numBuffers()) 
			mCurrentBuffer = mFile.addBuffer(BUFFER_SIZE);
		else 
			mCurrentBuffer = mFile.getBuffer(mCurrentBufferIndex);
		
		mBufferPosition = 0;
		mBufferStart = (long) BUFFER_SIZE * (long)mCurrentBufferIndex;
		mBufferLength = mCurrentBuffer.length;
	}

	private void setFileLength() {
		long pointer = mBufferStart + mBufferPosition;
		if (pointer > mFile.mLength) 
			mFile.setLength(pointer);
	}

	@Override
	public void flush() throws IOException {
		setFileLength();
	}

	@Override
	public long getFilePointer() {
		return mCurrentBufferIndex < 0 ? 0 : mBufferStart + mBufferPosition;
	}

	/** Returns byte usage of all buffers. */
	public long sizeInBytes() {
		return (long) mFile.numBuffers() * (long) BUFFER_SIZE;
	}
  
	@Override
	public void copyBytes(IIndexInput input, long numBytes) throws IOException {
		assert numBytes >= 0: "numBytes=" + numBytes;

		while (numBytes > 0) {
			if (mBufferPosition == mBufferLength) {
				mCurrentBufferIndex ++;
				switchCurrentBuffer();
			}

			int toCopy = mCurrentBuffer.length - mBufferPosition;
			if (numBytes < toCopy) 
				toCopy = (int) numBytes;
			
			input.readBytes(mCurrentBuffer, mBufferPosition, toCopy);
			numBytes -= toCopy;
			mBufferPosition += toCopy;
		}
	}
  
}
