package org.javenstudio.cocoka.storage;

import org.javenstudio.cocoka.storage.fs.IFile;
import org.javenstudio.cocoka.util.MimeType;

public class TextFile extends StorageFile {

	public TextFile(Storage store, IFile file) {
		super(store, file); 
	}
	
	@Override 
	public String getContentType() { 
		return MimeType.TYPE_TEXT.getType(); 
	}
	
	@Override 
	protected FileCacheFactory getCacheFactory() { 
		return null; 
	}
	
}
