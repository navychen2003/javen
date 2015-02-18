package org.javenstudio.cocoka.storage;

import org.javenstudio.cocoka.storage.fs.IFile;
import org.javenstudio.cocoka.util.MimeType;

public class ApplicationFile extends StorageFile {

	public ApplicationFile(Storage store, IFile file) {
		super(store, file); 
	}
	
	@Override 
	public String getContentType() { 
		return MimeType.TYPE_APPLICATION.getType(); 
	}
	
	@Override 
	protected FileCacheFactory getCacheFactory() { 
		return null; 
	}
	
}
