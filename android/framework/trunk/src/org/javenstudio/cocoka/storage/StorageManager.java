package org.javenstudio.cocoka.storage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import org.javenstudio.cocoka.storage.fs.IFile;
import org.javenstudio.common.util.Logger;

public final class StorageManager {
	private static Logger LOG = Logger.getLogger(StorageManager.class);

	public interface Directories { 
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
	
	public final Context getContext() { 
		return mContext; 
	}
	
	public final String getStoragePath(String name) {
		return mDirectories.getStorageDirectory(name); 
	}
	
	public Storage getStorage(String name) throws IOException {
		return getStorage(name, getStoragePath(name)); 
	}
	
	private synchronized Storage getStorage(String name, 
			String path) throws IOException {
		LocalStorage localStorage = null; 
		Storage storage = mStorages.get(path); 
		
		if (storage == null) { 
			localStorage =  new LocalStorage(this, name, path); 
			mStorages.put(path, localStorage); 
			
			if (LOG.isInfoEnabled())
				LOG.info("opened LocalStorage at " + path); 
			
		} else if (storage instanceof LocalStorage) { 
			localStorage = (LocalStorage)storage; 
			
		} else { 
			throw new IOException("storage: " + path + " is a " + storage.getClass().getName() 
					+ ", not a LocalStorage"); 
		}
		
		return localStorage; 
	}
	
	public Storage getCacheStorage() throws IOException { 
		return getCacheStorage(null);
	}
	
	public Storage getCacheStorage(String cacheName) throws IOException { 
		synchronized (sCacheLock) { 
			if (!sCacheDiirectoryChecked) { 
				Storage cacheStorage = getStorage(CACHE_NAME);
				IFile versionFile = cacheStorage.newFsFileByName(CACHE_VERSION_FILE);
				if (!versionFile.exists()) { 
					if (LOG.isInfoEnabled()) 
						LOG.info("clear cache files in " + cacheStorage.getDirectory().getAbsolutePath());
					
					cacheStorage.clearDirectory();
					cacheStorage.getFileSystem().createNewFile(versionFile);
				}
				sCacheDiirectoryChecked = true;
			}
		}
		if (cacheName != null && cacheName.length() > 0)
			return getStorage(CACHE_NAME + "/" + cacheName);
		else
			return getStorage(CACHE_NAME);
	}
	
	private static final String CACHE_NAME = "cache";
	private static final String CACHE_VERSION_FILE = "version.0";
	private static boolean sCacheDiirectoryChecked = false;
	private static Object sCacheLock = new Object();
	
}
