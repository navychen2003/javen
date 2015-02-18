package org.javenstudio.falcon.datum.table.store;

import org.javenstudio.raptor.fs.Path;

/**
 * Interface that defines all actions that can be listened to coming
 * from the DBLog. The calls are done in sync with what happens over in the
 * DBLog so make sure your implementation is fast.
 */
public interface LogActionsListener {

  /**
   * Notify the listener that a new file is available
   * @param newFile the path to the new dblog
   */
  public void logRolled(Path newFile);
}
