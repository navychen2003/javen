package org.javenstudio.raptor.bigdb;

import java.io.IOException;

/**
 * This exception is thrown by the master when a region server reports and is
 * already being processed as dead. This can happen when a region server loses
 * its session but didn't figure it yet.
 */
public class YouAreDeadException extends IOException {
  private static final long serialVersionUID = 1L;

  public YouAreDeadException(String message) {
    super(message);
  }
}

