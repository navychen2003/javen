package org.javenstudio.falcon.message;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.util.job.Job;
import org.javenstudio.falcon.util.job.JobCancelListener;
import org.javenstudio.falcon.util.job.JobContext;
import org.javenstudio.falcon.util.job.JobSubmit;

public class MessageJob implements Job<Void>, JobCancelListener {
	private static final Logger LOG = Logger.getLogger(MessageJob.class);
	
	private final LinkedList<MessageSource> mSources = 
			new LinkedList<MessageSource>();
	
	private final Object mLock = new Object();
	private JobSubmit.JobWork<Void> mRunningWork = null;
	
	private final Map<String, String> mStatusMessages = 
			new HashMap<String, String>();
	
	private final MessageManager mManager;
	private MessageSource mCurrentSource = null;
	private boolean mCanceled = false;
	
	public MessageJob(MessageManager manager) { 
		if (manager == null) throw new NullPointerException();
		mManager = manager;
	}
	
	public MessageManager getManager() { 
		return mManager;
	}
	
	@Override
	public String getName() {
		return "MessageJob";
	}
	
	@Override
	public String getUser() { 
		return getManager().getUser().getUserKey();
	}
	
	public void startJob(MessageSource... sources) { 
		if (sources == null || sources.length == 0) 
			return;
		
		synchronized (mLock) { 
			for (MessageSource source : sources) {
				if (source != null) mSources.add(source);
			}
			
			startRun();
		}
	}
	
	private void startRun() { 
		synchronized (mLock) { 
			if (mRunningWork == null || mRunningWork.isDone()) { 
				MessageJob job = this; //new IndexJob();
				JobSubmit.JobWork<Void> work = JobSubmit.submit(job);
				if (work != null) 
					work.setCancelListener(job);
				
				mRunningWork = work;
			}
		}
	}
	
	private MessageSource popSource() { 
		synchronized (mLock) { 
			if (mSources.size() > 0)
				return mSources.removeFirst();
			
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
		MessageSource source = mCurrentSource;
		if (source != null) return source.getMessage();
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
			MessageSource source = null;
			while ((source = popSource()) != null) {
				mCurrentSource = source;
				if (LOG.isDebugEnabled())
					LOG.debug("run: " + source + " ...");
				
				try {
					source.process(this, jc);
					
					if (LOG.isDebugEnabled())
						LOG.debug("run: " + source + " done.");
				} catch (Throwable e) { 
					if (LOG.isWarnEnabled())
						LOG.warn("run: " + source + " error: " + e, e);
				} finally { 
					source.close();
					
					MessageHelper.notifySys(getUser(), source.getMessage(), 
							source.getMessageDetails());
				}
			}
		} catch (Throwable e) { 
			if (LOG.isErrorEnabled())
				LOG.error("run: job error: " + e, e);
		} finally { 
			mCurrentSource = null;
		}
		
		onFinished();
		return null;
	}
	
}
