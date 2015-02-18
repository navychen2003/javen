package org.javenstudio.cocoka.storage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import org.javenstudio.common.util.Logger;

public final class StorageManager {
	private static Logger LOG = Logger.getLogger(StorageManager.class);

	public interface Directories { 
		public String getCacheDirectory(); 
		public String getStorageDirectory(String name); 
	}
	
	private static StorageManager sInstance = null; 
	
	public static synchronized void initInstance(Context context, Directories dirs) { 
		if (context == null || dirs == null) 
			throw new RuntimeException("Context or Directories parameter is null");
		if (sInstance != null) 
			throw new RuntimeException("StorageManager already initialized");
		sInstance = new StorageManager(context, dirs);
	}
	
	public static synchronized final StorageManager getInstance() { 
		if (sInstance == null) 
			throw new RuntimeException("StorageManager not initialized");
		return sInstance; 
	}
	
	private final Map<String, Storage> mStorages = new HashMap<String, Storage>(); 
	private final Context mContext; 
	private final Directories mDirectories; 
	
	private StorageManager(Context context, Directories dirs) { 
		mContext = context;
		mDirectories = dirs;
	}
	
	public LocalStorage getLocalStorageWithName(String name) throws IOException {
		return getLocalStorageWithPath(getLocalDirectory(name)); 
	}
	
	public synchronized LocalStorage getLocalStorageWithPath(String path) throws IOException {
		LocalStorage localStorage = null; 
		Storage storage = mStorages.get(path); 
		
		if (storage == null) { 
			localStorage =  new LocalStorage(this, path); 
			mStorages.put(path, localStorage); 
			LOG.info("StorageManager: opened a LocalStorage at "+path); 
			
		} else if (storage instanceof LocalStorage) { 
			localStorage = (LocalStorage)storage; 
			
		} else { 
			throw new IOException("storage: "+path+" is a "+storage.getClass().getName()+", not a LocalStorage"); 
		}
		
		return localStorage; 
	}
	
	public synchronized CacheStorage getCacheStorage(String cachename, int version) throws IOException { 
		final String path = getCacheDirectory(cachename); 
		
		CacheStorage cacheStorage = null; 
		Storage storage = mStorages.get(path); 
		
		if (storage == null) { 
			cacheStorage = new CacheStorage(this, 
					cachename, path, getCacheDirectory(cachename, version), version); 
			mStorages.put(path, cacheStorage); 
			
			LOG.info("StorageManager: opened a CacheStorage at "+path); 
			
		} else if (storage instanceof CacheStorage) { 
			cacheStorage = (CacheStorage)storage; 
			
		} else { 
			throw new IOException("storage: "+path+" is a "+storage.getClass().getName()+", not a CacheStorage"); 
		}
		
		return cacheStorage; 
	}
	
	public final String getCacheDirectory() {
		return mDirectories.getCacheDirectory(); 
	}
	
	public final String getCacheDirectory(String cachename) {
		return getCacheDirectory() + "/" + cachename; 
	}
	
	public final String getCacheDirectory(String cachename, int version) {
		return getCacheDirectory() + "/" + cachename + "." + version; 
	}
	
	public final String getLocalDirectory(String name) {
		return mDirectories.getStorageDirectory(name); 
	}
	
	public final Context getContext() { 
		return mContext; 
	}
	
}
