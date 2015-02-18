package org.javenstudio.raptor.bigdb.client;

import org.javenstudio.raptor.bigdb.DoNotRetryIOException;

/**
 * Thrown when a scanner has timed out.
 */
public class ScannerTimeoutException extends DoNotRetryIOException {

  private static final long serialVersionUID = 8788838690290688313L;

  /** default constructor */
  ScannerTimeoutException() {
    super();
  }

  /** @param s */
  ScannerTimeoutException(String s) {
    super(s);
  }
}

