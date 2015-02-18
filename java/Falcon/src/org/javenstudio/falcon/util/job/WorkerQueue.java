package org.javenstudio.falcon.util.job;

public interface WorkerQueue<Result> {

	public boolean execute(WorkerTask<Result> task) throws InterruptedException; 
	public void stopWorkers() throws InterruptedException; 
	
}
