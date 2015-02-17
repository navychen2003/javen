package org.javenstudio.cocoka.util;

import org.javenstudio.common.util.Log;

public class LogHelper {

	public static Log.LogImpl createDefault() {
		return new Log.LogImpl() {
			@Override
			public boolean isHistoryEnabled() { return false; }
			
			@Override
			public boolean isLoggable(String tag, int level) { 
				switch (level) {
				case Log.LOG_LEVEL_I: 
					return android.util.Log.isLoggable(tag, android.util.Log.INFO);
				case Log.LOG_LEVEL_E: 
					return android.util.Log.isLoggable(tag, android.util.Log.ERROR);
				case Log.LOG_LEVEL_W: 
					return android.util.Log.isLoggable(tag, android.util.Log.WARN);
				case Log.LOG_LEVEL_D: 
					return android.util.Log.isLoggable(tag, android.util.Log.DEBUG);
				case Log.LOG_LEVEL_V: 
					return android.util.Log.isLoggable(tag, android.util.Log.VERBOSE);
				default: 
					return android.util.Log.isLoggable(tag, android.util.Log.DEBUG);
				}
			}
			
			@Override
			public void log(int level, String tag, String msg, Throwable tr) {
				switch (level) {
				case Log.LOG_LEVEL_I: 
					android.util.Log.i(tag, msg, tr); 
					break; 
				case Log.LOG_LEVEL_E: 
					android.util.Log.e(tag, msg, tr); 
					break; 
				case Log.LOG_LEVEL_W: 
					android.util.Log.w(tag, msg, tr); 
					break; 
				case Log.LOG_LEVEL_D: 
					android.util.Log.d(tag, msg, tr); 
					break; 
				case Log.LOG_LEVEL_V: 
					android.util.Log.v(tag, msg, tr); 
					break; 
				default: 
					android.util.Log.d(tag, msg, tr); 
					break; 
				}
			}
		};
	}
	
}
