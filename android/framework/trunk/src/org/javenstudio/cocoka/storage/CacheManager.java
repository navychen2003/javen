package org.javenstudio.cocoka.storage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.content.SharedPreferences;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.util.BlobCache;
import org.javenstudio.common.util.Logger;

public class CacheManager {
    private static final Logger LOG = Logger.getLogger(CacheManager.class);
    
    private static final String KEY_CACHE_UP_TO_DATE = "cache-up-to-date";
    
    private final Map<String, BlobCache> sCacheMap =
            new HashMap<String, BlobCache>();
    
    private final Storage mStorage;
    private boolean sOldCheckDone = false;

    public CacheManager(Storage storage) { 
    	mStorage = storage;
    	if (mStorage == null) 
    		throw new NullPointerException("Storage is null");
    }
    
    public final Storage getStorage() { return mStorage; }
    
    // Return null when we cannot instantiate a BlobCache, e.g.:
    // there is no SD card found.
    // This can only be called from data thread.
    public BlobCache getCache(String filename,
            int maxEntries, int maxBytes, int version) {
        synchronized (sCacheMap) {
            if (!sOldCheckDone) {
                removeOldFilesIfNecessary();
                sOldCheckDone = true;
            }
            
            BlobCache cache = sCacheMap.get(filename);
            if (cache == null) {
            	String cacheDir = getStorage().getDirectory().getAbsolutePath();
                String path = cacheDir + "/" + filename;
                
                try {
                    cache = new BlobCache(path, maxEntries, maxBytes, false, version);
                    sCacheMap.put(filename, cache);
                    
                    if (LOG.isInfoEnabled()) { 
                    	LOG.info("new BlobCache: path=" + path + " maxEntries=" + maxEntries 
                    			+ " maxBytes=" + maxBytes + " version=" + version);
                    }
                } catch (IOException e) {
                	if (LOG.isErrorEnabled())
                    	LOG.error("Cannot instantiate cache!", e);
                }
            }
            
            return cache;
        }
    }

    // Removes the old files if the data is wiped.
    private void removeOldFilesIfNecessary() {
        SharedPreferences pref = ResourceHelper.getSharedPreferences();
        
        int n = 0;
        try {
            n = pref.getInt(KEY_CACHE_UP_TO_DATE, 0);
        } catch (Throwable t) {
            // ignore.
        }
        
        if (n != 0) return;
        pref.edit().putInt(KEY_CACHE_UP_TO_DATE, 1).commit();

        String cacheDir = getStorage().getDirectory().getAbsolutePath();
        String prefix = cacheDir + "/";

        BlobCache.deleteFiles(prefix + "imgcache");
        BlobCache.deleteFiles(prefix + "rev_geocoding");
        BlobCache.deleteFiles(prefix + "bookmark");
    }
    
}
