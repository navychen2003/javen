package org.javenstudio.cocoka.worker.queue;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.javenstudio.cocoka.worker.Constants;
import org.javenstudio.cocoka.worker.WorkerQueue;
import org.javenstudio.cocoka.worker.WorkerTask;

public class SimpleWorkerQueue implements WorkerQueue<Boolean> {

	private final ReentrantLock mLock;
	private final Condition mCondIdle; 
	private final SimpleWorker[] mWorkers; 
	private final int mMin; 
	
	public SimpleWorkerQueue(int min, int max) {
		mLock = new ReentrantLock(false); 
		mCondIdle = mLock.newCondition(); 
		mWorkers = new SimpleWorker[max]; 
		mMin = min; 
		
		for (int i=0; i < mWorkers.length; i++) {
			if (i < min) {
				mWorkers[i] = new SimpleWorker(this, i); 
				mWorkers[i].startWork(); 
			} else
				mWorkers[i] = null; 
		}
	}
	
	private SimpleWorker waitForIdle() throws InterruptedException {
		final ReentrantLock lock = this.mLock;
		lock.lockInterruptibly();
		
		try {
			while (true) {
				SimpleWorker returnRunner = null; 
				
				for (int i=0; i < mWorkers.length; i++) {
					SimpleWorker runner = mWorkers[i]; 
					if (runner == null || !runner.isIdle()) continue; 
					if (returnRunner == null) {
						returnRunner = runner; 
					} else {
						int workerCount = 0; 
						for (int k=0; k < mWorkers.length; k++) {
							if (mWorkers[k] != null) 
								workerCount ++; 
						}
						if (workerCount <= mMin) 
							break; 
						if (runner.getIdleTime() > Constants.MAX_IDLE_TIME) {
							runner.requestStop(); 
							mWorkers[i] = null; 
						}
					}
				}
				
				if (returnRunner != null) 
					return returnRunner; 
				
				for (int i=0; i < mWorkers.length; i++) {
					if (mWorkers[i] == null) { 
						mWorkers[i] = new SimpleWorker(this, i); 
						mWorkers[i].startWork(); 
						return mWorkers[i]; 
					}
				}
				
				try {
					mCondIdle.await(); 
				} catch (InterruptedException ie) {
					mCondIdle.signal(); 
					throw ie; 
				}
			}
		} finally {
			lock.unlock(); 
		}
	}
	
	public void notifyIdle() {
		final ReentrantLock lock = this.mLock;
		lock.lock();
		
		try {
			mCondIdle.signal(); 
		} finally {
			lock.unlock(); 
		}
	}
	
	@Override 
	public boolean execute(WorkerTask<Boolean> task) throws InterruptedException { 
		if (task == null) return false; 
		
		SimpleWorker worker = waitForIdle(); 
		if (worker != null) {
			worker.execute(task); 
			return true; 
		} 
		
		return false; 
	}
	
	@Override 
	public void stopWorkers() throws InterruptedException {
		for (int i=0; i < mWorkers.length; i++) {
			mWorkers[i].stopWork(); 
		}
	}
	
}
