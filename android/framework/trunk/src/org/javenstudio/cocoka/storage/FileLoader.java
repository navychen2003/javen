package org.javenstudio.cocoka.storage;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map; 
import java.util.HashMap; 

import org.javenstudio.cocoka.storage.fs.IFile;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;

public class FileLoader {

	private final Map<String, FileCache> mCached = 
			new HashMap<String, FileCache>(); 
	
	private final Storage mStorage; 
	
	public FileLoader(Storage store) {
		mStorage = store; 
	}
	
	private Storage getStorage() { return mStorage; }
	
	public int getCachedCount() {
		int count = 0; 
		
		for (FileCache data : mCached.values()) {
			if (data != null) 
				count += data.getCachedCount(); 
		}
		
		return count; 
	}
	
	public long getCachedSize() {
		long size = 0; 
		
		for (FileCache data : mCached.values()) {
			if (data != null) 
				size += data.getCachedSize(); 
		}
		
		return size; 
	}
	
	//public void recycleCaches() {
	//	for (FileCache data : mCached.values()) {
	//		if (data != null) 
	//			data.recycle(); 
	//	}
	//}
	
	private FileCache getCache(IFile file) {
		if (file == null) return null; 
		
		String skey = file.getPath(); 
		
		synchronized (mCached) {
			return mCached.get(skey); 
		}
	}
	
	private void putCache(IFile file, FileCache data) {
		if (file == null || data == null) 
			return; 
		
		String skey = file.getPath(); 
		
		synchronized (mCached) {
			data.setKey(skey); 
			mCached.put(skey, data); 
		}
	}
	
	public FileCache getOrCreateCache(IFile file, FileCacheFactory factory) {
		if (file == null) return null; 
		
		FileCache data = getCache(file); 
		if (data == null) {
			if (factory == null) 
				return null; 
			
			data = factory.create(); 
			putCache(file, data); 
		}
		
		data.setLoadTime(System.currentTimeMillis()); 
		
		return data; 
	}
	
	//public BitmapRef loadBitmap(FileCache data) {
	//	return loadBitmap(getStorage(), data);
	//}
	
	public BitmapRef loadBitmap(BitmapHolder holder, FileCache data) {
		if (holder == null || data == null) 
			return null; 
		
		return loadBitmap(holder, data.getCacheFile()); 
	}
	
	//public BitmapRef loadBitmap(StorageFile file) {
	//	return loadBitmap(getStorage(), file);
	//}
	
	public BitmapRef loadBitmap(BitmapHolder holder, StorageFile file) {
		if (holder == null || file == null) 
			return null; 
		
		return loadBitmap(holder, file.getFile()); 
	}
	
	//public BitmapRef loadBitmap(IFile file) {
	//	return loadBitmap(getStorage(), file);
	//}
	
	public BitmapRef loadBitmap(BitmapHolder holder, IFile file) {
		if (file == null) return null; 
		
		return FileHelper.loadBitmap(holder, 
				getStorage().getFileSystem(), file, 
				getStorage().getReadBufferSize()); 
	}
	
	public byte[] loadFile(IFile file) {
		if (file == null) return null; 
		
		return FileHelper.loadFile( 
				getStorage().getFileSystem(), file, 
				getStorage().getReadBufferSize()); 
	}
	
	public InputStream openFile(IFile file) {
		if (file == null) return null; 
		
		return FileHelper.openFile( 
				getStorage().getFileSystem(), file, 
				getStorage().getReadBufferSize()); 
	}
	
	public OutputStream createFile(IFile file) {
		if (file == null) return null; 
		
		return FileHelper.createFile( 
				getStorage().getFileSystem(), file, 
				getStorage().getWriteBufferSize()); 
	}
	
	public OutputStream appendFile(IFile file) {
		if (file == null) return null; 
		
		return FileHelper.appendFile( 
				getStorage().getFileSystem(), file, 
				getStorage().getWriteBufferSize()); 
	}
	
}
