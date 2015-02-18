package org.javenstudio.falcon.util.job;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.javenstudio.common.util.ListenerObservable;
import org.javenstudio.common.util.Logger;

public abstract class WorkerTask<Result> implements Callable<Result> {
	private static final Logger LOG = Logger.getLogger(WorkerTask.class);
	
	public static final int STATUS_PENDING = 1; 
	public static final int STATUS_RUNNING = 2; 
	public static final int STATUS_FINISHED = 3; 
	public static final int STATUS_STOPPED = 4; 
	
	public static interface WorkerInfo { 
		public String getName(); 
		public Object getData(); 
	}
	
	public static interface OnChangeListener { 
		public void onChange(WorkerTask<?> task); 
	}
	
	public static interface WorkerChecker { 
		public boolean checkWorker(WorkerTask<?> worker); 
	}
	
	private static final AtomicInteger sIdGenerator = new AtomicInteger(1);
	
	private static final List<WorkerTask<?>> sTasks = new ArrayList<WorkerTask<?>>(); 
	private static final ListenerObservable sChangeObservable = new ListenerObservable(); 
	
	public static void registerListener(ListenerObservable.OnChangeListener l) { 
		sChangeObservable.registerListener(l); 
	}
	
	public static void unregisterListener(ListenerObservable.OnChangeListener l) { 
		sChangeObservable.unregisterListener(l); 
	}
	
	public static void registerAndNotifyListener(ListenerObservable.OnChangeListener l) { 
		if (sChangeObservable.registerListener(l)) 
			notifyListener(l); 
	}
	
	public static void notifyListener(ListenerObservable.OnChangeListener l) { 
		if (l != null) { 
			WorkerTask<?>[] tasks = getWorkerTasks(); 
			for (int i=0; tasks != null && i < tasks.length; i++) { 
				WorkerTask<?> task = tasks[i]; 
				if (task != null) 
					l.onChangeEvent(0, task); 
			}
		}
	}
	
	private static void addWorkerTask(WorkerTask<?> newTask) { 
		if (newTask == null) return; 
		
		boolean changed = false; 
		int changeAction = 0; 
		WorkerTask<?> changedTask = null; 
		
		synchronized (sTasks) { 
			boolean found = false; 
			long current = System.currentTimeMillis(); 
			
			for (int i=0; i < sTasks.size(); ) { 
				WorkerTask<?> task = sTasks.get(i); 
				if (task.getStatus() == STATUS_FINISHED) { 
					long elapsed = current - task.getFinishTime(); 
					if (elapsed > Constants.WORKERTASK_RETAIN_TIME) { 
						sTasks.remove(i); 
						changedTask = task; 
						changeAction = ListenerObservable.ACTION_DELETE; 
						changed = true; 
						continue; 
					}
				}
				if (task == newTask) found = true; 
				i ++; 
			}
			
			if (!found) { 
				sTasks.add(newTask); 
				changedTask = newTask; 
				changeAction = ListenerObservable.ACTION_ADD; 
				changed = true; 
			}
		}
		
		if (changed && changedTask != null) 
			sChangeObservable.notifyChange(changeAction, changedTask); 
	}
	
	public static WorkerTask<?>[] getWorkerTasks() { 
		synchronized (sTasks) { 
			return sTasks.toArray(new WorkerTask<?>[0]); 
		}
	}
	
	public static WorkerTask<?> getWorkerTask(WorkerChecker checker) { 
		synchronized (sTasks) { 
			for (WorkerTask<?> worker : sTasks) { 
				if (checker.checkWorker(worker)) 
					return worker; 
			}
			return null; 
		}
	}
	
	public static WorkerTask<?>[] getWorkerTasks(WorkerChecker checker) { 
		synchronized (sTasks) { 
			ArrayList<WorkerTask<?>> list = new ArrayList<WorkerTask<?>>(); 
			
			for (WorkerTask<?> worker : sTasks) { 
				if (checker.checkWorker(worker)) 
					list.add(worker); 
			}
			
			return list.toArray(new WorkerTask<?>[list.size()]); 
		}
	}
	
	private int mId = sIdGenerator.getAndIncrement();
	private int mStatus = STATUS_PENDING; 
	private boolean mRequestStop = false; 
	private long mPendTime = 0; 
	private long mStartTime = 0; 
	private long mFinishTime = 0; 
	
	public WorkerTask() { 
		addWorkerTask(this); 
		mPendTime = System.currentTimeMillis(); 
	}
	
	public final synchronized int getStatus() { 
		return mStatus; 
	}
	
	public final long getPendTime() { 
		return mPendTime; 
	}
	
	public final synchronized long getStartTime() { 
		return mStartTime; 
	}
	
	public final synchronized long getFinishTime() { 
		return mFinishTime; 
	}
	
	@Override 
	public final Result call() throws Exception {
		try { 
			synchronized (this) { 
				mStartTime = System.currentTimeMillis(); 
				
				if (mRequestStop) 
					return null; 
				
				mStatus = STATUS_RUNNING; 
				mFinishTime = 0; 
			}
			
			if (LOG.isDebugEnabled())
				LOG.debug(getWorkerInfo().getName() + " calling ...");
			
			sChangeObservable.notifyChange(ListenerObservable.ACTION_START, this); 
			
			return onCall(); 
			
		} finally { 
			synchronized (this) { 
				if (mRequestStop) 
					mStatus = STATUS_STOPPED; 
				else 
					mStatus = STATUS_FINISHED; 
				
				mFinishTime = System.currentTimeMillis(); 
			}
			
			sChangeObservable.notifyChange(ListenerObservable.ACTION_STOP, this); 
			
			if (LOG.isInfoEnabled()) 
				LOG.info(toString() + " finished in " + (mFinishTime - mStartTime) + " ms");
		}
	}
	
	public final void requestStop() { 
		synchronized (this) { 
			if (!mRequestStop) 
				mRequestStop = true; 
			
			if (mStatus != STATUS_RUNNING) { 
				if (mStatus != STATUS_FINISHED) 
					mStatus = STATUS_STOPPED; 
				
				if (mStartTime <= 0) 
					mStartTime = System.currentTimeMillis(); 
				
				if (mFinishTime <= 0) 
					mFinishTime = System.currentTimeMillis(); 
			}
		}
	}
	
	public final boolean isRequestStop() { 
		synchronized (this) { 
			return mRequestStop; 
		}
	}
	
	protected abstract Result onCall() throws Exception; 
	
	public abstract WorkerInfo getWorkerInfo(); 
	
	@Override
	public String toString() {
		return "WorkTask-" + mId + "[" + getWorkerInfo().getName() + "]"; 
	}
	
}
