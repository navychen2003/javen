package org.javenstudio.hornet.store;

import java.io.EOFException;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Iterator;

import org.javenstudio.common.indexdb.AlreadyClosedException;
import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.store.IndexInput;

/**
 * Base IndexInput implementation that uses an array
 * of ByteBuffers to represent a file.
 * <p>
 * Because Java's ByteBuffer uses an int to address the
 * values, it's necessary to access a file greater
 * Integer.MAX_VALUE in size using multiple byte buffers.
 * <p>
 * For efficiency, this class requires that the buffers
 * are a power-of-two (<code>chunkSizePower</code>).
 */
public abstract class ByteBufferIndexInput extends IndexInput {
	
	private final WeakIdentityMap<ByteBufferIndexInput,Boolean> mClones = 
			WeakIdentityMap.newConcurrentHashMap();
	
	private ByteBuffer[] mBuffers;
  
	private final long mChunkSizeMask;
	private final int mChunkSizePower;
  
	private int mOffset;
	private long mLength;

	private int mCurBufIndex;

	private ByteBuffer mCurBuf; // redundant for speed: buffers[curBufIndex]

	private boolean mIsClone = false;
	
	public ByteBufferIndexInput(IIndexContext context, ByteBuffer[] buffers, 
			long length, int chunkSizePower) throws IOException {
		super(context);
		
		mBuffers = buffers;
		mLength = length;
		mChunkSizePower = chunkSizePower;
		mChunkSizeMask = (1L << chunkSizePower) - 1L;
    
		assert chunkSizePower >= 0 && chunkSizePower <= 30;   
		assert (length >>> chunkSizePower) < Integer.MAX_VALUE;

		seek(0L);
	}
  
	@Override
	public final byte readByte() throws IOException {
		try {
			return mCurBuf.get();
		} catch (BufferUnderflowException e) {
			do {
				mCurBufIndex ++;
				if (mCurBufIndex >= mBuffers.length) 
					throw new EOFException("read past EOF: " + this);
				
				mCurBuf = mBuffers[mCurBufIndex];
				mCurBuf.position(0);
			} while (!mCurBuf.hasRemaining());
			return mCurBuf.get();
		} catch (NullPointerException npe) {
			throw new AlreadyClosedException("Already closed: " + this);
		}
	}

	@Override
	public final void readBytes(byte[] b, int offset, int len) throws IOException {
		try {
			mCurBuf.get(b, offset, len);
		} catch (BufferUnderflowException e) {
			int curAvail = mCurBuf.remaining();
			while (len > curAvail) {
				mCurBuf.get(b, offset, curAvail);
				len -= curAvail;
				offset += curAvail;
				mCurBufIndex ++;
				if (mCurBufIndex >= mBuffers.length) 
					throw new EOFException("read past EOF: " + this);
				
				mCurBuf = mBuffers[mCurBufIndex];
				mCurBuf.position(0);
				curAvail = mCurBuf.remaining();
			}
			mCurBuf.get(b, offset, len);
		} catch (NullPointerException npe) {
			throw new AlreadyClosedException("Already closed: " + this);
		}
	}

	@Override
	public final short readShort() throws IOException {
		try {
			return mCurBuf.getShort();
		} catch (BufferUnderflowException e) {
			return super.readShort();
		} catch (NullPointerException npe) {
			throw new AlreadyClosedException("Already closed: " + this);
		}
	}

	@Override
	public final int readInt() throws IOException {
		try {
			return mCurBuf.getInt();
		} catch (BufferUnderflowException e) {
			return super.readInt();
		} catch (NullPointerException npe) {
			throw new AlreadyClosedException("Already closed: " + this);
		}
	}

	@Override
	public final long readLong() throws IOException {
		try {
			return mCurBuf.getLong();
		} catch (BufferUnderflowException e) {
			return super.readLong();
		} catch (NullPointerException npe) {
			throw new AlreadyClosedException("Already closed: " + this);
		}
	}
  
	@Override
	public final long getFilePointer() {
		try {
			return (((long) mCurBufIndex) << mChunkSizePower) + mCurBuf.position() - mOffset;
		} catch (NullPointerException npe) {
			throw new AlreadyClosedException("Already closed: " + this);
		}
	}

	@Override
	public final void seek(long pos) throws IOException {
		// necessary in case offset != 0 and pos < 0, but pos >= -offset
		if (pos < 0L) 
			throw new IllegalArgumentException("Seeking to negative position: " + this);
		
		pos += mOffset;
		
		// we use >> here to preserve negative, so we will catch AIOOBE,
		// in case pos + offset overflows.
		final int bi = (int) (pos >> mChunkSizePower);
		
		try {
			final ByteBuffer b = mBuffers[bi];
			b.position((int) (pos & mChunkSizeMask));
			// write values, on exception all is unchanged
			mCurBufIndex = bi;
			mCurBuf = b;
		} catch (ArrayIndexOutOfBoundsException aioobe) {
			throw new EOFException("seek past EOF: " + this);
		} catch (IllegalArgumentException iae) {
			throw new EOFException("seek past EOF: " + this);
		} catch (NullPointerException npe) {
			throw new AlreadyClosedException("Already closed: " + this);
		}
	}

	@Override
	public final long length() {
		return mLength;
	}

	@Override
	public final ByteBufferIndexInput clone() {
		final ByteBufferIndexInput clone = buildSlice(0L, mLength);
		try {
			clone.seek(getFilePointer());
		} catch(IOException ioe) {
			throw new RuntimeException("Should never happen: " + this, ioe);
		}
    
		return clone;
	}
  
	/**
	 * Creates a slice of this index input, with the given description, offset, and length. 
	 * The slice is seeked to the beginning.
	 */
	public final ByteBufferIndexInput slice(long offset, long length) {
		if (mIsClone) // well we could, but this is stupid
			throw new IllegalStateException("cannot slice() from a cloned IndexInput: " + this);
		
		final ByteBufferIndexInput clone = buildSlice(offset, length);
		try {
			clone.seek(0L);
		} catch(IOException ioe) {
			throw new RuntimeException("Should never happen: " + this, ioe);
		}
    
		return clone;
	}
  
	private ByteBufferIndexInput buildSlice(long offset, long length) {
		if (mBuffers == null) 
			throw new AlreadyClosedException("Already closed: " + this);
		
		if (offset < 0 || length < 0 || offset+length > mLength) {
			throw new IllegalArgumentException("slice() out of bounds: offset=" + offset 
					+ ",length=" + length + ",fileLength="  + mLength + ": "  + this);
		}
    
		// include our own offset into the final offset:
		offset += mOffset;
    
		final ByteBufferIndexInput clone = (ByteBufferIndexInput)super.clone();
		clone.mIsClone = true;
		// we keep clone.clones, so it shares the same map with original 
		// and we have no additional cost on clones
		assert clone.mClones == this.mClones;
		clone.mBuffers = buildSlice(mBuffers, offset, length);
		clone.mOffset = (int) (offset & mChunkSizeMask);
		clone.mLength = length;

		// register the new clone in our clone list to clean it up on closing:
		this.mClones.put(clone, Boolean.TRUE);
    
		return clone;
	}
  
	/** 
	 * Returns a sliced view from a set of already-existing buffers: 
	 *  the last buffer's limit() will be correct, but
	 *  you must deal with offset separately (the first buffer will not be adjusted) 
	 */
	private ByteBuffer[] buildSlice(ByteBuffer[] buffers, long offset, long length) {
		final long sliceEnd = offset + length;
    
		final int startIndex = (int) (offset >>> mChunkSizePower);
		final int endIndex = (int) (sliceEnd >>> mChunkSizePower);

		// we always allocate one more slice, the last one may be a 0 byte one
		final ByteBuffer slices[] = new ByteBuffer[endIndex - startIndex + 1];
    
		for (int i = 0; i < slices.length; i++) {
			slices[i] = buffers[startIndex + i].duplicate();
		}

		// set the last buffer's limit for the sliced view.
		slices[slices.length - 1].limit((int) (sliceEnd & mChunkSizeMask));
    
		return slices;
	}

	private void unsetBuffers() {
		mBuffers = null;
		mCurBuf = null;
		mCurBufIndex = 0;
	}

	@Override
	public final void close() throws IOException {
		try {
			if (mBuffers == null) return;
      
			// make local copy, then un-set early
			final ByteBuffer[] bufs = mBuffers;
			unsetBuffers();
      
			if (mIsClone) return;
      
			// for extra safety unset also all clones' buffers:
			for (Iterator<ByteBufferIndexInput> it = mClones.keyIterator(); it.hasNext();) {
				final ByteBufferIndexInput clone = it.next();
				assert clone.mIsClone;
				clone.unsetBuffers();
			}
			
			mClones.clear();
      
			for (final ByteBuffer b : bufs) {
				freeBuffer(b);
			}
		} finally {
			unsetBuffers();
		}
	}
  
	/**
	 * Called when the contents of a buffer will be no longer needed.
	 */
	protected abstract void freeBuffer(ByteBuffer b) throws IOException;

	@Override
	public final String toString() {
		return super.toString();
	}
	
	@Override
	protected void toString(StringBuilder sbuf) { 
		super.toString(sbuf);
		sbuf.append(",isClone=").append(mIsClone);
	}
	
}
