package org.javenstudio.falcon.datum.util;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.util.Utils;

public abstract class BlobCacheHelper {
	private static final Logger LOG = Logger.getLogger(BlobCacheHelper.class);

	private boolean mOldCheckDone = false;
	
	private final Map<String, BlobCache> mCacheMap =
            new HashMap<String, BlobCache>();
	
	public abstract String getBlobCacheDir();
	
    // Return null when we cannot instantiate a BlobCache, e.g.:
    // there is no SD card found.
    // This can only be called from data thread.
	public final BlobCache getBlobCache(String filename,
            int maxEntries, int maxBytes, int version) {
        synchronized (mCacheMap) {
            if (!mOldCheckDone) {
                removeBlobCacheIfNecessary();
                mOldCheckDone = true;
            }
            
            BlobCache cache = mCacheMap.get(filename);
            if (cache == null) {
                try {
                	String path = getBlobCacheDir();
                	String filepath = path + "/" + filename;
                	
                	File pathFile = new File(filepath);
                	File parentFile = pathFile.getParentFile();
                	if (!parentFile.exists())
                		parentFile.mkdirs();
                	
                    cache = new BlobCache(filepath, maxEntries, maxBytes, false, version);
                    mCacheMap.put(filename, cache);
                    
                    if (LOG.isInfoEnabled()) { 
                    	LOG.info("getBlobCache: new cache, filename=" + filename 
                    			+ " path=" + filepath 
                    			+ " maxEntries=" + maxEntries 
                    			+ " maxBytes=" + maxBytes 
                    			+ " version=" + version);
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
	public void removeBlobCacheIfNecessary() {
        //String cacheDir = getBlobCacheDir();
        //String prefix = cacheDir + "/";

        //BlobCache.deleteFiles(prefix + "imgcache");
        //BlobCache.deleteFiles(prefix + "rev_geocoding");
        //BlobCache.deleteFiles(prefix + "bookmark");
    }
    
	public static boolean getBlobData(BlobCache cache, String path, int type, 
    		BytesBufferPool.BytesBuffer buffer) {
    	if (cache == null) return false;
    	
        byte[] key = makeKey(path, type);
        long cacheKey = Utils.crc64Long(key);
        
        try {
        	BlobCache.LookupRequest request = new BlobCache.LookupRequest();
            request.key = cacheKey;
            request.buffer = buffer.data;
            
            synchronized (cache) {
                if (!cache.lookup(request)) {
                	if (LOG.isDebugEnabled()) { 
                    	LOG.debug("getBlobData: path=" + path + " key=" + cacheKey 
                    			+ " lookup false");
                    }
                	
                	return false;
                }
            }
            
            if (isSameKey(key, request.buffer)) {
                buffer.data = request.buffer;
                buffer.offset = key.length;
                buffer.length = request.length - buffer.offset;
                
            	if (LOG.isDebugEnabled()) { 
                	LOG.debug("getBlobData: path=" + path + " key=" + cacheKey 
                			+ " dataLength=" + (buffer.data != null ? buffer.data.length : 0) 
                			+ " offset=" + buffer.offset + " length=" + buffer.length);
                }
                
                return true;
            } else {
	            if (LOG.isDebugEnabled()) { 
	            	LOG.debug("getBlobData: path=" + path + " key=" + cacheKey 
	            			+ " lookup not same key");
	            }
            }
        } catch (IOException ex) {
        	if (LOG.isDebugEnabled()) { 
            	LOG.debug("getBlobData: path=" + path + " key=" + cacheKey 
            			+ " lookup error: " + ex, ex);
            }
        }
        
        return false;
    }
    
	public static boolean putBlobData(BlobCache cache, 
    		String path, int type, byte[] value) {
    	if (cache == null || path == null || value == null) 
    		return false;
    	
        byte[] key = makeKey(path, type);
        long cacheKey = Utils.crc64Long(key);
        
        ByteBuffer buffer = ByteBuffer.allocate(key.length + value.length);
        buffer.put(key);
        buffer.put(value);
        
        synchronized (cache) {
            try {
            	byte[] bytes = buffer.array();
            	if (bytes != null && bytes.length > 0) { 
	            	cache.insert(cacheKey, bytes);
	            	
	            	if (LOG.isDebugEnabled()) { 
	                	LOG.debug("putBlobData: path=" + path + " key=" + cacheKey 
	                			+ " length=" + bytes.length);
	                }
	            	
	            	return true;
            	}
            } catch (IOException ex) {
            	if (LOG.isDebugEnabled()) {
                	LOG.debug("putBlobData: path=" + path + " key=" + cacheKey 
                			+ " error: " + ex, ex);
            	}
            }
            return false;
        }
    }
    
	public static boolean clearBlobData(BlobCache cache, 
    		String path, int type) {
    	if (cache == null || path == null) return false;
    	
        byte[] key = makeKey(path, type);
        long cacheKey = Utils.crc64Long(key);
        
    	if (LOG.isDebugEnabled()) { 
        	LOG.debug("clearBlobData: path=" + path + " key=" + cacheKey 
        			+ " type=" + type);
        }
        
        synchronized (cache) {
            try {
            	return cache.clearEntry(cacheKey);
            } catch (IOException ex) {
                if (LOG.isDebugEnabled()) {
                	LOG.debug("clearBlobData: path=" + path + " key=" + cacheKey 
                			+ " error: " + ex, ex);
                }
            }
            return false;
        }
    }

    private static byte[] makeKey(String path, int type) {
        return getBytes(path + "+" + type);
    }

    private static byte[] getBytes(String in) {
        byte[] result = new byte[in.length() * 2];
        int output = 0;
        for (char ch : in.toCharArray()) {
            result[output++] = (byte) (ch & 0xFF);
            result[output++] = (byte) (ch >> 8);
        }
        return result;
    }
    
    private static boolean isSameKey(byte[] key, byte[] buffer) {
        int n = key.length;
        if (buffer.length < n) 
            return false;
        
        for (int i = 0; i < n; ++i) {
            if (key[i] != buffer[i]) 
                return false;
        }
        
        return true;
    }
	
}
