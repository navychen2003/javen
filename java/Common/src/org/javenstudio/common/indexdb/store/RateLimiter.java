package org.javenstudio.common.indexdb.store;

import org.javenstudio.common.indexdb.ThreadInterruptedException;

/** 
 * Simple class to rate limit IO.  Typically it's shared
 *  across multiple IndexInputs or IndexOutputs (for example
 *  those involved all merging).  Those IndexInputs and
 *  IndexOutputs would call {@link #pause} whenever they
 *  want to read bytes or write bytes. 
 */
public final class RateLimiter {
	private volatile double mMbPerSec;
	private volatile double mNsPerByte;
	private volatile long mLastNS;

	// TODO: we could also allow eg a sub class to dynamically
	// determine the allowed rate, eg if an app wants to
	// change the allowed rate over time or something

	/** mbPerSec is the MB/sec max IO rate */
	public RateLimiter(double mbPerSec) {
		setMbPerSec(mbPerSec);
	}

	/**
	 * Sets an updated mb per second rate limit.
	 */
	public void setMbPerSec(double mbPerSec) {
		mMbPerSec = mbPerSec;
		mNsPerByte = 1000000000. / (1024*1024*mbPerSec);
	}

	/**
	 * The current mb per second rate limit.
	 */
	public double getMbPerSec() {
		return mMbPerSec;
	}

	/** 
	 * Pauses, if necessary, to keep the instantaneous IO
	 *  rate at or below the target. NOTE: multiple threads
	 *  may safely use this, however the implementation is
	 *  not perfectly thread safe but likely in practice this
	 *  is harmless (just means in some rare cases the rate
	 *  might exceed the target).  It's best to call this
	 *  with a biggish count, not one byte at a time. 
	 */
	public void pause(long bytes) {
		if (bytes == 1) 
			return;

		// TODO: this is purely instantaneous rate; maybe we
		// should also offer decayed recent history one?
		final long targetNS = mLastNS = mLastNS + ((long) (bytes * mNsPerByte));
		long curNS = System.nanoTime();
		if (mLastNS < curNS) 
			mLastNS = curNS;

		// While loop because Thread.sleep doesn't always sleep
		// enough:
		while (true) {
			final long pauseNS = targetNS - curNS;
			if (pauseNS > 0) {
				try {
					Thread.sleep((int) (pauseNS/1000000), (int) (pauseNS % 1000000));
				} catch (InterruptedException ie) {
					throw new ThreadInterruptedException(ie);
				}
				curNS = System.nanoTime();
				continue;
			}
			break;
		}
	}
	
}
