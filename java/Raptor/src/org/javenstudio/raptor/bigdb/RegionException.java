package org.javenstudio.raptor.bigdb;

import java.io.IOException;

/**
 * Thrown when something happens related to region handling.
 * Subclasses have to be more specific.
 */
public class RegionException extends IOException {
  private static final long serialVersionUID = 1473510258071111371L;

  /** default constructor */
  public RegionException() {
    super();
  }

  /**
   * Constructor
   * @param s message
   */
  public RegionException(String s) {
    super(s);
  }

}

