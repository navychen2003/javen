package org.javenstudio.android.data.image;

import java.io.File;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.graphics.BitmapRegionDecoder;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import org.javenstudio.android.app.TouchHelper;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.data.IMediaDetails;
import org.javenstudio.cocoka.util.BitmapHelper;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.util.ImageFile;
import org.javenstudio.cocoka.util.MediaUtils;
import org.javenstudio.cocoka.util.MimeType;
import org.javenstudio.cocoka.worker.job.Job;
import org.javenstudio.common.util.Logger;

public abstract class Image implements ImageFile, ImageBitmap.ImageItem {
	private static final Logger LOG = Logger.getLogger(Image.class);
	
	public interface ImageDetails { 
		public void getDetails(IMediaDetails details);
		public String getShareText();
		public String getShareType();
	}
	
	private final List<WeakReference<Drawable>> mDrawables = 
			new ArrayList<WeakReference<Drawable>>(); 
	
	private final List<WeakReference<ImageListener>> mListeners = 
			new ArrayList<WeakReference<ImageListener>>(); 
	
	private ImageDetails mDetails = null;
	public void setImageDetails(ImageDetails details) { mDetails = details; }
	public ImageDetails getImageDetails() { return mDetails; }
	
	private final long mIdentity = ResourceHelper.getIdentity();
	public final long getIdentity() { return mIdentity; }
	
	public abstract Job<BitmapRef> requestImage(BitmapHolder holder, int type);
	public abstract boolean existBitmap();
	public abstract void setBitmapVisible(boolean visible);
	
    public final Job<BitmapRef> requestThumbnail(BitmapHolder holder) { 
    	return requestImage(holder, BitmapHelper.TYPE_THUMBNAIL);
    }
	
    public Job<BitmapRegionDecoder> requestLargeImage(BitmapHolder holder) {
    	return null;
    }
    
    public final Drawable getThumbnailDrawable(int width, int height) {
    	return getThumbnailDrawable(width, height, 0, 0, 0, 0);
    }
    
    public final Drawable getThumbnailDrawable(int width, int height, int padding) {
    	return getThumbnailDrawable(width, height, padding, padding, padding, padding);
    }
    
    public final Drawable getThumbnailDrawable(int width, int height, 
    		int paddingLeft, int paddingTop, int paddingRight, int paddingBottom) { 
    	return getDrawable(BitmapHelper.TYPE_THUMBNAIL, width, height, 
    			paddingLeft, paddingTop, paddingRight, paddingBottom);
    }
    
    public final Drawable getRoundThumbnailDrawable(int width, int height) {
    	return getRoundThumbnailDrawable(width, height, 0, 0, 0, 0);
    }
    
    public final Drawable getRoundThumbnailDrawable(int width, int height, 
    		int paddingLeft, int paddingTop, int paddingRight, int paddingBottom) { 
    	return getDrawable(BitmapHelper.TYPE_ROUNDTHUMBNAIL, width, height, 
    			paddingLeft, paddingTop, paddingRight, paddingBottom);
    }
    
    private final Drawable getDrawable(int type, int width, int height, 
    		int paddingLeft, int paddingTop, int paddingRight, int paddingBottom) { 
		Drawable d = createDrawable(type, width, height, 
				paddingLeft, paddingTop, paddingRight, paddingBottom);
		addDrawable(d);
		TouchHelper.addListener(d);
		return d;
	}
	
	protected Drawable createDrawable(int type, int width, int height, 
			int paddingLeft, int paddingTop, int paddingRight, int paddingBottom) { 
		return getBitmapFile().getDrawable(
				ImageHelper.toCustomName(type, width, height), 
				null, width, height, 
				paddingLeft, paddingTop, paddingRight, paddingBottom);
	}
	
	private void addDrawable(Drawable d) { 
    	synchronized (mDrawables) { 
    		boolean found = false; 
    		for (int i=0; i < mDrawables.size(); ) { 
    			WeakReference<Drawable> ref = mDrawables.get(i); 
    			Drawable drawable = ref != null ? ref.get() : null; 
    			if (drawable == null) { 
    				mDrawables.remove(i); continue; 
    			} else if (drawable == d) 
    				found = true; 
    			i ++; 
    		}
    		if (!found && d != null) 
    			mDrawables.add(new WeakReference<Drawable>(d)); 
    	}
    }
    
	public final void invalidateDrawables() { 
    	synchronized (mDrawables) { 
    		for (int i=0; i < mDrawables.size(); ) { 
    			WeakReference<Drawable> ref = mDrawables.get(i); 
    			Drawable drawable = ref != null ? ref.get() : null; 
    			if (drawable == null) { 
    				mDrawables.remove(i); continue; 
    			} else { 
    				drawable.invalidateSelf(); 
    			}
    			i ++; 
    		}
    	}
    }
	
	public final void addListener(ImageListener listener) { 
    	synchronized (mListeners) { 
    		boolean found = false; 
    		for (int i=0; i < mListeners.size(); ) { 
    			WeakReference<ImageListener> ref = mListeners.get(i); 
    			ImageListener lsr = ref != null ? ref.get() : null; 
    			if (lsr == null) { 
    				mListeners.remove(i); continue; 
    			} else if (lsr == listener) 
    				found = true; 
    			i ++; 
    		}
    		if (!found && listener != null) 
    			mListeners.add(new WeakReference<ImageListener>(listener)); 
    	}
    }
    
    protected final void dispatchEvent(ImageEvent event) { 
    	synchronized (mListeners) { 
    		for (int i=0; i < mListeners.size(); ) { 
    			WeakReference<ImageListener> ref = mListeners.get(i); 
    			ImageListener listener = ref != null ? ref.get() : null; 
    			if (listener == null) { 
    				mListeners.remove(i); continue; 
    			} else { 
    				listener.onImageEvent(this, event);
    			}
    			i ++; 
    		}
    	}
    }
	
	@Override 
	public MimeType getMimeType() { 
		return MimeType.TYPE_IMAGE; 
	}
    
	@Override 
	public long getContentLength() { return 0; }
    
	@Override 
	public InputStream openFile() { return null; }
	
	public long getDateInMs() { return 0; }
	
	public void getDetails(IMediaDetails details) { 
		ImageDetails id = mDetails;
		if (id != null) id.getDetails(details);
	}
	
	public void getExifs(IMediaDetails details) {}
	public Drawable getProviderIcon() { return null; }
	
	public String getShareText() { return null; }
	public String getShareType() { return MediaUtils.MIME_TYPE_IMAGE; }
	
	public Uri getContentUri() { 
		try { 
			return Uri.fromFile(new File(getFilePath()));
		} catch (Throwable e) { 
			if (LOG.isWarnEnabled())
				LOG.warn("getContentUri: error: " + e.toString(), e);
			return null;
		}
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{location=" + getLocation() + "}";
	}
	
}
