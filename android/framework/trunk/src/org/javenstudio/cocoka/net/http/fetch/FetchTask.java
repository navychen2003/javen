package org.javenstudio.cocoka.net.http.fetch;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.methods.HttpUriRequest;
import org.javenstudio.cocoka.net.http.HttpHelper;
import org.javenstudio.cocoka.net.metrics.IMetricsUpdater;
import org.javenstudio.cocoka.net.metrics.MetricsContext;
import org.javenstudio.cocoka.storage.StorageFile;
import org.javenstudio.cocoka.storage.TempStorageFile;
import org.javenstudio.cocoka.util.MimeType;
import org.javenstudio.common.util.Logger;

public final class FetchTask {
	private static Logger LOG = Logger.getLogger(FetchTask.class);

	private final int STATUS_SCHEDULED = 1; 
	private final int STATUS_STARTED = 2; 
	private final int STATUS_FINISHED = 3; 
	private final int STATUS_CANCELED = 4; 
	private final int STATUS_CLOSED = 5; 
	
	private final FetchManager mManager;
	private final String mSource; 
	private final List<WeakReference<IMetricsUpdater>> mUpdaterRefs; 
	private FetchCache mTaskCache;
	private Fetcher mFetcher = null; 
	private String mFetchSource = null; 
	private HttpUriRequest mHttpUriRequest = null; 
	private StorageFile mFile = null; 
	private MimeType mMimeType = null; 
	private FetchCallback mCallback = null; 
	private FetchListener mListener = null;
	private Throwable mException = null; 
	private int mStatus = 0; 
	
	FetchTask(FetchManager manager, String source, FetchCache cache) {
		mManager = manager;
		mSource = source; 
		mFetchSource = source; 
		mTaskCache = cache;
		mUpdaterRefs = new ArrayList<WeakReference<IMetricsUpdater>>(); 
	}
	
	void setFetchCache(FetchCache cache) { 
		mTaskCache = cache;
	}
	
	void setFetchSource(String source) { 
		if (source != null && source.length() > 0) 
			mFetchSource = source; 
	}
	
	void setHttpUriRequest(HttpUriRequest request) { 
		if (request != null) 
			mHttpUriRequest = request; 
	}
	
	public FetchManager getManager() { return mManager; }
	public String getSource() { return mSource; } 
	public String getFetchSource() { return mFetchSource; } 
	
	public HttpUriRequest getHttpUriRequest() { return mHttpUriRequest; } 
	public MimeType getMimeType() { return mMimeType; } 
	public StorageFile getCacheFile() { return mFile; } 
	
	public FetchCallback getCallback() { return mCallback; } 
	public FetchListener getListener() { return mListener; }
	public FetchCache getFetchCache() { return mTaskCache; }
	
	public void setListener(FetchListener listener) { 
		mListener = listener;
	}
	
	public void addUpdater(IMetricsUpdater updater) {
		if (updater == null) return; 
		
		synchronized (this) {
			for (int i=0; i < mUpdaterRefs.size(); i++) {
				WeakReference<IMetricsUpdater> updaterRef = mUpdaterRefs.get(i); 
				if (updaterRef != null) { 
					IMetricsUpdater up = updaterRef.get(); 
					if (up != null) { 
						if (up == updater) return; 
						continue; 
					}
				}
				mUpdaterRefs.remove(i); 
				if ((--i) < 0) i = 0; 
			}
			mUpdaterRefs.add(new WeakReference<IMetricsUpdater>(updater)); 
		}
	}
	
	public void updateMetrics(MetricsContext context) {
		synchronized (this) {
			for (int i=0; i < mUpdaterRefs.size(); i++) {
				WeakReference<IMetricsUpdater> updaterRef = mUpdaterRefs.get(i); 
				if (updaterRef != null) { 
					IMetricsUpdater up = updaterRef.get(); 
					if (up != null) { 
						up.updateMetrics(context); 
						continue; 
					} 
				}
				mUpdaterRefs.remove(i); 
				if ((--i) < 0) i = 0; 
			}
		}
	}
	
	public int getStatus() { return mStatus; } 
	public Throwable getException() { return mException; } 
	public Fetcher getFetcher() { return mFetcher; } 
	
	public boolean isStatus(int status) {
		synchronized (this) {
			return mStatus == status; 
		}
	}
	
	public boolean isScheduled() { return isStatus(STATUS_SCHEDULED); } 
	public boolean isStarted() { return isStatus(STATUS_STARTED); } 
	public boolean isFinished() { return isStatus(STATUS_FINISHED); } 
	public boolean isCanceled() { return isStatus(STATUS_CANCELED); } 
	public boolean isClosed() { return isStatus(STATUS_CLOSED); } 
	
	boolean setScheduled(MimeType type, FetchCallback callback) {
		synchronized (this) {
			if (mStatus > 0) 
				return false; 
			
			mStatus = STATUS_SCHEDULED; 
			mMimeType = type; 
			mCallback = callback; 
			
			if (LOG.isDebugEnabled())
				LOG.debug("scheduled " + mSource); 
			
			return true; 
		}
	}
	
	boolean setStarted(Fetcher fetcher) {
		synchronized (this) {
			if (mStatus != STATUS_SCHEDULED || fetcher == null) 
				return false; 
			
			mStatus = STATUS_STARTED; 
			mFetcher = fetcher; 
			
			if (LOG.isDebugEnabled())
				LOG.debug("started " + mSource); 
			
			return true; 
		}
	}
	
	boolean setFinished(StorageFile file, Throwable exception) {
		synchronized (this) {
			if (mStatus >= STATUS_FINISHED) 
				return false; 
			
			if (file != null && exception == null && file instanceof TempStorageFile) { 
				try { 
					StorageFile f = ((TempStorageFile)file).restore(); 
					if (f != null && f != file) { 
						StorageFile old = file; 
						file = f; 
						
						if (LOG.isDebugEnabled()) 
							LOG.debug("renamed " + old.getFilePath() + " to " + file.getFilePath()); 
					}
				} catch (IOException e) { 
					exception = e; 
				}
			}
			
			mStatus = STATUS_FINISHED; 
			mFile = file; 
			mException = exception; 
			mFetcher = null; 
			
			if (exception != null) 
				HttpHelper.addFailed(mSource, exception);
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("finished " + mSource + 
						(file != null ? ", saved to " + file.getFile() : ", not saved") + 
						(exception != null ? ", " + exception : "")); 
			}
			
			return true; 
		}
	}
	
	boolean setCanceled() { 
		synchronized (this) {
			if (mStatus >= STATUS_FINISHED) 
				return false; 
			
			mStatus = STATUS_FINISHED; 
			mFile = null; 
			mException = null; 
			mFetcher = null; 
			
			if (LOG.isDebugEnabled())
				LOG.debug("canceled " + mSource); 
			
			return true; 
		}
	}
	
	boolean setClosed() {
		synchronized (this) {
			if (mStatus != STATUS_FINISHED && mStatus != STATUS_CANCELED) 
				return false; 
			
			mStatus = STATUS_CLOSED; 
			mFetcher = null; 
			
			if (LOG.isDebugEnabled())
				LOG.debug("closed " + mSource); 
			
			return true; 
		}
	}
	
}
