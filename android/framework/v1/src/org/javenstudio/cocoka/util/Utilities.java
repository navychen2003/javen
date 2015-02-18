package org.javenstudio.cocoka.util;

import android.content.Context;
import android.util.DisplayMetrics;

@SuppressWarnings({"unused"})
public class Utilities {

	public static int getDensityDpi(Context context) {
    	return getDensityDpi(context.getResources().getDisplayMetrics()); 
    }
    
	public static int getDensityDpi(DisplayMetrics dm) {
    	int density = DisplayMetrics.DENSITY_DEFAULT; 
    	if (dm != null) 
    		density = dm.densityDpi; 
 
    	return density; 
    }

	public static int getScreenWidth(Context context) {
    	final DisplayMetrics dm = context.getResources().getDisplayMetrics(); 
    	int density = dm.densityDpi; 
    	return dm.widthPixels; 
    }
    
	public static int getScreenHeight(Context context) {
    	final DisplayMetrics dm = context.getResources().getDisplayMetrics(); 
    	int density = dm.densityDpi; 
    	return dm.heightPixels; 
    }
    
	public static int getScreenWidthHdpi(Context context) {
    	final DisplayMetrics dm = context.getResources().getDisplayMetrics(); 
    	int density = dm.densityDpi; 
    	float width = (float)dm.widthPixels * 240.0f / (float)density; 

    	return (int)width; 
    }
    
	public static int getScreenHeightHdpi(Context context) {
    	final DisplayMetrics dm = context.getResources().getDisplayMetrics(); 
    	int density = dm.densityDpi; 
    	float height = (float)dm.heightPixels * 240.0f / (float)density; 

    	return (int)height; 
    }
    
	public static int getDisplaySize(Context context, int hdpiSize) {
    	return (int)getDisplaySizeF(context, hdpiSize); 
	}
	
	public static int getDisplaySize(final int density, int hdpiSize) {
    	return (int)getDisplaySizeF(density, hdpiSize); 
    }
	
	public static float getDisplaySizeF(Context context, float hdpiSize) {
    	final DisplayMetrics dm = context.getResources().getDisplayMetrics(); 
    	int density = dm.densityDpi; 
    	return getDisplaySizeF(density, hdpiSize); 
	}
	
	public static float getDisplaySizeF(final int density, float hdpiSize) {
    	float size = density > 0 ? ((float)hdpiSize * (float)density / 240.0f) : hdpiSize; 

    	return size; 
    }
}
