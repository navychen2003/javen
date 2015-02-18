package org.javenstudio.raptor.bigdb.regionserver.wal;

import org.javenstudio.raptor.bigdb.DBRegionInfo;

public interface LogEntryVisitor {

  /**
   *
   * @param info
   * @param logKey
   * @param logEdit
   */
  public void visitLogEntryBeforeWrite(DBRegionInfo info, DBLogKey logKey,
                                       WALEdit logEdit);
}

