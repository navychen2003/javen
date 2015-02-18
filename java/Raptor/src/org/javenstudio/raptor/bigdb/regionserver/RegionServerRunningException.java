package org.javenstudio.raptor.bigdb.regionserver;

import java.io.IOException;

/**
 * Thrown if the region server log directory exists (which indicates another
 * region server is running at the same address)
 */
public class RegionServerRunningException extends IOException {
  private static final long serialVersionUID = 1L << 31 - 1L;

  /** Default Constructor */
  public RegionServerRunningException() {
    super();
  }

  /**
   * Constructs the exception and supplies a string as the message
   * @param s - message
   */
  public RegionServerRunningException(String s) {
    super(s);
  }

}

