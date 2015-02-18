package org.javenstudio.android.data;

import java.io.File;

import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.media.ExifInterface;

import org.javenstudio.cocoka.graphics.BitmapUtil;
import org.javenstudio.cocoka.util.BitmapHelper;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.util.BytesBufferPool;
import org.javenstudio.cocoka.worker.job.Job;
import org.javenstudio.cocoka.worker.job.JobContext;
import org.javenstudio.common.util.Logger;

public abstract class CacheRequest implements Job<BitmapRef> {
	private static final Logger LOG = Logger.getLogger(CacheRequest.class);
	
    private final CacheData mCache;
    private final BitmapHolder mHolder;
    private final DataPath mPath;
    private int mType;
    private int mTargetSize;

    public CacheRequest(CacheData cache, BitmapHolder holder, 
    		DataPath path, int type, int targetSize) {
    	if (cache == null || holder == null || path == null)
    		throw new NullPointerException();
        mCache = cache;
        mHolder = holder;
        mPath = path;
        mType = type;
        mTargetSize = targetSize;
    }

    public final CacheData getCacheData() { return mCache; }
    public final BitmapHolder getBitmapHolder() { return mHolder; }
    public final DataPath getPath() { return mPath; }
    
    public final int getType() { return mType; }
    public final int getTargetSize() { return mTargetSize; }
    
    @Override
    public BitmapRef run(JobContext jc) {
        final CacheData cache = getCacheData();
        final BitmapHolder holder = getBitmapHolder();
        final DataPath path = getPath();
        
        final String pathString = path != null ? path.toString() : null;
        if (pathString == null) return null;
        
        BytesBufferPool.BytesBuffer buffer = BitmapHelper.getBytesBufferPool().get();
        try {
            boolean found = cache.getImageData(pathString, getType(), buffer);
            if (jc.isCancelled()) return null;
            
            if (found) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = BitmapRef.getBitmapConfig();
                BitmapRef bitmap;
                
                if (getType() == BitmapHelper.TYPE_MICROTHUMBNAIL) {
                    bitmap = BitmapUtil.decode(holder, jc,
                            buffer.data, buffer.offset, buffer.length, options,
                            BitmapHelper.getMicroThumbPool());
                    
                } else {
                    bitmap = BitmapUtil.decode(holder, jc,
                            buffer.data, buffer.offset, buffer.length, options,
                            BitmapHelper.getThumbPool());
                }
                
                if (bitmap == null && !jc.isCancelled()) {
                	if (LOG.isDebugEnabled())
                    	LOG.debug("decode cached failed: " + pathString);
                }
                
                return bitmap;
            }
        } finally {
        	BitmapHelper.getBytesBufferPool().recycle(buffer);
        }
        
        BitmapRef bitmap = onDecodeOriginal(jc, getType());
        if (jc.isCancelled()) return null;

        if (bitmap == null) {
        	//if (LOG.isDebugEnabled())
            //	LOG.debug("decode orig failed: " + pathString);
        	
            return null;
        }

        if (getType() == BitmapHelper.TYPE_MICROTHUMBNAIL) 
            bitmap = BitmapUtil.resizeAndCropCenter(holder, bitmap, getTargetSize(), true);
        else 
            bitmap = BitmapUtil.resizeDownBySideLength(holder, bitmap, getTargetSize(), true);
        
        if (jc.isCancelled()) return null;

        byte[] array = BitmapUtil.compressToBytes(bitmap);
        if (jc.isCancelled()) return null;

        cache.putImageData(pathString, getType(), array);
        return bitmap;
    }

    public abstract BitmapRef onDecodeOriginal(JobContext jc, int targetSize);
    
    public static class LocalRequest extends CacheRequest {
        private final String mLocalFilePath;

        public LocalRequest(CacheData service, BitmapHolder holder, 
        		DataPath path, int type, String localFilePath) {
            super(service, holder, path, type, BitmapHelper.getTargetSize(type));
            mLocalFilePath = localFilePath;
        }

        @Override
        public final BitmapRef onDecodeOriginal(JobContext jc, final int type) {
        	File localFile = loadFile(mLocalFilePath);
        	if (!localFile.exists() || !localFile.isFile()) 
        		return null;
        	
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = BitmapRef.getBitmapConfig();
            
            BitmapHolder holder = getBitmapHolder();
            int targetSize = BitmapHelper.getTargetSize(type);

            // try to decode from JPEG EXIF
            if (type == BitmapHelper.TYPE_MICROTHUMBNAIL) {
                ExifInterface exif = null;
                byte [] thumbData = null;
                
                try {
                    exif = new ExifInterface(localFile.getAbsolutePath());
                    if (exif != null) 
                        thumbData = exif.getThumbnail();
                    
                } catch (Throwable t) {
                	if (LOG.isWarnEnabled())
                    	LOG.warn("fail to get exif thumb", t);
                }
                
                if (thumbData != null) {
                    BitmapRef bitmap = BitmapUtil.decodeIfBigEnough(
                            holder, jc, thumbData, options, targetSize);
                    
                    if (bitmap != null) 
                    	return bitmap;
                }
            }

            if (LOG.isDebugEnabled()) {
            	LOG.debug("decode BitmapFile: " + localFile 
            			+ " length=" + localFile.length());
            }
            
            BitmapRef bitmap = BitmapUtil.decodeThumbnail(holder, jc, localFile, 
            		options, targetSize, type);
            
            if (bitmap == null) { 
            	try {
            		if (LOG.isDebugEnabled())
            			LOG.debug("delete error BitmapFile: " + localFile);
            		
            		localFile.delete();
            	} catch (Throwable e) { 
            		if (LOG.isDebugEnabled())
            			LOG.debug(e.toString(), e);
            	}
            }
            
            return bitmap;
        }
        
        protected File loadFile(String filePath) { 
        	return filePath != null && filePath.length() > 0 ? 
        			new File(filePath) : null; 
        }
    }
    
    public static class LocalLargeRequest implements Job<BitmapRegionDecoder> {
        private final String mLocalFilePath;

        public LocalLargeRequest(String localFilePath) {
            mLocalFilePath = localFilePath;
        }

        @Override
        public final BitmapRegionDecoder run(JobContext jc) {
        	File localFile = loadFile(mLocalFilePath);
        	if (!localFile.exists() || !localFile.isFile()) 
        		return null;
        	
        	final String filepath = localFile.getAbsolutePath();
        	if (LOG.isDebugEnabled()) {
            	LOG.debug("create BitmapRegionDecoder: " + filepath 
            			+ " length=" + localFile.length());
        	}
        	
            return BitmapUtil.createBitmapRegionDecoder(jc, filepath, false);
        }
        
        protected File loadFile(String filePath) { 
        	return filePath != null && filePath.length() > 0 ? 
        			new File(filePath) : null; 
        }
    }
    
}
