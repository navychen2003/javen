package org.javenstudio.cocoka.graphics.media;

import org.javenstudio.cocoka.graphics.BitmapCache;
import org.javenstudio.cocoka.storage.FileCache;
import org.javenstudio.cocoka.storage.StorageFile;
import org.javenstudio.cocoka.util.BitmapCacheFile;
import org.javenstudio.cocoka.util.BitmapLoader;

class ImageCache extends FileCache {

	private final BitmapLoader mLoader; 
	private BitmapCache mBitmapCache = BitmapCache.getInstance(); 
	
	ImageCache(StorageFile file, BitmapLoader loader) { 
		super(file); 
		
		mLoader = loader; 
	}
	
	BitmapCacheFile getBitmapFile() { 
		return mBitmapCache.getOrCreate(getCacheFile().getLocation(), mLoader); 
	}
	
	@Override 
	public int getCachedCount() {
		return getBitmapFile().getCachedCount(); 
	}
	
	@Override 
	public long getCachedSize() {
		return getBitmapFile().getCachedSize(); 
	}
	
	@Override 
	public void recycle() {
		getBitmapFile().recycle(); 
	}
	
}
