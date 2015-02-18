package org.javenstudio.android.data.image;

import java.util.concurrent.atomic.AtomicLong;

import android.content.Context;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.graphics.BitmapUtil;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.common.util.Logger;

public class ImageHelper {
	private static final Logger LOG = Logger.getLogger(ImageHelper.class);

	public static final String CUSTOM_PREFIX = "bitmap_custom_";
	
	private static final AtomicLong sImageVersion = new AtomicLong(0);
	
	public static long increaseImageVersion(Context activity) { 
		long version = sImageVersion.incrementAndGet();
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("increaseImageVersion: version=" + version);
		
		return version;
	}
	
	public static long getImageVersion() { 
		return sImageVersion.get();
	}
	
	public static String toCustomName(int type, int width, int height) { 
		return CUSTOM_PREFIX + type + "_" + width + "_" + height;
	}
	
	public static int[] getCustomTypeWidthHeight(String name) { 
		if (name != null && name.startsWith(CUSTOM_PREFIX)) { 
			String[] ss = name.split("_");
			if (ss != null && ss.length == 5) { 
				try {
					int type = Integer.parseInt(ss[2]);
					int width = Integer.parseInt(ss[3]);
					int height = Integer.parseInt(ss[4]);
					
					return new int[]{type, width, height};
				} catch (Throwable ex) { 
					// ignore
				}
			}
		}
		
		return null;
	}
	
	public static BitmapRef createPreviewBitmap(BitmapHolder holder, 
			BitmapRef bitmap) { 
		if (bitmap != null) { 
			BitmapRef preview = BitmapUtil.createPreviewBitmap(holder, bitmap); 
    		if (preview != bitmap) 
    			bitmap.recycle(); 
    		
    		return preview; 
    	}
		
    	return null; 
	}
	
	public static BitmapRef createThumbBitmap(BitmapHolder holder, 
			BitmapRef bitmap) { 
		if (bitmap != null) { 
			BitmapRef thumb = BitmapUtil.createSmallBitmap(holder, bitmap); 
    		if (thumb != bitmap) 
    			bitmap.recycle(); 
    		
    		return thumb; 
    	}
		
    	return null;
	}
	
	public static BitmapRef createCustomBitmap(BitmapHolder holder, 
			BitmapRef bitmap, int width, int height) { 
		if (bitmap != null) { 
			if (width <= 0 || height <= 0) 
				return null;
			
			BitmapRef thumb = BitmapUtil.createScaledCropBitmap(
    				holder, bitmap, width, height, 0, true); 
    		
    		if (thumb != bitmap) 
    			bitmap.recycle(); 
    		
    		return thumb; 
    	}
		
		return null; 
	}
	
	public static BitmapRef createRoundBitmap(BitmapHolder holder, 
			BitmapRef bitmap, int width, int height) { 
		if (bitmap != null) { 
			float size = width;
			if (height < size) size = height;
			if (size <= 0) return null;
			
			BitmapRef thumb = BitmapUtil.createRoundBitmap(holder, bitmap); 
    		
    		if (thumb != bitmap) 
    			bitmap.recycle(); 
    		
    		return thumb; 
    	}
		
		return null; 
	}
	
	public static int getExpectedBitmapWidth(String name) { 
		return Utilities.getDisplaySize(ResourceHelper.getContext(), 96); 
	}
	
	public static int getExpectedBitmapHeight(String name) { 
		return Utilities.getDisplaySize(ResourceHelper.getContext(), 96); 
	}
	
}
