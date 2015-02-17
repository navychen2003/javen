package org.javenstudio.cocoka.storage;

import java.io.InputStream;
import java.io.OutputStream;

import org.javenstudio.cocoka.storage.fs.IFile;
import org.javenstudio.cocoka.util.ContentFile;

public abstract class StorageFile implements ContentFile {

	private final Storage mStore; 
	private final IFile mFile; 
	
	public StorageFile(Storage store, IFile file) {
		mStore = store; 
		mFile = file; 
		
		if (store == null || file == null) 
			throw new NullPointerException();
	}
	
	@Override 
	public abstract String getContentType();
	
	protected abstract FileCacheFactory getCacheFactory(); 
	
	public final Storage getStorage() {
		return mStore; 
	}
	
	@Override 
	public String getLocation() { 
		return mFile.getLocation(); 
	}
	
	@Override 
	public String getFilePath() { 
		return mFile.getPath(); 
	}
	
	@Override 
	public String getFileName() { 
		return mFile.getName(); 
	}
	
	@Override 
	public long getContentLength() { 
		return mFile.length(); 
	}
	
	public final IFile getFile() {
		return mFile; 
	}
	
	protected final FileCache getCache() {
		return getStorage().getLoader().getOrCreateCache(
				getFile(), getCacheFactory()); 
	}
	
	public byte[] loadFile() {
		final FileLoader loader = getStorage().getLoader(); 
		final FileCache cache = getCache(); 
		
		byte[] content = null;
		if (cache != null) 
			content = cache.getContent(); 
		
		if (content == null) {
			content = loader.loadFile(getFile()); 
			
			if (content != null && cache != null) { 
				if (content.length < Storage.CACHE_CONTENT_SIZE) 
					cache.setContent(content); 
			}
		}
		
		return content; 
	}
	
	@Override 
	public InputStream openFile() {
		final FileLoader loader = getStorage().getLoader(); 
		return loader.openFile(getFile()); 
	}
	
	public OutputStream createFile() {
		final FileLoader loader = getStorage().getLoader(); 
		return loader.createFile(getFile()); 
	}
	
	public OutputStream appendFile() {
		final FileLoader loader = getStorage().getLoader(); 
		return loader.appendFile(getFile()); 
	}
	
	public String toString() {
		return getClass().getSimpleName() + "{file=" + mFile + "}"; 
	}
}
