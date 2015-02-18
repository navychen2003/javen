package org.javenstudio.cocoka.storage;

import java.io.IOException;

import org.javenstudio.cocoka.storage.fs.IFile;
import org.javenstudio.cocoka.storage.fs.IFileSystem;
import org.javenstudio.cocoka.storage.fs.Path;

public class TempFile extends StorageFile {

	private final StorageFile mStorageFile; 
	
	public TempFile(Storage store, StorageFile file) throws IOException { 
		super(store, createTempFile(store, file)); 
		
		mStorageFile = file; 
	}
	
	@Override 
	public String getContentType() {
		return mStorageFile.getContentType(); 
	}
	
	@Override 
	protected FileCacheFactory getCacheFactory() { 
		return null; //mCacheFile.getCacheDataFactory(); 
	}
	
	public final StorageFile getStorageFile() { 
		return mStorageFile; 
	}
	
	public StorageFile restore() throws IOException { 
		synchronized (this) { 
			IFileSystem fs = getStorage().getFileSystem(); 
			if (fs.exists(mStorageFile.getFile())) 
				throw new IOException("file: "+mStorageFile.getFilePath()+" already existed"); 
			if (fs.exists(getFile())) { 
				if (fs.renameTo(getFile(), mStorageFile.getFile())) 
					return mStorageFile; 
			}
			return null; 
		}
	}
	
	private static String getTempName(String filename) { 
		return "." + filename.hashCode() + ".tmp"; 
	}
	
	public static String getTempFileName(String filename) { 
		if (filename == null || filename.length() == 0) 
			return filename; 
		
		return filename + getTempName(filename); 
	}
	
	public static IFile createTempFile(Storage store, StorageFile file) throws IOException { 
		String filename = file.getFileName(); 
		String filepath = file.getFilePath(); 
		
		return store.getFileSystem().getFile(new Path(filepath + getTempName(filename))); 
	}
	
}
