package org.javenstudio.common.indexdb;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;

public interface IDirectory extends Closeable {

	/** 
	 * Creates a new, empty file in the directory with the given name.
     * Returns a stream writing this file. 
     */
	public IIndexOutput createOutput(IIndexContext context, String name)
			throws IOException;
	
	/** 
	 * Returns a stream reading an existing file, with the
	 * specified read buffer size.  The particular Directory
	 * implementation may ignore the buffer size.  Currently
	 * the only Directory implementations that respect this
	 * parameter are {@link FSDirectory} and {@link
	 * CompoundFileDirectory}.
	 */
	public IIndexInput openInput(IIndexContext context, String name) 
			throws IOException; 
	
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
	public IInputSlicer createSlicer(IIndexContext context, final String name) 
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
	public void sync(Collection<String> names) throws IOException;
	
	/**
	 * Returns an array of strings, one for each file in the directory.
	 * 
	 * @throws NoSuchDirectoryException if the directory is not prepared for any
	 *         write operations (such as {@link #createOutput(String, IOContext)}).
	 * @throws IOException in case of other IO errors
	 */
	public String[] listAll() throws IOException;
	
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
	public long getFileLength(String name) throws IOException;
	
	/** Returns true iff a file with the given name exists. */
	public boolean fileExists(String name) throws IOException;
	
	/** Removes an existing file in the directory. */
	public void deleteFile(String name) throws IOException;
	
	/** 
	 * Construct a {@link Lock}.
	 * @param name the name of the lock file
	 */
	public Lock makeLock(String name);
	
	/**
	 * Attempt to clear (forcefully unlock and remove) the
	 * specified lock.  Only call this at a time when you are
	 * certain this lock is no longer in use.
	 * @param name name of the lock to be cleared.
	 */
	public void clearLock(String name) throws IOException;
	
	/**
	 * Set the LockFactory that this Directory instance should
	 * use for its locking implementation.  Each * instance of
	 * LockFactory should only be used for one directory (ie,
	 * do not share a single instance across multiple
	 * Directories).
	 *
	 * @param lockFactory instance of {@link LockFactory}.
	 */
	public void setLockFactory(LockFactory lockFactory) throws IOException;
	
	/**
	 * Get the LockFactory that this Directory instance is
	 * using for its locking implementation.  Note that this
	 * may be null for Directory implementations that provide
	 * their own locking implementation.
	 */
	public LockFactory getLockFactory();
	
	/**
	 * Return a string identifier that uniquely differentiates
	 * this Directory instance from other Directory instances.
	 * This ID should be the same if two Directory instances
	 * (even in different JVMs and/or on different machines)
	 * are considered "the same index".  This is how locking
	 * "scopes" to the right index.
	 */
	public String getLockID();
	
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
	public long copy(IIndexContext context, IDirectory to, String src, String dest) 
			throws IOException;
	
	/** return true the store is closed. */
	public boolean isClosed();
	
}
