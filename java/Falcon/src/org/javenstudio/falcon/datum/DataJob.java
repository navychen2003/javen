package org.javenstudio.falcon.datum;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.message.MessageHelper;
import org.javenstudio.falcon.user.IMember;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.util.job.Job;
import org.javenstudio.falcon.util.job.JobCancelListener;
import org.javenstudio.falcon.util.job.JobContext;
import org.javenstudio.falcon.util.job.JobSubmit;

public class DataJob implements Job<Void>, JobCancelListener {
	private static final Logger LOG = Logger.getLogger(DataJob.class);
	
	private final LinkedList<DataSource> mSources = 
			new LinkedList<DataSource>();
	
	private final Object mLock = new Object();
	private JobSubmit.JobWork<Void> mRunningWork = null;
	
	private final Map<String, String> mStatusMessages = 
			new HashMap<String, String>();
	
	private final DataManager mManager;
	private DataSource mIndexSource = null;
	private DataSource mCurrentSource = null;
	private boolean mCanceled = false;
	
	public DataJob(DataManager manager) { 
		if (manager == null) throw new NullPointerException();
		mManager = manager;
	}
	
	public DataManager getManager() { 
		return mManager;
	}
	
	@Override
	public String getName() {
		return "DataJob";
	}
	
	@Override
	public String getUser() { 
		return getManager().getUserKey();
	}
	
	public void startJob(DataSource... sources) { 
		if (sources == null || sources.length == 0) 
			return;
		
		synchronized (mLock) { 
			for (DataSource source : sources) {
				if (source != null) mSources.add(source);
			}
			
			startRun();
		}
	}
	
	public void startIndex(DataSource source) { 
		if (source == null) return;
		
		synchronized (mLock) { 
			mIndexSource = source;
			
			startRun();
		}
	}
	
	private void startRun() { 
		synchronized (mLock) { 
			if (mRunningWork == null || mRunningWork.isDone()) { 
				DataJob job = this; //new IndexJob();
				JobSubmit.JobWork<Void> work = JobSubmit.submit(job);
				if (work != null) 
					work.setCancelListener(job);
				
				mRunningWork = work;
			}
		}
	}
	
	private DataSource popSource() { 
		synchronized (mLock) { 
			if (mSources.size() > 0)
				return mSources.removeFirst();
			
			DataSource source = mIndexSource;
			mIndexSource = null;
			
			return source;
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
		DataSource source = mCurrentSource;
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
			DataSource source = null;
			while ((source = popSource()) != null) {
				mCurrentSource = source;
				if (LOG.isDebugEnabled())
					LOG.debug("run: " + source + " ...");
				
				final Set<String> contentIds = new HashSet<String>();
				
				try {
					source.process(this, jc, new DataSource.Collector() {
							@Override
							public void addContentId(String contentId) {
								if (contentId != null && contentId.length() > 0)
									contentIds.add(contentId);
							}
						});
					
					if (LOG.isDebugEnabled())
						LOG.debug("run: " + source + " done.");
				} catch (Throwable e) { 
					if (LOG.isWarnEnabled())
						LOG.warn("run: " + source + " error: " + e, e);
				} finally { 
					source.close();
					
					MessageHelper.notifySys(getUser(), source.getMessage());
					
					IUser user = source.getUser();
					if (user != null && user instanceof IMember) {
						IMember member = (IMember)user;
						UserHelper.addHistory(member, 
								contentIds.toArray(new String[contentIds.size()]),
								null, "import");
					}
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
