package org.javenstudio.cocoka.storage;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map; 
import java.util.HashMap; 

import android.graphics.Bitmap;

import org.javenstudio.cocoka.storage.fs.IFile;

public class FileLoader {

	private Map<String, FileCache> mCached = new HashMap<String, FileCache>(); 
	private final Storage mStore; 
	
	public FileLoader(Storage store) {
		mStore = store; 
	}
	
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
	
	public void recycleCaches() {
		for (FileCache data : mCached.values()) {
			if (data != null) 
				data.recycle(); 
		}
	}
	
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
	
	public Bitmap loadBitmap(FileCache data) {
		if (data == null) 
			return null; 
		
		return loadBitmap(data.getCacheFile()); 
	}
	
	public Bitmap loadBitmap(StorageFile file) {
		if (file == null) 
			return null; 
		
		return loadBitmap(file.getFile()); 
	}
	
	public Bitmap loadBitmap(IFile file) {
		if (file == null) return null; 
		
		return FileHelper.loadBitmap(mStore.getManager().getContext(), 
				mStore.getFileSystem(), file, 
				mStore.getReadBufferSize()); 
	}
	
	public byte[] loadFile(IFile file) {
		if (file == null) return null; 
		
		return FileHelper.loadFile( 
				mStore.getFileSystem(), file, 
				mStore.getReadBufferSize()); 
	}
	
	public InputStream openFile(IFile file) {
		if (file == null) return null; 
		
		return FileHelper.openFile( 
				mStore.getFileSystem(), file, 
				mStore.getReadBufferSize()); 
	}
	
	public OutputStream createFile(IFile file) {
		if (file == null) return null; 
		
		return FileHelper.createFile( 
				mStore.getFileSystem(), file, 
				mStore.getWriteBufferSize()); 
	}
	
	public OutputStream appendFile(IFile file) {
		if (file == null) return null; 
		
		return FileHelper.appendFile( 
				mStore.getFileSystem(), file, 
				mStore.getWriteBufferSize()); 
	}
	
}
