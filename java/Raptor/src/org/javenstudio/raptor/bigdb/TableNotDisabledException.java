package org.javenstudio.raptor.bigdb;

import java.io.IOException;

import org.javenstudio.raptor.bigdb.util.Bytes;

/**
 * Thrown if a table should be offline but is not
 */
public class TableNotDisabledException extends IOException {
  private static final long serialVersionUID = 1L << 19 - 1L;
  /** default constructor */
  public TableNotDisabledException() {
    super();
  }

  /**
   * Constructor
   * @param s message
   */
  public TableNotDisabledException(String s) {
    super(s);
  }

  /**
   * @param tableName Name of table that is not disabled
   */
  public TableNotDisabledException(byte[] tableName) {
    this(Bytes.toString(tableName));
  }
}
