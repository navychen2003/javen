package org.javenstudio.common.indexdb.store.local;

import java.io.File;
import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.store.BufferedIndexOutput;
import org.javenstudio.common.indexdb.store.RateLimiter;

class FSIndexOutput extends BufferedIndexOutput {
	
	private final FSDirectory mParent;
	private final FSDescriptor mFile;
	private final RateLimiter mRateLimiter;
	// remember if the file is open, so that we don't try to close it more than once
	private volatile boolean mIsOpen; 

	public FSIndexOutput(IIndexContext context, FSDirectory parent, 
			String name, RateLimiter rateLimiter) throws IOException {
		super(context); 
		
		mParent = parent;
		mFile = new FSDescriptor(new File(parent.getDirectoryFile(), name), "rw");
		mIsOpen = true;
		mRateLimiter = rateLimiter;
	}

	final String getName() { return mFile.getName(); }
	
	/** output methods: */
	@Override
	public void flushBuffer(byte[] b, int offset, int size) throws IOException {
		assert mIsOpen;
		if (mRateLimiter != null) 
			mRateLimiter.pause(size);
		
		mFile.write(b, offset, size);
	}

	@Override
	public void close() throws IOException {
		mParent.onIndexOutputClosed(this);
		// only close the file if it has not been closed yet
		if (mIsOpen) {
			boolean success = false;
			try {
				super.close();
				success = true;
			} finally {
				mIsOpen = false;
				if (!success) {
					try {
						mFile.close();
					} catch (Throwable t) {
						// Suppress so we don't mask original exception
					}
				} else {
					mFile.close();
				}
			}
		}
	}

	/** Random-access methods */
	@Override
	public void seek(long pos) throws IOException {
		super.seek(pos);
		mFile.seek(pos);
	}

	@Override
	public long length() throws IOException {
		return mFile.length();
	}

	@Override
	public void setLength(long length) throws IOException {
		mFile.setLength(length);
	}
	
	@Override
	protected void toString(StringBuilder sbuf) { 
		super.toString(sbuf);
		
		sbuf.append(",name=");
		sbuf.append(mFile.getName());
	}
	
}
