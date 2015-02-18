package org.javenstudio.cocoka.android;

import java.lang.reflect.Method;

import android.content.Context;

public interface ModuleContext {

	public Context getPackageContext();
	public void initModule(Class<?> mainClass, Context moduleContext);
	public void onResourceChanged(Context packageContext, String className);
	public Method getStaticMethod(String className, String methodName);
	
}
