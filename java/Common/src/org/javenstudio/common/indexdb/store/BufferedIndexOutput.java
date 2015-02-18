package org.javenstudio.common.indexdb.store;

import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexContext;

/** Base implementation class for buffered {@link IndexOutput}. */
public abstract class BufferedIndexOutput extends IndexOutput {

	private final int mBufferSize;
	private final byte[] mBuffer;
	private long mBufferStart = 0;           // position in file of buffer
	private int mBufferPosition = 0;         // position in buffer

	public BufferedIndexOutput(IIndexContext context) { 
		this(context, context.getOutputBufferSize());
	}
	
	public BufferedIndexOutput(IIndexContext context, int bufferSize) { 
		super(context);
		
		checkBufferSize(bufferSize);
		mBufferSize = bufferSize;
		mBuffer = new byte[bufferSize];
	}
	
	private void checkBufferSize(int bufferSize) {
		if (bufferSize <= 0)
			throw new IllegalArgumentException("bufferSize must be greater than 0 (got " + bufferSize + ")");
	}
	
	/** 
	 * Writes a single byte.
	 * @see IndexInput#readByte()
	 */
	@Override
	public void writeByte(byte b) throws IOException {
		if (mBufferPosition >= mBufferSize)
			flush();
		mBuffer[mBufferPosition++] = b;
	}

	/** 
	 * Writes an array of bytes.
	 * @param b the bytes to write
	 * @param length the number of bytes to write
	 * @see IndexInput#readBytes(byte[],int,int)
	 */
	@Override
	public void writeBytes(byte[] b, int offset, int length) throws IOException {
		int bytesLeft = mBufferSize - mBufferPosition;
		
		// is there enough space in the buffer?
		if (bytesLeft >= length) {
			// we add the data to the end of the buffer
			System.arraycopy(b, offset, mBuffer, mBufferPosition, length);
			mBufferPosition += length;
			// if the buffer is full, flush it
			if (mBufferSize - mBufferPosition == 0)
				flush();
			
		// is data larger then buffer?
		} else if (length > mBufferSize) {
			// we flush the buffer
			if (mBufferPosition > 0)
				flush();
			// and write data at once
			flushBuffer(b, offset, length);
			mBufferStart += length;
			
		} else {
			// we fill/flush the buffer (until the input is written)
			int pos = 0; // position in the input data
			int pieceLength;
			
			while (pos < length) {
				pieceLength = (length - pos < bytesLeft) ? length - pos : bytesLeft;
				System.arraycopy(b, pos + offset, mBuffer, mBufferPosition, pieceLength);
				pos += pieceLength;
				mBufferPosition += pieceLength;
				
				// if the buffer is full, flush it
				bytesLeft = mBufferSize - mBufferPosition;
				if (bytesLeft == 0) {
					flush();
					bytesLeft = mBufferSize;
				}
			}
		}
	}

	/** Forces any buffered output to be written. */
	@Override
	public void flush() throws IOException {
		flushBuffer(mBuffer, mBufferPosition);
		mBufferStart += mBufferPosition;
		mBufferPosition = 0;
	}

	/** 
	 * Expert: implements buffer write.  Writes bytes at the current position in
	 * the output.
	 * @param b the bytes to write
	 * @param len the number of bytes to write
	 */
	private void flushBuffer(byte[] b, int len) throws IOException {
		flushBuffer(b, 0, len);
	}

	/** 
	 * Expert: implements buffer write.  Writes bytes at the current position in
	 * the output.
	 * @param b the bytes to write
	 * @param offset the offset in the byte array
	 * @param len the number of bytes to write
	 */
	protected abstract void flushBuffer(byte[] b, int offset, int len) throws IOException;
  
	/** Closes this stream to further operations. */
	@Override
	public void close() throws IOException {
		flush();
		onClosed();
	}

	/** 
	 * Returns the current position in this file, where the next write will
	 * occur.
	 * @see #seek(long)
	 */
	@Override
	public long getFilePointer() {
		return mBufferStart + mBufferPosition;
	}

	/** 
	 * Sets current position in this file, where the next write will occur.
	 * @see #getFilePointer()
	 */
	@Override
	public void seek(long pos) throws IOException {
		flush();
		mBufferStart = pos;
	}

	/** The number of bytes in the file. */
	@Override
	public abstract long length() throws IOException;

}
