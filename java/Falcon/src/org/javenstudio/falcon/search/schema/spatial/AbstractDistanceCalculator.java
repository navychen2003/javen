package org.javenstudio.falcon.search.schema.spatial;

/**
 */
public abstract class AbstractDistanceCalculator implements DistanceCalculator {

  @Override
  public double distance(Point from, Point to) {
    return distance(from, to.getX(), to.getY());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
