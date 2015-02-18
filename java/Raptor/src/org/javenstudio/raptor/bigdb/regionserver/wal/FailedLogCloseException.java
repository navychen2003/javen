package org.javenstudio.raptor.bigdb.regionserver.wal;

import java.io.IOException;

/**
 * Thrown when we fail close of the write-ahead-log file.
 * Package private.  Only used inside this package.
 */
public class FailedLogCloseException extends IOException {
  private static final long serialVersionUID = 1759152841462990925L;

  /**
   *
   */
  public FailedLogCloseException() {
    super();
  }

  /**
   * @param arg0
   */
  public FailedLogCloseException(String arg0) {
    super(arg0);
  }
}

