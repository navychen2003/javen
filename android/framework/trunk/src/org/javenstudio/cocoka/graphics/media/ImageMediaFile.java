package org.javenstudio.cocoka.graphics.media;

import org.javenstudio.cocoka.graphics.BitmapCache;
import org.javenstudio.cocoka.graphics.BitmapUtil;
import org.javenstudio.cocoka.storage.BaseMediaFile;
import org.javenstudio.cocoka.storage.FileCache;
import org.javenstudio.cocoka.storage.FileCacheFactory;
import org.javenstudio.cocoka.storage.FileLoader;
import org.javenstudio.cocoka.storage.Storage;
import org.javenstudio.cocoka.storage.fs.IFile;
import org.javenstudio.cocoka.util.BitmapFile;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapLoader;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.util.ImageFile;
import org.javenstudio.cocoka.util.MimeType;
import org.javenstudio.cocoka.util.Utilities;

public class ImageMediaFile extends BaseMediaFile implements ImageFile {
	
	private final BitmapLoader mLoader; 
	private final FileCacheFactory mFactory; 
	//private boolean mRecycled = false; 
	
	public ImageMediaFile(Storage store, IFile file) {
		super(store, file); 
		
		mLoader = new BitmapCache.Loader() {
				@Override
				public BitmapHolder getBitmapHolder(String name) { 
					return getStorage();
				}
				
				@Override
				public BitmapRef loadBitmap(BitmapHolder holder, String name) {
					if (BitmapCache.BITMAP_FULLSIZE.equals(name)) { 
						return doLoadFullsizeBitmap(holder);
						
					} else if (BitmapCache.BITMAP_PREVIEW.equals(name)) { 
						return doLoadPreviewBitmap(holder);
						
					} else if (BitmapCache.BITMAP_THUMBNAILS.equals(name)) { 
						return doLoadThumbBitmap(holder);
						
					} else 
						return null;
				}
				
				@Override 
				public boolean isShouldRecycleBitmaps() { 
					return isShouldRecycleBitmaps();
				}
				
				@Override 
				public boolean isShouldRecycle(String name) { 
					return isShouldRecycleBitmap(name);
				}
				
				@Override
				public int getExpectedBitmapWidth(String name) { 
					return Utilities.getDisplaySize(getStorage().getManager().getContext(), 96); 
				}
				
				@Override
				public int getExpectedBitmapHeight(String name) { 
					return Utilities.getDisplaySize(getStorage().getManager().getContext(), 96); 
				}
			};
		
		mFactory = new FileCacheFactory() {
				@Override
				public FileCache create() {
					return new ImageCache(ImageMediaFile.this, mLoader); 
				}
			};
	}
	
	protected boolean isShouldRecycleBitmaps() { 
		return false;
	}
	
	protected boolean isShouldRecycleBitmap(String name) { 
		return false;
	}
	
	@Override 
	public MimeType getMimeType() {
		return MimeType.TYPE_IMAGE; 
	}
	
	@Override 
	protected FileCacheFactory getCacheFactory() { 
		return mFactory; 
	}
	
	protected ImageCache getImageCache() { 
		return (ImageCache)getCache(); 
	}
	
	private BitmapRef doLoadFullsizeBitmap(BitmapHolder holder) {
		final FileLoader loader = getStorage().getLoader(); 
		
		FileCache data = getCache(); 
		if (data == null) 
			return null; 
		
		return loader.loadBitmap(holder, data); 
	}
	
	private BitmapRef doLoadPreviewBitmap(BitmapHolder holder) {
		final FileLoader loader = getStorage().getLoader(); 
		
		FileCache data = getCache(); 
		if (data == null) 
			return null; 
		
		BitmapRef preview = null; 
		
		if (preview == null) {
			BitmapRef bitmap = loader.loadBitmap(holder, data); 
			
			if (bitmap != null) {
				preview = BitmapUtil.createPreviewBitmap(getStorage(), bitmap); 
				
				if (bitmap != preview) 
					bitmap.recycle(); 
			}
		}

		return preview; 
	}
	
	private BitmapRef doLoadThumbBitmap(BitmapHolder holder) {
		final FileLoader loader = getStorage().getLoader(); 
		
		FileCache data = getCache(); 
		if (data == null) 
			return null; 
		
		BitmapRef thumb = null; 
		
		if (thumb == null) {
			BitmapRef bitmap = loader.loadBitmap(holder, data); 
			
			if (bitmap != null) {
				thumb = BitmapUtil.createSmallBitmap(getStorage(), bitmap); 
				
				if (bitmap != thumb) 
					bitmap.recycle(); 
			}
		}

		return thumb; 
	}
	
	@Override 
	public BitmapFile getBitmapFile() { 
		return getImageCache().getBitmapFile(); 
	}
	
	//@Override 
	//public void recycle() {
	//	if (mRecycled) return; 
	//	
	//	FileCache data = getCache(); 
	//	if (data != null) 
	//		data.recycle(); 
	//	
	//	mRecycled = true; 
	//}
	
	//@Override 
	//public boolean isRecycled() {
	//	return mRecycled; 
	//}
	
}
