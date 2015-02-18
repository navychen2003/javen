package org.javenstudio.raptor.dfs.server.namenode;

import java.io.IOException;

/**
 * The file has not finished being written to enough datanodes yet.
 */
public class NotReplicatedYetException extends IOException {
  private static final long serialVersionUID = 1L;

  public NotReplicatedYetException(String msg) {
    super(msg);
  }
}

