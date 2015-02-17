package org.javenstudio.cocoka.worker.work;

import java.util.concurrent.atomic.AtomicInteger;

import org.javenstudio.cocoka.worker.AbstractTask;
import org.javenstudio.cocoka.worker.Constants;
import org.javenstudio.cocoka.worker.TaskQueue;
import org.javenstudio.cocoka.worker.TaskQueueHelper;
import org.javenstudio.cocoka.worker.queue.AdvancedQueueFactory;
import org.javenstudio.common.util.Logger;

public final class Scheduler {
	private static Logger LOG = Logger.getLogger(Scheduler.class); 

	static final AtomicInteger sWorkflowIdGenerator = new AtomicInteger(1);
	static final AtomicInteger sWorkIdGenerator = new AtomicInteger(1);
	
	public static int newWorkflowID() { return sWorkflowIdGenerator.getAndIncrement(); } 
	public static int newWorkID() { return sWorkIdGenerator.getAndIncrement(); } 
	
	private final AdvancedQueueFactory mFactory;
	private final TaskQueue mQueue; 
	
	public Scheduler() {
		this(TaskQueueHelper.getDefaultFactory());
	}
	
	public Scheduler(AdvancedQueueFactory factory) {
		mFactory = factory;
		mQueue = new TaskQueue(factory, null); 
	} 
	
	public final TaskQueue getQueue() { return mQueue; } 
	public final AdvancedQueueFactory getQueueFactory() { return mFactory; }
	
	public Workflow newWorkflow(String name) {
		return new Workflow(name); 
	}
	
	public final void post(final Work work) 
			throws InterruptedException, WorkException {
		if (work == null) 
			throw new NullPointerException("work is null"); 
		
		checkThread(); 
		
		synchronized (work) {
			if (work.mWorkflow != null) 
				throw new WorkException("work: "+work+" must posted by its workflow");
				
			mQueue.post(work); 
		}
	}
	
	public final void post(final Workflow flow) 
			throws InterruptedException, WorkException {
		if (flow == null) 
			throw new NullPointerException("workflow is null"); 
		
		checkThread(); 
		
		if (flow.mStatus != Workflow.STATUS_STARTADD) 
			throw new WorkException("workflow: "+flow+" already posted"); 
		
		synchronized (flow) {
			flow.mStatus = Workflow.STATUS_STARTRUN; 
			
			mQueue.post(new AbstractTask() {
				public String getName() { return flow.getName(); } 
				public void onRun() { onStartWorkflow(flow); } 
			});
		}
	}
	
	private final void postAtTimeOrDelayed(final Work work, 
			long uptimeMillis, long delayMillis) 
			throws InterruptedException, WorkException {
		if (work == null) 
			throw new NullPointerException("work is null"); 
		
		checkThread(); 
		
		synchronized (work) {
			if (work.mWorkflow != null) 
				throw new WorkException("work: "+work+" must posted by its workflow");
			
			if (uptimeMillis > 0) {
				mQueue.postAtTime(work, uptimeMillis); 
				
			} else if (delayMillis > 0) {
				mQueue.postDelayed(work, delayMillis); 
				
			} else
				mQueue.post(work); 
		}
	}
	
	private final void postAtTimeOrDelayed(final Workflow flow, 
			long uptimeMillis, long delayMillis) 
			throws InterruptedException, WorkException {
		if (flow == null) 
			throw new NullPointerException("workflow is null"); 
		
		checkThread(); 
		
		if (flow.mStatus != Workflow.STATUS_STARTADD) 
			throw new WorkException("workflow: "+flow+" already posted"); 
		
		synchronized (flow) {
			flow.mStatus = Workflow.STATUS_STARTRUN; 
			
			AbstractTask task = new AbstractTask() {
					public String getName() { return flow.getName(); } 
					public void onRun() { onStartWorkflow(flow); } 
				}; 
				
			if (uptimeMillis > 0) {
				mQueue.postAtTime(task, uptimeMillis);
				
			} else if (delayMillis > 0) {
				mQueue.postDelayed(task, delayMillis);
				
			} else {
				mQueue.post(task);
			}
		}
	}
	
	public final void postAtTime(final Work work, long uptimeMillis) 
			throws InterruptedException, WorkException {
		postAtTimeOrDelayed(work, uptimeMillis, 0); 
	}

	public final void postAtTime(final Workflow flow, long uptimeMillis) 
			throws InterruptedException, WorkException {
		postAtTimeOrDelayed(flow, uptimeMillis, 0); 
	}
	
	public final void postDelayed(final Work work, long delayMillis) 
			throws InterruptedException, WorkException {
		postAtTimeOrDelayed(work, 0, delayMillis); 
	}
	
	public final void postDelayed(final Workflow flow, long delayMillis) 
			throws InterruptedException, WorkException {
		postAtTimeOrDelayed(flow, 0, delayMillis); 
	}
	
	private void onStartWorkflow(final Workflow flow) {
		if (LOG.isDebugEnabled())
			LOG.debug("start workflow: " + flow);
		
		for (int i=0; i < flow.getWorkSize(); i++) {
			Work work = flow.getWork(i); 
			
			try {
				mQueue.postAfter(work, work.mAfter); 
			} catch (InterruptedException e) {
				
			}
		}
	}
	
	private void checkThread() {
		String threadName = Thread.currentThread().getName(); 
		if (threadName.startsWith(Constants.THREAD_PREFIX)) 
			throw new RuntimeException("Cannot post new work from Worker thread: "+threadName); 
	}
	
}
