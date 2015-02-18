package org.javenstudio.raptor.bigdb;

import java.io.IOException;

import org.javenstudio.raptor.bigdb.util.Bytes;

/**
 * Thrown by a region server if it is sent a request for a region it is not
 * serving.
 */
public class NotServingRegionException extends IOException {
  private static final long serialVersionUID = 1L << 17 - 1L;

  /** default constructor */
  public NotServingRegionException() {
    super();
  }

  /**
   * Constructor
   * @param s message
   */
  public NotServingRegionException(String s) {
    super(s);
  }

  /**
   * Constructor
   * @param s message
   */
  public NotServingRegionException(final byte [] s) {
    super(Bytes.toString(s));
  }
}
