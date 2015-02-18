package org.javenstudio.falcon.datum.table.store;

import java.io.IOException;

/**
 * Subclass if exception is not meant to be retried: e.g.
 * {@link UnknownScannerException}
 */
public class DoNotRetryIOException extends IOException {
  private static final long serialVersionUID = 1197446454511704139L;

  /**
   * default constructor
   */
  public DoNotRetryIOException() {
    super();
  }

  /**
   * @param message
   */
  public DoNotRetryIOException(String message) {
    super(message);
  }

  /**
   * @param message
   * @param cause
   */
  public DoNotRetryIOException(String message, Throwable cause) {
    super(message, cause);
  }
}
