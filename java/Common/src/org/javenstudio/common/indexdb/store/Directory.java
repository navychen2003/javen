package org.javenstudio.common.indexdb.store;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

import org.javenstudio.common.indexdb.AlreadyClosedException;
import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IIndexInput;
import org.javenstudio.common.indexdb.IIndexOutput;
import org.javenstudio.common.indexdb.IInputSlicer;
import org.javenstudio.common.indexdb.Lock;
import org.javenstudio.common.indexdb.LockFactory;
import org.javenstudio.common.indexdb.util.IOUtils;

/** 
 * A Directory is a flat list of files.  Files may be written once, when they
 * are created.  Once a file is created it may only be opened for read, or
 * deleted.  Random access is permitted both when reading and writing.
 *
 * <p> Java's i/o APIs not used directly, but rather all i/o is
 * through this API.  This permits things such as: <ul>
 * <li> implementation of RAM-based indices;
 * <li> implementation indices stored in a database, via JDBC;
 * <li> implementation of an index as a single file;
 * </ul>
 *
 * Directory locking is implemented by an instance of {@link
 * LockFactory}, and can be changed for each Directory
 * instance using {@link #setLockFactory}.
 *
 */
public abstract class Directory implements IDirectory {

	private static final AtomicLong sNumCounter = new AtomicLong();
	
	private final long mIdentity;
	protected volatile boolean mIsOpen = true;

	/** 
	 * Holds the LockFactory instance (implements locking for
	 * this Directory instance). 
	 */
	protected LockFactory mLockFactory;

	protected Directory() { 
		mIdentity = sNumCounter.incrementAndGet();
	}
	
	protected final String getIdentityName() { 
		return getClass().getName() + "-" + mIdentity;
	}
	
	/**
	 * Returns an array of strings, one for each file in the directory.
	 * 
	 * @throws NoSuchDirectoryException if the directory is not prepared for any
	 *         write operations (such as {@link #createOutput(String, IOContext)}).
	 * @throws IOException in case of other IO errors
	 */
	public abstract String[] listAll() throws IOException;

	/** Returns true iff a file with the given name exists. */
	public abstract boolean fileExists(String name) throws IOException;

	/** Removes an existing file in the directory. */
	public abstract void deleteFile(String name) throws IOException;

	/**
	 * Returns the length of a file in the directory. This method follows the
	 * following contract:
	 * <ul>
	 * <li>Throws {@link FileNotFoundException} if the file does not exist
	 * <li>Returns a value &ge;0 if the file exists, which specifies its length.
	 * </ul>
	 * 
	 * @param name the name of the file for which to return the length.
	 * @throws FileNotFoundException if the file does not exist.
	 * @throws IOException if there was an IO error while retrieving the file's
	 *         length.
	 */
	public abstract long getFileLength(String name) throws IOException;

	/** 
	 * Creates a new, empty file in the directory with the given name.
	 * Returns a stream writing this file. 
	 */
	@Override
	public IIndexOutput createOutput(IIndexContext context, String name)
			throws IOException { 
		IndexOutput output = createIndexOutput(context, name);
		output.onOpened();
		return output;
	}

	/** 
	 * Creates a new, empty file in the directory with the given name.
	 * Returns a stream writing this file. 
	 */
	protected abstract IndexOutput createIndexOutput(IIndexContext context, String name)
			throws IOException;
	
	/**
	 * Ensure that any writes to these files are moved to
	 * stable storage.  Indexdb uses this to properly commit
	 * changes to the index, to prevent a machine/OS crash
	 * from corrupting the index.<br/>
	 * <br/>
	 * NOTE: Clients may call this method for same files over
	 * and over again, so some impls might optimize for that.
	 * For other impls the operation can be a noop, for various
	 * reasons.
	 */
	public abstract void sync(Collection<String> names) throws IOException;

	/** 
	 * Returns a stream reading an existing file, with the
	 * specified read buffer size.  The particular Directory
	 * implementation may ignore the buffer size.  Currently
	 * the only Directory implementations that respect this
	 * parameter are {@link FSDirectory} and {@link
	 * CompoundFileDirectory}.
	 */
	@Override
	public IIndexInput openInput(IIndexContext context, String name) 
			throws IOException { 
		IndexInput input = openIndexInput(context, name);
		input.onOpened();
		return input;
	}
  
	/** 
	 * Returns a stream reading an existing file, with the
	 * specified read buffer size.  The particular Directory
	 * implementation may ignore the buffer size.  Currently
	 * the only Directory implementations that respect this
	 * parameter are {@link FSDirectory} and {@link
	 * CompoundFileDirectory}.
	 */
	protected abstract IndexInput openIndexInput(IIndexContext context, String name) 
			throws IOException; 
	
	/** 
	 * Construct a {@link Lock}.
	 * @param name the name of the lock file
	 */
	@Override
	public Lock makeLock(String name) {
		return mLockFactory.makeLock(name);
	}
	
	/**
	 * Attempt to clear (forcefully unlock and remove) the
	 * specified lock.  Only call this at a time when you are
	 * certain this lock is no longer in use.
	 * @param name name of the lock to be cleared.
	 */
	@Override
	public void clearLock(String name) throws IOException {
		if (mLockFactory != null) 
			mLockFactory.clearLock(name);
	}

	/** Closes the store. */
	public abstract void close() throws IOException;

	/** return true the store is closed. */
	@Override
	public boolean isClosed() { 
		return !mIsOpen;
	}
	
	/**
	 * Set the LockFactory that this Directory instance should
	 * use for its locking implementation.  Each * instance of
	 * LockFactory should only be used for one directory (ie,
	 * do not share a single instance across multiple
	 * Directories).
	 *
	 * @param lockFactory instance of {@link LockFactory}.
	 */
	@Override
	public void setLockFactory(LockFactory lockFactory) throws IOException {
		assert lockFactory != null;
		mLockFactory = lockFactory;
		mLockFactory.setLockPrefix(this.getLockID());
	}

	/**
	 * Get the LockFactory that this Directory instance is
	 * using for its locking implementation.  Note that this
	 * may be null for Directory implementations that provide
	 * their own locking implementation.
	 */
	@Override
	public LockFactory getLockFactory() {
		return mLockFactory;
	}

	/**
	 * Return a string identifier that uniquely differentiates
	 * this Directory instance from other Directory instances.
	 * This ID should be the same if two Directory instances
	 * (even in different JVMs and/or on different machines)
	 * are considered "the same index".  This is how locking
	 * "scopes" to the right index.
	 */
	@Override
	public String getLockID() {
		return this.toString();
	}

	/**
	 * Copies the file <i>src</i> to {@link Directory} <i>to</i> under the new
	 * file name <i>dest</i>.
	 * <p>
	 * If you want to copy the entire source directory to the destination one, you
	 * can do so like this:
	 * 
	 * <pre>
	 * Directory to; // the directory to copy to
	 * for (String file : dir.listAll()) {
	 *   dir.copy(to, file, newFile); // newFile can be either file, or a new name
	 * }
	 * </pre>
	 * <p>
	 * <b>NOTE:</b> this method does not check whether <i>dest<i> exist and will
	 * overwrite it if it does.
	 */
	@Override
	public long copy(IIndexContext context, IDirectory to, String src, String dest) 
			throws IOException {
		IIndexOutput os = null;
		IIndexInput is = null;
		IOException priorException = null;
		try {
			os = to.createOutput(context, dest);
			is = openInput(context, src);
			long length = is.length();
			is.copyBytes(os, length);
			return length;
		} catch (IOException ioe) {
			priorException = ioe;
			return -1;
		} finally {
			IOUtils.closeWhileHandlingException(priorException, os, is);
		}
	}

	/**
	 * Creates an {@link IndexInputSlicer} for the given file name.
	 * IndexInputSlicer allows other {@link Directory} implementations to
	 * efficiently open one or more sliced {@link IndexInput} instances from a
	 * single file handle. The underlying file handle is kept open until the
	 * {@link IndexInputSlicer} is closed.
	 *
	 * @throws IOException
	 *           if an {@link IOException} occurs
	 */
	@Override
	public IInputSlicer createSlicer(final IIndexContext context, final String name) 
			throws IOException {
		ensureOpen();
		return new IndexInputSlicer() {
				private final IndexInput mBase = (IndexInput)Directory.this.openInput(context, name);
				@Override
				public IndexInput openSlice(long offset, long length) {
					return new SlicedIndexInput(mBase, offset, length);
				}
				@Override
				public void close() throws IOException {
					mBase.close();
				}
				@Override
				public IndexInput openFullSlice() throws IOException {
					return (IndexInput) mBase.clone();
				}
			};
	}

	/**
	 * @throws AlreadyClosedException if this Directory is closed
	 */
	protected final void ensureOpen() throws AlreadyClosedException {
		if (!mIsOpen)
			throw new AlreadyClosedException("this Directory is closed");
	}
  
	@Override
	public String toString() {
		return getIdentityName() + "{lockFactory=" + getLockFactory() + "}";
	}
	
}
