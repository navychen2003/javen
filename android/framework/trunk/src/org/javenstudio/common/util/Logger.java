package org.javenstudio.common.util;

public abstract class Logger {
	
	public interface LoggerFactory { 
		public Logger createLogger(Class<?> clazz);
	}
	
	private static LoggerFactory sFactory = null;
	private static synchronized Logger createLogger(Class<?> clazz) { 
		if (sFactory == null) { 
			sFactory = new LoggerFactory() { 
				public Logger createLogger(Class<?> clazz) { 
					return new Log.LoggerImpl(clazz); 
				}
			};
		}
		return sFactory.createLogger(clazz);
	}
	
	public static synchronized void setFactory(LoggerFactory factory) { 
		if (factory == null) 
			throw new IllegalArgumentException("factory cannot be null");
		
		if (factory == sFactory) 
			return;
		
		if (sFactory != null) 
			throw new IllegalArgumentException("factory already set");
		
		sFactory = factory;
	}
	
	public static Logger getLogger(Class<?> clazz) { return createLogger(clazz); }
	public static Logger getLogger() { return createLogger(null); }
	
	public static String toString(int level) { 
		switch (level) { 
		case Log.LOG_LEVEL_I:
			return "INFO";
		case Log.LOG_LEVEL_E:
			return "ERROR";
		case Log.LOG_LEVEL_W:
			return "WARN";
		case Log.LOG_LEVEL_D:
			return "DEBUG";
		case Log.LOG_LEVEL_V:
			return "TRACE";
		}
		return "INFO";
	}
	
	public abstract boolean isInfoEnabled();
	public abstract boolean isWarnEnabled();
	public abstract boolean isErrorEnabled();
	public abstract boolean isDebugEnabled();
	public abstract boolean isTraceEnabled();
	public abstract boolean isFatalEnabled();
	
	public abstract void info(String msg);
	public abstract void info(String msg, Throwable e);
	public abstract void warn(String msg);
	public abstract void warn(String msg, Throwable e);
	public abstract void error(String msg);
	public abstract void error(String msg, Throwable e);
	public abstract void debug(String msg);
	public abstract void debug(String msg, Throwable e);
	public abstract void trace(String msg);
	public abstract void trace(String msg, Throwable e);
	public abstract void fatal(String msg);
	public abstract void fatal(String msg, Throwable e);
	
}
