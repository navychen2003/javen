package org.javenstudio.common.indexdb.store;

import java.io.IOException;

import org.javenstudio.common.indexdb.Lock;
import org.javenstudio.common.indexdb.LockFactory;

/**
 * Use this {@link LockFactory} to disable locking entirely.
 * Only one instance of this lock is created.  You should call {@link
 * #getNoLockFactory()} to get the instance.
 *
 * @see LockFactory
 */
public class NoLockFactory extends LockFactory {

	// Single instance returned whenever makeLock is called.
	private static NoLock sSingletonLock = new NoLock();
	private static NoLockFactory sSingleton = new NoLockFactory();
  
	private NoLockFactory() {}

	public static NoLockFactory getNoLockFactory() {
		return sSingleton;
	}

	@Override
	public Lock makeLock(String lockName) {
		return sSingletonLock;
	}

	@Override
	public void clearLock(String lockName) {
		// do nothing
	}
	
	static class NoLock extends Lock {
		@Override
		public boolean obtain() throws IOException {
			return true;
		}

		@Override
		public void release() {
		}

		@Override
		public boolean isLocked() {
			return false;
		}

		@Override
		public String toString() {
			return "NoLock";
		}
	}
	
}
