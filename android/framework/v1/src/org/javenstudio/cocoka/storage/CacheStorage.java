package org.javenstudio.cocoka.storage;

import java.io.IOException;

import org.javenstudio.cocoka.storage.fs.IFile;
import org.javenstudio.cocoka.storage.fs.Path;

public final class CacheStorage extends LocalStorage {

	private final IFile mCacheDirectory; 
	private final int mVersion; 
	
	CacheStorage(StorageManager manager, String cachename, String cachepath, String cachepathVersion, int version) throws IOException {
		super(manager, cachepathVersion); 
		
		mCacheDirectory = getFileSystem().getFile(new Path(cachepath)); 
		mVersion = version; 
		
		clearOldFiles(manager, cachename, version); 
	}
	
	@Override 
	public final String getDirectoryLocation() { 
		return mCacheDirectory.getLocation(); 
	}
	
	public final int getCacheVersion() { 
		return mVersion; 
	}
	
	private void clearOldFiles(StorageManager manager, String cachename, int version) {
		final String cacheDir = manager.getCacheDirectory(); 
		final String directoryName = cachename + "."; 
		final String cacheVersion = "." + version; 
		
		IFile[] files = listFiles(cacheDir); 
		
		for (int i=0; files != null && i < files.length; i++) {
			IFile file = files[i]; 
			String name = file != null ? file.getName() : null; 
			if (name == null) continue; 
			if (!name.startsWith(directoryName) || name.endsWith(cacheVersion)) 
				continue; 
			
			removeFile(file); 
		}
	}
	
}
