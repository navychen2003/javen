package org.javenstudio.android.entitydb.content;

import org.javenstudio.android.entitydb.TFetch;
import org.javenstudio.android.entitydb.TFetchUpdater;

final class FetchDataImpl extends AbstractContentImpl 
		implements FetchData {

	private final TFetch mEntity; 
	
	FetchDataImpl(TFetch data) { 
		this(data, false); 
	}
	
	FetchDataImpl(TFetch data, boolean updateable) { 
		super(updateable); 
		mEntity = data; 
	}
	
	TFetch getEntity() { return mEntity; }
	
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
	public String getPrefix() { 
		return mEntity.prefix;
	}
	
	@Override
	public String getAccount() { 
		return mEntity.account;
	}
	
	@Override
	public String getEntryId() { 
		return mEntity.entryId;
	}
	
	@Override
	public int getEntryType() { 
		return toInt(mEntity.entryType);
	}
	
	@Override
	public int getTotalResults() { 
		return toInt(mEntity.totalResults);
	}
	
	@Override
	public int getStartIndex() { 
		return toInt(mEntity.startIndex);
	}
	
	@Override
	public int getItemsPerPage() { 
		return toInt(mEntity.itemsPerPage);
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
	public String getFailedMessage() { 
		return mEntity.failedMessage;
	}
	
	@Override
	public long getCreateTime() { 
		return toLong(mEntity.createTime);
	}
	
	@Override
	public long getUpdateTime() { 
		return toLong(mEntity.updateTime);
	}
	
	@Override 
	public FetchData startUpdate() { 
		TFetch data = new TFetch(mEntity.getId()); 
		
		return new FetchDataImpl(data, true); 
	}
	
	@Override 
    public synchronized long commitUpdates() { 
		checkUpdateable(); 
		
		synchronized (sContentLock) { 
			TFetchUpdater provider = new TFetchUpdater(mEntity); 
			provider.commitUpdate();
			return provider.getFetchKey();
		}
    }
	
	@Override 
    public synchronized void commitDelete() { 
		checkUpdateable(); 
		
		synchronized (sContentLock) { 
			TFetchUpdater provider = new TFetchUpdater(mEntity); 
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
	public void setPrefix(String text) { 
		checkUpdateable(); 
		mEntity.prefix = text;
	}
	
	@Override
	public void setAccount(String text) { 
		checkUpdateable(); 
		mEntity.account = text;
	}
	
	@Override
	public void setEntryId(String text) { 
		checkUpdateable(); 
		mEntity.entryId = text;
	}
	
	@Override 
	public void setEntryType(int type) { 
		checkUpdateable(); 
		mEntity.entryType = type;
	}
	
	@Override 
	public void setTotalResults(int results) { 
		checkUpdateable(); 
		mEntity.totalResults = results;
	}
	
	@Override 
	public void setStartIndex(int index) { 
		checkUpdateable(); 
		mEntity.startIndex = index;
	}
	
	@Override 
	public void setItemsPerPage(int items) { 
		checkUpdateable(); 
		mEntity.itemsPerPage = items;
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
	public void setFailedMessage(String text) { 
		checkUpdateable(); 
		mEntity.failedMessage = text;
	}
	
	@Override
	public void setCreateTime(long time) { 
		checkUpdateable(); 
		mEntity.createTime = time;
	}
	
	@Override
	public void setUpdateTime(long time) { 
		checkUpdateable(); 
		mEntity.updateTime = time;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "{id=" + getId() 
				+ ",name=" + getContentName() + ",url=" + getContentUri() 
				+ "}";
	}
	
}
