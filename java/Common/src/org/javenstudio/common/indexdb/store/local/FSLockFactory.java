package org.javenstudio.common.indexdb.store.local;

import java.io.File;

import org.javenstudio.common.indexdb.LockFactory;

/**
 * Base class for file system based locking implementation.
 */
public abstract class FSLockFactory extends LockFactory {

	/**
	 * Directory for the lock files.
	 */
	protected File mLockDir = null;

	/**
	 * Set the lock directory. This method can be only called
	 * once to initialize the lock directory. It is used by {@link FSDirectory}
	 * to set the lock directory to itself.
	 * Subclasses can also use this method to set the directory
	 * in the constructor.
	 */
	protected final void setLockDir(File lockDir) {
		if (mLockDir != null)
			throw new IllegalStateException("You can set the lock directory for this factory only once.");
		mLockDir = lockDir;
	}
  
	/**
	 * Retrieve the lock directory.
	 */
	public File getLockDir() {
		return mLockDir;
	}

}
