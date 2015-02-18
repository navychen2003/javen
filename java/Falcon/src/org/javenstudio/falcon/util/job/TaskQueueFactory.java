package org.javenstudio.falcon.util.job;

public interface TaskQueueFactory {

	public WorkerQueue<Boolean> createWorkerQueue(); 
	public WorkerTask<Boolean> createWorkerTask(PriorityRunnable r); 
	
}
