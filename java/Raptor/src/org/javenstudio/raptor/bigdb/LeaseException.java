package org.javenstudio.raptor.bigdb;

/**
 * Reports a problem with a lease
 */
public class LeaseException extends DoNotRetryIOException {

  private static final long serialVersionUID = 8179703995292418650L;

  /** default constructor */
  public LeaseException() {
    super();
  }

  /**
   * @param message
   */
  public LeaseException(String message) {
    super(message);
  }
}
