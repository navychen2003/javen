package org.javenstudio.cocoka.util;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

public interface BitmapCacheLoader {

	public Bitmap loadBitmap(); 
	public Drawable getDefaultDrawable(); 
	public int getExpectedBitmapWidth(); 
	public int getExpectedBitmapHeight(); 
	
}
