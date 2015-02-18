package org.javenstudio.falcon.datum.table.store;

import java.io.IOException;

/**
 * Thrown during flush if the possibility snapshot content was not properly
 * persisted into store files.  Response should include replay of dblog content.
 */
public class DroppedSnapshotException extends IOException {
  private static final long serialVersionUID = -5463156580831677374L;

  /**
   * @param msg
   */
  public DroppedSnapshotException(String msg) {
    super(msg);
  }

  /**
   * default constructor
   */
  public DroppedSnapshotException() {
    super();
  }
}
