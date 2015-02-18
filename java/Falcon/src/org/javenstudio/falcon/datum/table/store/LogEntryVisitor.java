package org.javenstudio.falcon.datum.table.store;

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

