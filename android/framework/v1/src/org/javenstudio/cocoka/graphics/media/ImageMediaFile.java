package org.javenstudio.cocoka.graphics.media;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import org.javenstudio.cocoka.graphics.BitmapCache;
import org.javenstudio.cocoka.graphics.BitmapUtil;
import org.javenstudio.cocoka.storage.FileCache;
import org.javenstudio.cocoka.storage.FileCacheFactory;
import org.javenstudio.cocoka.storage.FileLoader;
import org.javenstudio.cocoka.storage.Storage;
import org.javenstudio.cocoka.storage.fs.IFile;
import org.javenstudio.cocoka.storage.media.BaseMediaFile;
import org.javenstudio.cocoka.util.BitmapCacheFile;
import org.javenstudio.cocoka.util.BitmapLoader;
import org.javenstudio.cocoka.util.ImageFile;
import org.javenstudio.cocoka.util.MimeType;

public class ImageMediaFile extends BaseMediaFile implements ImageFile {
	
	private final BitmapLoader mLoader; 
	private final FileCacheFactory mFactory; 
	private boolean mRecycled = false; 
	
	public ImageMediaFile(Storage store, IFile file) {
		super(store, file); 
		
		mLoader = new BitmapLoader() {
				@Override
				public Bitmap loadBitmap(String name) {
					if (BitmapCache.BITMAP_FULLSIZE.equals(name)) { 
						return doLoadFullsizeBitmap();
						
					} else if (BitmapCache.BITMAP_PREVIEW.equals(name)) { 
						return doLoadPreviewBitmap();
						
					} else if (BitmapCache.BITMAP_THUMBNAILS.equals(name)) { 
						return doLoadThumbBitmap();
						
					} else 
						return null;
				}
				
				@Override 
				public Drawable getDefaultDrawable(String name) { 
					return null; 
				}
				
				@Override
				public int getExpectedBitmapWidth(String name) { 
					//if (BitmapCache.BITMAP_FULLSIZE.equals(name)) 
					//	return 1024; 
					//else if (BitmapCache.BITMAP_PREVIEW.equals(name)) 
					//	return 320; 
					//else if (BitmapCache.BITMAP_THUMB.equals(name)) 
					//	return 96; 
					//else
						return 96; 
				}
				
				@Override
				public int getExpectedBitmapHeight(String name) { 
					//if (BitmapCache.BITMAP_FULLSIZE.equals(name)) 
					//	return 1024; 
					//else if (BitmapCache.BITMAP_PREVIEW.equals(name)) 
					//	return 320; 
					//else if (BitmapCache.BITMAP_THUMB.equals(name)) 
					//	return 96; 
					//else
						return 96; 
				}
			};
		
		mFactory = new FileCacheFactory() {
				@Override
				public FileCache create() {
					return new ImageCache(ImageMediaFile.this, mLoader); 
				}
			};
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
	
	private Bitmap doLoadFullsizeBitmap() {
		final FileLoader loader = getStorage().getLoader(); 
		
		FileCache data = getCache(); 
		if (data == null) 
			return null; 
		
		return loader.loadBitmap(data); 
	}
	
	private Bitmap doLoadPreviewBitmap() {
		final FileLoader loader = getStorage().getLoader(); 
		
		FileCache data = getCache(); 
		if (data == null) 
			return null; 
		
		Bitmap preview = null; 
		
		if (preview == null) {
			Bitmap bitmap = loader.loadBitmap(data); 
			
			if (bitmap != null) {
				preview = BitmapUtil.createPreviewBitmap(getStorage().getManager().getContext(), bitmap); 
				
				if (bitmap != preview) 
					bitmap.recycle(); 
			}
		}

		return preview; 
	}
	
	private Bitmap doLoadThumbBitmap() {
		final FileLoader loader = getStorage().getLoader(); 
		
		FileCache data = getCache(); 
		if (data == null) 
			return null; 
		
		Bitmap thumb = null; 
		
		if (thumb == null) {
			Bitmap bitmap = loader.loadBitmap(data); 
			
			if (bitmap != null) {
				thumb = BitmapUtil.createSmallBitmap(getStorage().getManager().getContext(), bitmap); 
				
				if (bitmap != thumb) 
					bitmap.recycle(); 
			}
		}

		return thumb; 
	}
	
	@Override 
	public BitmapCacheFile getBitmapFile() { 
		return getImageCache().getBitmapFile(); 
	}
	
	@Override 
	public void recycle() {
		if (mRecycled) return; 
		
		FileCache data = getCache(); 
		if (data != null) 
			data.recycle(); 
		
		mRecycled = true; 
	}
	
	@Override 
	public boolean isRecycled() {
		return mRecycled; 
	}
	
}
