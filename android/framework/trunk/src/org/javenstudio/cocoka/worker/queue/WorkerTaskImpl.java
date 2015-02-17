package org.javenstudio.cocoka.worker.queue;

import android.os.Process; 

import org.javenstudio.cocoka.worker.PriorityRunnable;
import org.javenstudio.cocoka.worker.WorkerTask;

public class WorkerTaskImpl extends WorkerTask<Boolean> {

	private final PriorityRunnable mRunnable; 
	private final WorkerTask.WorkerInfo mWorkerInfo; 
	
	public WorkerTaskImpl(PriorityRunnable r) {
		mRunnable = r; 
		mWorkerInfo = new WorkerTask.WorkerInfo() { 
				public String getName() { return mRunnable.getName(); }
				public Object getData() { return mRunnable; }
			};
	}
	
	@Override 
	public WorkerInfo getWorkerInfo() { 
		return mWorkerInfo; 
	}
	
	@Override 
	public Boolean onCall() throws Exception {
		if (mRunnable != null) { 
			if (mRunnable.getPriority() <= 0) { 
				Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND); 
			} else { 
				Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT); 
			}
			
			mRunnable.run(); 
			return true; 
		}
		
		return false; 
	}
	
}
