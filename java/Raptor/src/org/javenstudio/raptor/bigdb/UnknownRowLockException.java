package org.javenstudio.raptor.bigdb;


/**
 * Thrown if a region server is passed an unknown row lock id
 */
public class UnknownRowLockException extends DoNotRetryIOException {
  private static final long serialVersionUID = 993179627856392526L;

  /** constructor */
  public UnknownRowLockException() {
    super();
  }

  /**
   * Constructor
   * @param s message
   */
  public UnknownRowLockException(String s) {
    super(s);
  }
}

