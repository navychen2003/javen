package org.javenstudio.cocoka.graphics;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

import org.javenstudio.cocoka.android.ActivityHelper;
import org.javenstudio.cocoka.util.BitmapCacheFile;
import org.javenstudio.cocoka.util.BitmapCacheLoader;
import org.javenstudio.cocoka.util.BitmapLoader;
import org.javenstudio.cocoka.util.ImageRefreshable;
import org.javenstudio.common.util.Logger;

public final class BitmapCache {
	private static Logger LOG = Logger.getLogger(BitmapCache.class);

	private static final BitmapCache sInstance = new BitmapCache(); 
	public static final BitmapCache getInstance() { 
		return sInstance; 
	}
	
	public static final String BITMAP_FULLSIZE = "bitmap_fullsize"; 
	public static final String BITMAP_PREVIEW = "bitmap_preview"; 
	public static final String BITMAP_THUMBNAILS = "bitmap_thumbnails"; 
	
	private static final long AUTORECYCLE_PERIOD_TIME = 5 * 60 * 1000; // 60s
	
	private class BitmapRef { 
		private final BitmapFile mFile; 
		private final String mName; 
		private final List<WeakReference<BitmapDrawable>> mDrawables; 
		private Bitmap mBitmap; 
		private boolean mAutoRecycle = true; 
		private long mTimestamp; 
		
		public BitmapRef(BitmapFile file, String name) { 
			mDrawables = new ArrayList<WeakReference<BitmapDrawable>>(); 
			mFile = file; 
			mName = name; 
			mBitmap = null; 
		}
		
		public Bitmap get() { 
			synchronized (this) { 
				mTimestamp = SystemClock.elapsedRealtime(); 
				return mBitmap; 
			}
		}
		
		public void set(Bitmap bitmap) { 
			synchronized (this) { 
				if (bitmap == mBitmap) 
					return; 
				
				recycle(); 
				mBitmap = bitmap; 
				mTimestamp = SystemClock.elapsedRealtime(); 
				
				logW(mName, this, "cached", mFile.mLocation); 
			}
		}
		
		public void removeReference(BitmapDrawable d) { 
			synchronized (this) { 
				for (int i=0; i < mDrawables.size();) { 
					WeakReference<BitmapDrawable> ref = mDrawables.get(i); 
					BitmapDrawable drawable = ref != null ? ref.get() : null; 
					if (drawable == null || d == drawable) { 
						mDrawables.remove(i); 
						continue; 
					}
					i ++; 
				}
			}
		}
		
		public void addReference(BitmapDrawable d) { 
			synchronized (this) { 
				boolean found = false; 
				for (int i=0; i < mDrawables.size();) { 
					WeakReference<BitmapDrawable> ref = mDrawables.get(i); 
					BitmapDrawable drawable = ref != null ? ref.get() : null; 
					if (drawable == null) { 
						mDrawables.remove(i); 
						continue; 
					} else if (d == drawable) 
						found = true; 
					i ++; 
				}
				if (!found && d != null) 
					mDrawables.add(new WeakReference<BitmapDrawable>(d)); 
			}
		}
		
		private int getReferenceCount() { 
			synchronized (this) { 
				for (int i=0; i < mDrawables.size();) { 
					WeakReference<BitmapDrawable> ref = mDrawables.get(i); 
					BitmapDrawable drawable = ref != null ? ref.get() : null; 
					if (drawable == null) { 
						mDrawables.remove(i); 
						continue; 
					}
					i ++; 
				}
				return mDrawables.size(); 
			}
		}
		
		public void invalidateDrawables() { 
			synchronized (this) { 
				for (int i=0; i < mDrawables.size();) { 
					WeakReference<BitmapDrawable> ref = mDrawables.get(i); 
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
		
		public void refreshDrawables() { 
			synchronized (this) { 
				for (int i=0; i < mDrawables.size();) { 
					WeakReference<BitmapDrawable> ref = mDrawables.get(i); 
					BitmapDrawable drawable = ref != null ? ref.get() : null; 
					if (drawable == null) { 
						mDrawables.remove(i); 
						continue; 
					} else 
						drawable.refreshSelf(); 
					i ++; 
				}
			}
		}
		
		@SuppressWarnings({"unused"})
		public void setAutoRecycle(boolean b) { 
			synchronized (this) { 
				mAutoRecycle = b; 
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
			if (getReferenceCount() <= 0) 
				return true; 
			
			if (mAutoRecycle && this.mTimestamp > 0) { 
				long elapsed = BitmapCache.this.mTimestamp - this.mTimestamp; 
				if (elapsed > AUTORECYCLE_PERIOD_TIME) 
					return true; 
			}
			
			return false; 
		}
		
		public int getWidth() { 
			synchronized (this) { 
				return mBitmap != null ? mBitmap.getWidth() : 0; 
			}
		}
		
		public int getHeight() { 
			synchronized (this) { 
				return mBitmap != null ? mBitmap.getHeight() : 0; 
			}
		}
	}
	
	public class BitmapFile implements BitmapCacheFile { 
		private final Map<String, BitmapRef> mBitmaps; 
		private final String mLocation; 
		private final BitmapLoader mLoader; 
		private final Object mLoadLock = new Object(); 
		
		public BitmapFile(String location, BitmapLoader loader) { 
			mBitmaps = new HashMap<String, BitmapRef>(); 
			mLocation = location; 
			mLoader = loader; 
			
			if (loader == null) 
				throw new NullPointerException("BitmapLoader is null for "+location); 
		}
		
		public final String getLocation() { 
			return mLocation; 
		}
		
		@Override
		public Bitmap getFullsizeBitmap() { 
			return getBitmap(BITMAP_FULLSIZE); 
		}
		
		@Override
		public Bitmap getPreviewBitmap() { 
			return getBitmap(BITMAP_PREVIEW); 
		}
		
		@Override
		public Bitmap getThumbnailsBitmap() { 
			return getBitmap(BITMAP_THUMBNAILS); 
		}
		
		public Bitmap getBitmap(String name) {
			return getBitmap(name, (BitmapCacheLoader)null); 
		}
		
		@Override
		public Bitmap getBitmap(String name, BitmapCacheLoader loader) {
			return loadBitmap(name, loader); 
		}
		
		@Override
		public Bitmap loadFullsizeBitmap() { 
			return mLoader.loadBitmap(BitmapCache.BITMAP_FULLSIZE); 
		}
		
		public void refreshBitmaps() { 
			synchronized (mLoadLock) { 
				synchronized (this) {
					for (BitmapRef bitmapRef : mBitmaps.values()) { 
						bitmapRef.get();
					}
				}
			}
		}
		
		public Bitmap loadBitmap(String name, BitmapCacheLoader loader) {
			if (name == null) return null; 
			
			synchronized (mLoadLock) { 
				synchronized (this) {
					BitmapRef bitmapRef = mBitmaps.get(name); 
					if (bitmapRef != null && !bitmapRef.isRecycled()) 
						return bitmapRef.get(); 
				} 
				
				recycleUnusedBitmaps(); 
				
				Bitmap bitmap = null; 
				
				if (loader != null) 
					bitmap = loader.loadBitmap(); 
				
				if (bitmap == null) { 
					bitmap = mLoader.loadBitmap(name); 
				}
				
				if (bitmap != null && !bitmap.isRecycled()) { 
					synchronized (this) {
						getBitmapRef(name).set(bitmap); 
						return bitmap; 
					}
				}
				
				return null; 
			}
		}
		
		public void scheduleLoadBitmap(String name, BitmapCacheLoader loader) { 
			if (name == null) return; 
			
			scheduleLoadBitmapInThread(this, name, loader); 
		}
		
		public Bitmap getExpectedBitmap(String name, BitmapCacheLoader loader) { 
			return getExpectedBitmap(name, loader, false); 
		}
		
		public Bitmap getExpectedBitmap(String name, BitmapCacheLoader loader, boolean scheduleLoad) { 
			if (name == null) return null; 
			
			synchronized (this) {
				BitmapRef bitmapRef = mBitmaps.get(name); 
				if (bitmapRef != null) { 
					Bitmap bitmap = bitmapRef.get(); 
					if (bitmap != null && !bitmap.isRecycled()) 
						return bitmap; 
				}
				
				if (scheduleLoad)
					scheduleLoadBitmap(name, loader); 
				
				return null; 
			}
		}
		
		public Drawable getDefaultDrawable(String name, BitmapCacheLoader loader) { 
			if (name == null) return null; 
			
			Drawable def = null; 
			if (loader != null) 
				def = loader.getDefaultDrawable(); 
			
			if (def == null) 
				def = mLoader.getDefaultDrawable(name); 
			
			return def; 
		}
		
		public int getExpectedBitmapWidth(String name, BitmapCacheLoader loader) { 
			if (name == null) return 0; 
			
			synchronized (this) {
				BitmapRef bitmapRef = mBitmaps.get(name); 
				if (bitmapRef != null) { 
					Bitmap bitmap = bitmapRef.get(); 
					if (bitmap != null) 
						return bitmap.getWidth(); 
				}
				
				if (loader != null) 
					return loader.getExpectedBitmapWidth(); 
				else 
					return mLoader.getExpectedBitmapWidth(name); 
			}
		}
		
		public int getExpectedBitmapHeight(String name, BitmapCacheLoader loader) { 
			if (name == null) return 0; 
			
			synchronized (this) {
				BitmapRef bitmapRef = mBitmaps.get(name); 
				if (bitmapRef != null) { 
					Bitmap bitmap = bitmapRef.get(); 
					if (bitmap != null) 
						return bitmap.getHeight(); 
				}
				
				if (loader != null) 
					return loader.getExpectedBitmapHeight(); 
				else 
					return mLoader.getExpectedBitmapHeight(name); 
			}
		}
		
		public BitmapRef[] getBitmapRefs() { 
			synchronized (this) {
				return mBitmaps.values().toArray(new BitmapRef[0]); 
			}
		}
		
		public String[] getBitmapRefNames() { 
			synchronized (this) {
				return mBitmaps.keySet().toArray(new String[0]); 
			}
		}
		
		public BitmapRef getBitmapRef(String name) {
			return getBitmapRef(name, true); 
		}
		
		private BitmapRef getBitmapRef(String name, boolean create) {
			if (name == null) return null; 
			
			synchronized (this) {
				BitmapRef bitmapRef = mBitmaps.get(name); 
				if (bitmapRef == null && create) { 
					bitmapRef = new BitmapRef(this, name); 
					mBitmaps.put(name, bitmapRef); 
				}
				return bitmapRef; 
			}
		}
		
		public void invalidateDrawables() { 
			String[] names = getBitmapRefNames(); 
			for (int i=0; names != null && i < names.length; i++) { 
				BitmapRef ref = getBitmapRef(names[i], false); 
				if (ref != null) 
					ref.invalidateDrawables(); 
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
				BitmapRef bitmapRef = getBitmapRef(name); 
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
				BitmapRef bitmapRef = getBitmapRef(name); 
				if (bitmapRef != null) { 
					bitmapRef.recycle(); 
				
					if (remove)
						mBitmaps.remove(name); 
				}
			}
		}
		
		public boolean isShouldRecycle() { 
			synchronized (this) {
				for (String name : mBitmaps.keySet()) {
					BitmapRef bitmapRef = mBitmaps.get(name); 
					if (bitmapRef != null && bitmapRef.isShouldRecycle()) 
						return true; 
				}
				return false; 
			}
		}
		
		@Override
		public int getCachedCount() {
			int count = 0; 
			
			synchronized (this) {
				for (String name : mBitmaps.keySet()) {
					BitmapRef bitmapRef = mBitmaps.get(name); 
					if (bitmapRef != null && !bitmapRef.isRecycled()) {
						count += 1; 
					}
				}
			}
			
			return count; 
		}
		
		@Override
		public long getCachedSize() {
			long size = 0; 
			
			synchronized (this) {
				for (String name : mBitmaps.keySet()) {
					BitmapRef bitmapRef = mBitmaps.get(name); 
					if (bitmapRef != null && !bitmapRef.isRecycled()) {
						size += bitmapRef.getWidth() * bitmapRef.getHeight(); 
					}
				}
			}

			return size; 
		}
		
		public BitmapDrawable getFullsizeDrawable() {
			return getFullsizeDrawable(0); 
		}
		
		public BitmapDrawable getFullsizeDrawable(final int padding) {
			return getFullsizeDrawable(padding, padding, padding, padding); 
		}
		
		public BitmapDrawable getFullsizeDrawable(final int paddingLeft, final int paddingTop, final int paddingRight, final int paddingBottom) {
			return getBitmapDrawable(BITMAP_FULLSIZE, null, paddingLeft, paddingTop, paddingRight, paddingBottom); 
		}
		
		@Override
		public BitmapDrawable getFullsizeDrawable(final int width, final int height) {
			return getFullsizeDrawable(width, height, 0); 
		}
		
		public BitmapDrawable getFullsizeDrawable(final int width, final int height, final int padding) {
			return getFullsizeDrawable(width, height, padding, padding, padding, padding); 
		}
		
		public BitmapDrawable getFullsizeDrawable(final int width, final int height, 
				final int paddingLeft, final int paddingTop, final int paddingRight, final int paddingBottom) {
			return getBitmapDrawable(BITMAP_FULLSIZE, null, width, height, 
					paddingLeft, paddingTop, paddingRight, paddingBottom); 
		}
		
		public BitmapDrawable getPreviewDrawable() {
			return getPreviewDrawable(0); 
		}
		
		public BitmapDrawable getPreviewDrawable(int padding) {
			return getPreviewDrawable(padding, padding, padding, padding); 
		}
		
		public BitmapDrawable getPreviewDrawable(final int paddingLeft, final int paddingTop, final int paddingRight, final int paddingBottom) {
			return getBitmapDrawable(BITMAP_PREVIEW, null, paddingLeft, paddingTop, paddingRight, paddingBottom); 
		}
		
		public BitmapDrawable getPreviewDrawable(final int width, final int height) {
			return getPreviewDrawable(width, height, 0); 
		}
		
		public BitmapDrawable getPreviewDrawable(final int width, final int height, int padding) {
			return getPreviewDrawable(width, height, padding, padding, padding, padding); 
		}
		
		public BitmapDrawable getPreviewDrawable(final int width, final int height, 
				final int paddingLeft, final int paddingTop, final int paddingRight, final int paddingBottom) {
			return getBitmapDrawable(BITMAP_PREVIEW, null, width, height, 
					paddingLeft, paddingTop, paddingRight, paddingBottom); 
		}
		
		public BitmapDrawable getThumbnailsDrawable() {
			return getThumbnailsDrawable(0); 
		}
		
		public BitmapDrawable getThumbnailsDrawable(final int padding) {
			return getThumbnailsDrawable(padding, padding, padding, padding); 
		}
		
		public BitmapDrawable getThumbnailsDrawable(final int paddingLeft, final int paddingTop, final int paddingRight, final int paddingBottom) {
			return getBitmapDrawable(BITMAP_THUMBNAILS, null, paddingLeft, paddingTop, paddingRight, paddingBottom); 
		}
		
		public BitmapDrawable getThumbnailsDrawable(final int width, final int height) {
			return getThumbnailsDrawable(width, height, 0); 
		}
		
		public BitmapDrawable getThumbnailsDrawable(final int width, final int height, final int padding) {
			return getThumbnailsDrawable(width, height, padding, padding, padding, padding); 
		}
		
		public BitmapDrawable getThumbnailsDrawable(final int width, final int height, 
				final int paddingLeft, final int paddingTop, final int paddingRight, final int paddingBottom) {
			return getBitmapDrawable(BITMAP_THUMBNAILS, null, width, height, 
					paddingLeft, paddingTop, paddingRight, paddingBottom); 
		}
		
		public BitmapDrawable getBitmapDrawable(final String name, final BitmapCacheLoader loader) {
			return getBitmapDrawable(name, loader, 0); 
		}
		
		public BitmapDrawable getBitmapDrawable(final String name, final BitmapCacheLoader loader, final int padding) {
			return getBitmapDrawable(name, loader, padding, padding, padding, padding); 
		}
		
		public BitmapDrawable getBitmapDrawable(final String name, final BitmapCacheLoader loader, 
				final int paddingLeft, final int paddingTop, final int paddingRight, final int paddingBottom) { 
			return getBitmapDrawable(name, loader, 0, 0, paddingLeft, paddingTop, paddingRight, paddingBottom); 
		}
		
		public BitmapDrawable getBitmapDrawable(final String name, final BitmapCacheLoader loader, 
				final int width, final int height) {
			return getBitmapDrawable(name, loader, width, height, 0); 
		}
		
		public BitmapDrawable getBitmapDrawable(final String name, final BitmapCacheLoader loader, 
				final int width, final int height, final int padding) {
			return getBitmapDrawable(name, loader, width, height, padding, padding, padding, padding); 
		}
		
		public BitmapDrawable getBitmapDrawable(final String name, final BitmapCacheLoader loader, 
				final int width, final int height, 
				final int paddingLeft, final int paddingTop, final int paddingRight, final int paddingBottom) {
			final Rect paddingRect = new Rect(paddingLeft, paddingTop, paddingRight, paddingBottom); 
			
			final BitmapGetterImpl getter = new BitmapGetterImpl(this, 
					name, loader, width, height, paddingRect); 

			return createBitmapDrawable(name, getter); 
		}
		
		private BitmapDrawable createBitmapDrawable(final String name, BitmapGetterImpl getter) { 
			BitmapDrawable d = new BitmapDrawable(this, name, getter); 
			if (isRecycled(name)) 
				d.setBackground(getter.getDefaultDrawable()); 
			
			return d; 
		}
	}
	
	private class BitmapGetterImpl implements DelegatedHelper.BitmapGetter { 
		private final BitmapFile mBitmapFile; 
		private final String mBitmapName; 
		private final BitmapCacheLoader mBitmapLoader; 
		private final int mBitmapWidth; 
		private final int mBitmapHeight; 
		private final Rect mPaddingRect; 
		
		public BitmapGetterImpl(BitmapFile file, String name, BitmapCacheLoader loader, 
				int width, int height, Rect paddingRect) { 
			mBitmapFile = file; 
			mBitmapName = name; 
			mBitmapLoader = loader; 
			mBitmapWidth = width; 
			mBitmapHeight = height; 
			mPaddingRect = paddingRect; 
		}

		public Drawable getDefaultDrawable() { 
			return mBitmapFile.getDefaultDrawable(mBitmapName, mBitmapLoader); 
		}
		
		@Override
		public Bitmap getExpectedBitmap() {
			BitmapCache.this.mTimestamp = SystemClock.elapsedRealtime(); 
			return mBitmapFile.getExpectedBitmap(mBitmapName, mBitmapLoader, true); 
		}
		
		@Override
		public int getExpectedBitmapWidth() { 
			return mBitmapWidth > 0 ? mBitmapWidth : 
				mBitmapFile.getExpectedBitmapWidth(mBitmapName, mBitmapLoader); 
		}
		
		@Override
		public int getExpectedBitmapHeight() { 
			return mBitmapHeight > 0 ? mBitmapHeight : 
				mBitmapFile.getExpectedBitmapHeight(mBitmapName, mBitmapLoader); 
		}
		
		@Override
		public boolean isRecycled() {
			return mBitmapFile.isRecycled(mBitmapName); 
		}
		
		@Override 
		public void recycle() {
			mBitmapFile.recycleBitmap(mBitmapName); 
		}
		
		@Override 
		public Rect getPaddingRect() { 
			return mPaddingRect; 
		}
	}
	
	public class BitmapDrawable extends DelegatedBitmapDrawable { 
		private final BitmapFile mBitmapFile; 
		private final BitmapGetterImpl mBitmapGetter; 
		private final String mBitmapName; 
		private final BitmapRef mBitmapRef; 
		private ImageRefreshable mImageRefresher = null; 
		
		public BitmapDrawable(BitmapFile file, String name, BitmapGetterImpl getter) { 
			super(DelegatedHelper.createBitmap(getter)); 
			
			mBitmapFile = file; 
			mBitmapGetter = getter; 
			mBitmapName = name; 
			mBitmapRef = file.getBitmapRef(name); 
			
			setOwner(file); 
			mBitmapRef.addReference(this); 
		}
		
		public void setImageRefresher(ImageRefreshable refresher) { 
			mImageRefresher = refresher; 
		}
		
		@Override 
	    public void refreshSelf() { 
			ImageRefreshable refresher = mImageRefresher; 
			if (refresher != null) { 
				BitmapDrawable d = mBitmapFile.createBitmapDrawable(mBitmapName, mBitmapGetter); 
				refresher.refreshDrawable(d); 
				mBitmapRef.removeReference(this); 
				return; 
			}
			
			super.refreshSelf(); 
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
	
	private void recycleUnusedBitmaps() { 
		BitmapFile[] files = null; 
		synchronized (this) { 
			files = mBitmapFiles.values().toArray(new BitmapFile[0]); 
		}
		
		for (int i=0; files != null && i < files.length; i++) { 
			BitmapFile file = files[i]; 
			BitmapRef[] refs = file != null ? file.getBitmapRefs() : null; 
			for (int j=0; refs != null && j < refs.length; j++) { 
				BitmapRef ref = refs[j]; 
				if (ref != null) 
					ref.recycleIfNecessary(); 
			}
		}
	}
	
	@SuppressWarnings({"unused"})
	private void scheduleRecycleUnusedBitmaps() { 
		synchronized (mBitmapTasks) { 
			BitmapTask task = new BitmapRecycleTask(); 
			String key = task.getKey(); 
			
			if (!mBitmapTasks.containsKey(key) && !mSortedTasks.containsKey(key)) { 
				mBitmapTasks.put(key, task); 
				mSortedTasks.put(key, task); 
			}
			
			checkBitmapThread();
		}
	}
	
	private interface BitmapTask { 
		public String getKey(); 
		public void execute(); 
		public boolean isExecuting(); 
		public long getScheduleTime(); 
	}
	
	private class BitmapRecycleTask implements BitmapTask { 
		private final long mScheduleTime; 
		private boolean mRecycling = false; 
		
		public BitmapRecycleTask() { 
			mScheduleTime = SystemClock.elapsedRealtime(); 
		}
		
		@Override 
		public String getKey() { 
			return "recycle:"; 
		}
		
		@Override 
		public void execute() { 
			setRecycling(true); 
			
			recycleUnusedBitmaps(); 
			
			setRecycling(false); 
		}
		
		public void setRecycling(boolean doing) { 
			synchronized (this) { 
				mRecycling = doing; 
			}
		}
		
		@Override 
		public boolean isExecuting() { 
			synchronized (this) { 
				return mRecycling; 
			}
		}
		
		@Override 
		public long getScheduleTime() { 
			return mScheduleTime; 
		}
	}
	
	private class BitmapLoadTask implements BitmapTask { 
		private final BitmapFile mBitmapFile; 
		private final String mBitmapName; 
		private final BitmapCacheLoader mBitmapLoader; 
		private final String mScheduleKey; 
		private final long mScheduleTime; 
		private boolean mLoading = false; 
		
		public BitmapLoadTask(BitmapFile file, String name, BitmapCacheLoader loader) { 
			mBitmapFile = file; 
			mBitmapName = name; 
			mBitmapLoader = loader; 
			mScheduleKey = "load:" + mBitmapFile.mLocation + "@" + mBitmapName; 
			mScheduleTime = SystemClock.elapsedRealtime(); 
		}
		
		@Override
		public String getKey() { 
			return mScheduleKey; 
		}
		
		@Override
		public void execute() { 
			setLoading(true); 
			
			if (mBitmapFile.isRecycled(mBitmapName)) { 
				mBitmapFile.loadBitmap(mBitmapName, mBitmapLoader); 
				if (!mBitmapFile.isRecycled(mBitmapName)) 
					postRefreshDrawables(mBitmapFile.getBitmapRef(mBitmapName)); 
			}
			
			setLoading(false); 
		}
		
		public void setLoading(boolean loading) { 
			synchronized (this) { 
				mLoading = loading; 
			}
		}
		
		@Override 
		public boolean isExecuting() { 
			synchronized (this) { 
				return mLoading; 
			}
		}
		
		@Override 
		public long getScheduleTime() { 
			return mScheduleTime; 
		}
	}
	
	private void postRefreshDrawables(final BitmapRef ref) { 
		if (ref == null) return; 
		
		ActivityHelper.getHandler().post(new Runnable() { 
				public void run() { 
					ref.refreshDrawables(); 
				}
			}); 
	}
	
	private final Map<String, BitmapTask> mBitmapTasks = new HashMap<String, BitmapTask>(); 
	private final TreeMap<String, BitmapTask> mSortedTasks = new TreeMap<String, BitmapTask>(
			new Comparator<String>() { 
				public int compare(String a, String b) { 
					return compareTask(a, b); 
				}
			}
		); 
	
	private int compareTask(String a, String b) { 
		if (a == null) 
			return b == null ? 0 : -1; 
		else if (b == null) 
			return 1; 
		
		BitmapTask bta = mBitmapTasks.get(a); 
		BitmapTask btb = mBitmapTasks.get(b); 
		
		return compareTask(bta, btb); 
	}
	
	private int compareTask(BitmapTask a, BitmapTask b) { 
		if (a == null) 
			return b == null ? 0 : -1; 
		else if (b == null) 
			return 1; 
		
		long sta = a.getScheduleTime(); 
		long stb = b.getScheduleTime(); 
		if (sta > stb) 
			return 1; 
		else if (sta < stb) 
			return -1; 
		
		String keya = a.getKey(); 
		String keyb = b.getKey(); 
		
		return keya.compareTo(keyb); 
	}
	
	private void scheduleLoadBitmapInThread(BitmapFile file, String name, BitmapCacheLoader loader) { 
		if (file == null || name == null) return; 
		
		synchronized (mBitmapTasks) { 
			BitmapTask task = new BitmapLoadTask(file, name, loader); 
			String key = task.getKey(); 
			
			if (!mBitmapTasks.containsKey(key) && !mSortedTasks.containsKey(key)) { 
				mBitmapTasks.put(key, task); 
				mSortedTasks.put(key, task); 
			}
			
			checkBitmapThread(); 
		}
	}
	
	private void checkBitmapThread() { 
		synchronized (this) { 
			if (mBitmapThread == null) { 
				mBitmapThread = new BitmapThread(); 
				mBitmapThread.start(); 
			}
		}
	}
	
	private void runBitmapThread() { 
		while (true) { 
			BitmapTask task = null; 
			synchronized (mBitmapTasks) {
				for (String key : mSortedTasks.keySet()) { 
					BitmapTask bt = mSortedTasks.get(key); 
					if (bt != null && !bt.isExecuting()) { 
						task = bt; break; 
					}
				}
			}
			if (task == null) 
				break; 
			
			task.execute(); 
			
			synchronized (mBitmapTasks) {
				mBitmapTasks.remove(task.getKey()); 
				mSortedTasks.remove(task.getKey()); 
			}
		}
		
		synchronized (this) { 
			mBitmapThread = null; 
		}
	}
	
	private class BitmapThread extends Thread { 
		public void run() { 
			runBitmapThread(); 
		}
	}
	
	private final Map<String, BitmapFile> mBitmapFiles; 
	private BitmapThread mBitmapThread = null; 
	private volatile long mTimestamp = 0;  
	
	private BitmapCache() {
		mBitmapFiles = new HashMap<String, BitmapFile>(); 
	} 
	
	public final BitmapFile getOrNull(final String location) { 
		if (location == null || location.length() == 0) 
			throw new IllegalArgumentException("bitmap location is null"); 
		
		synchronized (this) { 
			return mBitmapFiles.get(location); 
		}
	}
	
	public final BitmapFile getOrCreate(final String location, final BitmapLoader loader) { 
		if (location == null || location.length() == 0) 
			throw new IllegalArgumentException("bitmap location is null"); 
		
		synchronized (this) { 
			BitmapFile file = mBitmapFiles.get(location); 
			if (file == null) { 
				file = new BitmapFile(location, loader); 
				mBitmapFiles.put(location, file); 
			}
			return file; 
		}
	}
	
	private void logW(String name, BitmapRef bitmap, String title, String location) {
		if (LOG.isDebugEnabled() && name != null && bitmap != null) { 
			//LOG.debug("BitmapCache: " + title + " " + name + 
			//		": " + bitmap.getWidth() + " * " + bitmap.getHeight() + " location: "+location); 
		}
	}
	
}
