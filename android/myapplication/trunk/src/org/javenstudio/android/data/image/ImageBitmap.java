package org.javenstudio.android.data.image;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import org.javenstudio.android.app.TouchHelper;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.graphics.BitmapCache;
import org.javenstudio.cocoka.util.BitmapHelper;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.worker.job.Job;
import org.javenstudio.cocoka.worker.job.JobCancelListener;
import org.javenstudio.cocoka.worker.job.JobContext;

public class ImageBitmap extends BitmapCache.Loader 
		implements BitmapHolder, JobContext {
	//private static final Logger LOG = Logger.getLogger(ImageBitmap.class);
	
	public static interface ImageItem { 
		public Job<BitmapRef> requestImage(BitmapHolder holder, int type);
		public boolean isLocalItem();
		public long getIdentity();
		public String getLocation();
	}
	
	private static class BitmapVersion { 
		public final BitmapRef bitmap;
		public final long version;
		public BitmapVersion(BitmapRef bitmap, long version) { 
			this.bitmap = bitmap;
			this.version = version;
		}
	}
	
	private final ImageItem mImage; 
	private final Map<String, BitmapVersion> mVersions;
	private boolean mVisible = true;
	
	public ImageBitmap(ImageItem image) { 
		if (image == null) throw new NullPointerException();
		mImage = image; 
		mVersions = new HashMap<String, BitmapVersion>();
	}
	
	@Override
	public BitmapHolder getBitmapHolder(String name) { 
		return this;
	}
	
	public void setBitmapVisible(boolean visible) { 
		synchronized (mVersions) {
			//boolean changed = visible != mVisible;
			mVisible = visible;
			//if (!visible) mVersions.clear();
			
			//if (changed && LOG.isDebugEnabled()) { 
			//	LOG.debug("setBitmapVisible: changed, id=" + mImage.getIdentity() 
			//			+ " location=" + mImage.getLocation() + " visible=" + visible);
			//}
		}
	}
	
	@Override
	public BitmapRef loadBitmap(BitmapHolder holder, String name) {
		if (BitmapCache.BITMAP_FULLSIZE.equals(name)) 
			return null; 
		
		int[] widthHeight = ImageHelper.getCustomTypeWidthHeight(name);
		BitmapRef bitmap = null;
		
		if (widthHeight != null && widthHeight.length == 3) {
			synchronized (mVersions) {
				BitmapVersion version = mVersions.get(name);
				if (version != null) { 
					// load failed or reuse
					if (version.bitmap == null && mImage.isLocalItem()) { 
						//if (LOG.isDebugEnabled()) {
						//	LOG.debug("loadBitmap: local bitmap is null, id=" 
						//			+ mImage.getIdentity() + " name=" + name);
						//}
						
						return null;
					}
					
					if (version.bitmap != null) { 
						if (!version.bitmap.isRecycled())
							return version.bitmap;
						
						//if (LOG.isDebugEnabled()) {
						//	LOG.debug("loadBitmap: cached bitmap recycled, id=" 
						//			+ mImage.getIdentity() + " name=" + name 
						//			+ " bitmap=" + version.bitmap);
						//}
					}
				}
			}
			
			int type = widthHeight[0];
			int width = widthHeight[1];
			int height = widthHeight[2];
			
			Job<BitmapRef> job = mImage.requestImage(holder, type);
			if (job != null) { 
				BitmapRef original = TouchHelper.loadBitmap(this, job);
				if (original != null) {
					if (type == BitmapHelper.TYPE_ROUNDTHUMBNAIL) { 
						bitmap = ImageHelper.createRoundBitmap(
								holder, original, width, height);
					} else { 
						bitmap = ImageHelper.createCustomBitmap(
								holder, original, width, height);
					}
					if (bitmap != original)
						original.recycle();
				}
			} else { 
				//if (LOG.isDebugEnabled()) { 
				//	LOG.debug("loadBitmap: request image is null, id=" 
				//			+ mImage.getIdentity() + " name=" + name + " type=" + type);
				//}
			}
			
			synchronized (mVersions) {
				boolean touching = TouchHelper.isMotionTouching();
				if (bitmap != null || !touching) {
					mVersions.put(name, new BitmapVersion(bitmap, 
							ImageHelper.getImageVersion()));
					
				}
			}
			
		} else { 
			//if (LOG.isDebugEnabled()) { 
			//	LOG.debug("loadBitmap: wrong bitmap name, id=" 
			//			+ mImage.getIdentity() + " name=" + name);
			//}
		}
		
		return bitmap;
	}
	
	@Override 
	public boolean isShouldRecycleBitmaps() { 
		return !TouchHelper.isMotionTouching();
	}
	
	@Override
	public boolean isShouldRecycle(String name) { 
		synchronized (mVersions) {
			BitmapVersion version = mVersions.get(name);
			if (version != null) { 
				boolean visible = mVisible;
				boolean touching = TouchHelper.isMotionTouching();
				boolean result = !visible && !touching;
				
				if (version.bitmap == null || version.bitmap.isRecycled()) { 
					mVersions.remove(name);
					
					//if (LOG.isDebugEnabled()) {
					//	LOG.debug("isShouldRecycle: already recycled, id=" 
					//			+ mImage.getIdentity() + " name=" + name 
					//			+ " visible=" + visible);
					//}
					
					return true;
				}
				
				long cacheVersion = ImageHelper.getImageVersion();
				if (version.version != cacheVersion)
					result = true;
				
				//if (visible) result = false;
				
				//if (LOG.isDebugEnabled()) {
				//	LOG.debug("isShouldRecycle: return " + result 
				//			+ ", id=" + mImage.getIdentity() + " name=" + name 
				//			+ " version=" + version.version + " current=" + cacheVersion 
				//			+ " visible=" + visible + " touching=" + touching);
				//}
				
				return result;
			}
			
			return true;
		}
	}
	
	@Override
	public int getExpectedBitmapWidth(String name) { 
		return ImageHelper.getExpectedBitmapWidth(name); 
	}
	
	@Override
	public int getExpectedBitmapHeight(String name) { 
		return ImageHelper.getExpectedBitmapHeight(name); 
	}

	@Override
	public Context getContext() {
		return ResourceHelper.getContext();
	}

	@Override
	public void addBitmap(BitmapRef bitmap) {
		// do nothing
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public void setCancelListener(JobCancelListener listener) {
		// do nothing
	}

	@Override
	public boolean setMode(int mode) {
		return false;
	}
	
}
