package org.javenstudio.common.indexdb.store.local;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IIndexOutput;

public class SimpleFSIndexInput extends FSIndexInput {
	
    //  LUCENE-1566 - maximum read length on a 32bit JVM to prevent incorrect OOM 
    protected final int mChunkSize;
    protected final long mOffset;
    protected final long mEnd;
    protected boolean mIsClone;
    
    public SimpleFSIndexInput(IIndexContext context, File path, int chunkSize) throws IOException {
    	super(context, new FSDescriptor(path, "r"));
    	
    	mChunkSize = chunkSize;
    	mOffset = 0L;
    	mEnd = mFile.getLength();
    	mIsClone = false;
    }
    
    public SimpleFSIndexInput(IIndexContext context, FSDescriptor file, 
    		long off, long length, int bufferSize, int chunkSize) throws IOException {
    	super(context, file, bufferSize);
    	
    	mChunkSize = chunkSize;
    	mOffset = off;
    	mEnd = off + length;
    	mIsClone = true; // well, we are sorta?
    }
  
    /** IndexInput methods */
    @Override
    protected void readInternal(byte[] b, int offset, int len)
    		throws IOException {
    	synchronized (mFile) {
    		int total = 0;
    		long position = mOffset + getFilePointer();
    		if (position != mFile.getPosition()) {
    			mFile.seek(position);
    			mFile.setPosition(position);
    		}
    		
    		if (position + len > mEnd) 
    			throw new EOFException("read past EOF: " + this);

    		try {
    			do {
    				final int readLength;
    				if (total + mChunkSize > len) {
    					readLength = len - total;
    				} else {
    					// LUCENE-1566 - work around JVM Bug by breaking very large reads into chunks
    					readLength = mChunkSize;
    				}
    				final int n = mFile.read(b, offset + total, readLength);
    				mFile.mPosition += n;
    				total += n;
    			} while (total < len);
    			
    		} catch (OutOfMemoryError e) {
    			// propagate OOM up and add a hint for 32bit VM Users hitting the bug
    			// with a large chunk size in the fast path.
    			final OutOfMemoryError outOfMemoryError = new OutOfMemoryError(
    					"OutOfMemoryError likely caused by the Sun VM Bug described in " +
    					"https://issues.apache.org/jira/browse/LUCENE-1566; try calling FSDirectory.setReadChunkSize " +
    					"with a value smaller than the current chunk size (" + mChunkSize + ")");
    			outOfMemoryError.initCause(e);
    			throw outOfMemoryError;
    			
    		} catch (IOException ioe) {
    			throw new IOException(ioe.getMessage() + ": " + this, ioe);
    		}
    	}
    }
  
    @Override
    public void close() throws IOException {
    	// only close the file if this is not a clone
    	if (!mIsClone) { 
    		mFile.close();
    		onClosed();
    	}
    }
  
    @Override
    protected void seekInternal(long position) {
    }
  
    @Override
    public long length() {
    	return mEnd - mOffset;
    }
  
    @Override
    public SimpleFSIndexInput clone() {
    	SimpleFSIndexInput clone = (SimpleFSIndexInput)super.clone();
    	clone.mIsClone = true;
    	return clone;
    }
  
    /** 
     * Method used for testing. Returns true if the underlying
     *  file descriptor is valid.
     */
    boolean isFDValid() throws IOException {
    	return mFile.getFD().valid();
    }
    
    @Override
    public void copyBytes(IIndexOutput out, long numBytes) throws IOException {
    	numBytes -= flushBuffer(out, numBytes);
    	// If out is FSIndexOutput, the copy will be optimized
    	out.copyBytes(this, numBytes);
    }
    
}
