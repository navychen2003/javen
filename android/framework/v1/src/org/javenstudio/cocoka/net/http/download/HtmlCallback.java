package org.javenstudio.cocoka.net.http.download;

import java.io.Writer;

import org.javenstudio.cocoka.net.http.HttpException;
import org.javenstudio.cocoka.storage.StorageFile;

public abstract class HtmlCallback extends AbstractCallback {
	
	@Override 
	public final void onContentDownloaded(Object content) {
		if (content != null) { 
			if (content instanceof StorageFile) {
				onCacheFileSaved((StorageFile)content); 
				return; 
			} else if (content instanceof Writer) {
				onHtmlSaved((Writer)content); 
				return;
			}
		}
		
		onHtmlFetched((String)content); 
	}
	
	@Override 
	public void onFinished() {} 
	
	public void onHtmlFetched(String content) {} 
	public void onHtmlSaved(Writer writer) {} 
	public void onCacheFileSaved(StorageFile file) {} 
	public void onHttpException(HttpException exception) {} 
	
}
