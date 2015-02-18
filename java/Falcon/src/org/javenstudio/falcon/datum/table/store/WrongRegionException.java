package org.javenstudio.falcon.datum.table.store;

import java.io.IOException;

/**
 * Thrown when a request contains a key which is not part of this region
 */
public class WrongRegionException extends IOException {
  private static final long serialVersionUID = 993179627856392526L;

  /** constructor */
  public WrongRegionException() {
    super();
  }

  /**
   * Constructor
   * @param s message
   */
  public WrongRegionException(String s) {
    super(s);
  }
}
