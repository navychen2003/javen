package org.javenstudio.common.indexdb.store;

import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IIndexOutput;

/** 
 * Abstract base class for output to a file in a Directory.  A random-access
 * output stream.  Used for all Indexdb index output operations.
 * @see Directory
 * @see IndexInput
 */
public abstract class IndexOutput extends DataOutput implements IIndexOutput {
	//private static Logger LOG = Logger.getLogger(IndexOutput.class);

	private final IIndexContext mContext;
	
	protected IndexOutput(IIndexContext context) { 
		if (context == null) 
			throw new IllegalArgumentException("context must not be null");
		
		mContext = context;
	}
	
	@Override
	public final IIndexContext getContext() { 
		return mContext;
	}
	
	/** Forces any buffered output to be written. */
	public abstract void flush() throws IOException;

	/** Closes this stream to further operations. */
	public abstract void close() throws IOException;

	/** 
	 * Returns the current position in this file, where the next write will
	 * occur.
	 * @see #seek(long)
	 */
	public abstract long getFilePointer();

	/** 
	 * Sets current position in this file, where the next write will occur.
	 * @see #getFilePointer()
	 */
	public abstract void seek(long pos) throws IOException;

	/** The number of bytes in the file. */
	public abstract long length() throws IOException;

	/** 
	 * Set the file length. By default, this method does
	 * nothing (it's optional for a Directory to implement
	 * it).  But, certain Directory implementations (for
	 * example @see FSDirectory) can use this to inform the
	 * underlying IO system to pre-allocate the file to the
	 * specified size.  If the length is longer than the
	 * current file length, the bytes added to the file are
	 * undefined.  Otherwise the file is truncated.
	 * @param length file length
	 */
	public void setLength(long length) throws IOException {}

	protected void onOpened() { 
		//if (LOG.isDebugEnabled()) 
		//	LOG.debug("created IndexOutput: " + this);
	}
	
	protected void onClosed() { 
		//if (LOG.isDebugEnabled()) 
		//	LOG.debug("closed IndexOutput: " + this);
	}
	
	@Override
	public String toString() {
		StringBuilder sbuf = new StringBuilder();
		sbuf.append(getClass().getSimpleName());
		sbuf.append("{");
		toString(sbuf);
		sbuf.append("}");
		return sbuf.toString();
	}
	
	protected void toString(StringBuilder sbuf) { 
		sbuf.append("content=");
		sbuf.append(mContext);
		sbuf.append(",hashCode=");
		sbuf.append(hashCode());
	}
	
}
