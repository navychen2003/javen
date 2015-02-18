package org.javenstudio.raptor.bigdb;

import java.io.IOException;

/**
 * Thrown if the master is not running
 */
public class MasterNotRunningException extends IOException {
  private static final long serialVersionUID = 1L << 23 - 1L;
  /** default constructor */
  public MasterNotRunningException() {
    super();
  }

  /**
   * Constructor
   * @param s message
   */
  public MasterNotRunningException(String s) {
    super(s);
  }

  /**
   * Constructor taking another exception.
   * @param e Exception to grab data from.
   */
  public MasterNotRunningException(Exception e) {
    super(e);
  }
  
  public MasterNotRunningException(String msg, Exception e) {
    super(msg, e);
  }
}

