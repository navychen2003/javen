package org.javenstudio.falcon.util.job;

public interface PriorityRunnable extends Runnable {

	public int getPriority(); 
	public String getName(); 
	public boolean isFinished(); 
	public void waitForFinished() throws InterruptedException;
	
}
