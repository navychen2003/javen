package org.javenstudio.android.data.media;

import android.graphics.BitmapRegionDecoder;
import android.net.Uri;

import org.javenstudio.android.data.DataPath;
import org.javenstudio.android.data.image.ImageBitmap;
import org.javenstudio.cocoka.util.BitmapHelper;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.worker.job.Job;

public abstract class MediaItem extends MediaObject 
		implements ImageBitmap.ImageItem {

    public static final int CACHED_IMAGE_QUALITY = 95;

    public static final int IMAGE_READY = 0;
    public static final int IMAGE_WAIT = 1;
    public static final int IMAGE_ERROR = -1;
	
	public MediaItem(DataPath path, long version) { 
		super(path, version);
	}
	
	public abstract Uri getContentUri();
	public Uri getPlayUri() { return null; }
	
    // Returns width and height of the media item.
    // Returns 0, 0 if the information is not available.
    public abstract int getWidth();
    public abstract int getHeight();
    
    public abstract String getMimeType();
    public abstract int getMediaType();
    
    public abstract Job<BitmapRef> requestImage(BitmapHolder holder, int type);
    public abstract Job<BitmapRegionDecoder> requestLargeImage(BitmapHolder holder);
    
    public final Job<BitmapRef> requestThumbnail(BitmapHolder holder) { 
    	return requestImage(holder, BitmapHelper.TYPE_THUMBNAIL);
    }
    
	@Override
	public boolean isLocalItem() { return false; }
    
    public long getDateInMs() { return 0; }
    public int getRotation() { return 0; }
    public long getSize() { return 0; }
    
    public int getSupportedOperations() { 
    	if (isLocalItem()) return SUPPORT_DELETE;
    	return 0; 
    }
    
}
