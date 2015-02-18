package org.javenstudio.cocoka.widget.activity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

import org.javenstudio.cocoka.Constants;
import org.javenstudio.cocoka.android.ResourceContext;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.common.util.Log;

public abstract class BaseResources {

	public static BaseResources newInstance(Context context, String className) { 
		// Pull in the actual implementation of the TalkConfiguration at run-time
        try {
            Class<?> clazz = Class.forName(className);
            BaseResources instance = (BaseResources)clazz.newInstance();
            if (instance != null) { 
            	instance.initialize(context); 
            	return instance; 
            }
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(
            		className + " could not be loaded", ex);
        } catch (InstantiationException ex) {
            throw new RuntimeException(
            		className + " could not be instantiated", ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(
            		className + " could not be instantiated", ex);
        }
        
        throw new RuntimeException(className + " could not be instantiated");
	}
	
	protected void initialize(Context context) { 
		// do nothing
	}
	
	private static DateFormat sTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static String formatDateTime(long date) { 
		return formatDateTime(new Date(date)); 
	}
	
	public static String formatDateTime(Date date) { 
		return sTimeFormat.format(date); 
	}
	
	public static ResourceContext getResourceContext() { 
		return ResourceHelper.getResourceContext(); 
	}
	
	public static String getString(int resid) {
		if (resid != 0) 
			return getResourceContext().getString(resid); 
		else
			return null; 
	}
	
	public static Drawable getDrawable(int resid) {
		if (resid != 0) 
			return getResourceContext().getDrawable(resid); 
		else
			return null; 
	}
	
	public static int getDisplaySize(int hdpiSize) {
		return getDisplaySize(ResourceHelper.getContext().getResources(), hdpiSize); 
	}
	
	public static int getDisplaySize(Resources res, int hdpiSize) {
		final DisplayMetrics dm = res.getDisplayMetrics(); 
		return Utilities.getDisplaySize(dm.densityDpi, hdpiSize); 
	}
	
	public static float getDisplaySizeF(float hdpiSize) {
		return getDisplaySizeF(ResourceHelper.getContext().getResources(), hdpiSize); 
	}
	
	public static float getDisplaySizeF(Resources res, float hdpiSize) {
		final DisplayMetrics dm = res.getDisplayMetrics(); 
		return Utilities.getDisplaySizeF(dm.densityDpi, hdpiSize); 
	}
	
	public static int toInt(Integer num) { 
		return num != null ? num.intValue() : 0; 
	}
	
	public static long toLong(Long num) { 
		return num != null ? num.longValue() : 0; 
	}
	
	public static String getPreferenceString(String key, String defValue) { 
		try { 
			return ResourceHelper.getSharedPreferences().getString(key, defValue); 
		} catch (Exception ex) { 
			Log.w(Constants.getTag(), "preference field: "+key+" error", ex);
		}
		return defValue;
	}
	
	public static void setPreference(String key, String value) { 
		try { 
			SharedPreferences.Editor editor = ResourceHelper.getSharedPreferences().edit(); 
			editor.putString(key, value); 
			editor.commit(); 
		} catch (Exception ex) { 
			Log.w(Constants.getTag(), "preference field: "+key+" error", ex);
		}
	}
	
	public static boolean getPreferenceBoolean(String key, boolean defValue) { 
		try { 
			return ResourceHelper.getSharedPreferences().getBoolean(key, defValue); 
		} catch (Exception ex) { 
			Log.w(Constants.getTag(), "preference field: "+key+" error", ex);
		}
		return defValue;
	}
	
	public static void setPreference(String key, boolean value) { 
		try { 
			SharedPreferences.Editor editor = ResourceHelper.getSharedPreferences().edit(); 
			editor.putBoolean(key, value); 
			editor.commit(); 
		} catch (Exception ex) { 
			Log.w(Constants.getTag(), "preference field: "+key+" error", ex);
		}
	}
	
	public static int getPreferenceInt(String key, int defValue) { 
		try { 
			return ResourceHelper.getSharedPreferences().getInt(key, defValue); 
		} catch (Exception ex) { 
			Log.w(Constants.getTag(), "preference field: "+key+" error", ex);
		}
		return defValue;
	}
	
	public static void setPreference(String key, int value) { 
		try { 
			SharedPreferences.Editor editor = ResourceHelper.getSharedPreferences().edit(); 
			editor.putInt(key, value); 
			editor.commit(); 
		} catch (Exception ex) { 
			Log.w(Constants.getTag(), "preference field: "+key+" error", ex);
		}
	}
	
	public static long getPreferenceLong(String key, long defValue) { 
		try { 
			return ResourceHelper.getSharedPreferences().getLong(key, defValue); 
		} catch (Exception ex) { 
			Log.w(Constants.getTag(), "preference field: "+key+" error", ex);
		}
		return defValue;
	}
	
	public static void setPreference(String key, long value) { 
		try { 
			SharedPreferences.Editor editor = ResourceHelper.getSharedPreferences().edit(); 
			editor.putLong(key, value); 
			editor.commit(); 
		} catch (Exception ex) { 
			Log.w(Constants.getTag(), "preference field: "+key+" error", ex);
		}
	}
	
	public static float getPreferenceFloat(String key, float defValue) { 
		try { 
			return ResourceHelper.getSharedPreferences().getFloat(key, defValue); 
		} catch (Exception ex) { 
			Log.w(Constants.getTag(), "preference field: "+key+" error", ex);
		}
		return defValue;
	}
	
	public static void setPreference(String key, float value) { 
		try { 
			SharedPreferences.Editor editor = ResourceHelper.getSharedPreferences().edit(); 
			editor.putFloat(key, value); 
			editor.commit(); 
		} catch (Exception ex) { 
			Log.w(Constants.getTag(), "preference field: "+key+" error", ex);
		}
	}
	
}
