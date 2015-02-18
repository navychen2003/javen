package org.javenstudio.android.data;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.javenstudio.cocoka.storage.CacheManager;
import org.javenstudio.cocoka.storage.StorageFile;
import org.javenstudio.cocoka.util.BlobCache;
import org.javenstudio.cocoka.util.BytesBufferPool;
import org.javenstudio.cocoka.util.MediaUtils;
import org.javenstudio.cocoka.util.MimeType;
import org.javenstudio.cocoka.util.Utils;

public class CacheData {
	//private static final Logger LOG = Logger.getLogger(CacheData.class);

	private static final String IMAGE_CACHE_FILE = "myimage";
    private static final int IMAGE_CACHE_MAX_ENTRIES = 5000;
    private static final int IMAGE_CACHE_MAX_BYTES = 200 * 1024 * 1024;
    private static final int IMAGE_CACHE_VERSION = 1;

    //private static final String LOCAL_CACHE_FILE = "mylocal";
    //private static final int LOCAL_CACHE_MAX_ENTRIES = 50000;
    //private static final int LOCAL_CACHE_MAX_BYTES = 200 * 1024 * 1024;
    //private static final int LOCAL_CACHE_VERSION = 1;
    
    private final CacheManager mManager;
	private final BlobCache mImageCache; 
	//private final BlobCache mLocalCache;

    public CacheData(CacheManager manager) {
    	mManager = manager;
    	
        mImageCache = manager.getCache(IMAGE_CACHE_FILE,
                IMAGE_CACHE_MAX_ENTRIES, IMAGE_CACHE_MAX_BYTES,
                IMAGE_CACHE_VERSION);
        
        //mLocalCache = manager.getCache(LOCAL_CACHE_FILE,
        //        LOCAL_CACHE_MAX_ENTRIES, LOCAL_CACHE_MAX_BYTES,
        //        LOCAL_CACHE_VERSION);
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
    public boolean getImageData(String path, int type, 
    		BytesBufferPool.BytesBuffer buffer) {
    	return getData(mImageCache, path, type, buffer);
    }

    public void putImageData(String path, int type, byte[] value) {
    	putData(mImageCache, path, type, value);
    }

    public void clearImageData(String path, int type) {
    	clearData(mImageCache, path, type);
    }
    
    //public boolean getLocalData(String path, int type, 
    //		BytesBufferPool.BytesBuffer buffer) {
    //	return getData(mLocalCache, path, type, buffer);
    //}
    
    //public void putLocalData(String path, int type, byte[] value) {
    //	putData(mLocalCache, path, type, value);
    //}

    //public void clearLocalData(String path, int type) {
    //	clearData(mLocalCache, path, type);
    //}
    
    public StorageFile openFile(String path, int type) throws IOException { 
    	byte[] key = makeKey(path, type);
        long cacheKey = Utils.crc64Long(key);
        
        String filename = "" + cacheKey + ".dat";
        return mManager.getStorage().getFile(MimeType.TYPE_APPLICATION, filename);
    }
    
    private static boolean getData(BlobCache cache, String path, int type, 
    		BytesBufferPool.BytesBuffer buffer) {
        byte[] key = makeKey(path, type);
        long cacheKey = Utils.crc64Long(key);
        
        try {
        	BlobCache.LookupRequest request = new BlobCache.LookupRequest();
            request.key = cacheKey;
            request.buffer = buffer.data;
            
            synchronized (cache) {
                if (!cache.lookup(request)) 
                	return false;
            }
            
            if (isSameKey(key, request.buffer)) {
                buffer.data = request.buffer;
                buffer.offset = key.length;
                buffer.length = request.length - buffer.offset;
                
                //if (buffer.data != null && LOG.isDebugEnabled()) { 
                //	LOG.debug("getData: key=" + cacheKey + " dataLength=" + buffer.data.length 
                //			+ " offset=" + buffer.offset + " length=" + buffer.length);
                //}
                
                return true;
            }
        } catch (IOException ex) {
            // ignore.
        }
        
        return false;
    }
    
    private static void putData(BlobCache cache, String path, int type, byte[] value) {
        byte[] key = makeKey(path, type);
        long cacheKey = Utils.crc64Long(key);
        
        ByteBuffer buffer = ByteBuffer.allocate(key.length + value.length);
        buffer.put(key);
        buffer.put(value);
        
        synchronized (cache) {
            try {
            	cache.insert(cacheKey, buffer.array());
            } catch (IOException ex) {
                // ignore.
            }
        }
    }
    
    private static void clearData(BlobCache cache, String path, int type) {
        byte[] key = makeKey(path, type);
        long cacheKey = Utils.crc64Long(key);
        
        synchronized (cache) {
            try {
            	cache.clearEntry(cacheKey);
            } catch (IOException ex) {
                // ignore.
            }
        }
    }

    private static byte[] makeKey(String path, int type) {
        return MediaUtils.getBytes(path + "+" + type);
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
