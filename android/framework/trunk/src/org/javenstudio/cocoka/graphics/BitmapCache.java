package org.javenstudio.cocoka.graphics;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import org.javenstudio.cocoka.util.BitmapFile;
import org.javenstudio.cocoka.util.BitmapLoader;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.common.util.Logger;

public final class BitmapCache {
	private static Logger LOG = Logger.getLogger(BitmapCache.class);
	private static final boolean DEBUG = BaseDrawable.DEBUG;

	private static final BitmapCache sInstance = new BitmapCache(); 
	public static final BitmapCache getInstance() { 
		return sInstance; 
	}
	
	public static final String BITMAP_FULLSIZE = "bitmap_fullsize"; 
	public static final String BITMAP_PREVIEW = "bitmap_preview"; 
	public static final String BITMAP_THUMBNAILS = "bitmap_thumbnails"; 
	
	private class CacheItem { 
		private final CacheFile mFile; 
		private final String mName; 
		private final List<WeakReference<CacheDrawable>> mDrawables; 
		private BitmapRef mBitmap; 
		private long mGetTime = 0;
		
		public CacheItem(CacheFile file, String name) { 
			mDrawables = new ArrayList<WeakReference<CacheDrawable>>(); 
			mFile = file; 
			mName = name; 
			mBitmap = null; 
		}
		
		public BitmapRef get() { 
			synchronized (this) { 
				mGetTime = System.currentTimeMillis();
				return mBitmap; 
			}
		}
		
		public void set(BitmapRef bitmap) { 
			synchronized (this) { 
				if (bitmap == mBitmap) 
					return; 
				
				recycle(); 
				mBitmap = bitmap; 
				logW(mName, this, "cached", mFile.mLocation); 
			}
		}
		
		@SuppressWarnings("unused")
		public void removeReference(CacheDrawable d) { 
			synchronized (this) { 
				for (int i=0; i < mDrawables.size();) { 
					WeakReference<CacheDrawable> ref = mDrawables.get(i); 
					CacheDrawable drawable = ref != null ? ref.get() : null; 
					if (drawable == null || d == drawable) { 
						mDrawables.remove(i); 
						continue; 
					}
					i ++; 
				}
			}
		}
		
		public void addReference(CacheDrawable d) { 
			synchronized (this) { 
				boolean found = false; 
				for (int i=0; i < mDrawables.size();) { 
					WeakReference<CacheDrawable> ref = mDrawables.get(i); 
					CacheDrawable drawable = ref != null ? ref.get() : null; 
					if (drawable == null) { 
						mDrawables.remove(i); 
						continue; 
					} else if (d == drawable) 
						found = true; 
					i ++; 
				}
				if (!found && d != null) 
					mDrawables.add(new WeakReference<CacheDrawable>(d)); 
			}
		}
		
		@SuppressWarnings("unused")
		private int getReferenceCount() { 
			synchronized (this) { 
				for (int i=0; i < mDrawables.size();) { 
					WeakReference<CacheDrawable> ref = mDrawables.get(i); 
					CacheDrawable drawable = ref != null ? ref.get() : null; 
					if (drawable == null) { 
						mDrawables.remove(i); 
						continue; 
					}
					i ++; 
				}
				return mDrawables.size(); 
			}
		}
		
		@SuppressWarnings("unused")
		private boolean hasCallback() { 
			synchronized (this) { 
				boolean result = false;
				for (int i=0; i < mDrawables.size();) { 
					WeakReference<CacheDrawable> ref = mDrawables.get(i); 
					CacheDrawable drawable = ref != null ? ref.get() : null; 
					if (drawable == null) { 
						mDrawables.remove(i); 
						continue; 
					} else if (drawable.hasCallback())
						result = true;
					i ++; 
				}
				return result; 
			}
		}
		
		public void invalidateDrawables() { 
			synchronized (this) { 
				for (int i=0; i < mDrawables.size();) { 
					WeakReference<CacheDrawable> ref = mDrawables.get(i); 
					Drawable drawable = ref != null ? ref.get() : null; 
					if (drawable == null) { 
						mDrawables.remove(i); 
						continue; 
					} else 
						drawable.invalidateSelf(); 
					i ++; 
				}
			}
		}
		
		@SuppressWarnings("unused")
		public void refreshDrawables() { 
			synchronized (this) { 
				for (int i=0; i < mDrawables.size();) { 
					WeakReference<CacheDrawable> ref = mDrawables.get(i); 
					CacheDrawable drawable = ref != null ? ref.get() : null; 
					if (drawable == null) { 
						mDrawables.remove(i); 
						continue; 
					} else 
						drawable.refreshSelf(); 
					i ++; 
				}
			}
		}
		
		public boolean isRecycled() { 
			synchronized (this) { 
				return mBitmap == null || mBitmap.isRecycled(); 
			}
		}
		
		public void recycle() { 
			synchronized (this) { 
				if (mBitmap != null && !mBitmap.isRecycled()) { 
					mBitmap.recycle(); 
					logW(mName, this, "recycled", mFile.mLocation); 
				}
			}
		}
		
		public void recycleIfNecessary() { 
			if (isShouldRecycle()) 
				recycle(); 
		}
		
		public boolean isShouldRecycle() { 
			if (System.currentTimeMillis() - mGetTime < 50) 
				return false;
			
			if (mFile.mLoader.isShouldRecycle(mName)) 
				return true;
			
			//if (getReferenceCount() <= 0) 
			//	return true;
			
			//if (!hasCallback()) 
			//	return true;
			
			return false;
		}
		
	}
	
	public static abstract class Loader implements BitmapLoader { 
		@Override 
		public boolean isShouldRecycle(String name) { 
			return false;
		}
		
		@Override 
		public Drawable getDefaultDrawable(String name) { 
			return null; 
		}
		
		@Override
		public long getRefreshDelayMillis() { 
			return 0;
		}
	}
	
	public class CacheFile implements BitmapFile { 
		private final Map<String, CacheItem> mBitmaps; 
		private final String mLocation; 
		private final BitmapLoader mLoader; 
		private final Object mLoadLock = new Object(); 
		
		public CacheFile(String location, BitmapLoader loader) { 
			if (location == null || loader == null) throw new NullPointerException(); 
			
			mBitmaps = new HashMap<String, CacheItem>(); 
			mLocation = location; 
			mLoader = loader; 
		}
		
		public final String getLocation() { 
			return mLocation; 
		}
		
		public long getRefreshDelayMillis() { 
			long ms = mLoader.getRefreshDelayMillis();
			return ms > 0 ? ms : 0;
		}
		
		@Override
		public BitmapRef getFullsizeBitmap() { 
			return getBitmap(BITMAP_FULLSIZE); 
		}
		
		@Override
		public BitmapRef getPreviewBitmap() { 
			return getBitmap(BITMAP_PREVIEW); 
		}
		
		@Override
		public BitmapRef getThumbnailsBitmap() { 
			return getBitmap(BITMAP_THUMBNAILS); 
		}
		
		@Override
		public BitmapRef getBitmap(String name) {
			return getBitmap(name, (BitmapLoader)null); 
		}
		
		@Override
		public BitmapRef getBitmap(String name, BitmapLoader loader) {
			return loadBitmap(name, loader); 
		}
		
		@Override
		public BitmapRef loadFullsizeBitmap() { 
			return mLoader.loadBitmap(
					mLoader.getBitmapHolder(BitmapCache.BITMAP_FULLSIZE), 
					BitmapCache.BITMAP_FULLSIZE); 
		}
		
		public BitmapRef loadBitmap(String name, BitmapLoader loader) {
			if (name == null) return null; 
			
			synchronized (mLoadLock) { 
				synchronized (this) {
					CacheItem bitmapRef = mBitmaps.get(name); 
					if (bitmapRef != null && !bitmapRef.isRecycled()) 
						return bitmapRef.get(); 
				} 
				
				BitmapRef bitmap = null; 
				
				if (loader != null) 
					bitmap = loader.loadBitmap(loader.getBitmapHolder(name), name); 
				
				if (bitmap == null || bitmap.isRecycled()) 
					bitmap = mLoader.loadBitmap(mLoader.getBitmapHolder(name), name); 
				
				if (bitmap != null && !bitmap.isRecycled()) { 
					synchronized (this) {
						getCacheItem(name).set(bitmap); 
						
						return bitmap; 
					}
				}
				
				return null; 
			}
		}
		
		public BitmapRef getExpectedBitmap(String name, BitmapLoader loader) { 
			return getExpectedBitmap(name, loader, false); 
		}
		
		public BitmapRef getExpectedBitmap(String name, BitmapLoader loader, boolean loadBitmap) { 
			if (name == null) return null; 
			
			synchronized (this) {
				CacheItem bitmapRef = mBitmaps.get(name); 
				if (bitmapRef != null) { 
					BitmapRef bitmap = bitmapRef.get(); 
					if (bitmap != null) {
						if (!bitmap.isRecycled())
							return bitmap; 
					}
				}
				
				if (loadBitmap) 
					return loadBitmap(name, loader);
				
				return null; 
			}
		}
		
		public Drawable getDefaultDrawable(String name, BitmapLoader loader) { 
			if (name == null) return null; 
			
			Drawable def = null; 
			if (loader != null) 
				def = loader.getDefaultDrawable(name); 
			
			if (def == null) 
				def = mLoader.getDefaultDrawable(name); 
			
			return def; 
		}
		
		public int getExpectedBitmapWidth(String name, BitmapLoader loader) { 
			if (name == null) return 0; 
			
			synchronized (this) {
				CacheItem bitmapRef = mBitmaps.get(name); 
				if (bitmapRef != null) { 
					BitmapRef bitmap = bitmapRef.get(); 
					if (bitmap != null) 
						return bitmap.getWidth(); 
				}
				
				if (loader != null) 
					return loader.getExpectedBitmapWidth(name); 
				else 
					return mLoader.getExpectedBitmapWidth(name); 
			}
		}
		
		public int getExpectedBitmapHeight(String name, BitmapLoader loader) { 
			if (name == null) return 0; 
			
			synchronized (this) {
				CacheItem bitmapRef = mBitmaps.get(name); 
				if (bitmapRef != null) { 
					BitmapRef bitmap = bitmapRef.get(); 
					if (bitmap != null) 
						return bitmap.getHeight(); 
				}
				
				if (loader != null) 
					return loader.getExpectedBitmapHeight(name); 
				else 
					return mLoader.getExpectedBitmapHeight(name); 
			}
		}
		
		public CacheItem getCacheItem(String name) {
			return getCacheItem(name, true); 
		}
		
		private CacheItem getCacheItem(String name, boolean create) {
			if (name == null) return null; 
			
			synchronized (this) {
				CacheItem bitmapRef = mBitmaps.get(name); 
				if (bitmapRef == null && create) { 
					bitmapRef = new CacheItem(this, name); 
					mBitmaps.put(name, bitmapRef); 
				}
				return bitmapRef; 
			}
		}
		
		public void invalidateDrawables() { 
			synchronized (this) { 
				for (String name : mBitmaps.keySet()) { 
					CacheItem ref = getCacheItem(name, false); 
					if (ref != null) 
						ref.invalidateDrawables(); 
				}
			}
		}
		
		@Override
		public void recycle() {
			synchronized (this) {
				for (String name : mBitmaps.keySet()) {
					recycleBitmap(name, false); 
				}
				mBitmaps.clear(); 
			}
		}
		
		@Override
		public boolean isRecycled(String name) { 
			if (name == null) return true; 
			
			synchronized (this) {
				CacheItem bitmapRef = getCacheItem(name); 
				return bitmapRef == null || bitmapRef.isRecycled(); 
			}
		}
		
		@Override
		public void recycleFullsizeBitmap() {
			recycleBitmap(BITMAP_FULLSIZE); 
		}
		
		@Override
		public void recyclePreviewBitmap() {
			recycleBitmap(BITMAP_PREVIEW); 
		}
		
		@Override
		public void recycleThumbnailsBitmap() {
			recycleBitmap(BITMAP_THUMBNAILS); 
		}
		
		@Override
		public void recycleBitmap(String name) {
			recycleBitmap(name, true); 
		}
		
		public void recycleBitmap(String name, boolean remove) {
			if (name == null) return; 
			
			synchronized (this) {
				CacheItem bitmapRef = getCacheItem(name); 
				if (bitmapRef != null) { 
					bitmapRef.recycle(); 
				
					if (remove)
						mBitmaps.remove(name); 
				}
			}
		}
		
		public CacheDrawable getFullsizeDrawable() {
			return getFullsizeDrawable(0); 
		}
		
		public CacheDrawable getFullsizeDrawable(final int padding) {
			return getFullsizeDrawable(padding, padding, padding, padding); 
		}
		
		public CacheDrawable getFullsizeDrawable(final int paddingLeft, final int paddingTop, final int paddingRight, final int paddingBottom) {
			return getDrawable(BITMAP_FULLSIZE, null, paddingLeft, paddingTop, paddingRight, paddingBottom); 
		}
		
		@Override
		public CacheDrawable getFullsizeDrawable(final int width, final int height) {
			return getFullsizeDrawable(width, height, 0); 
		}
		
		public CacheDrawable getFullsizeDrawable(final int width, final int height, final int padding) {
			return getFullsizeDrawable(width, height, padding, padding, padding, padding); 
		}
		
		public CacheDrawable getFullsizeDrawable(final int width, final int height, 
				final int paddingLeft, final int paddingTop, final int paddingRight, final int paddingBottom) {
			return getDrawable(BITMAP_FULLSIZE, null, width, height, 
					paddingLeft, paddingTop, paddingRight, paddingBottom); 
		}
		
		public CacheDrawable getPreviewDrawable() {
			return getPreviewDrawable(0); 
		}
		
		public CacheDrawable getPreviewDrawable(int padding) {
			return getPreviewDrawable(padding, padding, padding, padding); 
		}
		
		public CacheDrawable getPreviewDrawable(final int paddingLeft, final int paddingTop, final int paddingRight, final int paddingBottom) {
			return getDrawable(BITMAP_PREVIEW, null, paddingLeft, paddingTop, paddingRight, paddingBottom); 
		}
		
		public CacheDrawable getPreviewDrawable(final int width, final int height) {
			return getPreviewDrawable(width, height, 0); 
		}
		
		public CacheDrawable getPreviewDrawable(final int width, final int height, int padding) {
			return getPreviewDrawable(width, height, padding, padding, padding, padding); 
		}
		
		public CacheDrawable getPreviewDrawable(final int width, final int height, 
				final int paddingLeft, final int paddingTop, final int paddingRight, final int paddingBottom) {
			return getDrawable(BITMAP_PREVIEW, null, width, height, 
					paddingLeft, paddingTop, paddingRight, paddingBottom); 
		}
		
		public CacheDrawable getThumbnailsDrawable() {
			return getThumbnailsDrawable(0); 
		}
		
		public CacheDrawable getThumbnailsDrawable(final int padding) {
			return getThumbnailsDrawable(padding, padding, padding, padding); 
		}
		
		public CacheDrawable getThumbnailsDrawable(final int paddingLeft, final int paddingTop, final int paddingRight, final int paddingBottom) {
			return getDrawable(BITMAP_THUMBNAILS, null, paddingLeft, paddingTop, paddingRight, paddingBottom); 
		}
		
		public CacheDrawable getThumbnailsDrawable(final int width, final int height) {
			return getThumbnailsDrawable(width, height, 0); 
		}
		
		public CacheDrawable getThumbnailsDrawable(final int width, final int height, final int padding) {
			return getThumbnailsDrawable(width, height, padding, padding, padding, padding); 
		}
		
		public CacheDrawable getThumbnailsDrawable(final int width, final int height, 
				final int paddingLeft, final int paddingTop, final int paddingRight, final int paddingBottom) {
			return getDrawable(BITMAP_THUMBNAILS, null, width, height, 
					paddingLeft, paddingTop, paddingRight, paddingBottom); 
		}
		
		@Override
		public CacheDrawable getDrawable(final String name) {
			return getDrawable(name, (BitmapLoader)null, 0); 
		}
		
		public CacheDrawable getDrawable(final String name, final BitmapLoader loader) {
			return getDrawable(name, loader, 0); 
		}
		
		public CacheDrawable getDrawable(final String name, final BitmapLoader loader, final int padding) {
			return getDrawable(name, loader, padding, padding, padding, padding); 
		}
		
		public CacheDrawable getDrawable(final String name, final BitmapLoader loader, 
				final int paddingLeft, final int paddingTop, final int paddingRight, final int paddingBottom) { 
			return getDrawable(name, loader, 0, 0, paddingLeft, paddingTop, paddingRight, paddingBottom); 
		}
		
		public CacheDrawable getDrawable(final String name, final BitmapLoader loader, 
				final int width, final int height) {
			return getDrawable(name, loader, width, height, 0); 
		}
		
		public CacheDrawable getDrawable(final String name, final BitmapLoader loader, 
				final int width, final int height, final int padding) {
			return getDrawable(name, loader, width, height, padding, padding, padding, padding); 
		}
		
		public CacheDrawable getDrawable(final String name, final BitmapLoader loader, 
				final int width, final int height, 
				final int paddingLeft, final int paddingTop, final int paddingRight, final int paddingBottom) {
			final Rect paddingRect = new Rect(paddingLeft, paddingTop, paddingRight, paddingBottom); 
			
			final BitmapGetterImpl getter = new BitmapGetterImpl(this, 
					name, loader, width, height, paddingRect); 

			return createCacheDrawable(name, getter); 
		}
		
		private CacheDrawable createCacheDrawable(final String name, BitmapGetterImpl getter) { 
			CacheDrawable d = new CacheDrawable(this, name, getter); 
			if (isRecycled(name)) 
				d.setBackground(getter.getDefaultDrawable()); 
			
			return d; 
		}
	}
	
	private class BitmapGetterImpl implements DelegatedHelper.BitmapGetter { 
		private final CacheFile mCacheFile; 
		private final String mBitmapName; 
		private final BitmapLoader mBitmapLoader; 
		private final int mBitmapWidth; 
		private final int mBitmapHeight; 
		private final Rect mPaddingRect; 
		private BitmapRef mBitmap = null;
		
		public BitmapGetterImpl(CacheFile file, String name, BitmapLoader loader, 
				int width, int height, Rect paddingRect) { 
			mCacheFile = file; 
			mBitmapName = name; 
			mBitmapLoader = loader; 
			mBitmapWidth = width; 
			mBitmapHeight = height; 
			mPaddingRect = paddingRect; 
		}

		private BitmapRef getBitmap(boolean load) { 
			BitmapRef bitmap = mBitmap;
			if (bitmap != null && !bitmap.isRecycled()) 
				return bitmap;
			
			bitmap = mCacheFile.getExpectedBitmap(mBitmapName, mBitmapLoader, load);
			mBitmap = bitmap;
			
			return bitmap;
		}
		
		private Drawable getDefaultDrawable() { 
			return mCacheFile.getDefaultDrawable(mBitmapName, mBitmapLoader); 
		}
		
		@Override
		public BitmapRef getExpectedBitmap() {
			return getBitmap(true); 
		}
		
		@Override
		public int getExpectedBitmapWidth() { 
			return mBitmapWidth > 0 ? mBitmapWidth : 
				mCacheFile.getExpectedBitmapWidth(mBitmapName, mBitmapLoader); 
		}
		
		@Override
		public int getExpectedBitmapHeight() { 
			return mBitmapHeight > 0 ? mBitmapHeight : 
				mCacheFile.getExpectedBitmapHeight(mBitmapName, mBitmapLoader); 
		}
		
		@Override
		public boolean isRecycled() {
			BitmapRef bitmap = getBitmap(false);
			return bitmap == null || bitmap.isRecycled();
			//return mCacheFile.isRecycled(mBitmapName); 
		}
		
		@Override 
		public void recycle() {
			mCacheFile.recycleBitmap(mBitmapName); 
		}
		
		@Override 
		public Rect getPaddingRect() { 
			return mPaddingRect; 
		}
	}
	
	public class CacheDrawable extends DelegatedBitmapDrawable { 
		@SuppressWarnings("unused")
		private final CacheFile mCacheFile; 
		private final BitmapGetterImpl mBitmapGetter; 
		@SuppressWarnings("unused")
		private final String mBitmapName; 
		private final CacheItem mCacheItem; 
		
		private CacheDrawable(CacheFile file, String name, BitmapGetterImpl getter) { 
			super(DelegatedHelper.createBitmap(getter)); 
			
			mCacheFile = file; 
			mBitmapGetter = getter; 
			mBitmapName = name; 
			mCacheItem = file.getCacheItem(name); 
			
			setOwner(file); 
			mCacheItem.addReference(this); 
			
			//if (DEBUG && LOG.isDebugEnabled())
			//	LOG.debug("CacheDrawable: created: " + this);
		}
		
		@Override 
		public boolean isRecycled() { 
			return mBitmapGetter.isRecycled(); 
		}
		
		@Override 
		public void recycle() { 
			mBitmapGetter.recycle(); 
		}
	}
	
	private final Map<String, CacheFile> mCacheFiles; 
	
	private BitmapCache() {
		mCacheFiles = new HashMap<String, CacheFile>(); 
	} 
	
	public final CacheFile getOrNull(final String location) { 
		if (location == null || location.length() == 0) 
			throw new IllegalArgumentException("bitmap location is null"); 
		
		synchronized (mCacheFiles) { 
			return mCacheFiles.get(location); 
		}
	}
	
	public final CacheFile getOrCreate(final String location, final BitmapLoader loader) { 
		if (location == null || location.length() == 0) 
			throw new IllegalArgumentException("bitmap location is null"); 
		
		synchronized (mCacheFiles) { 
			CacheFile file = mCacheFiles.get(location); 
			if (file == null) { 
				file = new CacheFile(location, loader); 
				mCacheFiles.put(location, file); 
			}
			return file; 
		}
	}
	
	public void recycleBitmapsIfNecessary() { 
		if (BitmapRef.getTotalBitmapSize() > 16 * 1024 * 1024)
			recycleUnusedBitmaps();
	}
	
	private void recycleUnusedBitmaps() { 
		synchronized (mCacheFiles) { 
			if (DEBUG && LOG.isDebugEnabled())
				LOG.debug("recycleUnusedBitmaps: count=" + mCacheFiles.size());
			
			for (Map.Entry<String, CacheFile> cacheEntry : mCacheFiles.entrySet()) { 
				final CacheFile file = cacheEntry.getValue();
				if (file == null) continue;
				
				synchronized (file) { 
					for (Map.Entry<String, CacheItem> fileEntry : file.mBitmaps.entrySet()) { 
						final CacheItem item = fileEntry.getValue();
						if (item != null) 
							item.recycleIfNecessary();
					}
				}
			}
		}
	}
	
	private void logW(String name, CacheItem bitmap, String title, String location) {
		//if (DEBUG && LOG.isDebugEnabled() && name != null && bitmap != null) { 
			//LOG.debug("BitmapCache: " + title + " " + name + 
			//		": size=" + bitmap.getWidth() + "x" + bitmap.getHeight() + " location=" + location); 
		//}
	}
	
}
