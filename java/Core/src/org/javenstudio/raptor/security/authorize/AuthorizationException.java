package org.javenstudio.raptor.security.authorize;

import java.io.PrintStream;
import java.io.PrintWriter;

import org.javenstudio.raptor.security.AccessControlException;


/**
 * An exception class for authorization-related issues.
 * 
 * This class <em>does not</em> provide the stack trace for security purposes.
 */
public class AuthorizationException extends AccessControlException {
  private static final long serialVersionUID = 1L;

  public AuthorizationException() {
    super();
  }

  public AuthorizationException(String message) {
    super(message);
  }
  
  /**
   * Constructs a new exception with the specified cause and a detail
   * message of <tt>(cause==null ? null : cause.toString())</tt> (which
   * typically contains the class and detail message of <tt>cause</tt>).
   * @param  cause the cause (which is saved for later retrieval by the
   *         {@link #getCause()} method).  (A <tt>null</tt> value is
   *         permitted, and indicates that the cause is nonexistent or
   *         unknown.)
   */
  public AuthorizationException(Throwable cause) {
    super(cause);
  }
  
  private static StackTraceElement[] stackTrace = new StackTraceElement[0];
  @Override
  public StackTraceElement[] getStackTrace() {
    // Do not provide the stack-trace
    return stackTrace;
  }

  @Override
  public void printStackTrace() {
    // Do not provide the stack-trace
  }

  @Override
  public void printStackTrace(PrintStream s) {
    // Do not provide the stack-trace
  }

  @Override
  public void printStackTrace(PrintWriter s) {
    // Do not provide the stack-trace
  }
  
}
