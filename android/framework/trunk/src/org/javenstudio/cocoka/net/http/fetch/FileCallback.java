package org.javenstudio.cocoka.net.http.fetch;

import java.io.OutputStream;

import org.javenstudio.cocoka.net.http.HttpException;
import org.javenstudio.cocoka.storage.StorageFile;

public abstract class FileCallback extends AbstractCallback {

	@Override 
	public void onContentFetched(Object content, HttpException exception) {
		try { 
			if (content != null) { 
				if (content instanceof StorageFile) {
					onCacheFileSaved((StorageFile)content); 
					return; 
				} else if (content instanceof OutputStream) { 
					onFileSaved((OutputStream)content); 
					return;
				}
			}
			
			onFileFetched((byte[])content); 
		} finally { 
			if (exception != null) 
				onHttpException(exception);
		}
	}
	
	@Override 
	public void onFinished() {} 
	
	public void onFileFetched(byte[] content) {} 
	public void onFileSaved(OutputStream output) {} 
	public void onCacheFileSaved(StorageFile file) {} 
	public void onHttpException(HttpException exception) {} 
	public void onFetchException(Throwable exception) {} 
	
}
