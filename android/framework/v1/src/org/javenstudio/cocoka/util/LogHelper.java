package org.javenstudio.cocoka.util;

import org.javenstudio.common.util.Log;

public class LogHelper {

	public static Log.LogImpl createDefault() {
		return new Log.LogImpl() {
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
