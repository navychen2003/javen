package org.javenstudio.falcon.datum.table.store;

/**
 * Mechanism by which the DBLog requests a log roll
 */
public interface LogRollListener {

  /** Request that the log be rolled */
  public void logRollRequested();
}
