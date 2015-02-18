package org.javenstudio.falcon.search.store;

import java.io.Closeable;
import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.falcon.util.NamedListPlugin;

/**
 * Provides access to a Directory implementation. You must release every
 * Directory that you get.
 */
public abstract class DirectoryFactory implements NamedListPlugin, Closeable {
  
	private IIndexContext mContext = null;
	
	public void setContext(IIndexContext context) { 
		mContext = context;
	}
	
	public IIndexContext getContext() { 
		return mContext;
	}
	
	/**
	 * Normalize a given path.
	 * 
	 * @param path to normalize
	 * @return normalized path
	 * @throws IOException on io error
	 */
	public String normalize(String path) throws IOException {
		return path;
	}
	
	/**
	 * Indicates a Directory will no longer be used, and when it's ref count
	 * hits 0, it can be closed. On shutdown all directories will be closed
	 * whether this has been called or not. This is simply to allow early cleanup.
	 * 
	 * @throws IOException If there is a low-level I/O error.
	 */
	public abstract void doneWithDirectory(IDirectory directory) 
			throws IOException;
  
	/**
	 * Adds a close listener for a Directory.
	 */
	public abstract void addCloseListener(IDirectory dir, 
			DirectoryCloseListener closeListener);
  
	/**
	 * Close the this and all of the Directories it contains.
	 * 
	 * @throws IOException If there is a low-level I/O error.
	 */
	public abstract void close() throws IOException;
  
	/**
	 * Creates a new Directory for a given path.
	 * 
	 * @throws IOException If there is a low-level I/O error.
	 */
	protected abstract IDirectory create(String path) throws IOException;
  
	/**
	 * Returns true if a Directory exists for a given path.
	 */
	public abstract boolean exists(String path);
  
	/**
	 * Returns the Directory for a given path, using the specified rawLockType.
	 * Will return the same Directory instance for the same path.
	 * 
	 * @throws IOException If there is a low-level I/O error.
	 */
	public abstract IDirectory get(String path, String rawLockType)
			throws IOException;
  
	/**
	 * Returns the Directory for a given path, using the specified rawLockType.
	 * Will return the same Directory instance for the same path unless forceNew,
	 * in which case a new Directory is returned. There is no need to call
	 * {@link #doneWithDirectory(Directory)} in this case - the old Directory
	 * will be closed when it's ref count hits 0.
	 * 
	 * @throws IOException If there is a low-level I/O error.
	 */
	public abstract IDirectory get(String path, String rawLockType,
			boolean forceNew) throws IOException;
  
	/**
	 * Increment the number of references to the given Directory. You must call
	 * release for every call to this method.
	 */
	public abstract void increaseRef(IDirectory directory);
  
	/**
	 * Releases the Directory so that it may be closed when it is no longer
	 * referenced.
	 * 
	 * @throws IOException If there is a low-level I/O error.
	 */
	public abstract void release(IDirectory directory) throws IOException;
  
}
