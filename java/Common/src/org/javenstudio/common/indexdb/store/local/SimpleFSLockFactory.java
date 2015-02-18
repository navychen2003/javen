package org.javenstudio.common.indexdb.store.local;

import java.io.File;
import java.io.IOException;

import org.javenstudio.common.indexdb.Lock;
import org.javenstudio.common.indexdb.LockReleaseFailedException;

/**
 * <p>Implements {@link LockFactory} using {@link
 * File#createNewFile()}.</p>
 *
 * <p><b>NOTE:</b> the <a target="_top"
 * href="http://java.sun.com/j2se/1.4.2/docs/api/java/io/File.html#createNewFile()">javadocs
 * for <code>File.createNewFile</code></a> contain a vague
 * yet spooky warning about not using the API for file
 * locking.  This warning was added due to <a target="_top"
 * href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4676183">this
 * bug</a>, and in fact the only known problem with using
 * this API for locking is that the Indexdb write lock may
 * not be released when the JVM exits abnormally.</p>

 * <p>When this happens, a {@link LockObtainFailedException}
 * is hit when trying to create a writer, in which case you
 * need to explicitly clear the lock file first.  You can
 * either manually remove the file, or use the {@link
 * IndexWriter#unlock(Directory)}
 * API.  But, first be certain that no writer is in fact
 * writing to the index otherwise you can easily corrupt
 * your index.</p>
 *
 * <p>If you suspect that this or any other LockFactory is
 * not working properly in your environment, you can easily
 * test it by using {@link VerifyingLockFactory}, {@link
 * LockVerifyServer} and {@link LockStressTest}.</p>
 *
 * @see LockFactory
 */
public class SimpleFSLockFactory extends FSLockFactory {

	/**
	 * Create a SimpleFSLockFactory instance, with null (unset)
	 * lock directory. When you pass this factory to a {@link FSDirectory}
	 * subclass, the lock directory is automatically set to the
	 * directory itself. Be sure to create one instance for each directory
	 * your create!
	 */
	public SimpleFSLockFactory() throws IOException {
		this((File) null);
	}

	/**
	 * Instantiate using the provided directory (as a File instance).
	 * @param lockDir where lock files should be created.
	 */
	public SimpleFSLockFactory(File lockDir) throws IOException {
		setLockDir(lockDir);
	}

	/**
	 * Instantiate using the provided directory name (String).
	 * @param lockDirName where lock files should be created.
	 */
	public SimpleFSLockFactory(String lockDirName) throws IOException {
		setLockDir(new File(lockDirName));
	}

	@Override
	public Lock makeLock(String lockName) {
		if (mLockPrefix != null) 
			lockName = mLockPrefix + "-" + lockName;
		
		return new SimpleFSLock(mLockDir, lockName);
	}

	@Override
	public void clearLock(String lockName) throws IOException {
		if (mLockDir.exists()) {
			if (mLockPrefix != null) 
				lockName = mLockPrefix + "-" + lockName;
			
			File lockFile = new File(mLockDir, lockName);
			if (lockFile.exists() && !lockFile.delete()) 
				throw new IOException("Cannot delete " + lockFile);
		}
	}
	
	class SimpleFSLock extends Lock {
		private final File mLockFile;
		private final File mLockDir;

		public SimpleFSLock(File lockDir, String lockFileName) {
			mLockDir = lockDir;
			mLockFile = new File(lockDir, lockFileName);
		}

		@Override
		public boolean obtain() throws IOException {
			// Ensure that lockDir exists and is a directory:
			if (!mLockDir.exists()) {
				if (!mLockDir.mkdirs()) {
					throw new IOException("Cannot create directory: " +
							mLockDir.getAbsolutePath());
				}
		    } else if (!mLockDir.isDirectory()) {
		    	// TODO: NoSuchDirectoryException instead?
		    	throw new IOException("Found regular file where directory expected: " + 
		    			mLockDir.getAbsolutePath());
		    }
		    return mLockFile.createNewFile();
		}

		@Override
		public void release() throws LockReleaseFailedException {
			if (mLockFile.exists() && !mLockFile.delete())
				throw new LockReleaseFailedException("failed to delete " + mLockFile);
		}

		@Override
		public boolean isLocked() {
			return mLockFile.exists();
		}

		@Override
		public String toString() {
			return "SimpleFSLock@" + mLockFile;
		}
	}
	
}
