package org.javenstudio.lightning.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

public class DefaultLogger {
	private static final String DEBUG_ENV = "org.javenstudio.common.util.logger.debug";
	private static final AtomicLong sCounter = new AtomicLong(1);
	private static volatile boolean sLoggerDebug = false;
	
	static final Level[] LEVELS = {
		null, // aka unset
		Level.FINEST,
		Level.FINE,
		Level.CONFIG,
		Level.INFO,
		Level.WARNING,
		Level.SEVERE,
		Level.OFF
		// Level.ALL -- ignore. It is useless.
	};
	
	static { 
		String debugProp = System.getenv(DEBUG_ENV);
		if (debugProp == null || debugProp.length() == 0) 
			debugProp = System.getProperty(DEBUG_ENV);
		if (debugProp == null) debugProp = "false";
		sLoggerDebug = debugProp.startsWith("true");
		
		final boolean loggerJava = debugProp.endsWith(".std") ? false : true;
		final java.util.logging.Logger logger = loggerJava ? 
				java.util.logging.Logger.getLogger("lightning") : null;
				
		if (logger != null) {
			//java.util.logging.Handler[] handlers = logger.getHandlers();
			//if (handlers != null) {
			//	for (java.util.logging.Handler handler : handlers) { 
			//		if (handler != null) 
			//			logger.removeHandler(handler);
			//	}
			//}
			
			//logger.addHandler(new DefaultHandler(loggerDebug));
		}
		
		org.javenstudio.common.util.Log.setLogImpl(
			new org.javenstudio.common.util.Log.ThreadLogImpl() {
				@Override
				public boolean isLoggable(String tag, int level) { 
					if (logger != null) {
						switch (level) { 
						case org.javenstudio.common.util.Log.LOG_LEVEL_I:
							return sLoggerDebug || logger.isLoggable(java.util.logging.Level.INFO);
						case org.javenstudio.common.util.Log.LOG_LEVEL_E:
							return sLoggerDebug || logger.isLoggable(java.util.logging.Level.SEVERE);
						case org.javenstudio.common.util.Log.LOG_LEVEL_W:
							return sLoggerDebug || logger.isLoggable(java.util.logging.Level.WARNING);
						case org.javenstudio.common.util.Log.LOG_LEVEL_D:
						case org.javenstudio.common.util.Log.LOG_LEVEL_V:
						default:
							return sLoggerDebug || logger.isLoggable(java.util.logging.Level.FINEST);
						}
					} else { 
						switch (level) { 
						case org.javenstudio.common.util.Log.LOG_LEVEL_I:
							return true; //loggerDebug;
						case org.javenstudio.common.util.Log.LOG_LEVEL_E:
							return true; //loggerDebug;
						case org.javenstudio.common.util.Log.LOG_LEVEL_W:
							return true; //loggerDebug;
						case org.javenstudio.common.util.Log.LOG_LEVEL_D:
						case org.javenstudio.common.util.Log.LOG_LEVEL_V:
						default:
							return sLoggerDebug;
						}
					}
				}
				
				@Override
				protected void logOnThread(int level, String tag, String message, Throwable e) {
					if (logger != null) {
						switch (level) { 
						case org.javenstudio.common.util.Log.LOG_LEVEL_I:
							if (logger.isLoggable(java.util.logging.Level.INFO)) {
								String text = formatMessage(level, tag, message, e);
								logger.info(text);
							}
							return;
						case org.javenstudio.common.util.Log.LOG_LEVEL_E:
							if (logger.isLoggable(java.util.logging.Level.SEVERE)) {
								String text = formatMessage(level, tag, message, e);
								logger.severe(text);
							}
							return;
						case org.javenstudio.common.util.Log.LOG_LEVEL_W:
							if (logger.isLoggable(java.util.logging.Level.WARNING)) {
								String text = formatMessage(level, tag, message, e);
								logger.warning(text);
							}
							return;
						case org.javenstudio.common.util.Log.LOG_LEVEL_D:
						case org.javenstudio.common.util.Log.LOG_LEVEL_V:
						default:
							if (logger.isLoggable(java.util.logging.Level.FINEST)) {
								String text = formatMessage(level, tag, message, e);
								logger.finest(text);
							}
							return;
						}
					} else { 
						String msg = org.javenstudio.common.util.Log.currentTimeString() 
								+ ":" + org.javenstudio.common.util.Logger.toString(level) 
								+ ":" + tag + "(" + sCounter.getAndIncrement() + "): " + message;
						System.err.println(msg);
						if (e != null) 
							e.printStackTrace();
					}
				}
				
				private String formatMessage(int level, String tag, String message, Throwable e) { 
					StringBuilder sb = new StringBuilder();
					sb.append(org.javenstudio.common.util.Logger.toString(level)).append(':');
					sb.append(tag).append("(").append(sCounter.getAndIncrement()).append("): ");
					sb.append(message);
					
					if (e != null) { 
						try {
							StringWriter writer = new StringWriter();
							e.printStackTrace(new PrintWriter(writer));
							writer.flush();
							
							sb.append("\r\n");
							sb.append(writer.toString());
							
							writer.close();
						} catch (Throwable ex) { 
							// ignore
						}
					}
					
					return sb.toString();
				}
			});
	}
	
	public static void setDebug(boolean debug) { sLoggerDebug = debug; }
	public static boolean isDebug() { return sLoggerDebug; }
	
	public static org.javenstudio.common.util.Logger getLogger(Class<?> clazz) { 
		return org.javenstudio.common.util.Logger.getLogger(clazz);
	}
	
	public static LogWatcher<?> getLogWatcher() { 
		return new DefaultLogWatcher();
	}
	
}
