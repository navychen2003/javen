package org.javenstudio.falcon.search.schema.spatial;

/**
 * A Point with X & Y coordinates.
 */
public interface Point extends Shape {

  /**
   * Expert: Resets the state of this shape given the arguments. This is a
   * performance feature to avoid excessive Shape object allocation as well as
   * some argument error checking. Mutable shapes is error-prone so use with
   * care.
   */
  public void reset(double x, double y);

  /** The X coordinate, or Longitude in geospatial contexts. */
  public double getX();

  /** The Y coordinate, or Latitude in geospatial contexts. */
  public double getY();

}
