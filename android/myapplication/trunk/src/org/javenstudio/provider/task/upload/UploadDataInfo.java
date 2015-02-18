package org.javenstudio.provider.task.upload;

import org.apache.http.client.methods.HttpPost;

import org.javenstudio.android.entitydb.content.ContentHelper;
import org.javenstudio.android.entitydb.content.UploadData;
import org.javenstudio.common.util.Logger;

public class UploadDataInfo {
	private static final Logger LOG = Logger.getLogger(UploadDataInfo.class);

	private UploadData mData = null;
	private HttpPost mPost = null;
	private boolean mPostAborted = false;
	private long mQueueTime = 0;
	private long mLastUpdate = 0;
	private long mTotalBytes = 0;
	private long mCurrentBytes = 0;
	
	public UploadDataInfo(UploadData data) { 
		setUploadData(data); 
		if (mData == null) throw new NullPointerException();
	}
	
	public synchronized UploadData getUploadData() { return mData; }
	
	synchronized void setUploadData(UploadData data) { 
		if (data == null || data == mData) return;
		if (mData != null && data.getId() != mData.getId()) return;
		mData = data;
	}
	
	void requeryUploadData() { 
		UploadData data = ContentHelper.getInstance().getUpload(getUploadId());
		setUploadData(data);
	}
	
	public synchronized HttpPost getHttpPost() { return mPost; }
	public synchronized boolean isPostAborted() { return mPostAborted; }
	
	public synchronized void setHttpPost(HttpPost post) { 
		mPost = post; mPostAborted = false; 
	}
	
	synchronized void setQueueTime(long time) { mQueueTime = time; }
	public synchronized long getQueueTime() { return mQueueTime; }
	
	public long getUploadId() { return getUploadData().getId(); }
	public int getUploadStatus() { return getUploadData().getStatus(); }
	
	public long getStartTime() { return getUploadData().getStartTime(); }
	public long getUpdateTime() { return getUploadData().getUpdateTime(); }
	public long getFinishTime() { return getUploadData().getFinishTime(); }
	
	public String getContentUri() { return getUploadData().getContentUri(); }
	public String getPrefix() { return getUploadData().getDestPrefix(); }
	
	public String getTitle() { return null; }
	public String getAccountName() { return getUploadData().getDestAccount(); }
	public String getContentName() { return getUploadData().getContentName(); }
	
	public long getTotalBytes() { return mTotalBytes; }
	void setTotalBytes(long bytes) { mTotalBytes = bytes; }
	
	public long getCurrentBytes() { return mCurrentBytes; }
	void setCurrentBytes(long bytes) { mCurrentBytes = bytes; setLastUpdate(); }
	
	public long getLastUpdate() { return mLastUpdate; }
	void setLastUpdate() { mLastUpdate = System.currentTimeMillis(); }
	
	public synchronized long nextAction(long now) { return 0; }
	
	public boolean isUploadPending() { 
		return getUploadStatus() == UploadData.STATUS_PENDING;
	}
	
	public boolean isUploadRunning() { 
		return getUploadStatus() == UploadData.STATUS_RUNNING;
	}
	
	public boolean isUploadFinished() { 
		return getUploadStatus() == UploadData.STATUS_FINISHED;
	}
	
	public boolean isUploadFailed() { 
		return getUploadStatus() == UploadData.STATUS_FAILED;
	}
	
	public boolean isUploadAborted() { 
		return getUploadStatus() == UploadData.STATUS_ABORTED;
	}
	
	public boolean isUploadDone() { 
		return isUploadFinished() || isUploadFailed() || isUploadAborted();
	}
	
	public boolean isConnectTimeout() { 
		String message = getUploadData().getFailedMessage();
		return message != null && message.indexOf("ConnectTimeoutException") >= 0;
	}
	
	public synchronized boolean isReadyToStart(long now) { 
		switch (getUploadData().getStatus()) { 
		case UploadData.STATUS_ADDED: 
			return true;
		case UploadData.STATUS_FAILED: 
			return isConnectTimeout() && System.currentTimeMillis() - getUpdateTime() > 30 * 60 * 1000;
		case UploadData.STATUS_ABORTED: 
			return false;
		case UploadData.STATUS_FINISHED: 
			return false;
		case UploadData.STATUS_PENDING: 
			return true;
		case UploadData.STATUS_RUNNING: 
			return true;
		}
		return false; 
	}
	
	void postAbortUpload() { 
		new Thread() { 
			@Override
			public void run() {
				abortUpload();
			}
		}.start();
	}
	
	synchronized void abortUpload() { 
		HttpPost post = getHttpPost();
		if (post != null) { 
			
			if (LOG.isDebugEnabled())
				LOG.debug("abortUpload: uploadId=" + getUploadId());
			
			mPostAborted = true;
			post.abort(); 
		}
	}
	
	public synchronized void onPending() { 
		setLastUpdate();
		
		if (isUploadPending() || isUploadRunning()) 
			return;
		
		if (LOG.isDebugEnabled())
			LOG.debug("onPending: uploadId=" + getUploadId());
		
		try {
			UploadData data = getUploadData().startUpdate();
			data.setStatus(UploadData.STATUS_PENDING);
			data.commitUpdates();
			
			requeryUploadData();
			
		} catch (Throwable e) { 
			if (LOG.isErrorEnabled()) {
				LOG.error("onPending: uploadId=" + getUploadId() 
						+ " error: " + e.toString(), e);
			}
		}
	}
	
	public synchronized void onStarting(HttpPost post) { 
		setLastUpdate();
		setHttpPost(post);
		
		if (isUploadRunning()) return;
		
		if (LOG.isDebugEnabled())
			LOG.debug("onStarting: uploadId=" + getUploadId());
		
		try {
			UploadData data = getUploadData().startUpdate();
			data.setStatus(UploadData.STATUS_RUNNING);
			data.commitUpdates();
			
			requeryUploadData();
			
		} catch (Throwable e) { 
			if (LOG.isErrorEnabled()) {
				LOG.error("onStarting: uploadId=" + getUploadId() 
						+ " error: " + e.toString(), e);
			}
		}
	}
	
	public synchronized void onFinished(int code, String message) {
		setLastUpdate();
		setHttpPost(null);
		
		if (isUploadDone()) return;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("onFinished: uploadId=" + getUploadId() 
					+ " code=" + code + " message=" + message);
		}
		
		try { 
			long current = System.currentTimeMillis();
			
			UploadData data = getUploadData().startUpdate();
			data.setStatus(UploadData.STATUS_FINISHED);
			data.setFailedCode(code);
			data.setFailedMessage(message);
			data.setUpdateTime(current);
			data.setFinishTime(current);
			data.commitUpdates();
			
			requeryUploadData();
			
			ContentHelper.getInstance().updateFetchDirtyWithAccount(getAccountName());
		} catch (Throwable e) { 
			if (LOG.isErrorEnabled()) {
				LOG.error("onFinished: uploadId=" + getUploadId() 
						+ " error: " + e.toString(), e);
			}
		}
	}
	
	public synchronized void onFailed(int code, String message) { 
		setLastUpdate();
		setHttpPost(null);
		
		if (isUploadDone()) return;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("onFailed: uploadId=" + getUploadId() 
					+ " code=" + code + " message=" + message);
		}
		
		try {
			long current = System.currentTimeMillis();
			
			UploadData data = getUploadData().startUpdate();
			data.setStatus(UploadData.STATUS_FAILED);
			data.setFailedCount(getUploadData().getFailedCount() + 1);
			data.setFailedCode(code);
			data.setFailedMessage(message);
			data.setUpdateTime(current);
			//data.setFinishTime(current);
			data.commitUpdates();
			
			requeryUploadData();
			
		} catch (Throwable e) { 
			if (LOG.isErrorEnabled()) {
				LOG.error("onFailed: uploadId=" + getUploadId() 
						+ " error: " + e.toString(), e);
			}
		}
	}
	
	public synchronized void onAborted(int code, String message) { 
		setLastUpdate();
		setHttpPost(null);
		
		if (isUploadDone()) return;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("onAborted: uploadId=" + getUploadId() 
					+ " code=" + code + " message=" + message);
		}
		
		try {
			long current = System.currentTimeMillis();
			
			UploadData data = getUploadData().startUpdate();
			data.setStatus(UploadData.STATUS_ABORTED);
			//data.setFailedCount(getUploadData().getFailedCount() + 1);
			data.setFailedCode(code);
			data.setFailedMessage(message);
			data.setUpdateTime(current);
			data.setFinishTime(current);
			data.commitUpdates();
			
			requeryUploadData();
			
		} catch (Throwable e) { 
			if (LOG.isErrorEnabled()) {
				LOG.error("onAborted: uploadId=" + getUploadId() 
						+ " error: " + e.toString(), e);
			}
		}
	}
	
	public synchronized void onRemove() { 
		if (isUploadRunning()) return;
		
		setLastUpdate();
		setHttpPost(null);
		
		if (LOG.isDebugEnabled())
			LOG.debug("onRemove: uploadId=" + getUploadId());
		
		try { 
			UploadData data = getUploadData().startUpdate();
			data.commitDelete();
			
		} catch (Throwable e) { 
			if (LOG.isErrorEnabled()) {
				LOG.error("onRemove: uploadId=" + getUploadId() 
						+ " error: " + e.toString(), e);
			}
		}
	}
	
}
