package org.javenstudio.cocoka.worker;

public interface WorkerQueue<Result> {

	public boolean execute(WorkerTask<Result> task) throws InterruptedException; 
	
	public void stopWorkers() throws InterruptedException; 
	
}
