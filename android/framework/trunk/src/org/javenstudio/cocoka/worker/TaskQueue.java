package org.javenstudio.cocoka.worker;

public class TaskQueue {

	private final TaskQueueFactory mFactory;
	private volatile WorkerTaskHandler mHandler;
	private final WorkerQueue<Boolean> mQueue; 
	private final LooperThread mThread; 
	
	public TaskQueue(TaskQueueFactory factory, WorkerTaskHandler handler) {
		mFactory = factory; 
		mQueue = factory.createWorkerQueue(); 
		mThread = new LooperThread(); 
		mHandler = handler; 
		
		mThread.start(); 
	}
	
	public LooperThread getLooperThread() { 
		return mThread; 
	}
	
	public final boolean post(PriorityRunnable r) throws InterruptedException {
		return mThread.getHandler().post(getPostRunnable(r)); 
	}
	
	public final boolean postAfter(PriorityRunnable r, PriorityRunnable after) 
			throws InterruptedException {
		if (after != null) 
			after.waitForFinished(); 
		
		return post(r); 
	}
	
	public final boolean postAtTime(PriorityRunnable r, long uptimeMillis) 
			throws InterruptedException {
		return mThread.getHandler().postAtTime(getPostRunnable(r), uptimeMillis); 
	}
	
	public final boolean postDelayed(PriorityRunnable r, long delayMillis) 
			throws InterruptedException {
		return mThread.getHandler().postDelayed(getPostRunnable(r), delayMillis); 
	}
	
	private Runnable getPostRunnable(final PriorityRunnable r) {
		return new Runnable() {
				public void run() {
					dispatch(mFactory.createWorkerTask(r)); 
				}
			};
	}
	
	private boolean dispatch(WorkerTask<Boolean> task) {
		try {
			return mQueue.execute(task); 
		} catch (InterruptedException ie) {
			// ignore
		}
		
		// dispatch failed, try default handler
		if (mHandler != null) 
			return mHandler.handle(task); 
		
		return false; 
	}
	
}
