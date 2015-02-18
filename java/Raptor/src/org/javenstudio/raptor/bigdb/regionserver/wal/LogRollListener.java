package org.javenstudio.raptor.bigdb.regionserver.wal;

/**
 * Mechanism by which the HLog requests a log roll
 */
public interface LogRollListener {
  /** Request that the log be rolled */
  public void logRollRequested();
}

