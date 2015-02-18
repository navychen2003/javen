package org.javenstudio.cocoka.worker.work;

import java.util.ArrayList;
import java.util.List;

public final class Workflow implements Workable {

	public static final int STATUS_STARTADD = 0; 
	public static final int STATUS_STARTRUN = 1; 
	public static final int STATUS_FINISHED = 2; 
	
	private final List<Work> mWorks; 
	private final String mName; 
	
	final int mID; 
	int mStatus = STATUS_STARTADD; 
	
	Workflow(String name) {
		mWorks = new ArrayList<Work>(); 
		mID = Scheduler.newWorkflowID(); 
		mName = name; 
	} 
	
	public int getID() { return mID; } 
	public String getName() { return mName; }
	public int getStatus() { return mStatus; } 
	
	public void addParallel(Work work) throws WorkException {
		add(work, null, true); 
	}
	
	public void addParallel(Work work, Work after) throws WorkException {
		add(work, after, true); 
	}
	
	public void addAfter(Work work) throws WorkException {
		add(work, null, false); 
	}
	
	public void addAfter(Work work, Work after) throws WorkException {
		add(work, after, false); 
	}
	
	private void add(Work work, Work after, boolean parallelMode) throws WorkException {
		if (work == null) throw new WorkException("work is null"); 

		if (work.mWorkflow != null) 
			throw new WorkException("work: "+work+" already in workflow");
		
		synchronized (this) {
			if (mStatus != STATUS_STARTADD) 
				throw new WorkException("workflow: "+toString()+" cannot add new work now"); 
			
			if (after == null) {
				if (work.mAfter != null) 
					throw new WorkException("work: "+work+" already set after?");
				
				if (!parallelMode) {
					for (int i=mWorks.size()-1; i >= 0; i--) {
						Work wk = mWorks.get(i); 
						if (wk.mParallelMode) continue; 
						after = wk; 
						break; 
					}
				}
			}
			
			if (after != null) {
				if (after.mWorkflow == null) 
					throw new WorkException("after work: "+after+" not in workflow"); 
				
				if (after.mWorkflow != this) 
					throw new WorkException("after work: "+after+" not in this workflow: "+toString()); 
				
				work.mAfter = after; 
			}
			
			work.mWorkflow = this; 
			work.mParallelMode = parallelMode; 
			
			mWorks.add(work); 
		}
	}
	
	public int getWorkSize() { 
		synchronized (this) {
			return mWorks.size(); 
		}
	}
	
	public Work getWork(int position) {
		synchronized (this) {
			if (position >= 0 && position < mWorks.size()) 
				return mWorks.get(position); 
			else
				return null; 
		}
	}
	
	public String toString() {
		return "Workflow-"+mID+"["+mName+"]"; 
	}
	
}
