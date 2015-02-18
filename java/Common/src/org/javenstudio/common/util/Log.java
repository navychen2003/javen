package org.javenstudio.common.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public final class Log {
	private static SimpleDateFormat sTimeFormat = 
			new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	public static final int LOG_LEVEL_I = 1; 
	public static final int LOG_LEVEL_E = 2; 
	public static final int LOG_LEVEL_W = 3; 
	public static final int LOG_LEVEL_D = 4; 
	public static final int LOG_LEVEL_V = 5; 
	
	public static String toString(int level) { 
		switch (level) { 
		case LOG_LEVEL_I:
			return "I";
		case LOG_LEVEL_E:
			return "E";
		case LOG_LEVEL_W:
			return "W";
		case LOG_LEVEL_D:
			return "D";
		case LOG_LEVEL_V:
			return "V";
		}
		return "I";
	}
	
	public static abstract class LogImpl {
		public abstract void log(int level, String tag, String msg, Throwable tr); 
		public boolean isLoggable(String tag, int level) { return true; }
		public boolean isHistoryEnabled() { return false; }
	}
	
	public static abstract class ThreadLogImpl extends LogImpl { 
		private class LogItem { 
			private final int mLevel;
			private final String mTag;
			private final String mMessage;
			private final Throwable mException;
			
			public LogItem(int level, String tag, String msg, Throwable tr) { 
				mLevel = level;
				mTag = tag;
				mMessage = msg;
				mException = tr;
			}
		}
		
		private class QueueLogger implements Runnable { 
			@Override 
		    public final void run() {
				while (true) {
		            try {
		            	final LogItem item = mLogs.take();
		            	if (item != null)
		            		logOnThread(item.mLevel, item.mTag, item.mMessage, item.mException);
		            } catch (InterruptedException e) {
		                continue; //re-test the condition on the eclosing while
		            }
				}
			}
		}
		
		private final AtomicInteger sThreadNumber = new AtomicInteger(1);
		private final BlockingQueue<LogItem> mLogs;
	    private final Thread mQueueThread;
		
	    public ThreadLogImpl() { 
	    	mLogs = new LinkedBlockingQueue<LogItem>();
	    	mQueueThread = new Thread(
	    			Thread.currentThread().getThreadGroup(), 
	    			new QueueLogger(), 
	    			getThreadName(sThreadNumber.getAndIncrement()));
	    	mQueueThread.start();
	    }
	    
	    protected String getThreadName(int number) { 
	    	return "logthread-" + getClass().getName() + "-" + number;
	    }
	    
	    @Override
	    public final void log(int level, String tag, String msg, Throwable tr) { 
	    	mLogs.add(new LogItem(level, tag, msg, tr));
	    }
	    
	    protected abstract void logOnThread(int level, String tag, String msg, Throwable tr);
	    
	}
	
	private static String getPropertyKey(String name) { 
		return Log.class.getPackage().getName() + "." + name;
	}
	
	private static boolean getPropertyBool(String name, boolean def) { 
		String value = System.getProperty(getPropertyKey(name));
		if (value != null && value.equalsIgnoreCase("true"))
			return true;
		
		return def;
	}
	
	private static final class DelegateLogger { 
		private LogImpl mLogImpl = null;
		private String mLogTag = "LOG"; 
		private boolean mDebug = false; 
		private boolean mDisabled = false;
		
		public DelegateLogger() { 
			mDisabled = getPropertyBool("logger.disabled", false);
			mDebug = getPropertyBool("logger.debug", false);
		}
		
		public boolean isLoggable(String tag, int level) { 
			LogImpl impl = mLogImpl;
			return mDebug || (impl != null && impl.isLoggable(tag, level)); 
		}
		
		public void log(int level, String tag, String msg) { 
			log(level, tag, msg, (Throwable)null);
		}
		
		public void log(int level, String tag, String msg, Throwable tr) { 
			if (!mDisabled) {
				LogImpl impl = mLogImpl;
				if (impl != null) { 
					impl.log(level, tag, msg, tr); 
					if (impl.isHistoryEnabled()) 
						LogHistory.getInstance().log(level, tag, msg, tr); 
				}
			}
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
	
	public static boolean isDisabled() { 
		return getLogger().mDisabled; 
	}
	
	public static void setLogDebug(boolean debug) { 
		getLogger().mDebug = debug;
	}
	
	public static boolean getLogDebug() { 
		return getLogger().mDebug;
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
		getLogger().log(LOG_LEVEL_D, tag, "" + clazz + ": " + msg); 
	}
	
	public static void v(String tag, String msg) {
		getLogger().log(LOG_LEVEL_V, tag, msg); 
	}
	
	public static void v(String tag, String msg, Throwable tr) {
		getLogger().log(LOG_LEVEL_V, tag, msg, tr); 
	}
	
	public static String currentTimeString() { 
		return sTimeFormat.format(new Date(System.currentTimeMillis()));
	}
	
	public static String formatTime(long time) { 
		return sTimeFormat.format(new Date(time));
	}
	
	static Map<Class<?>, String> sClassNames = null;
	static final synchronized String getClassName(Class<?> clazz) { 
		if (clazz == null) return null;
		
		if (sClassNames == null) 
			sClassNames = new HashMap<Class<?>, String>();
		
		String name = sClassNames.get(clazz);
		if (name == null) { 
			name = clazz.getName();
			int count = 0;
			for (int i = name.length()-1; i >= 0; i--) { 
				char chr = name.charAt(i);
				if (chr == '.') { 
					count ++; 
					if (count >= 2) { 
						name = name.substring(i+1);
						break;
					}
				}
			}
			sClassNames.put(clazz, name);
		}
		
		return name;
	}
	
	static final class LoggerImpl extends Logger {
		private final String mClassName;
		
		LoggerImpl(Class<?> clazz) { 
			if (clazz == null) clazz = Log.class; 
			mClassName = getClassName(clazz);
		}
		
		private String getTag() { 
			return mClassName; 
		}
		
		@Override
		public boolean isInfoEnabled() {
			return Log.isLoggable(getTag(), Log.LOG_LEVEL_I);
		}
		
		@Override
		public void info(String msg) {
			info(msg, null); 
		}
		
		@Override
		public void info(String msg, Throwable e) {
			Log.i(getTag(), msg, e); 
		}
		
		@Override
		public boolean isWarnEnabled() {
			return Log.isLoggable(getTag(), Log.LOG_LEVEL_W);
		}
		
		@Override
		public void warn(String msg) {
			warn(msg, null); 
		}
		
		@Override
		public void warn(String msg, Throwable e) {
			Log.w(getTag(), msg, e); 
		}
		
		@Override
		public boolean isErrorEnabled() {
			return Log.isLoggable(getTag(), Log.LOG_LEVEL_E);
		}
		
		@Override
		public void error(String msg) {
			error(msg, null); 
		}
		
		@Override
		public void error(String msg, Throwable e) {
			Log.e(getTag(), msg, e); 
		}
		
		@Override
		public boolean isDebugEnabled() {
			return Log.isLoggable(getTag(), Log.LOG_LEVEL_D);
		}
		
		@Override
		public void debug(String msg) {
			debug(msg, null); 
		}
		
		@Override
		public void debug(String msg, Throwable e) {
			Log.d(getTag(), msg, e); 
		}
		
		@Override
		public boolean isTraceEnabled() {
			return Log.isLoggable(getTag(), Log.LOG_LEVEL_V);
		}
		
		@Override
		public void trace(String msg) {
			trace(msg, null); 
		}
		
		@Override
		public void trace(String msg, Throwable e) {
			Log.d(getTag(), msg, e); 
		}
		
		@Override
		public boolean isFatalEnabled() {
			return Log.isLoggable(getTag(), Log.LOG_LEVEL_E);
		}
		
		@Override
		public void fatal(String msg) {
			fatal(msg, null); 
		}
		
		@Override
		public void fatal(String msg, Throwable e) {
			Log.e(getTag(), msg, e); 
		}
	}
	
}
