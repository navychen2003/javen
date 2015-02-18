package org.javenstudio.falcon.datum.table.store;

import java.io.IOException;

/**
 * If set of MapFile.Readers in Store change, implementors are notified.
 */
public interface ChangedReadersObserver {

  /**
   * Notify observers.
   * @throws IOException e
   */
  void updateReaders() throws IOException;
}
