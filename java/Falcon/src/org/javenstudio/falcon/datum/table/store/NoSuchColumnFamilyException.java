package org.javenstudio.falcon.datum.table.store;

/**
 * Thrown if request for nonexistent column family.
 */
public class NoSuchColumnFamilyException extends DoNotRetryIOException {
  private static final long serialVersionUID = -6569952730832331274L;

  /** default constructor */
  public NoSuchColumnFamilyException() {
    super();
  }

  /**
   * @param message exception message
   */
  public NoSuchColumnFamilyException(String message) {
    super(message);
  }
}
