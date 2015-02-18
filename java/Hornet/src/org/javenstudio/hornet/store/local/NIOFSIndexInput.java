package org.javenstudio.hornet.store.local;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.store.local.FSDescriptor;
import org.javenstudio.common.indexdb.store.local.SimpleFSIndexInput;

class NIOFSIndexInput extends SimpleFSIndexInput {

	private final FileChannel mChannel;
    private ByteBuffer mByteBuf; // wraps the buffer for NIO
    
	public NIOFSIndexInput(IIndexContext context, File path, int chunkSize) throws IOException {
		super(context, path, chunkSize);
		
		mChannel = mFile.getChannel();
    }
    
    public NIOFSIndexInput(IIndexContext context, 
    		File path, FSDescriptor file, FileChannel fc, long off, long length, int bufferSize, 
    		int chunkSize) throws IOException {
    	super(context, file, off, length, bufferSize, chunkSize);
    	
    	mChannel = fc;
    	mIsClone = true;
    }

    @Override
    protected void newBuffer(byte[] newBuffer) {
    	super.newBuffer(newBuffer);
    	mByteBuf = ByteBuffer.wrap(newBuffer);
    }

    @Override
    public void close() throws IOException {
    	if (!mIsClone && mFile.isOpen()) {
    		// Close the channel & file
    		try {
    			mChannel.close();
    		} finally {
    			mFile.close();
    			onClosed();
    		}
    	}
    }

    @Override
    protected void readInternal(byte[] b, int offset, int len) throws IOException {
    	final ByteBuffer bb;

    	// Determine the ByteBuffer we should use
    	if (b == mBuffer && 0 == offset) {
    		// Use our own pre-wrapped byteBuf:
    		assert mByteBuf != null;
    		mByteBuf.clear();
    		mByteBuf.limit(len);
    		bb = mByteBuf;
    	} else {
    		bb = ByteBuffer.wrap(b, offset, len);
    	}

    	int readOffset = bb.position();
    	int readLength = bb.limit() - readOffset;
    	assert readLength == len;

    	long pos = getFilePointer() + mOffset;
    	if (pos + len > mEnd) 
    		throw new EOFException("read past EOF: " + this);

    	try {
    		while (readLength > 0) {
    			final int limit;
    			if (readLength > mChunkSize) {
    				// LUCENE-1566 - work around JVM Bug by breaking
    				// very large reads into chunks
    				limit = readOffset + mChunkSize;
    			} else {
    				limit = readOffset + readLength;
    			}
    			bb.limit(limit);
    			int n = mChannel.read(bb, pos);
    			pos += n;
    			readOffset += n;
    			readLength -= n;
    		}
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