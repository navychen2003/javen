package org.javenstudio.common.indexdb.store;

import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IIndexInput;
import org.javenstudio.common.indexdb.IIndexOutput;

/** 
 * Abstract base class for input from a file in a {@link Directory}.  A
 * random-access input stream.  Used for all Indexdb index input operations.
 * @see Directory
 */
public abstract class IndexInput extends DataInput implements IIndexInput {
	//private static Logger LOG = Logger.getLogger(IndexInput.class);
	
	private final IIndexContext mContext;

	/** 
	 * resourceDescription should be a non-null, opaque string
	 *  describing this resource; it's returned from
	 *  {@link #toString}. 
	 */
	protected IndexInput(IIndexContext context) {
		if (context == null) 
			throw new IllegalArgumentException("context must not be null");
		
		mContext = context;
	}

	@Override
	public final IIndexContext getContext() { 
		return mContext;
	}
	
	/** Closes the stream to further operations. */
	public abstract void close() throws IOException;

	/** 
	 * Returns the current position in this file, where the next read will
	 * occur.
	 * @see #seek(long)
	 */
	public abstract long getFilePointer();

	/** 
	 * Sets current position in this file, where the next read will occur.
	 * @see #getFilePointer()
	 */
	public abstract void seek(long pos) throws IOException;

	/** The number of bytes in the file. */
	public abstract long length();

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
	@Override
	public void copyBytes(IIndexOutput out, long numBytes) throws IOException {
		assert numBytes >= 0: "numBytes=" + numBytes;
		final byte copyBuf[] = new byte[getContext().getInputBufferSize()];

		while (numBytes > 0) {
			final int toCopy = (int) (numBytes > copyBuf.length ? copyBuf.length : numBytes);
			readBytes(copyBuf, 0, toCopy);
			out.writeBytes(copyBuf, 0, toCopy);
			numBytes -= toCopy;
		}
	}

	protected void onOpened() {
		//if (LOG.isDebugEnabled()) 
		//	LOG.debug("created IndexInput: " + this);
	}
	
	protected void onClosed() { 
		//if (LOG.isDebugEnabled()) 
		//	LOG.debug("closed IndexInput: " + this);
	}
	
	@Override
	public IndexInput clone() { 
		return (IndexInput)super.clone();
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
		sbuf.append(",start=");
		sbuf.append(getFilePointer());
	}
	
}
