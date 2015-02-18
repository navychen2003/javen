package org.javenstudio.cocoka.worker.queue;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.javenstudio.cocoka.worker.Constants;
import org.javenstudio.cocoka.worker.WorkerTask;
import org.javenstudio.common.util.Logger;

public class SimpleWorker extends Thread {
	private static Logger LOG = Logger.getLogger(SimpleWorker.class); 

	private final SimpleWorkerQueue mQueue; 
	private final int mRunnerId; 
	private final ReentrantLock mLock;
	private final Condition mCondIdle; 
	private final Condition mCondNew; 
	
	private WorkerTask<Boolean> mTask = null; 
	private long mIdleStart = 0; 
	private boolean mRequestStop = false; 
	
	public SimpleWorker(SimpleWorkerQueue queue, int id) {
		super(Constants.THREAD_PREFIX+"-thread-"+id); 
		mLock = new ReentrantLock(false); 
		mCondIdle = mLock.newCondition(); 
		mCondNew = mLock.newCondition(); 
		mQueue = queue; 
		mRunnerId = id; 
		mTask = null; 
	}
	
	public int getWorkerId() {
		return mRunnerId; 
	}
	
	public void startWork() {
		start(); 
	}
	
	public boolean isIdle() {
		final ReentrantLock lock = this.mLock;
		lock.lock();
		
		try {
			return mTask == null; 
			
		} finally { 
			lock.unlock(); 
		}
	}
	
	public long getIdleTime() {
		final ReentrantLock lock = this.mLock;
		lock.lock();
		
		try {
			return mIdleStart > 0 ? System.currentTimeMillis() - mIdleStart : 0; 
			
		} finally { 
			lock.unlock(); 
		}
	}
	
	public void requestStop() {
		final ReentrantLock lock = this.mLock;
		lock.lock();
		
		try {
			mRequestStop = true; 
			mCondNew.signal(); 
			
		} finally { 
			lock.unlock(); 
		}
	}
	
	public void stopWork() throws InterruptedException {
		final ReentrantLock lock = this.mLock;
		lock.lockInterruptibly();
		
		try {
			try {
				while (mTask != null) 
					mCondIdle.await(); 
				
				interrupt(); 
				join(); 
			} catch (InterruptedException ie) {
				mCondIdle.signal(); 
				throw ie; 
			}
			
		} finally { 
			lock.unlock(); 
		}
	}
	
	public void execute(WorkerTask<Boolean> task) throws InterruptedException {
		final ReentrantLock lock = this.mLock;
		lock.lockInterruptibly();
		
		try {
			try {
				while (mTask != null) 
					mCondIdle.await(); 
				
			} catch (InterruptedException ie) {
				mCondIdle.signal(); 
				throw ie; 
			}

			mTask = task; 
			mCondNew.signal(); 

		} finally {
			lock.unlock(); 
		}
	}
	
	@Override
	public void run() {
		final ReentrantLock lock = this.mLock;
		
		LOG.info("Scheduler: SimpleWorker-"+mRunnerId+" started"); 
		
		while (!Thread.interrupted()) {
			lock.lock(); 
			try {
				while (mTask == null && mRequestStop == false) 
					mCondNew.await(); 
				
				mIdleStart = 0; 
				
			} catch (InterruptedException ie) {
				mCondNew.signal(); 
				continue; 
				
			} finally {
				lock.unlock(); 
			}
			
			try { 
				if (mTask != null)
					mTask.call(); 
			} catch (Exception e) { 
				LOG.error("Scheduler: run task error", e); 
			}
			
			lock.lock(); 
			try {
				mTask = null; 
				if (mRequestStop) break; 
				
				mIdleStart = System.currentTimeMillis(); 
				mCondIdle.signal(); 
				mQueue.notifyIdle(); 

			} finally {
				lock.unlock(); 
			}
		}
		
		LOG.info("Scheduler: SimpleWorker-"+mRunnerId+" stopped"); 
	}
	
}
