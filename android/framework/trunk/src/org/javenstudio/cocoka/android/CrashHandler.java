package org.javenstudio.cocoka.android;

import android.app.Application;
import android.content.Context;

public final class CrashHandler implements Thread.UncaughtExceptionHandler {
	//private static Logger LOG = Logger.getLogger(CrashHandler.class);

	private final Application mApp; 
	private final Context mContext; 
	private final Thread.UncaughtExceptionHandler mDefaultHandler; 
	
	public CrashHandler(Application app, Context context, boolean isModuleApp) { 
		mApp = app; 
		mContext = context; 
		mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler(); 
		Thread.setDefaultUncaughtExceptionHandler(this); 
	}
	
	public final Application getApplication() { 
		return mApp; 
	}
	
	public final Context getContext() { 
		return mContext; 
	}

	@Override
	public final void uncaughtException(Thread thread, Throwable ex) {
		//if (LOG.isDebugEnabled()) 
		//	LOG.debug("CrashHandler: Thread: "+thread+" uncaughtException: "+ex, ex); 
		
		if (!handleException(thread, ex)) 
			mDefaultHandler.uncaughtException(thread, ex); 
	}
	
	protected boolean handleException(Thread thread, Throwable ex) { 
		return false; 
	}
	
}
