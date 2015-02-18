package org.javenstudio.raptor.security;


/**
 * An exception class for access control related issues.
 */
@SuppressWarnings("deprecation")
public class AccessControlException extends 
	org.javenstudio.raptor.fs.permission.AccessControlException {

  //Required by {@link java.io.Serializable}.
  private static final long serialVersionUID = 1L;

  /**
   * Default constructor is needed for unwrapping from 
   * {@link org.javenstudio.raptor.ipc.RemoteException}.
   */
  public AccessControlException() {
    super("Permission denied.");
  }

  /**
   * Constructs an {@link AccessControlException}
   * with the specified detail message.
   * @param s the detail message.
   */
  public AccessControlException(String s) {super(s);}
  
  /**
   * Constructs a new exception with the specified cause and a detail
   * message of <tt>(cause==null ? null : cause.toString())</tt> (which
   * typically contains the class and detail message of <tt>cause</tt>).
   * @param  cause the cause (which is saved for later retrieval by the
   *         {@link #getCause()} method).  (A <tt>null</tt> value is
   *         permitted, and indicates that the cause is nonexistent or
   *         unknown.)
   */
  public AccessControlException(Throwable cause) {
    super(cause);
  }
}
