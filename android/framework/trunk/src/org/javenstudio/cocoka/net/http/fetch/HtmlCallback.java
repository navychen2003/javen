package org.javenstudio.cocoka.net.http.fetch;

import java.io.Writer;

import org.javenstudio.cocoka.net.http.HttpException;
import org.javenstudio.cocoka.storage.StorageFile;

public abstract class HtmlCallback extends AbstractCallback {
	
	@Override 
	public void onContentFetched(Object content, HttpException exception) {
		try { 
			if (content != null) { 
				if (content instanceof StorageFile) {
					onCacheFileSaved((StorageFile)content); 
					return; 
					
				} else if (content instanceof Writer) {
					onHtmlSaved((Writer)content); 
					return;
					
				} else if (content instanceof String) { 
					onHtmlFetched((String)content); 
					return;
					
				} else if (content instanceof CharSequence) { 
					onHtmlFetched(content.toString()); 
					return;
					
				} else if (content instanceof byte[]) { 
					onHtmlFetched(bytesToString((byte[])content));
					return;
					
				} else { 
					onUnknownFetched(content); 
					return;
				}
			}
			
			onHtmlFetched(null); 
		}  finally { 
			if (exception != null) 
				onHttpException(exception);
		}
	}
	
	@Override 
	public void onFinished() {} 
	
	public void onHtmlFetched(String content) {} 
	public void onHtmlSaved(Writer writer) {} 
	public void onCacheFileSaved(StorageFile file) {} 
	public void onHttpException(HttpException exception) {} 
	public void onFetchException(Throwable exception) {} 
	
	public void onUnknownFetched(Object content) {}
	
	protected String bytesToString(byte[] content) { 
		return new String(content);
	}
	
}
