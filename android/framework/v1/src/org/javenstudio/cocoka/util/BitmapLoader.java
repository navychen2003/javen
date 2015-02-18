package org.javenstudio.cocoka.util;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

public interface BitmapLoader {
	
	public Bitmap loadBitmap(String name); 
	public Drawable getDefaultDrawable(String name); 
	public int getExpectedBitmapWidth(String name); 
	public int getExpectedBitmapHeight(String name); 
	
}
