package org.javenstudio.raptor.bigdb;

/** Thrown when a table can not be located */
public class TableNotFoundException extends RegionException {
  private static final long serialVersionUID = 993179627856392526L;

  /** default constructor */
  public TableNotFoundException() {
    super();
  }

  /** @param s message */
  public TableNotFoundException(String s) {
    super(s);
  }
}

