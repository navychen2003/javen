package org.javenstudio.cocoka.worker.work;

import org.javenstudio.cocoka.worker.AbstractTask;
import org.javenstudio.common.util.Logger;

public abstract class Work extends AbstractTask implements Workable {
	private static Logger LOG = Logger.getLogger(Work.class); 

	final int mID; 
	final String mName; 
	
	Workflow mWorkflow = null; 
	Work mAfter = null; 
	boolean mParallelMode = false; 
	
	long mStartTime = 0; 
	
	public Work(String name) {
		mID = Scheduler.newWorkID(); 
		mName = name; 
	}
	
	public int getID() { return mID; }
	
	@Override 
	public String getName() { return mName; }
	
	public void onStart() {
		if (LOG.isDebugEnabled())
			LOG.debug(toString() + " start run ..."); 
		
		mStartTime = System.currentTimeMillis(); 
		
		super.onStart(); 
	}
	
	public void onFinished() {
		super.onFinished(); 
		
		long elapsed = System.currentTimeMillis() - mStartTime; 
		
		if (LOG.isInfoEnabled())
			LOG.info(toString() + " finished in " + elapsed + " ms");
	}
	
	@Override
	public String toString() {
		return "Work-" + mID + "[" + mName + "]"; 
	}
	
}
