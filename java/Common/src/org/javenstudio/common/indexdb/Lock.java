package org.javenstudio.common.indexdb;

import java.io.IOException;

/** 
 * An interprocess mutex lock.
 * <p>Typical use might look like:<pre>
 * new Lock.With(directory.makeLock("my.lock")) {
 *     public Object doBody() {
 *       <i>... code to execute while locked ...</i>
 *     }
 *   }.run();
 * </pre>
 *
 * @see IDirectory#makeLock(String)
 */
public abstract class Lock {

	/** 
	 * How long {@link #obtain(long)} waits, in milliseconds,
	 *  in between attempts to acquire the lock. 
	 */
	public static long LOCK_POLL_INTERVAL = 1000;

	/** 
	 * Pass this value to {@link #obtain(long)} to try
	 *  forever to obtain the lock. 
	 */
	public static final long LOCK_OBTAIN_WAIT_FOREVER = -1;

	/** 
	 * Attempts to obtain exclusive access and immediately return
	 *  upon success or failure.
	 * @return true iff exclusive access is obtained
	 */
	public abstract boolean obtain() throws IOException;

	/**
	 * If a lock obtain called, this failureReason may be set
	 * with the "root cause" Exception as to why the lock was
	 * not obtained.
	 */
	protected Throwable mFailureReason;

	/** 
	 * Attempts to obtain an exclusive lock within amount of
	 *  time given. Polls once per {@link #LOCK_POLL_INTERVAL}
	 *  (currently 1000) milliseconds until lockWaitTimeout is
	 *  passed.
	 * @param lockWaitTimeout length of time to wait in
	 *        milliseconds or {@link
	 *        #LOCK_OBTAIN_WAIT_FOREVER} to retry forever
	 * @return true if lock was obtained
	 * @throws LockObtainFailedException if lock wait times out
	 * @throws IllegalArgumentException if lockWaitTimeout is
	 *         out of bounds
	 * @throws IOException if obtain() throws IOException
	 */
	public boolean obtain(long lockWaitTimeout) 
			throws LockObtainFailedException, IOException {
		mFailureReason = null;
		boolean locked = obtain();
		
		if (lockWaitTimeout < 0 && lockWaitTimeout != LOCK_OBTAIN_WAIT_FOREVER) {
			throw new IllegalArgumentException(
					"lockWaitTimeout should be LOCK_OBTAIN_WAIT_FOREVER or a non-negative number (got " 
					+ lockWaitTimeout + ")");
		}
		
		long maxSleepCount = lockWaitTimeout / LOCK_POLL_INTERVAL;
		long sleepCount = 0;
		
		while (!locked) {
			if (lockWaitTimeout != LOCK_OBTAIN_WAIT_FOREVER && sleepCount++ >= maxSleepCount) {
				String reason = "Lock obtain timed out: " + this.toString();
				if (mFailureReason != null) 
					reason += ": " + mFailureReason;
				
				LockObtainFailedException e = new LockObtainFailedException(reason);
				if (mFailureReason != null) 
					e.initCause(mFailureReason);
				
				throw e;
			}
			try {
				Thread.sleep(LOCK_POLL_INTERVAL);
			} catch (InterruptedException ie) {
				throw new ThreadInterruptedException(ie);
			}
			locked = obtain();
		}
		
		return locked;
	}

	/** Releases exclusive access. */
	public abstract void release() throws IOException;

	/** 
	 * Returns true if the resource is currently locked.  Note that one must
	 * still call {@link #obtain()} before using the resource. 
	 */
	public abstract boolean isLocked() throws IOException;

	/** Utility class for executing code with exclusive access. */
	public abstract static class With {
		private final Lock mLock;
		private final long mLockWaitTimeout;

		/** Constructs an executor that will grab the named lock. */
		public With(Lock lock, long lockWaitTimeout) {
			mLock = lock;
			mLockWaitTimeout = lockWaitTimeout;
		}

		/** Code to execute with exclusive access. */
		protected abstract Object doBody() throws IOException;

		/** 
		 * Calls {@link #doBody} while <i>lock</i> is obtained.  Blocks if lock
		 * cannot be obtained immediately.  Retries to obtain lock once per second
		 * until it is obtained, or until it has tried ten times. Lock is released when
		 * {@link #doBody} exits.
		 * @throws LockObtainFailedException if lock could not
		 * be obtained
		 * @throws IOException if {@link Lock#obtain} throws IOException
		 */
		public Object run() throws LockObtainFailedException, IOException {
			boolean locked = false;
			try {
				locked = mLock.obtain(mLockWaitTimeout);
				return doBody();
			} finally {
				if (locked)
					mLock.release();
			}
		}
	}

}
