package org.javenstudio.common.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class Log {

	private static SimpleDateFormat sTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	public static final int LOG_LEVEL_I = 1; 
	public static final int LOG_LEVEL_E = 2; 
	public static final int LOG_LEVEL_W = 3; 
	public static final int LOG_LEVEL_D = 4; 
	public static final int LOG_LEVEL_V = 5; 
	
	public static interface LogImpl {
		public void log(int level, String tag, String msg, Throwable tr); 
	}
	
	private static final class DelegateLogger { 
		private LogImpl mLogImpl = null;
		private String mLogTag = "LOG"; 
		private boolean mLogDebug = false; 
		
		public boolean isLoggable(String tag, int level) { 
			return mLogDebug || (level != LOG_LEVEL_D && level != LOG_LEVEL_V); 
		}
		
		public void log(int level, String tag, String msg) { 
			log(level, tag, msg, (Throwable)null);
		}
		
		public void log(int level, String tag, String msg, Throwable tr) { 
			LogImpl impl = mLogImpl;
			if (impl != null) impl.log(level, tag, msg, tr); 
			LogHistory.getInstance().log(level, tag, msg, tr); 
		}
	}
	
	private static final DelegateLogger sLogger = new DelegateLogger();
	private static DelegateLogger getLogger() {
		return sLogger; 
	}
	
	public static void setLogImpl(LogImpl impl) { 
		if (impl != null && impl != getLogger().mLogImpl) 
			getLogger().mLogImpl = impl;
	}
	
	public static void setLogDebug(boolean debug) { 
		getLogger().mLogDebug = debug;
	}
	
	public static boolean getLogDebug() { 
		return getLogger().mLogDebug;
	}
	
	public static void setLogTag(String tag) { 
		if (tag != null && tag.length() > 0)
			getLogger().mLogTag = tag;
	}
	
	public static String getLogTag() { 
		return getLogger().mLogTag;
	}
	
	public static boolean isLoggable(String tag, int level) {
		return getLogger().isLoggable(tag, level); 
	}
	
	public static void i(String tag, String msg) {
		getLogger().log(LOG_LEVEL_I, tag, msg); 
	}
	
	public static void i(String tag, String msg, Throwable tr) {
		getLogger().log(LOG_LEVEL_I, tag, msg, tr); 
	}
	
	public static void e(String tag, String msg) {
		getLogger().log(LOG_LEVEL_E, tag, msg); 
	}
	
	public static void e(String tag, String msg, Throwable tr) {
		getLogger().log(LOG_LEVEL_E, tag, msg, tr); 
	}
	
	public static void w(String tag, String msg) {
		getLogger().log(LOG_LEVEL_W, tag, msg); 
	}
	
	public static void w(String tag, String msg, Throwable tr) {
		getLogger().log(LOG_LEVEL_W, tag, msg, tr); 
	}
	
	public static void d(String tag, String msg) {
		getLogger().log(LOG_LEVEL_D, tag, msg); 
	}
	
	public static void d(String tag, String msg, Throwable tr) {
		getLogger().log(LOG_LEVEL_D, tag, msg, tr); 
	}
	
	public static void d(String tag, String msg, Object obj) {
		d(tag, msg, obj != null ? obj.getClass() : null);
	}
	
	public static void d(String tag, String msg, Class<?> clazz) {
		getLogger().log(LOG_LEVEL_D, tag, "DEBUG " + 
				sTimeFormat.format(new Date(System.currentTimeMillis())) + " " + clazz + ": " + msg); 
	}
	
	public static void v(String tag, String msg) {
		getLogger().log(LOG_LEVEL_V, tag, msg); 
	}
	
	public static void v(String tag, String msg, Throwable tr) {
		getLogger().log(LOG_LEVEL_V, tag, msg, tr); 
	}
	
}
