package org.javenstudio.cocoka.util;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

public interface BitmapCacheFile {

	public Bitmap loadFullsizeBitmap(); 
	
	public Bitmap getBitmap(String name, BitmapCacheLoader loader);
	public Drawable getBitmapDrawable(String name, BitmapCacheLoader loader);
	
	public Drawable getFullsizeDrawable(int width, int height);
	//public Drawable getThumbnailsDrawable(int padding);
	
	public Bitmap getFullsizeBitmap(); 
	public Bitmap getPreviewBitmap(); 
	public Bitmap getThumbnailsBitmap(); 
	
	public Drawable getFullsizeDrawable(); 
	public Drawable getPreviewDrawable(); 
	public Drawable getThumbnailsDrawable(); 
	
	public void recycleFullsizeBitmap(); 
	public void recyclePreviewBitmap(); 
	public void recycleThumbnailsBitmap(); 
	public void recycleBitmap(String name); 
	
	public int getCachedCount(); 
	public long getCachedSize(); 
	
	public void recycle(); 
	public boolean isRecycled(String name); 
	
}
