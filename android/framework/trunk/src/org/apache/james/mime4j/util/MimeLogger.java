package org.apache.james.mime4j.util;

public class MimeLogger {

	private static org.javenstudio.common.util.Logger getLoggerImpl() { 
		return org.javenstudio.common.util.Logger.getLogger();
	}
	
	private static final MimeLogger sLog = new MimeLogger(); 
	
	public static MimeLogger getLogger(Class<?> clazz) {
		return getLogger(); 
	}
	
	public static MimeLogger getLogger() {
		return sLog; 
	}
	
	private MimeLogger() {}
	
	public boolean isInfoEnabled() {
		return getLoggerImpl().isInfoEnabled();
	}
	
	public void info(String msg) {
		getLoggerImpl().info(msg);
	}
	
	public void info(String msg, Throwable e) {
		getLoggerImpl().info(msg, e);
	}
	
	public boolean isWarnEnabled() {
		return getLoggerImpl().isWarnEnabled();
	}
	
	public void warn(String msg) {
		getLoggerImpl().warn(msg);
	}
	
	public void warn(String msg, Throwable e) {
		getLoggerImpl().warn(msg, e);
	}
	
	public boolean isErrorEnabled() {
		return getLoggerImpl().isErrorEnabled();
	}
	
	public void error(String msg) {
		getLoggerImpl().error(msg);
	}
	
	public void error(String msg, Throwable e) {
		getLoggerImpl().error(msg, e);
	}
	
	public boolean isDebugEnabled() {
		return getLoggerImpl().isDebugEnabled();
	}
	
	public void debug(String msg) {
		getLoggerImpl().debug(msg);
	}
	
	public void debug(String msg, Throwable e) {
		getLoggerImpl().debug(msg, e);
	}
	
	public boolean isTraceEnabled() {
		return getLoggerImpl().isTraceEnabled();
	}
	
	public void trace(String msg) {
		getLoggerImpl().trace(msg);
	}
	
	public void trace(String msg, Throwable e) {
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

}
