package org.javenstudio.common.indexdb.store;

import java.io.EOFException;
import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IIndexOutput;

/** Base implementation class for buffered {@link IndexInput}. */
public abstract class BufferedIndexInput extends IndexInput {

	private static class BufferedInput implements IndexUtil.Input { 
		private final BufferedIndexInput mInput;
		
		public BufferedInput(BufferedIndexInput input) { 
			mInput = input;
		}
		
		@Override
		public byte readByte() throws IOException {
			return mInput.mBuffer[mInput.mBufferPosition++];
		}
	}
	
	private BufferedInput mBufferedInput;
	private int mBufferSize;
	protected byte[] mBuffer;
  
	private long mBufferStart = 0;			// position in file of buffer
	private int mBufferLength = 0;			// end of valid bytes
	private int mBufferPosition = 0;		// next byte to read

	@Override
	public final byte readByte() throws IOException {
		if (mBufferPosition >= mBufferLength)
			refill();
		return mBuffer[mBufferPosition++];
	}

	public BufferedIndexInput(IIndexContext context) {
		this(context, context.getInputBufferSize());
	}
  
	/** Inits BufferedIndexInput with a specific bufferSize */
	public BufferedIndexInput(IIndexContext context, int bufferSize) {
		super(context);
		
		checkBufferSize(bufferSize);
		mBufferSize = bufferSize;
		mBufferedInput = new BufferedInput(this);
	}

	/** Change the buffer size used by this IndexInput */
	public final void setBufferSize(int newSize) {
		assert mBuffer == null || mBufferSize == mBuffer.length: 
			"buffer=" + mBuffer + " bufferSize=" + mBufferSize + " buffer.length=" 
				+ (mBuffer != null ? mBuffer.length : 0);
		
		if (newSize != mBufferSize) {
			checkBufferSize(newSize);
			
			mBufferSize = newSize;
			if (mBuffer != null) {
				// Resize the existing buffer and carefully save as
				// many bytes as possible starting from the current
				// bufferPosition
				byte[] newBuffer = new byte[newSize];
				final int leftInBuffer = mBufferLength-mBufferPosition;
				final int numToCopy;
				if (leftInBuffer > newSize)
					numToCopy = newSize;
				else
					numToCopy = leftInBuffer;
				
				System.arraycopy(mBuffer, mBufferPosition, newBuffer, 0, numToCopy);
				
				mBufferStart += mBufferPosition;
				mBufferPosition = 0;
				mBufferLength = numToCopy;
				
				newBuffer(newBuffer);
			}
		}
	}

	protected void newBuffer(byte[] newBuffer) {
		// Subclasses can do something here
		mBuffer = newBuffer;
	}

	/** Returns buffer size.  @see #setBufferSize */
	public final int getBufferSize() {
		return mBufferSize;
	}

	private void checkBufferSize(int bufferSize) {
		if (bufferSize <= 0)
			throw new IllegalArgumentException("bufferSize must be greater than 0 (got " + bufferSize + ")");
	}

	@Override
	public final void readBytes(byte[] b, int offset, int len) throws IOException {
		readBytes(b, offset, len, true);
	}

	@Override
	public final void readBytes(byte[] b, int offset, int len, boolean useBuffer) throws IOException {
		if (len <= (mBufferLength - mBufferPosition)){
			// the buffer contains enough data to satisfy this request
			if (len > 0) // to allow b to be null if len is 0...
				System.arraycopy(mBuffer, mBufferPosition, b, offset, len);
			mBufferPosition += len;
			
		} else {
			// the buffer does not have enough data. First serve all we've got.
			int available = mBufferLength - mBufferPosition;
			if (available > 0){
				System.arraycopy(mBuffer, mBufferPosition, b, offset, available);
				offset += available;
				len -= available;
				mBufferPosition += available;
			}
			
			// and now, read the remaining 'len' bytes:
			if (useBuffer && len<mBufferSize){
				// If the amount left to read is small enough, and
				// we are allowed to use our buffer, do it in the usual
				// buffered way: fill the buffer and copy from it:
				refill();
				
				if (mBufferLength < len){
					// Throw an exception when refill() could not read len bytes:
					System.arraycopy(mBuffer, 0, b, offset, mBufferLength);
					throw new EOFException("read past EOF: " + this);
					
				} else {
					System.arraycopy(mBuffer, 0, b, offset, len);
					mBufferPosition=len;
				}
				
			} else {
				// The amount left to read is larger than the buffer
				// or we've been asked to not use our buffer -
				// there's no performance reason not to read it all
				// at once. Note that unlike the previous code of
				// this function, there is no need to do a seek
				// here, because there's no need to reread what we
				// had in the buffer.
				long after = mBufferStart+mBufferPosition+len;
				if (after > length())
					throw new EOFException("read past EOF: " + this);
				
				readInternal(b, offset, len);
				mBufferStart = after;
				mBufferPosition = 0;
				mBufferLength = 0;                    // trigger refill() on read
			}
		}
	}

	@Override
	public final short readShort() throws IOException {
		if (2 <= (mBufferLength-mBufferPosition)) {
			return (short) (((mBuffer[mBufferPosition++] & 0xFF) <<  8) | (mBuffer[mBufferPosition++] & 0xFF));
		} else {
			return super.readShort();
		}
	}
  
	@Override
	public final int readInt() throws IOException {
		if (4 <= (mBufferLength-mBufferPosition)) {
			return ((mBuffer[mBufferPosition++] & 0xFF) << 24) | ((mBuffer[mBufferPosition++] & 0xFF) << 16)
				 | ((mBuffer[mBufferPosition++] & 0xFF) <<  8) |  (mBuffer[mBufferPosition++] & 0xFF);
		} else {
			return super.readInt();
		}
	}
  
	@Override
	public final long readLong() throws IOException {
		if (8 <= (mBufferLength-mBufferPosition)) {
			final int i1 = ((mBuffer[mBufferPosition++] & 0xff) << 24) | ((mBuffer[mBufferPosition++] & 0xff) << 16) |
						   ((mBuffer[mBufferPosition++] & 0xff) << 8) | (mBuffer[mBufferPosition++] & 0xff);
			final int i2 = ((mBuffer[mBufferPosition++] & 0xff) << 24) | ((mBuffer[mBufferPosition++] & 0xff) << 16) |
						   ((mBuffer[mBufferPosition++] & 0xff) << 8) | (mBuffer[mBufferPosition++] & 0xff);
			return (((long)i1) << 32) | (i2 & 0xFFFFFFFFL);
		} else {
			return super.readLong();
		}
	}

	@Override
	public final int readVInt() throws IOException {
		if (5 <= (mBufferLength - mBufferPosition)) {
			return IndexUtil.readVInt(mBufferedInput);
		} else {
			return super.readVInt();
		}
	}
  
	@Override
	public final long readVLong() throws IOException {
		if (9 <= mBufferLength - mBufferPosition) {
			return IndexUtil.readVLong(mBufferedInput);
		} else {
			return super.readVLong();
		}
	}
  
	private void refill() throws IOException {
		long start = mBufferStart + mBufferPosition;
		long end = start + mBufferSize;
		if (end > length())				  // don't read past EOF
			end = length();
		int newLength = (int)(end - start);
		if (newLength <= 0)
			throw new EOFException("read past EOF: " + this);

		if (mBuffer == null) {
			newBuffer(new byte[mBufferSize]);  // allocate buffer lazily
			seekInternal(mBufferStart);
		}
		readInternal(mBuffer, 0, newLength);
		mBufferLength = newLength;
		mBufferStart = start;
		mBufferPosition = 0;
	}

	/** 
	 * Expert: implements buffer refill.  Reads bytes from the current position
	 * in the input.
	 * @param b the array to read bytes into
	 * @param offset the offset in the array to start storing bytes
	 * @param length the number of bytes to read
	 */
	protected abstract void readInternal(byte[] b, int offset, int length)
			throws IOException;

	@Override
	public final long getFilePointer() { return mBufferStart + mBufferPosition; }

	@Override
	public final void seek(long pos) throws IOException {
		if (pos >= mBufferStart && pos < (mBufferStart + mBufferLength)) {
			mBufferPosition = (int)(pos - mBufferStart);  // seek within buffer
		} else {
			mBufferStart = pos;
			mBufferPosition = 0;
			mBufferLength = 0;				  // trigger refill() on read()
			seekInternal(pos);
		}
	}

	/** 
	 * Expert: implements seek.  Sets current position in this file, where the
	 * next {@link #readInternal(byte[],int,int)} will occur.
	 * @see #readInternal(byte[],int,int)
	 */
	protected abstract void seekInternal(long pos) throws IOException;

	@Override
	public BufferedIndexInput clone() {
		BufferedIndexInput clone = (BufferedIndexInput)super.clone();

		clone.mBuffer = null;
		clone.mBufferLength = 0;
		clone.mBufferPosition = 0;
		clone.mBufferStart = getFilePointer();
		clone.mBufferedInput = new BufferedInput(clone);

		return clone;
	}

	/**
	 * Flushes the in-memory bufer to the given output, copying at most
	 * <code>numBytes</code>.
	 * <p>
	 * <b>NOTE:</b> this method does not refill the buffer, however it does
	 * advance the buffer position.
	 * 
	 * @return the number of bytes actually flushed from the in-memory buffer.
	 */
	protected final int flushBuffer(IIndexOutput out, long numBytes) throws IOException {
		int toCopy = mBufferLength - mBufferPosition;
		if (toCopy > numBytes) 
			toCopy = (int) numBytes;
		
		if (toCopy > 0) {
			out.writeBytes(mBuffer, mBufferPosition, toCopy);
			mBufferPosition += toCopy;
		}
		
		return toCopy;
	}
  
	@Override
	public void copyBytes(IIndexOutput out, long numBytes) throws IOException {
		assert numBytes >= 0: "numBytes=" + numBytes;

		while (numBytes > 0) {
			if (mBufferLength == mBufferPosition) 
				refill();
			
			numBytes -= flushBuffer(out, numBytes);
		}
	}
  
	/** Closes this stream to further operations. */
	@Override
	public void close() throws IOException {
		onClosed();
	}
	
}
