package org.javenstudio.falcon.user.auth;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.datum.util.BlobCache;
import org.javenstudio.falcon.datum.util.BlobCacheHelper;
import org.javenstudio.falcon.datum.util.BytesBufferPool;

final class AuthCache extends BlobCacheHelper {
	private static final Logger LOG = Logger.getLogger(AuthCache.class);

	public static final int MAX_ENTRIES = 500000;
	
    private static final int CACHE_MAX_BYTES = 500 * 1024 * 1024;
    private static final int CACHE_VERSION = 1;
    
    private static final int BYTESBUFFE_POOL_SIZE = 100;
    private static final int BYTESBUFFER_SIZE = 100 * 1024;
    
    private static final String USERFILE = "user.db";
    private static final String NAMEFILE = "name.db";
	
    private final BytesBufferPool mBufferPool =
            new BytesBufferPool(BYTESBUFFE_POOL_SIZE, BYTESBUFFER_SIZE);
    
    private final String mCacheDir;
    private final int mMaxEntries;
    
    private BlobCache mUserCache = null; 
    private BlobCache mNameCache = null; 
    
    public AuthCache(String cacheDir) { 
    	this(cacheDir, MAX_ENTRIES);
    }
    
    public AuthCache(String cacheDir, int maxEntries) { 
    	if (cacheDir == null) throw new NullPointerException();
    	mCacheDir = cacheDir;
    	
    	if (maxEntries <= 0)
    		maxEntries = MAX_ENTRIES;
    	
    	mMaxEntries = maxEntries;
    	
    	if (LOG.isDebugEnabled()) 
    		LOG.debug("created: maxEntries=" + maxEntries);
    }
    
    public int getMaxEntries() { return mMaxEntries; }
    public String getBlobCacheDir() { return mCacheDir; }
    
    private synchronized BlobCache getUserCache(boolean init) { 
    	if (mUserCache == null && init) {
	    	mUserCache = getBlobCache(USERFILE,
	    			getMaxEntries(), CACHE_MAX_BYTES,
	                CACHE_VERSION);
    	}
    	return mUserCache;
    }
    
    private synchronized BlobCache getNameCache(boolean init) { 
    	if (mNameCache == null && init) {
    		mNameCache = getBlobCache(NAMEFILE,
	    			getMaxEntries(), CACHE_MAX_BYTES,
	                CACHE_VERSION);
    	}
    	return mNameCache;
    }
    
    public synchronized void close() { 
    	if (LOG.isDebugEnabled())
    		LOG.debug("close");
    	
    	BlobCache userCache = mUserCache; 
    	if (userCache != null) userCache.close();
    	mUserCache = null;
    	
    	BlobCache nameCache = mNameCache; 
    	if (nameCache != null) nameCache.close();
    	mNameCache = null;
    }
    
    public BytesBufferPool.BytesBuffer getUserData(String path) { 
    	BytesBufferPool.BytesBuffer buffer = mBufferPool.get();
    	if (getUserData(path, buffer)) return buffer;
    	return null;
    }
    
    private boolean getUserData(String path, BytesBufferPool.BytesBuffer buffer) {
    	if (path == null || buffer == null || path.length() == 0)
    		return false;
    	return getBlobData(getUserCache(true), path, 0, buffer);
    }

    public boolean putUserData(String path, byte[] value) {
    	return putBlobData(getUserCache(true), path, 0, value);
    }

    public boolean clearUserData(String path) {
    	return clearBlobData(getUserCache(true), path, 0);
    }
    
    public BytesBufferPool.BytesBuffer getNameData(String path) { 
    	BytesBufferPool.BytesBuffer buffer = mBufferPool.get();
    	if (getNameData(path, buffer)) return buffer;
    	return null;
    }
    
    private boolean getNameData(String path, BytesBufferPool.BytesBuffer buffer) {
    	if (path == null || buffer == null || path.length() == 0)
    		return false;
    	return getBlobData(getNameCache(true), path, 0, buffer);
    }

    public boolean putNameData(String path, byte[] value) {
    	return putBlobData(getNameCache(true), path, 0, value);
    }

    public boolean clearNameData(String path) {
    	return clearBlobData(getNameCache(true), path, 0);
    }
    
}
