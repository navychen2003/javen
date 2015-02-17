package org.javenstudio.cocoka.worker;

public interface PriorityRunnable extends Runnable {

	public int getPriority(); 
	public String getName(); 
	public boolean isFinished(); 
	public void waitForFinished() throws InterruptedException;
	
}
