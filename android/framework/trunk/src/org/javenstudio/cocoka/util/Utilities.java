package org.javenstudio.cocoka.util;

import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.util.DisplayMetrics;

import org.javenstudio.util.StringUtils;

public class Utilities {

	private static final List<WeakReference<OutOfMemoryListener>> mOOMListeners = 
			new ArrayList<WeakReference<OutOfMemoryListener>>(); 
	
	public static final void addOOMListener(OutOfMemoryListener listener) { 
    	synchronized (mOOMListeners) { 
    		boolean found = false; 
    		for (int i=0; i < mOOMListeners.size(); ) { 
    			WeakReference<OutOfMemoryListener> ref = mOOMListeners.get(i); 
    			OutOfMemoryListener lsr = ref != null ? ref.get() : null; 
    			if (lsr == null) { 
    				mOOMListeners.remove(i); continue; 
    			} else if (lsr == listener) 
    				found = true; 
    			i ++; 
    		}
    		if (!found) 
    			mOOMListeners.add(new WeakReference<OutOfMemoryListener>(listener)); 
    	}
    }
    
    static final void dispatchOOM(OutOfMemoryError e) { 
    	synchronized (mOOMListeners) { 
    		for (int i=0; i < mOOMListeners.size(); ) { 
    			WeakReference<OutOfMemoryListener> ref = mOOMListeners.get(i); 
    			OutOfMemoryListener listener = ref != null ? ref.get() : null; 
    			if (listener == null) { 
    				mOOMListeners.remove(i); continue; 
    			} else { 
    				listener.onOutOfMemoryError(e);
    			}
    			i ++; 
    		}
    	}
    }
	
	public static void handleOOM(OutOfMemoryError e) { 
		if (e == null) return;
		
		try {
			BitmapRef.onHandleOOM();
			System.gc();
		} catch (Throwable ex) {
			// ignore
		}
	
		//if (LOG.isErrorEnabled())
		//	LOG.error("load bitmap out of memory: " + e.toString(), e); 
		
		//throw new RuntimeException(e);
		dispatchOOM(e);
	}
	
	public static String toMD5(String val) {
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.update(val.getBytes());
			byte[] m = md5.digest();
			if (m == null) 
				return null;
			
			return StringUtils.byteToHexString(m);
		} catch (NoSuchAlgorithmException e) { 
			return val;
		}
	}
	
	public static <T> boolean isEquals(T a, T b) { 
		if (a == null && b == null) 
			return true;
		
		if (a == null) return false;
		if (b == null) return false;
		
		return a.equals(b);
	}
	
	public static Date parseDate(String text) { 
		return TimeUtils.parseDate(text);
	}
	
	public static long parseTime(String text) { 
		return TimeUtils.parseTime(text);
	}
	
	public static String formatTimeOnly(long time) { 
		return TimeUtils.formatTimeOnly(time);
	}
	
	public static String formatTimeOnly(Date date) { 
		return TimeUtils.formatTimeOnly(date);
	}
	
	public static String formatDate(long time) { 
		return TimeUtils.formatDate(time);
	}
	
	public static String formatDate(Date date) { 
		return TimeUtils.formatDate(date);
	}
	
	public static String formatDate(String time) { 
		return TimeUtils.formatDate(time);
	}
	
	public static String formatSize(long size) { 
		return StringUtils.byteDesc(size);
	}
	
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
    	//int density = dm.densityDpi; 
    	return dm.widthPixels; 
    }
    
	public static int getScreenHeight(Context context) {
    	final DisplayMetrics dm = context.getResources().getDisplayMetrics(); 
    	//int density = dm.densityDpi; 
    	return dm.heightPixels; 
    }
    
	@SuppressWarnings("unused")
	private static int getScreenWidthHdpi(Context context) {
    	final DisplayMetrics dm = context.getResources().getDisplayMetrics(); 
    	int density = dm.densityDpi; 
    	float width = (float)dm.widthPixels * 240.0f / (float)density; 

    	return (int)width; 
    }
    
	@SuppressWarnings("unused")
	private static int getScreenHeightHdpi(Context context) {
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
