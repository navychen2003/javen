package org.javenstudio.common.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;

/**
 * Utility class for logging.
 *
 * @author J&eacute;r&ocirc;me Charron
 */
public class LogUtil {
  private final static Logger LOG = Logger.getLogger(LogUtil.class);
 
  private static Method TRACE = null;
  private static Method DEBUG = null;
  private static Method INFO  = null;
  private static Method WARN  = null;
  private static Method ERROR = null;
  private static Method FATAL = null;
 
  static {
    try {
      TRACE = Logger.class.getMethod("trace", new Class[] { Object.class });
      DEBUG = Logger.class.getMethod("debug", new Class[] { Object.class });
      INFO  = Logger.class.getMethod("info",  new Class[] { Object.class });
      WARN  = Logger.class.getMethod("warn",  new Class[] { Object.class });
      ERROR = Logger.class.getMethod("error", new Class[] { Object.class });
      FATAL = Logger.class.getMethod("fatal", new Class[] { Object.class });
    } catch(Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Cannot init log methods", e);
      }
    }
  }
  
  public static PrintStream getTraceStream(final Logger logger) {
    return getLogStream(logger, TRACE);
  }
 
  public static PrintStream getDebugStream(final Logger logger) {
    return getLogStream(logger, DEBUG);
  }
 
  public static PrintStream getInfoStream(final Logger logger) {
    return getLogStream(logger, INFO);
  }
  
  public static PrintStream getWarnStream(final Logger logger) {
    return getLogStream(logger, WARN);
  }
 
  public static PrintStream getErrorStream(final Logger logger) {
    return getLogStream(logger, ERROR);
  }
 
  public static PrintStream getFatalStream(final Logger logger) {
    return getLogStream(logger, FATAL);
  }
  
  /** Returns a stream that, when written to, adds log lines. */
  private static PrintStream getLogStream(final Logger logger, final Method method) {
    return new PrintStream(new ByteArrayOutputStream() {
        private int scan = 0;
 
        private boolean hasNewline() {
          for (; scan < count; scan++) {
            if (buf[scan] == '\n')
              return true;
          }
          return false;
        }
 
        public void flush() throws IOException {
          if (!hasNewline())
            return;
          try {
            method.invoke(logger, new Object[] { toString().trim() });
          } catch (Exception e) {
            if (LOG.isFatalEnabled()) {
              LOG.fatal("Cannot log with method [" + method + "]", e);
            }
          }
          reset();
          scan = 0;
        }
      }, true);
  }
 
}