package org.javenstudio.falcon.setting.cluster;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.job.Job;
import org.javenstudio.falcon.util.job.JobCancelListener;
import org.javenstudio.falcon.util.job.JobContext;
import org.javenstudio.falcon.util.job.JobSubmit;

public class HostJob implements Job<Void>, JobCancelListener {
	private static final Logger LOG = Logger.getLogger(HostJob.class);
	
	public static interface Task { 
		public String getUser();
		public String getMessage();
		
		public void process(HostJob job, JobContext jc) 
				throws ErrorException;
		
		public void close();
	}
	
	private final LinkedList<Task> mTasks = 
			new LinkedList<Task>();
	
	private final Object mLock = new Object();
	private JobSubmit.JobWork<Void> mRunningWork = null;
	
	private final Map<String, String> mStatusMessages = 
			new HashMap<String, String>();
	
	private final HostManager mManager;
	private Task mCurrentTask = null;
	private boolean mCanceled = false;
	
	public HostJob(HostManager manager) { 
		if (manager == null) throw new NullPointerException();
		mManager = manager;
	}
	
	public HostManager getManager() { 
		return mManager;
	}
	
	@Override
	public String getName() {
		return "HostJob";
	}
	
	@Override
	public String getUser() { 
		Task task = mCurrentTask;
		if (task != null) return task.getUser();
		return null;
	}
	
	public void startJob(Task... tasks) { 
		if (tasks == null || tasks.length == 0) 
			return;
		
		synchronized (mLock) { 
			for (Task task : tasks) {
				if (task != null) mTasks.add(task);
			}
			
			startRun();
		}
	}
	
	public boolean existJob(Task task) {
		if (task == null) return false;
		
		synchronized (mLock) { 
			for (Task t : mTasks) { 
				if (t == task) return true;
			}
			
			return task == mCurrentTask;
		}
	}
	
	private void startRun() { 
		synchronized (mLock) { 
			if (mRunningWork == null || mRunningWork.isDone()) { 
				HostJob job = this; //new IndexJob();
				JobSubmit.JobWork<Void> work = JobSubmit.submit(job);
				if (work != null) 
					work.setCancelListener(job);
				
				mRunningWork = work;
			}
		}
	}
	
	private Task popTask() { 
		synchronized (mLock) { 
			if (mTasks.size() > 0)
				return mTasks.removeFirst();
			
			return null;
		}
	}
	
	private void onFinished() { 
		synchronized (mLock) { 
			mRunningWork = null;
		}
	}
	
	public void close() { 
		if (LOG.isDebugEnabled())
			LOG.debug("close");
		
		synchronized (mLock) { 
			JobSubmit.JobWork<Void> work = mRunningWork;
			mRunningWork = null;
			
			if (work != null) 
				work.waitDone();
		}
	}
	
	@Override
	public String getMessage() {
		Task task = mCurrentTask;
		if (task != null) return task.getMessage();
		return null;
	}

	@Override
	public Map<String, String> getStatusMessages() {
		return mStatusMessages;
	}
	
	@Override
	public void onCancel() {
		mCanceled = true;
		
		if (LOG.isDebugEnabled())
			LOG.debug("onCancel: job canceled");
	}
	
	public boolean isCanceled() { 
		return mCanceled;
	}

	@Override
	public Void run(JobContext jc) {
		try {
			Task task = null;
			while ((task = popTask()) != null) {
				mCurrentTask = task;
				if (LOG.isDebugEnabled())
					LOG.debug("run: " + task + " ...");
				
				try {
					task.process(this, jc);
					
					if (LOG.isDebugEnabled())
						LOG.debug("run: " + task + " done.");
				} catch (Throwable e) { 
					if (LOG.isWarnEnabled())
						LOG.warn("run: " + task + " error: " + e, e);
				} finally { 
					task.close();
					
					//MessageHelper.notify(getUser(), source.getMessage());
				}
			}
		} catch (Throwable e) { 
			if (LOG.isErrorEnabled())
				LOG.error("run: job error: " + e, e);
		} finally { 
			mCurrentTask = null;
		}
		
		onFinished();
		return null;
	}
	
}
