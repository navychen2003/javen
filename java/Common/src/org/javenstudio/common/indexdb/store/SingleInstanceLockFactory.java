package org.javenstudio.common.indexdb.store;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.javenstudio.common.indexdb.Lock;
import org.javenstudio.common.indexdb.LockFactory;

/**
 * Implements {@link LockFactory} for a single in-process instance,
 * meaning all locking will take place through this one instance.
 * Only use this {@link LockFactory} when you are certain all
 * IndexReaders and IndexWriters for a given index are running
 * against a single shared in-process Directory instance.  This is
 * currently the default locking for RAMDirectory.
 *
 * @see LockFactory
 */
public class SingleInstanceLockFactory extends LockFactory {

	private Set<String> mLocks = new HashSet<String>();

	@Override
	public Lock makeLock(String lockName) {
		// We do not use the LockPrefix at all, because the private
		// HashSet instance effectively scopes the locking to this
		// single Directory instance.
		return new SingleInstanceLock(mLocks, lockName);
	}

	@Override
	public void clearLock(String lockName) throws IOException {
		synchronized (mLocks) {
			if (mLocks.contains(lockName)) 
				mLocks.remove(lockName);
		}
	}
  
	class SingleInstanceLock extends Lock {

		private final String mLockName;
		private final Set<String> mLocks;

		public SingleInstanceLock(Set<String> locks, String lockName) {
			mLocks = locks;
			mLockName = lockName;
		}

		@Override
		public boolean obtain() throws IOException {
			synchronized (mLocks) {
				return mLocks.add(mLockName);
			}
		}

		@Override
		public void release() {
			synchronized (mLocks) {
				mLocks.remove(mLockName);
			}
		}

		@Override
		public boolean isLocked() {
			synchronized (mLocks) {
				return mLocks.contains(mLockName);
			}
		}

		@Override
		public String toString() {
			return super.toString() + ": " + mLockName;
		}
	}
  
}
