package org.javenstudio.cocoka.net.http;

import org.javenstudio.cocoka.worker.WorkerTask;

public interface HttpTask {

	public static final int SC_EXCEPTION = -1; 
	
	public interface Request {}
	
	public interface Result {
		public Object getData(); 
	}
	
	public interface Progress {
		public float getProgress(); 
	}
	
	public interface Publisher {
		public boolean isAborted(); 
		public void publish(Progress progress); 
	}
	
	public Result execute(Request request, Publisher publisher); 
	
	public WorkerTask.WorkerInfo getWorkerInfo(); 
	
}
