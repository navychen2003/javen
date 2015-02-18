package org.javenstudio.raptor.bigdb;


/**
 * Thrown if a region server is passed an unknown scanner id.
 * Usually means the client has take too long between checkins and so the
 * scanner lease on the serverside has expired OR the serverside is closing
 * down and has cancelled all leases.
 */
public class UnknownScannerException extends DoNotRetryIOException {
  private static final long serialVersionUID = 993179627856392526L;

  /** constructor */
  public UnknownScannerException() {
    super();
  }

  /**
   * Constructor
   * @param s message
   */
  public UnknownScannerException(String s) {
    super(s);
  }
}
