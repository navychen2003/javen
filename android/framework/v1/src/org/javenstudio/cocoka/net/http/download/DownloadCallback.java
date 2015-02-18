package org.javenstudio.cocoka.net.http.download;

import java.io.OutputStream;
import java.io.Writer;

import org.apache.http.HttpEntity;

import org.javenstudio.cocoka.net.http.HttpException;
import org.javenstudio.cocoka.net.http.ResponseChecker;
import org.javenstudio.cocoka.util.ContentFile;

public interface DownloadCallback {

	public boolean isAutoFetch(); 
	public boolean isSaveContent();
	public boolean isRefetchContent();
	public ContentFile getSavedContent();
	public void onContentSaved(ContentFile file);
	
	public String getDefaultContentCharset(); 
	public String getDownloadName(); 
	public String getDownloadAgent(); 
	public ResponseChecker getResponseChecker(); 
	
	public Writer getWriter(HttpEntity entity); 
	public OutputStream getOutputStream(HttpEntity entity); 
	
	public void onStartFetching(String source);
	public void onContentDownloaded(Object content); 
	public void onHttpException(HttpException exception); 
	public void onFinished(); 
	
}
