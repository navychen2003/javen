package org.javenstudio.cocoka.worker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

public class LooperThread extends Thread {

	public static interface MessageListener {
		public void onHandleMessage(Message msg); 
	}
	
	private final ReentrantLock mLock;
	private final Condition mInited; 
	private final int mPriority; 
	private final List<MessageListener> mListeners; 
	private Handler mHandler = null; 
	
	public LooperThread() {
		this(false); 
	}
	
	public LooperThread(boolean fair) {
		this(Process.THREAD_PRIORITY_DEFAULT, fair); 
	}
	
	public LooperThread(int priority, boolean fair) {
		mListeners = new ArrayList<MessageListener>(); 
		mLock = new ReentrantLock(fair); 
		mInited = mLock.newCondition(); 
		mPriority = priority; 
	} 
	
	@Override
	public void run() {
		Process.setThreadPriority(mPriority);
		Looper.prepare();
		
		final ReentrantLock lock = this.mLock;
		lock.lock();
		try { 
			mHandler = new Handler() {
				public void handleMessage(Message msg) {
					synchronized (LooperThread.this) { 
						for (MessageListener l : mListeners)
							l.onHandleMessage(msg); 
					}
				}
			};
			
			mInited.signal();
		} finally { 
			lock.unlock();
		}
		
		Looper.loop(); // block
	}
	
	public Handler getHandler() throws InterruptedException {
		final ReentrantLock lock = this.mLock;
		lock.lockInterruptibly();
		try {
			try {
				while (mHandler == null) 
					mInited.await(); 
			} catch (InterruptedException ie) {
				mInited.signal(); // propagate to non-interrupted thread
				throw ie; 
			}
			return mHandler; 
		} finally {
			lock.unlock();
		}
	}
	
	public void addMessageListener(MessageListener l) {
		final ReentrantLock lock = this.mLock;
		lock.lock();
		try {
			synchronized (this) {
				if (l != null) 
					mListeners.add(l); 
			}
		} finally {
			lock.unlock();
		}
	}
	
}
