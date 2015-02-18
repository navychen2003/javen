package org.javenstudio.common.indexdb;

import java.io.Closeable;
import java.io.IOException;

public interface IIndexInput extends IDataInput, Cloneable, Closeable {
	
	/** The context for this IndexInput. */
	public IIndexContext getContext();
	
	/** 
	 * Returns the current position in this file, where the next read will
	 * occur.
	 * @see #seek(long)
	 */
	public long getFilePointer();

	/** 
	 * Sets current position in this file, where the next read will occur.
	 * @see #getFilePointer()
	 */
	public void seek(long pos) throws IOException;
	
	/** The number of bytes in the file. */
	public long length();
	
	/**
	 * Copies <code>numBytes</code> bytes to the given {@link IndexOutput}.
	 * <p>
	 * <b>NOTE:</b> this method uses an intermediate buffer to copy the bytes.
	 * Consider overriding it in your implementation, if you can make a better,
	 * optimized copy.
	 * <p>
	 * <b>NOTE</b> ensure that there are enough bytes in the input to copy to
	 * output. Otherwise, different exceptions may be thrown, depending on the
	 * implementation.
	 */
	public void copyBytes(IIndexOutput out, long numBytes) throws IOException;
	
	/** Cloneable interface */
	public IIndexInput clone();
	
}
