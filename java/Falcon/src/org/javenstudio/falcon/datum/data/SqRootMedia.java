package org.javenstudio.falcon.datum.data;

import java.io.IOException;
import java.util.Map;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IImageSource;
import org.javenstudio.falcon.datum.IScreenshot;
import org.javenstudio.falcon.datum.util.BytesBufferPool;
import org.javenstudio.falcon.datum.util.MediaCache;

public abstract class SqRootMedia extends SqRootFile 
		implements IImageSource, MediaCache.MediaData {
	//private static final Logger LOG = Logger.getLogger(SqRootImage.class);
	
	private MediaCache mCache = null;
	
	public SqRootMedia(SqRoot root, 
			NameData nameData, String contentType) {
		super(root, nameData, contentType);
	}
	
	private synchronized MediaCache getMediaCache() { 
		if (mCache == null) 
			mCache = new MediaCache(this);
		return mCache;
	}
	
	@Override
	public synchronized void close() { 
		MediaCache cache = mCache;
		if (cache != null) cache.close();
		mCache = null;
		super.close();
	}
	
	@Override
	public String getKey() { 
		return getContentKey();
	}
	
	@Override
	public void putCache(String key, byte[] data) {
		getLibrary().putImageData(key, data);
	}
	
	@Override
	public BytesBufferPool.BytesBuffer getCache(String key) { 
		return getLibrary().getImageData(key);
	}
	
	@Override
	public IScreenshot[] getScreenshots() throws IOException { 
		FileData data = getFileData();
		if (data != null) { 
			FileScreenshot[] shots = data.getScreenshots();
			return shots;
		}
		return null;
	}
	
	@Override
	protected boolean hasScreenshotPoster(NameData data) { 
		if (data != null) { 
			if (data.getAttrs().getPosterCount() > 0) 
				return true;
		}
		return false;
	}
	
	@Override
	public IImageSource.Bitmap getBitmap(IImageSource.Param param) throws IOException { 
		return getMediaCache().getBitmap(param);
	}
	
	@Override
	public int getMetaTag(Map<String,Object> tags) throws ErrorException { 
		if (tags == null) return 0;
		
		int count = super.getMetaTag(tags);
		if (count > 0) return count;
		
		count = 0;
		
		return count;
	}
	
}
