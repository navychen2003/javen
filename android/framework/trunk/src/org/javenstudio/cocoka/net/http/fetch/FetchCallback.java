package org.javenstudio.cocoka.net.http.fetch;

import java.io.OutputStream;
import java.io.Writer;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpUriRequest;

import org.javenstudio.cocoka.net.http.HttpClientTask;
import org.javenstudio.cocoka.net.http.HttpException;
import org.javenstudio.cocoka.net.http.ResponseChecker;
import org.javenstudio.cocoka.util.ContentFile;

public interface FetchCallback extends HttpClientTask.RequestIniter {

	public boolean isAutoFetch(); 
	public boolean isSaveContent();
	public boolean isRefetchContent();
	public boolean isFetchContent();
	public ContentFile getSavedContent();
	public void onContentSaved(ContentFile file);
	public long getSavedExpireTime();
	
	public String getDefaultContentCharset(); 
	public String getFetchName(); 
	public String getFetchAgent(); 
	public ResponseChecker getResponseChecker(); 
	public void initRequest(HttpUriRequest request);
	
	public Writer getWriter(HttpEntity entity); 
	public OutputStream getOutputStream(HttpEntity entity); 
	
	public void onStartFetching(String source);
	public void onContentFetched(Object content, HttpException exception); 
	public void onFetchException(Throwable exception); 
	public void onFinished(); 
	
}
