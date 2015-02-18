package org.javenstudio.lightning.http;

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
	
	//public WorkerTask.WorkerInfo getWorkerInfo(); 
	
}
