package org.javenstudio.cocoka.worker;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractTask implements PriorityRunnable {

	private final ReentrantLock mLock;
	private final Condition mFinishCond; 
	private boolean mFinished; 
	
	public AbstractTask() {
		mLock = new ReentrantLock(false); 
		mFinishCond = mLock.newCondition(); 
		mFinished = false; 
	} 
	
	@Override 
	public int getPriority() { return 0; } 
	
	@Override 
	public final void run() {
		final ReentrantLock lock = this.mLock;
		lock.lock();
		
		try {
			onStart(); 
			onRun(); 
			onFinished(); 
			
			mFinished = true; 
			mFinishCond.signal(); 
			
		} finally {
			lock.unlock(); 
		}
	}

	@Override 
	public boolean isFinished() {
		return mFinished; 
	}
	
	@Override 
	public final void waitForFinished() throws InterruptedException {
		final ReentrantLock lock = this.mLock;
		lock.lockInterruptibly();
		try {
			try {
				while (mFinished == false) 
					mFinishCond.await(); 
			} catch (InterruptedException ie) {
				mFinishCond.signal(); // propagate to non-interrupted thread
				throw ie; 
			}
		} finally {
			lock.unlock();
		}
	}
	
	public void onStart() {} 
	
	public abstract void onRun(); 
	
	public void onFinished() {} 
	
}
