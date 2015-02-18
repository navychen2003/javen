package org.javenstudio.android;

public interface ServiceCallback extends ServiceContext {

	public void requestStopSelf(int startId); 
	public void requestUpdateWidget(); 
	
}
