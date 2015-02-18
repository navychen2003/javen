package org.javenstudio.android.entitydb.content;

import org.javenstudio.android.entitydb.TDownload;
import org.javenstudio.android.entitydb.TDownloadUpdater;

final class DownloadDataImpl extends AbstractContentImpl 
		implements DownloadData {

	private final TDownload mEntity; 
	
	DownloadDataImpl(TDownload data) { 
		this(data, false); 
	}
	
	DownloadDataImpl(TDownload data, boolean updateable) { 
		super(updateable); 
		mEntity = data; 
	}
	
	TDownload getEntity() { return mEntity; }
	
	@Override
	public long getId() { 
		return mEntity.getId(); 
	}
	
	@Override
	public String getContentName() { 
		return mEntity.contentName; 
	}
	
	@Override
	public String getContentUri() { 
		return mEntity.contentUri;
	}
	
	@Override
	public String getContentType() { 
		return mEntity.contentType;
	}
	
	@Override
	public String getSaveApp() { 
		return mEntity.saveApp;
	}
	
	@Override
	public String getSavePath() { 
		return mEntity.savePath;
	}
	
	@Override
	public String getSourcePrefix() { 
		return mEntity.sourcePrefix;
	}
	
	@Override
	public String getSourceAccount() { 
		return mEntity.sourceAccount;
	}
	
	@Override
	public String getSourceAccountId() { 
		return mEntity.sourceAccountId;
	}
	
	@Override
	public String getSourceFile() { 
		return mEntity.sourceFile;
	}
	
	@Override
	public String getSourceFileId() { 
		return mEntity.sourceFileId;
	}
	
	@Override
	public int getStatus() { 
		return toInt(mEntity.status);
	}
	
	@Override
	public int getFailedCode() { 
		return toInt(mEntity.failedCode);
	}
	
	@Override
	public int getFailedCount() { 
		return toInt(mEntity.failedCount);
	}
	
	@Override
	public String getFailedMessage() { 
		return mEntity.failedMessage;
	}
	
	@Override
	public int getRetryAfter() { 
		return toInt(mEntity.retryAfter);
	}
	
	@Override
	public long getStartTime() { 
		return toLong(mEntity.startTime);
	}
	
	@Override
	public long getUpdateTime() { 
		return toLong(mEntity.updateTime);
	}
	
	@Override
	public long getFinishTime() { 
		return toLong(mEntity.finishTime);
	}
	
	@Override 
	public DownloadData startUpdate() { 
		TDownload data = new TDownload(mEntity.getId()); 
		
		return new DownloadDataImpl(data, true); 
	}
	
	@Override 
    public synchronized long commitUpdates() { 
		checkUpdateable(); 
		
		synchronized (sContentLock) { 
			TDownloadUpdater provider = new TDownloadUpdater(mEntity); 
			provider.commitUpdate();
			return provider.getDownloadKey();
		}
    }
	
	@Override 
    public synchronized void commitDelete() { 
		checkUpdateable(); 
		
		synchronized (sContentLock) { 
			TDownloadUpdater provider = new TDownloadUpdater(mEntity); 
			provider.commitDelete();
		}
    }
	
	@Override 
	public void setContentName(String text) { 
		checkUpdateable(); 
		mEntity.contentName = text; 
	}
	
	@Override 
	public void setContentUri(String uri) { 
		checkUpdateable(); 
		mEntity.contentUri = uri;
	}
	
	@Override 
	public void setContentType(String type) { 
		checkUpdateable(); 
		mEntity.contentType = type;
	}
	
	@Override 
	public void setSaveApp(String text) { 
		checkUpdateable(); 
		mEntity.saveApp = text;
	}
	
	@Override
	public void setSavePath(String text) { 
		checkUpdateable(); 
		mEntity.savePath = text;
	}
	
	@Override
	public void setSourcePrefix(String text) { 
		checkUpdateable(); 
		mEntity.sourcePrefix = text;
	}
	
	@Override
	public void setSourceAccount(String text) { 
		checkUpdateable(); 
		mEntity.sourceAccount = text;
	}
	
	@Override
	public void setSourceAccountId(String text) { 
		checkUpdateable(); 
		mEntity.sourceAccountId = text;
	}
	
	@Override
	public void setSourceFile(String text) { 
		checkUpdateable(); 
		mEntity.sourceFile = text;
	}
	
	@Override
	public void setSourceFileId(String text) { 
		checkUpdateable(); 
		mEntity.sourceFileId = text;
	}
	
	@Override 
	public void setStatus(int status) { 
		checkUpdateable(); 
		mEntity.status = status;
	}
	
	@Override 
	public void setFailedCode(int code) { 
		checkUpdateable(); 
		mEntity.failedCode = code;
	}
	
	@Override 
	public void setFailedCount(int count) { 
		checkUpdateable(); 
		mEntity.failedCount = count;
	}
	
	@Override
	public void setFailedMessage(String text) { 
		checkUpdateable(); 
		mEntity.failedMessage = text;
	}
	
	@Override 
	public void setRetryAfter(int after) { 
		checkUpdateable(); 
		mEntity.retryAfter = after;
	}
	
	@Override
	public void setStartTime(long time) { 
		checkUpdateable(); 
		mEntity.startTime = time;
	}
	
	@Override
	public void setUpdateTime(long time) { 
		checkUpdateable(); 
		mEntity.updateTime = time;
	}
	
	@Override
	public void setFinishTime(long time) { 
		checkUpdateable(); 
		mEntity.finishTime = time;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "{id=" + getId() 
				+ ",sourcePrefix=" + getSourcePrefix() + ",sourceAccountId=" + getSourceAccountId()
				+ ",name=" + getContentName() + ",url=" + getContentUri() 
				+ "}";
	}
	
}
