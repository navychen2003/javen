package org.javenstudio.cocoka.graphics.media;

import org.javenstudio.cocoka.graphics.BitmapCache;
import org.javenstudio.cocoka.storage.FileCache;
import org.javenstudio.cocoka.storage.StorageFile;
import org.javenstudio.cocoka.util.BitmapFile;
import org.javenstudio.cocoka.util.BitmapLoader;

class ImageCache extends FileCache {

	private final BitmapLoader mLoader; 
	private BitmapCache mBitmapCache = BitmapCache.getInstance(); 
	
	ImageCache(StorageFile file, BitmapLoader loader) { 
		super(file); 
		
		mLoader = loader; 
	}
	
	BitmapFile getBitmapFile() { 
		return mBitmapCache.getOrCreate(getCacheFile().getLocation(), mLoader); 
	}
	
}
