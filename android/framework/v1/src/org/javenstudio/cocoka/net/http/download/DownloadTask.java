package org.javenstudio.cocoka.net.http.download;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.methods.HttpUriRequest;
import org.javenstudio.cocoka.net.metrics.IMetricsUpdater;
import org.javenstudio.cocoka.net.metrics.MetricsContext;
import org.javenstudio.cocoka.storage.StorageFile;
import org.javenstudio.cocoka.storage.TempFile;
import org.javenstudio.cocoka.util.MimeType;
import org.javenstudio.common.util.Logger;

public final class DownloadTask {
	private static Logger LOG = Logger.getLogger(DownloadTask.class);

	private final int STATUS_SCHEDULED = 1; 
	private final int STATUS_STARTED = 2; 
	private final int STATUS_FINISHED = 3; 
	private final int STATUS_CANCELED = 4; 
	private final int STATUS_CLOSED = 5; 
	
	private final DownloadManager mManager;
	private final String mSource; 
	private final List<WeakReference<IMetricsUpdater>> mUpdaterRefs; 
	private DownloadCache mTaskCache;
	private Downloader mDownloader = null; 
	private String mDownloadSource = null; 
	private HttpUriRequest mHttpUriRequest = null; 
	private StorageFile mFile = null; 
	private MimeType mMimeType = null; 
	private DownloadCallback mCallback = null; 
	private DownloadListener mListener = null;
	private Throwable mException = null; 
	private int mStatus = 0; 
	
	DownloadTask(DownloadManager manager, String source, DownloadCache cache) {
		mManager = manager;
		mSource = source; 
		mDownloadSource = source; 
		mTaskCache = cache;
		mUpdaterRefs = new ArrayList<WeakReference<IMetricsUpdater>>(); 
	}
	
	void setDownloadCache(DownloadCache cache) { 
		mTaskCache = cache;
	}
	
	void setDownloadSource(String source) { 
		if (source != null && source.length() > 0) 
			mDownloadSource = source; 
	}
	
	void setHttpUriRequest(HttpUriRequest request) { 
		if (request != null) 
			mHttpUriRequest = request; 
	}
	
	public DownloadManager getManager() { return mManager; }
	public String getSource() { return mSource; } 
	public String getDownloadSource() { return mDownloadSource; } 
	
	public HttpUriRequest getHttpUriRequest() { return mHttpUriRequest; } 
	public MimeType getMimeType() { return mMimeType; } 
	public StorageFile getCacheFile() { return mFile; } 
	
	public DownloadCallback getCallback() { return mCallback; } 
	public DownloadListener getListener() { return mListener; }
	public DownloadCache getDownloadCache() { return mTaskCache; }
	
	public void setListener(DownloadListener listener) { 
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
	public Downloader getDownloader() { return mDownloader; } 
	
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
	
	boolean setScheduled(MimeType type, DownloadCallback callback) {
		synchronized (this) {
			if (mStatus > 0) 
				return false; 
			
			mStatus = STATUS_SCHEDULED; 
			mMimeType = type; 
			mCallback = callback; 
			
			LOG.info("Downloader: scheduled "+mSource); 
			
			return true; 
		}
	}
	
	boolean setStarted(Downloader downloader) {
		synchronized (this) {
			if (mStatus != STATUS_SCHEDULED || downloader == null) 
				return false; 
			
			mStatus = STATUS_STARTED; 
			mDownloader = downloader; 
			
			LOG.info("Downloader: started "+mSource); 
			
			return true; 
		}
	}
	
	boolean setFinished(StorageFile file, Throwable exception) {
		synchronized (this) {
			if (mStatus >= STATUS_FINISHED) 
				return false; 
			
			if (file != null && exception == null && file instanceof TempFile) { 
				try { 
					StorageFile f = ((TempFile)file).restore(); 
					if (f != null && f != file) { 
						StorageFile old = file; 
						file = f; 
						
						if (LOG.isDebugEnabled()) 
							LOG.debug("Downloader: renamed "+old.getFilePath()+" to "+file.getFilePath()); 
					}
				} catch (IOException e) { 
					exception = e; 
				}
			}
			
			mStatus = STATUS_FINISHED; 
			mFile = file; 
			mException = exception; 
			mDownloader = null; 
			
			LOG.info("Downloader: finished " + mSource + 
					(file != null ? ", saved to "+file.getFile() : ", not saved") + 
					(exception != null ? ", "+exception : "")); 
			
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
			mDownloader = null; 
			
			LOG.info("Downloader: canceled " + mSource); 
			
			return true; 
		}
	}
	
	boolean setClosed() {
		synchronized (this) {
			if (mStatus != STATUS_FINISHED && mStatus != STATUS_CANCELED) 
				return false; 
			
			mStatus = STATUS_CLOSED; 
			mDownloader = null; 
			
			LOG.info("Downloader: closed " + mSource); 
			
			return true; 
		}
	}
	
}
