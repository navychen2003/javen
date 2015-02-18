package org.javenstudio.falcon.datum;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.datum.util.BlobCache;
import org.javenstudio.falcon.datum.util.BlobCacheHelper;
import org.javenstudio.falcon.datum.util.BytesBufferPool;

public final class DataCache {
	private static final Logger LOG = Logger.getLogger(DataCache.class);

	public static final int MAX_ENTRIES = 100000;
	
    //private static final int IMAGE_CACHE_MAX_ENTRIES = 200000;
    private static final int IMAGE_CACHE_MAX_BYTES = 500 * 1024 * 1024;
    private static final int IMAGE_CACHE_VERSION = 1;
	
    //private static final int META_CACHE_MAX_ENTRIES = 200000;
    private static final int META_CACHE_MAX_BYTES = 500 * 1024 * 1024;
    private static final int META_CACHE_VERSION = 1;
    
    private final DataManager mManager;
    //private final String mPathName;
    private final String mFileName;
    private final int mMaxEntries;
    
    private BlobCache mImageCache = null; 
    private BlobCache mMetaCache = null; 
    
    public DataCache(DataManager manager, 
    		String filename, int maxEntries) { 
    	if (manager == null || filename == null) 
    		throw new NullPointerException();
    	mManager = manager;
    	//mPathName = pathname;
    	mFileName = filename;
    	
    	if (maxEntries <= 0)
    		maxEntries = MAX_ENTRIES;
    	
    	mMaxEntries = maxEntries;
    	
    	if (LOG.isDebugEnabled()) {
    		LOG.debug("created: name=" + filename 
    				+ " maxEntries=" + maxEntries);
    	}
    }
    
    public DataManager getManager() { return mManager; }
    public int getMaxEntries() { return mMaxEntries; }
    
    //public String getPathName() { return mPathName; }
    public String getFileName() { return mFileName; }
    //public String getName() { return mPathName + "/" + mFileName; }
    
    private synchronized BlobCache getImageCache(boolean init) { 
    	if (mImageCache == null && init) { 
    		mImageCache = getManager().getBlobCache(getFileName() + ".thumb",
        			getMaxEntries(), IMAGE_CACHE_MAX_BYTES,
                    IMAGE_CACHE_VERSION);
    	}
    	return mImageCache;
    }
    
    private synchronized BlobCache getMetaCache(boolean init) { 
    	if (mMetaCache == null && init) {
	    	mMetaCache = getManager().getBlobCache(getFileName() + ".meta",
	    			getMaxEntries(), META_CACHE_MAX_BYTES,
	                META_CACHE_VERSION);
    	}
    	return mMetaCache;
    }
    
    public synchronized void close() { 
    	if (LOG.isDebugEnabled())
    		LOG.debug("close: name=" + getFileName());
    	
    	BlobCache imageCache = mImageCache; 
    	if (imageCache != null) 
    		imageCache.close();
    	
    	BlobCache metaCache = mMetaCache; 
    	if (metaCache != null) 
    		metaCache.close();
    	
    	mImageCache = null;
    	mMetaCache = null;
    }
    
    /**
     * Gets the cached image data for the given <code>path</code> and <code>type</code>.
     *
     * The image data will be stored in <code>buffer.data</code>, started from
     * <code>buffer.offset</code> for <code>buffer.length</code> bytes. If the
     * buffer.data is not big enough, a new byte array will be allocated and returned.
     *
     * @return true if the image data is found; false if not found.
     */
    public boolean getImageData(String path, BytesBufferPool.BytesBuffer buffer) {
    	if (path == null || buffer == null || path.length() == 0)
    		return false;
    	return BlobCacheHelper.getBlobData(getImageCache(true), path, 0, buffer);
    }

    public void putImageData(String path, byte[] value) {
    	if (LOG.isDebugEnabled()) {
    		LOG.debug("putImageData: path=" + path + " bufferSize=" 
    				+ (value != null ? value.length : 0));
    	}
    	BlobCacheHelper.putBlobData(getImageCache(true), path, 0, value);
    }

    public void clearImageData(String path) {
    	if (LOG.isDebugEnabled()) LOG.debug("clearImageData: path=" + path);
    	BlobCacheHelper.clearBlobData(getImageCache(false), path, 0);
    }
    
    public boolean getMetaData(String path, BytesBufferPool.BytesBuffer buffer) {
    	if (path == null || buffer == null || path.length() == 0)
    		return false;
    	return BlobCacheHelper.getBlobData(getMetaCache(true), path, 0, buffer);
    }

    public void putMetaData(String path, byte[] value) {
    	if (LOG.isDebugEnabled()) {
    		LOG.debug("putMetaData: path=" + path + " bufferSize=" 
    				+ (value != null ? value.length : 0));
    	}
    	BlobCacheHelper.putBlobData(getMetaCache(true), path, 0, value);
    }

    public void clearMetaData(String path) {
    	if (LOG.isDebugEnabled()) LOG.debug("clearMetaData: path=" + path);
    	BlobCacheHelper.clearBlobData(getMetaCache(false), path, 0);
    }
    
}
