package org.javenstudio.cocoka.storage.content;

import java.util.HashMap;
import java.util.Map;

public final class StorageProviderManager {

	private static final StorageProviderManager sInstance = new StorageProviderManager(); 
	public static StorageProviderManager getInstance() { 
		return sInstance; 
	}
	
	protected static class ProviderInfo { 
		public final String mPath; 
		public final StorageProviderFactory mFactory; 
		private StorageProvider mProvider = null; 
		
		public ProviderInfo(String path, StorageProviderFactory factory) { 
			mPath = path; 
			mFactory = factory; 
		}
	}
	
	private final Map<String, ProviderInfo> mProviders = new HashMap<String, ProviderInfo>(); 
	
	private StorageProviderManager() {}
	
	public synchronized void registerProvider(String path, StorageProviderFactory factory) { 
		if (path == null || factory == null) 
			return; 
		
		synchronized (mProviders) { 
			if (mProviders.containsKey(path)) 
				throw new RuntimeException("storage provider: "+path+" already registered"); 
			
			mProviders.put(path, new ProviderInfo(path, factory)); 
		}
	}
	
	public synchronized String[] getProviderPaths() { 
		synchronized (mProviders) { 
			return mProviders.keySet().toArray(new String[0]); 
		}
	}
	
	public synchronized StorageProvider getProvider(String path) { 
		if (path == null) return null; 
		
		synchronized (mProviders) { 
			ProviderInfo info = mProviders.get(path); 
			if (info == null) 
				throw new RuntimeException("storage provider: "+path+" not registered"); 
			
			if (info.mProvider == null) 
				info.mProvider = info.mFactory.create(info.mPath); 
			
			if (info.mProvider == null) 
				throw new RuntimeException("storage provider: "+path+" cannot created"); 
			
			return info.mProvider; 
		}
	}
	
}
