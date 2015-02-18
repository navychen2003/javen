package org.javenstudio.cocoka.worker;

public interface WorkerTaskHandler {

	public boolean handle(WorkerTask<?> task); 
	
}
