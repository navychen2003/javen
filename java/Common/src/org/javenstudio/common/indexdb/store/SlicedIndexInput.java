package org.javenstudio.common.indexdb.store;

import java.io.EOFException;
import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexOutput;

/** 
 * Implementation of an IndexInput that reads from a portion of
 *  a file.
 */
final class SlicedIndexInput extends BufferedIndexInput {
	
	private IndexInput mBase;
	private long mFileOffset;
	private long mLength;
  
	SlicedIndexInput(final IndexInput base, final long fileOffset, final long length) {
		this(base, fileOffset, length, base.getContext().getInputBufferSize());
	}
  
	SlicedIndexInput(final IndexInput base, final long fileOffset, final long length, int readBufferSize) {
		super(base.getContext(), readBufferSize);
		
		mBase = (IndexInput) base.clone();
		mFileOffset = fileOffset;
		mLength = length;
	}
  
	@Override
	public SlicedIndexInput clone() {
		SlicedIndexInput clone = (SlicedIndexInput)super.clone();
		clone.mBase = (IndexInput)mBase.clone();
		clone.mFileOffset = mFileOffset;
		clone.mLength = mLength;
		return clone;
	}
  
	/** 
	 * Expert: implements buffer refill.  Reads bytes from the current
	 *  position in the input.
	 * @param b the array to read bytes into
	 * @param offset the offset in the array to start storing bytes
	 * @param len the number of bytes to read
	 */
	@Override
	protected void readInternal(byte[] b, int offset, int len) throws IOException {
		long start = getFilePointer();
		if (start + len > mLength)
			throw new EOFException("read past EOF: " + this);
		mBase.seek(mFileOffset + start);
		mBase.readBytes(b, offset, len, false);
	}
  
	/** 
	 * Expert: implements seek.  Sets current position in this file, where
	 *  the next {@link #readInternal(byte[],int,int)} will occur.
	 * @see #readInternal(byte[],int,int)
	 */
	@Override
	protected void seekInternal(long pos) {}
  
	/** Closes the stream to further operations. */
	@Override
	public void close() throws IOException {
		mBase.close();
	}
  
	@Override
	public long length() {
		return mLength;
	}
  
	@Override
	public void copyBytes(IIndexOutput out, long numBytes) throws IOException {
		// Copy first whatever is in the buffer
		numBytes -= flushBuffer(out, numBytes);
    
		// If there are more bytes left to copy, delegate the copy task to the
		// base IndexInput, in case it can do an optimized copy.
		if (numBytes > 0) {
			long start = getFilePointer();
			if (start + numBytes > mLength) 
				throw new EOFException("read past EOF: " + this);
			
			mBase.seek(mFileOffset + start);
			mBase.copyBytes(out, numBytes);
		}
	}
	
}
