package org.javenstudio.cocoka.util;

import android.graphics.drawable.Drawable;

public interface BitmapFile {

	public BitmapRef loadFullsizeBitmap(); 
	
	public BitmapRef getBitmap(String name);
	public BitmapRef getBitmap(String name, BitmapLoader loader);
	
	public Drawable getDrawable(String name);
	public Drawable getDrawable(String name, BitmapLoader loader);
	public Drawable getDrawable(String name, BitmapLoader loader, int width, int height);
	public Drawable getDrawable(String name, BitmapLoader loader, int width, int height, 
			int paddingLeft, int paddingTop, int paddingRight, int paddingBottom);
	
	public BitmapRef getFullsizeBitmap(); 
	public BitmapRef getPreviewBitmap(); 
	public BitmapRef getThumbnailsBitmap(); 
	
	public Drawable getFullsizeDrawable(); 
	public Drawable getFullsizeDrawable(int width, int height); 
	
	public Drawable getPreviewDrawable(); 
	public Drawable getPreviewDrawable(int width, int height); 
	
	public Drawable getThumbnailsDrawable(); 
	public Drawable getThumbnailsDrawable(int width, int height); 
	
	public void recycleFullsizeBitmap(); 
	public void recyclePreviewBitmap(); 
	public void recycleThumbnailsBitmap(); 
	public void recycleBitmap(String name); 
	
	public void recycle(); 
	public boolean isRecycled(String name); 
	
}
