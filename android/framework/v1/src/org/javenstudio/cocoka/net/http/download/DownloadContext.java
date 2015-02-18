package org.javenstudio.cocoka.net.http.download;

import org.javenstudio.cocoka.net.http.HttpTask;
import org.javenstudio.cocoka.net.metrics.MetricsContext;
import org.javenstudio.cocoka.storage.StorageFile;

public class DownloadContext extends MetricsContext implements HttpTask.Progress {

	public static final int STATUS_CONNECTING = 1; 
	public static final int STATUS_DOWNLOADING = 2; 
	public static final int STATUS_FINISHED = 3; 
	public static final int STATUS_ABORTED = 4; 
	public static final int STATUS_FAILED = 5; 
	
	private final int mStatus; 
	private final int mContentLength; 
	private final int mFetchedLength; 
	private final String mSource; 
	private final StorageFile mFile; 
	private final Throwable mException; 
	
	public static DownloadContext newFinished(String source, StorageFile file) { 
		return new DownloadContext(source, file); 
	}
	
	public static DownloadContext newDownloading(int contentLength, int fetchedLength, boolean aborted) { 
		return new DownloadContext(contentLength, fetchedLength, aborted); 
	}
	
	public static DownloadContext newConnecting(String source) { 
		return new DownloadContext(source, STATUS_CONNECTING, (Throwable)null); 
	}
	
	public static DownloadContext newFailed(String source, Throwable exception) { 
		return new DownloadContext(source, STATUS_FAILED, exception); 
	}
	
	private DownloadContext(String source, int status, Throwable exception) {
		mStatus = status; 
		mContentLength = 0; 
		mFetchedLength = 0; 
		mSource = source; 
		mFile = null; 
		mException = exception; 
	}
	
	private DownloadContext(String source, StorageFile file) {
		mStatus = STATUS_FINISHED; 
		mContentLength = 0; 
		mFetchedLength = 0; 
		mSource = source; 
		mFile = file; 
		mException = null; 
	}
	
	private DownloadContext(int contentLength, int fetchedLength, boolean aborted) {
		mStatus = aborted ? STATUS_ABORTED : STATUS_DOWNLOADING; 
		mContentLength = contentLength; 
		mFetchedLength = fetchedLength; 
		mSource = null; 
		mFile = null; 
		mException = null; 
	}
	
	public String getSource() { 
		return mSource; 
	}
	
	public Throwable getException() { 
		return mException; 
	}
	
	public int getStatus() { 
		return mStatus; 
	}
	
	public boolean isAborted() { 
		return mStatus == STATUS_ABORTED; 
	}
	
	public boolean isFinished() { 
		return mStatus == STATUS_FINISHED; 
	}
	
	public boolean isFailed() { 
		return mStatus == STATUS_FAILED; 
	}
	
	public StorageFile getCacheFile() { 
		return mFile; 
	}
	
	public int getContentLength() {
		return mContentLength; 
	}
	
	public int getTotalLength() {
		return mContentLength; 
	}
	
	public int getFetchedLength() {
		return mFetchedLength; 
	}
	
	public int getFinishedLength() {
		return mFetchedLength; 
	}
	
	public float getProgress() {
		float finishedLen = (float)getFinishedLength(); 
		float totalLen = (float)getTotalLength(); 
		if (totalLen > 0 && finishedLen >= 0) {
			return finishedLen/totalLen; 
		}
		return 0.0f; 
	}
	
	public String getInformation() {
		switch (mStatus) {
		case STATUS_CONNECTING: 
			return "connecting"; 
		case STATUS_ABORTED: 
			return "aborted"; 
		case STATUS_FAILED: 
			return "failed"; 
		case STATUS_FINISHED: 
			return "finished"; 
		case STATUS_DOWNLOADING: 
			return "" + getFinishedLength() + "/" + getTotalLength(); 
		}
		return ""; 
	}
	
}
