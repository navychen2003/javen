package org.javenstudio.raptor.bigdb.master;

import org.javenstudio.raptor.bigdb.DoNotRetryIOException;

/**
 * Thrown when an operation requires the root and all meta regions to be online
 */
public class NotAllMetaRegionsOnlineException extends DoNotRetryIOException {
  private static final long serialVersionUID = 6439786157874827523L;

  /**
   * default constructor
   */
  public NotAllMetaRegionsOnlineException() {
    super();
  }

  /**
   * @param message
   */
  public NotAllMetaRegionsOnlineException(String message) {
    super(message);
  }

}

