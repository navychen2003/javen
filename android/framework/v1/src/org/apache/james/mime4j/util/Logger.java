package org.apache.james.mime4j.util;

public class Logger {

	private static org.javenstudio.common.util.Logger getLoggerImpl() { 
		return org.javenstudio.common.util.Logger.getLogger();
	}
	
	private static final Logger sLog = new Logger(); 
	
	public static Logger getLogger(Class<?> clazz) {
		return getLogger(); 
	}
	
	public static Logger getLogger() {
		return sLog; 
	}
	
	private Logger() {}
	
	public boolean isInfoEnabled() {
		return getLoggerImpl().isInfoEnabled();
	}
	
	public void info(String msg) {
		getLoggerImpl().info(msg);
	}
	
	public void info(String msg, Exception e) {
		getLoggerImpl().info(msg, e);
	}
	
	public boolean isWarnEnabled() {
		return getLoggerImpl().isWarnEnabled();
	}
	
	public void warn(String msg) {
		getLoggerImpl().warn(msg);
	}
	
	public void warn(String msg, Exception e) {
		getLoggerImpl().warn(msg, e);
	}
	
	public boolean isErrorEnabled() {
		return getLoggerImpl().isErrorEnabled();
	}
	
	public void error(String msg) {
		getLoggerImpl().error(msg);
	}
	
	public void error(String msg, Exception e) {
		getLoggerImpl().error(msg, e);
	}
	
	public boolean isDebugEnabled() {
		return getLoggerImpl().isDebugEnabled();
	}
	
	public void debug(String msg) {
		getLoggerImpl().debug(msg);
	}
	
	public void debug(String msg, Exception e) {
		getLoggerImpl().debug(msg, e);
	}
	
	public boolean isTraceEnabled() {
		return getLoggerImpl().isTraceEnabled();
	}
	
	public void trace(String msg) {
		getLoggerImpl().trace(msg);
	}
	
	public void trace(String msg, Exception e) {
		getLoggerImpl().trace(msg, e);
	}
	
	public boolean isFatalEnabled() {
		return getLoggerImpl().isFatalEnabled();
	}
	
	public void fatal(String msg) {
		getLoggerImpl().fatal(msg);
	}
	
	public void fatal(String msg, Exception e) {
		getLoggerImpl().fatal(msg, e);
	}
	
	public void trace(Object message) {
		getLoggerImpl().trace(message);
    }

    public void trace(Object message, Throwable t) {
    	getLoggerImpl().trace(message, t);
    }

    public void debug(Object message) {
    	getLoggerImpl().debug(message);
    }

    public void debug(Object message, Throwable t) {
    	getLoggerImpl().debug(message, t);
    }

    public void info(Object message) {
    	getLoggerImpl().info(message);
    }

    public void info(Object message, Throwable t) {
    	getLoggerImpl().info(message, t);
    }

    public void warn(Object message) {
    	getLoggerImpl().warn(message);
    }

    public void warn(Object message, Throwable t) {
    	getLoggerImpl().warn(message, t);
    }

    public void error(Object message) {
    	getLoggerImpl().error(message);
    }

    public void error(Object message, Throwable t) {
    	getLoggerImpl().error(message, t);
    }

    public void fatal(Object message) {
    	getLoggerImpl().fatal(message);
    }

    public void fatal(Object message, Throwable t) {
    	getLoggerImpl().fatal(message, t);
    }
	
}
