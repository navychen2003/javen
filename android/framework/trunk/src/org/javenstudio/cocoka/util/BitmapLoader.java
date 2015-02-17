package org.javenstudio.cocoka.util;

import android.graphics.drawable.Drawable;

public interface BitmapLoader {
	
	public BitmapHolder getBitmapHolder(String name);
	
	public BitmapRef loadBitmap(BitmapHolder holder, String name); 
	public Drawable getDefaultDrawable(String name); 
	
	public int getExpectedBitmapWidth(String name); 
	public int getExpectedBitmapHeight(String name); 
	
	public boolean isShouldRecycle(String name);
	public boolean isShouldRecycleBitmaps();
	public long getRefreshDelayMillis();
	
}
