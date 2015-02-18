package org.javenstudio.common.util;

public class Logger {

	private static final Logger sLog = new Logger(); 
	
	public static Logger getLogger(Class<?> clazz) {
		return getLogger(); 
	}
	
	public static Logger getLogger() {
		return sLog; 
	}
	
	private Logger() {}
	
	public boolean isInfoEnabled() {
		return Log.isLoggable(Constants.getTag(), Log.LOG_LEVEL_I);
	}
	
	public void info(String msg) {
		Log.i(Constants.getTag(), msg); 
	}
	
	public void info(String msg, Exception e) {
		Log.i(Constants.getTag(), msg, e); 
	}
	
	public boolean isWarnEnabled() {
		return Log.isLoggable(Constants.getTag(), Log.LOG_LEVEL_W);
	}
	
	public void warn(String msg) {
		Log.w(Constants.getTag(), msg); 
	}
	
	public void warn(String msg, Exception e) {
		Log.w(Constants.getTag(), msg, e); 
	}
	
	public boolean isErrorEnabled() {
		return Log.isLoggable(Constants.getTag(), Log.LOG_LEVEL_E);
	}
	
	public void error(String msg) {
		Log.e(Constants.getTag(), msg); 
	}
	
	public void error(String msg, Exception e) {
		Log.e(Constants.getTag(), msg, e); 
	}
	
	public boolean isDebugEnabled() {
		return Log.isLoggable(Constants.getTag(), Log.LOG_LEVEL_D);
	}
	
	public void debug(String msg) {
		Log.d(Constants.getTag(), msg); 
	}
	
	public void debug(String msg, Exception e) {
		Log.d(Constants.getTag(), msg, e); 
	}
	
	public boolean isTraceEnabled() {
		return Log.isLoggable(Constants.getTag(), Log.LOG_LEVEL_V);
	}
	
	public void trace(String msg) {
		Log.d(Constants.getTag(), msg); 
	}
	
	public void trace(String msg, Exception e) {
		Log.d(Constants.getTag(), msg, e); 
	}
	
	public boolean isFatalEnabled() {
		return Log.isLoggable(Constants.getTag(), Log.LOG_LEVEL_E);
	}
	
	public void fatal(String msg) {
		Log.e(Constants.getTag(), msg); 
	}
	
	public void fatal(String msg, Exception e) {
		Log.e(Constants.getTag(), msg, e); 
	}
	
	public void trace(Object message) {
        if (!isTraceEnabled()) return;
        trace(toString(message));
    }

    public void trace(Object message, Throwable t) {
        if (!isTraceEnabled()) return;
        trace(toString(message), t);
    }

    public void debug(Object message) {
        if (!isDebugEnabled()) return;
        debug(toString(message));
    }

    public void debug(Object message, Throwable t) {
        if (!isDebugEnabled()) return;
        debug(toString(message), t);
    }

    public void info(Object message) {
        if (!isInfoEnabled()) return;
        info(toString(message));
    }

    public void info(Object message, Throwable t) {
        if (!isInfoEnabled()) return;
        info(toString(message), t);
    }

    public void warn(Object message) {
    	error(toString(message));
    }

    public void warn(Object message, Throwable t) {
    	warn(toString(message), t);
    }

    public void error(Object message) {
    	error(toString(message));
    }

    public void error(Object message, Throwable t) {
    	error(toString(message), t);
    }

    public void fatal(Object message) {
    	fatal(toString(message));
    }

    public void fatal(Object message, Throwable t) {
        fatal(toString(message), t);
    }

    private static String toString(Object o) {
    	return toString(o, (Throwable)null); 
    }
    
    private static String toString(Object o, Throwable t) {
        String m = (o == null) ? "(null)" : o.toString();
        if (t == null) {
            return m;
        } else {
            return m + " " + t.getMessage();
        }
    }
}
