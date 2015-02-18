package org.javenstudio.falcon.util.job;

import java.util.concurrent.atomic.AtomicInteger;

import org.javenstudio.common.util.Logger;

public abstract class Work extends AbstractTask implements Workable {
	private static Logger LOG = Logger.getLogger(Work.class); 

	static final AtomicInteger sWorkIdGenerator = new AtomicInteger(1);
	static int newWorkID() { return sWorkIdGenerator.getAndIncrement(); } 
	
	private final int mID; 
	private final String mName; 
	
	private long mStartTime = 0; 
	private long mFinishTime = 0;
	
	public Work(String name) {
		mID = newWorkID(); 
		mName = name; 
	}
	
	public int getID() { return mID; }
	
	public long getStartTime() { return mStartTime; }
	public long getFinishTime() { return mFinishTime; }
	
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
		
		mFinishTime = System.currentTimeMillis();
		long elapsed = mFinishTime - mStartTime; 
		
		if (LOG.isInfoEnabled())
			LOG.info(toString() + " finished in " + elapsed + " ms");
	}
	
	@Override
	public String toString() {
		return "Work-" + mID + "[" + mName + "]"; 
	}
	
}
