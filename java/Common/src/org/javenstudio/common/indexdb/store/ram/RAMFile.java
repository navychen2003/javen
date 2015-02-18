package org.javenstudio.common.indexdb.store.ram;

import java.util.ArrayList;

public class RAMFile {
	
	protected ArrayList<byte[]> mBuffers = new ArrayList<byte[]>();
	protected long mLength;
	protected RAMDirectory mDirectory;
	protected long mSizeInBytes;

	// File used as buffer, in no RAMDirectory
	public RAMFile() {}
  
	public RAMFile(RAMDirectory directory) {
		mDirectory = directory;
	}

	// For non-stream access from thread that might be concurrent with writing
	public synchronized long getLength() {
		return mLength;
	}

	protected synchronized void setLength(long length) {
		mLength = length;
	}

	protected final byte[] addBuffer(int size) {
		byte[] buffer = newBuffer(size);
		synchronized(this) {
			mBuffers.add(buffer);
			mSizeInBytes += size;
		}

		if (mDirectory != null) 
			mDirectory.mSizeInBytes.getAndAdd(size);
		
		return buffer;
	}

	protected final synchronized byte[] getBuffer(int index) {
		return mBuffers.get(index);
	}

	protected final synchronized int numBuffers() {
		return mBuffers.size();
	}

	/**
	 * Expert: allocate a new buffer. 
	 * Subclasses can allocate differently. 
	 * @param size size of allocated buffer.
	 * @return allocated buffer.
	 */
	protected byte[] newBuffer(int size) {
		return new byte[size];
	}

	public synchronized long getSizeInBytes() {
		return mSizeInBytes;
	}
  
}
