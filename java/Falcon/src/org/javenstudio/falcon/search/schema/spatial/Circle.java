package org.javenstudio.falcon.search.schema.spatial;

/**
 * A circle, also known as a point-radius since that is what it is comprised of.
 */
public interface Circle extends Shape {

  /**
   * Expert: Resets the state of this shape given the arguments. This is a
   * performance feature to avoid excessive Shape object allocation as well as
   * some argument error checking. Mutable shapes is error-prone so use with
   * care.
   */
  void reset(double x, double y, double radiusDEG) throws InvalidShapeException;

  /**
   * The distance from the point's center to its edge, measured in the same
   * units as x & y (e.g. degrees if WGS84).
   */
  double getRadius();

}
