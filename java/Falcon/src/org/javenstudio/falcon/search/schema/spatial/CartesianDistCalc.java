package org.javenstudio.falcon.search.schema.spatial;

/**
 * Calculates based on Euclidean / Cartesian 2d plane.
 */
public class CartesianDistCalc extends AbstractDistanceCalculator {

  private final boolean squared;

  public CartesianDistCalc() {
    this.squared = false;
  }

  /**
   * @param squared Set to true to have {@link #distance(com.spatial4j.core.shape.Point, com.spatial4j.core.shape.Point)}
   *                return the square of the correct answer. This is a
   *                performance optimization used when sorting in which the
   *                actual distance doesn't matter so long as the sort order is
   *                consistent.
   */
  public CartesianDistCalc(boolean squared) {
    this.squared = squared;
  }

  @Override
  public double distance(Point from, double toX, double toY) {
    double result = 0;

    double v = from.getX() - toX;
    result += (v * v);

    v = from.getY() - toY;
    result += (v * v);

    if( squared )
      return result;

    return Math.sqrt(result);
  }

  @Override
  public Point pointOnBearing(Point from, double distDEG, double bearingDEG, 
		  SpatialContext ctx, Point reuse) throws InvalidShapeException {
    if (distDEG == 0) {
      if (reuse == null)
        return from;
      reuse.reset(from.getX(), from.getY());
      return reuse;
    }
    double bearingRAD = DistanceUtils.toRadians(bearingDEG);
    double x = from.getX() + Math.sin(bearingRAD) * distDEG;
    double y = from.getY() + Math.cos(bearingRAD) * distDEG;
    if (reuse == null) {
      return ctx.makePoint(x, y);
    } else {
      reuse.reset(x, y);
      return reuse;
    }
  }

  @Override
  public Rectangle calcBoxByDistFromPt(Point from, double distDEG, 
		  SpatialContext ctx, Rectangle reuse) throws InvalidShapeException {
    double minX = from.getX() - distDEG;
    double maxX = from.getX() + distDEG;
    double minY = from.getY() - distDEG;
    double maxY = from.getY() + distDEG;
    if (reuse == null) {
      return ctx.makeRectangle(minX, maxX, minY, maxY);
    } else {
      reuse.reset(minX, maxX, minY, maxY);
      return reuse;
    }
  }

  @Override
  public double calcBoxByDistFromPt_yHorizAxisDEG(Point from, 
		  double distDEG, SpatialContext ctx) throws InvalidShapeException {
    return from.getY();
  }

  @Override
  public double area(Rectangle rect) {
    return rect.getArea(null);
  }

  @Override
  public double area(Circle circle) {
    return circle.getArea(null);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CartesianDistCalc that = (CartesianDistCalc) o;

    if (squared != that.squared) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return (squared ? 1 : 0);
  }
}
