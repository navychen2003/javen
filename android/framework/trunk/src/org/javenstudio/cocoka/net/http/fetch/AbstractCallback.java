package org.javenstudio.cocoka.net.http.fetch;

import java.io.OutputStream;
import java.io.Writer;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpUriRequest;
import org.javenstudio.cocoka.net.http.ResponseChecker;
import org.javenstudio.cocoka.util.ContentFile;

public abstract class AbstractCallback implements FetchCallback {

	protected FetchCache mCallbackCache = null;
	protected ContentFile mContentFile = null;
	protected String mDefaultContentCharset = "UTF-8";
	protected boolean mRefetchContent = true;
	protected boolean mFetchContent = true;
	protected boolean mSaveContent = false;
	protected boolean mAutoFetch = true;
	protected boolean mContentFetched = false;
	protected long mExpireTime = 0;
	
	public final void setFetchCache(FetchCache cache) { 
		mCallbackCache = cache;
	}
	
	public final FetchCache getFetchCache() { 
		return mCallbackCache;
	}
	
	public final void setDefaultContentCharset(String charset) { 
		if (charset != null && charset.length() > 0)
			mDefaultContentCharset = charset;
	}
	
	public final void setAutoFetch(boolean autoFetch) { 
		mAutoFetch = autoFetch;
	}
	
	public final void setSaveContent(boolean saveContent) { 
		mSaveContent = saveContent;
	}
	
	public final void setRefetchContent(boolean reloadContent) { 
		mRefetchContent = reloadContent;
	}
	
	public final void setFetchContent(boolean fetch) { 
		mFetchContent = fetch;
	}
	
	public final boolean isContentFetched() { 
		return mContentFetched;
	}
	
	@Override
	public void onStartFetching(String source) { 
		mContentFetched = true;
	}
	
	public final void setSavedExpireTime(long expireMillis) { 
		mExpireTime = expireMillis;
	}
	
	@Override
	public long getSavedExpireTime() { 
		return mExpireTime;
	}
	
	@Override
	public ContentFile getSavedContent() { 
		return mContentFile;
	}
	
	@Override
	public void onContentSaved(ContentFile file) { 
		mContentFile = file;
	}
	
	@Override 
	public boolean isRefetchContent() { 
		return mRefetchContent;
	}
	
	@Override 
	public boolean isFetchContent() { 
		return mFetchContent;
	}
	
	@Override 
	public boolean isSaveContent() { 
		return mSaveContent;
	}
	
	@Override 
	public boolean isAutoFetch() { 
		return mAutoFetch; 
	}
	
	@Override 
	public String getDefaultContentCharset() { 
		return mDefaultContentCharset; 
	}
	
	@Override 
	public String getFetchName() { 
		return null; // use default
	}
	
	@Override 
	public String getFetchAgent() { 
		return null; // use default
	}
	
	@Override 
	public ResponseChecker getResponseChecker() { 
		return null; 
	}
	
	@Override 
	public Writer getWriter(HttpEntity entity) { 
		return null; 
	}
	
	@Override 
	public OutputStream getOutputStream(HttpEntity entity) {
		return null; 
	}
	
	@Override 
	public void initRequest(HttpUriRequest request) { 
	}
	
}
