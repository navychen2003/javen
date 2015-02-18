package org.javenstudio.raptor.bigdb.master;

import org.javenstudio.raptor.bigdb.DoNotRetryIOException;


/**
 * Thrown when an invalid column name is encountered
 */
public class InvalidColumnNameException extends DoNotRetryIOException {
  private static final long serialVersionUID = 1L << 29 - 1L;
  /** default constructor */
  public InvalidColumnNameException() {
    super();
  }

  /**
   * Constructor
   * @param s message
   */
  public InvalidColumnNameException(String s) {
    super(s);
  }
}
