package org.javenstudio.raptor.dfs.protocol;

import java.io.IOException;

/**
 * The exception that happens when you ask to create a file that already
 * is being created, but is not closed yet.
 */
public class AlreadyBeingCreatedException extends IOException {
  private static final long serialVersionUID = 1L;

  public AlreadyBeingCreatedException(String msg) {
    super(msg);
  }
}

