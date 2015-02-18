package org.javenstudio.raptor.bigdb;

import java.io.IOException;

/**
 * Thrown when a table exists but should not
 */
public class TableExistsException extends IOException {
  private static final long serialVersionUID = 1L << 7 - 1L;
  /** default constructor */
  public TableExistsException() {
    super();
  }

  /**
   * Constructor
   *
   * @param s message
   */
  public TableExistsException(String s) {
    super(s);
  }
}
