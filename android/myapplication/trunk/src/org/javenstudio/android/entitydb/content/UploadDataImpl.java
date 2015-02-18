package org.javenstudio.android.entitydb.content;

import org.javenstudio.android.entitydb.TUpload;
import org.javenstudio.android.entitydb.TUploadUpdater;

final class UploadDataImpl extends AbstractContentImpl 
		implements UploadData {

	private final TUpload mEntity; 
	
	UploadDataImpl(TUpload data) { 
		this(data, false); 
	}
	
	UploadDataImpl(TUpload data, boolean updateable) { 
		super(updateable); 
		mEntity = data; 
	}
	
	TUpload getEntity() { return mEntity; }
	
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
	public String getDataPath() { 
		return mEntity.dataPath;
	}
	
	@Override
	public String getFilePath() { 
		return mEntity.filePath;
	}
	
	@Override
	public String getDestPrefix() { 
		return mEntity.destPrefix;
	}
	
	@Override
	public String getDestAccount() { 
		return mEntity.destAccount;
	}
	
	@Override
	public String getDestAccountId() { 
		return mEntity.destAccountId;
	}
	
	@Override
	public String getDestPath() { 
		return mEntity.destPath;
	}
	
	@Override
	public String getDestPathId() { 
		return mEntity.destPathId;
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
	public UploadData startUpdate() { 
		TUpload data = new TUpload(mEntity.getId()); 
		
		return new UploadDataImpl(data, true); 
	}
	
	@Override 
    public synchronized long commitUpdates() { 
		checkUpdateable(); 
		
		synchronized (sContentLock) { 
			TUploadUpdater provider = new TUploadUpdater(mEntity); 
			provider.commitUpdate();
			return provider.getUploadKey();
		}
    }
	
	@Override 
    public synchronized void commitDelete() { 
		checkUpdateable(); 
		
		synchronized (sContentLock) { 
			TUploadUpdater provider = new TUploadUpdater(mEntity); 
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
	public void setDataPath(String text) { 
		checkUpdateable(); 
		mEntity.dataPath = text;
	}
	
	@Override
	public void setFilePath(String text) { 
		checkUpdateable(); 
		mEntity.filePath = text;
	}
	
	@Override
	public void setDestPrefix(String text) { 
		checkUpdateable(); 
		mEntity.destPrefix = text;
	}
	
	@Override
	public void setDestAccount(String text) { 
		checkUpdateable(); 
		mEntity.destAccount = text;
	}
	
	@Override
	public void setDestAccountId(String text) { 
		checkUpdateable(); 
		mEntity.destAccountId = text;
	}
	
	@Override
	public void setDestPath(String text) { 
		checkUpdateable(); 
		mEntity.destPath = text;
	}
	
	@Override
	public void setDestPathId(String text) { 
		checkUpdateable(); 
		mEntity.destPathId = text;
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
				+ ",destPrefix=" + getDestPrefix() + ",destAccountId=" + getDestAccountId()
				+ ",name=" + getContentName() + ",url=" + getContentUri() 
				+ "}";
	}
	
}
