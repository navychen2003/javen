package org.javenstudio.common.indexdb.index;

import java.util.IdentityHashMap;
import java.util.Map;

import org.javenstudio.common.indexdb.ThreadInterruptedException;

/**
 * Controls the health status of a {@link DocumentsWriter} sessions. This class
 * used to block incoming indexing threads if flushing significantly slower than
 * indexing to ensure the {@link DocumentsWriter}s healthiness. If flushing is
 * significantly slower than indexing the net memory used within an
 * {@link IndexWriter} session can increase very quickly and easily exceed the
 * JVM's available memory.
 * <p>
 * To prevent OOM Errors and ensure IndexWriter's stability this class blocks
 * incoming threads from indexing once 2 x number of available
 * {@link DocumentWriterState}s in {@link DocumentsWriterPerThreadPool} is exceeded.
 * Once flushing catches up and the number of flushing DWPT is equal or lower
 * than the number of active {@link DocumentWriterState}s threads are released and can
 * continue indexing.
 */
public class StallControl {
  
	private final Map<Thread, Boolean> mWaiting = 
			new IdentityHashMap<Thread, Boolean>(); // only with assert
	
	private int mNumWaiting; // only with assert
	private boolean mWasStalled; // only with assert
	
	private volatile boolean mStalled;
	
	/**
	 * Update the stalled flag status. This method will set the stalled flag to
	 * <code>true</code> if the number of flushing
	 * {@link DocumentsWriterPerThread} is greater than the number of active
	 * {@link DocumentsWriterPerThread}. Otherwise it will reset the
	 * {@link StallControl} to healthy and release all threads
	 * waiting on {@link #waitIfStalled()}
	 */
	public synchronized void updateStalled(boolean stalled) {
		mStalled = stalled;
		if (stalled) 
			mWasStalled = true;
		
		notifyAll();
	}
  
	/**
	 * Blocks if documents writing is currently in a stalled state. 
	 * 
	 */
	public void waitIfStalled() {
		if (mStalled) {
			synchronized (this) {
				// react on the first wakeup call!
				if (mStalled) { 
					// don't loop here, higher level logic will re-stall!
					try {
						assert increaseWaiters();
						wait();
						assert decreaseWaiters();
					} catch (InterruptedException e) {
						throw new ThreadInterruptedException(e);
					}
				}
			}
		}
	}
  
	public boolean anyStalledWriters() {
		return mStalled;
	}
  
	private boolean increaseWaiters() {
		mNumWaiting ++;
		assert mWaiting.put(Thread.currentThread(), Boolean.TRUE) == null;
    
		return mNumWaiting > 0;
	}
  
	private boolean decreaseWaiters() {
		mNumWaiting --;
		assert mWaiting.remove(Thread.currentThread()) != null;
		
		return mNumWaiting >= 0;
	}
  
	public synchronized boolean hasBlocked() { // for tests
		return mNumWaiting > 0;
	}
  
	public boolean isHealthy() { // for tests
		return !mStalled; // volatile read!
	}
  
	public synchronized boolean isThreadQueued(Thread t) { // for tests
		return mWaiting.containsKey(t);
	}

	public synchronized boolean wasStalled() { // for tests
		return mWasStalled;
	}
	
}
