package org.javenstudio.raptor.bigdb.client;

import org.javenstudio.raptor.bigdb.RegionException;

/** Thrown when a table can not be located */
public class RegionOfflineException extends RegionException {
  private static final long serialVersionUID = 466008402L;
  /** default constructor */
  public RegionOfflineException() {
    super();
  }

  /** @param s message */
  public RegionOfflineException(String s) {
    super(s);
  }
}

