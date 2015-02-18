package org.javenstudio.raptor.bigdb.client;

import org.javenstudio.raptor.bigdb.RegionException;

/**
 * Thrown when no region server can be found for a region
 */
public class NoServerForRegionException extends RegionException {
  private static final long serialVersionUID = 1L << 11 - 1L;

  /** default constructor */
  public NoServerForRegionException() {
    super();
  }

  /**
   * Constructor
   * @param s message
   */
  public NoServerForRegionException(String s) {
    super(s);
  }
}

