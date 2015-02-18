package org.javenstudio.raptor.bigdb.regionserver.wal;

import org.javenstudio.raptor.fs.Path;

/**
 * Interface that defines all actions that can be listened to coming
 * from the HLog. The calls are done in sync with what happens over in the
 * HLog so make sure your implementation is fast.
 */
public interface LogActionsListener {

  /**
   * Notify the listener that a new file is available
   * @param newFile the path to the new hlog
   */
  public void logRolled(Path newFile);
}

