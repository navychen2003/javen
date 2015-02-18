package org.javenstudio.common.indexdb;

import java.io.Closeable;
import java.io.IOException;

public interface IIndexOutput extends IDataOutput, Closeable {
	
	/** The context for this Indexoutput. */
	public IIndexContext getContext();
	
	/** 
	 * Returns the current position in this file, where the next write will occur.
	 * @see #seek(long)
	 */
	public long getFilePointer();
	
	/** 
	 * Sets current position in this file, where the next write will occur.
	 * @see #getFilePointer()
	 */
	public void seek(long pos) throws IOException;

	/** The number of bytes in the file. */
	public long length() throws IOException;
	
	/** Copy numBytes bytes from input to ourself. */
	public void copyBytes(IIndexInput input, long numBytes) throws IOException;
	
	/** Forces any buffered output to be written. */
	public abstract void flush() throws IOException;
	
}
