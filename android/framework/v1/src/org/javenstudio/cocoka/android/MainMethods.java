package org.javenstudio.cocoka.android;

import java.lang.reflect.Method;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;

import org.javenstudio.cocoka.Implements;
import org.javenstudio.cocoka.ModuleHelper;
import org.javenstudio.cocoka.android.ModuleManager;

/**
 * Do not change these static functions name
 */
public class MainMethods {

	public static Context getContext() { 
		return Implements.getContext();
	}
	
	public static String getStringKeyWithId(String id) { 
		return Implements.getInstance().getStringKeyWithId(id);
	}
	
	public static String getLocalStorageDirectory() {
		return Implements.getInstance().getLocalStorageDirectory();
	}
	
	public static SharedPreferences getPreferences() { 
		return Implements.getInstance().getPreferences();
	}
	
	public static Method getMethod(String className, String methodName) { 
		return ModuleHelper.getStaticMethod(className, methodName);
	}
	
	public static void registerMethods(Class<?> clazz) { 
		ModuleHelper.registerStaticMethods(clazz);
	}
	
	public static void registerMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) { 
		ModuleHelper.registerStaticMethod(clazz, methodName, parameterTypes);
	}
	
	public static Method getGlobalMethod(String methodName) { 
		return ModuleHelper.getGlobalMethod(methodName);
	}
	
	public static void registerGlobalMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) { 
		ModuleHelper.registerGlobalMethod(clazz, methodName, parameterTypes);
	}
	
	public static Object newModuleClass(String packageName, String className) { 
		return Implements.getModuleManager().createModuleClass(packageName, className);
	}
	
	public static void registerModuleMethod(Object moduleClass, String methodName) { 
		((ModuleManager.ModuleClass)moduleClass).registerMethod(methodName);
	}
	
	public static ResourceContext getResourceContext() { 
		return Implements.getResourceContext();
	}
	
	public static String getString(int resId) {
		return getResourceContext().getString(resId);
	}

	public static String[] getStringArray(int resId) {
		return getResourceContext().getStringArray(resId);
	}

	public static String getQuantityString(int resId, int quantity, Object... formatArgs) {
		return getResourceContext().getQuantityString(resId, quantity, formatArgs);
	}

	public static Drawable getDrawable(int resId) {
		return getResourceContext().getDrawable(resId);
	}

	public static int getColor(int resId) {
		return getResourceContext().getColor(resId);
	}

	public static ColorStateList getColorStateList(int resId) {
		return getResourceContext().getColorStateList(resId);
	}

	public static XmlResourceParser getXml(int resId) {
		return getResourceContext().getXml(resId);
	}

	public static Bitmap decodeBitmap(int resId) {
		return getResourceContext().decodeBitmap(resId);
	}

	public static View inflateView(int resource, ViewGroup root, boolean attachToRoot) {
		return getResourceContext().inflateView(resource, root, attachToRoot);
	}

	public static View findViewById(View view, int resId) {
		return getResourceContext().findViewById(view, resId);
	}

	public static boolean bindView(int viewId, View view, Object... values) {
		return getResourceContext().bindView(viewId, view, values);
	}
	
}
