package org.javenstudio.falcon.util.job;

public interface WorkerTaskHandler {

	public boolean handle(WorkerTask<?> task); 
	
}
