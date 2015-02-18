package org.javenstudio.raptor.bigdb;

import java.io.IOException;

/**
 * This exception is thrown by the master when a region server was shut down
 * and restarted so fast that the master still hasn't processed the server
 * shutdown of the first instance.
 */
public class PleaseHoldException extends IOException {
  private static final long serialVersionUID = 1L;

  public PleaseHoldException(String message) {
    super(message);
  }
}

