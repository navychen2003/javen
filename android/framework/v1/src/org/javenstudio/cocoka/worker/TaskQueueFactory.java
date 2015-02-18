package org.javenstudio.cocoka.worker;

public interface TaskQueueFactory {

	public WorkerQueue<Boolean> createWorkerQueue(); 
	public WorkerTask<Boolean> createWorkerTask(PriorityRunnable r); 
	
}
