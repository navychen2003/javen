package org.javenstudio.falcon.search.schema.spatial;

/**
 * A base class for a Distance Calculator that assumes a spherical earth model.
 */
public abstract class GeodesicSphereDistCalc extends AbstractDistanceCalculator {

  private static final double radiusDEG = DistanceUtils.toDegrees(1);//in degrees

  @Override
  public Point pointOnBearing(Point from, double distDEG, 
		  double bearingDEG, SpatialContext ctx, Point reuse) throws InvalidShapeException {
    if (distDEG == 0) {
      if (reuse == null)
        return from;
      reuse.reset(from.getX(), from.getY());
      return reuse;
    }
    Point result = DistanceUtils.pointOnBearingRAD(
    		DistanceUtils.toRadians(from.getY()), DistanceUtils.toRadians(from.getX()),
    		DistanceUtils.toRadians(distDEG),
    		DistanceUtils.toRadians(bearingDEG), ctx, reuse);//output result is in radians
    result.reset(DistanceUtils.toDegrees(result.getX()), DistanceUtils.toDegrees(result.getY()));
    return result;
  }

  @Override
  public Rectangle calcBoxByDistFromPt(Point from, double distDEG, 
		  SpatialContext ctx, Rectangle reuse) throws InvalidShapeException {
    return DistanceUtils.calcBoxByDistFromPtDEG(from.getY(), from.getX(), distDEG, ctx, reuse);
  }

  @Override
  public double calcBoxByDistFromPt_yHorizAxisDEG(Point from, double distDEG, SpatialContext ctx) {
    return DistanceUtils.calcBoxByDistFromPt_latHorizAxisDEG(from.getY(), from.getX(), distDEG);
  }

  @Override
  public double area(Rectangle rect) {
    //From http://mathforum.org/library/drmath/view/63767.html
    double lat1 = DistanceUtils.toRadians(rect.getMinY());
    double lat2 = DistanceUtils.toRadians(rect.getMaxY());
    return Math.PI / 180 * radiusDEG * radiusDEG *
            Math.abs(Math.sin(lat1) - Math.sin(lat2)) *
            rect.getWidth();
  }

  @Override
  public double area(Circle circle) {
    //formula is a simplified case of area(rect).
    double lat = DistanceUtils.toRadians(90 - circle.getRadius());
    return 2 * Math.PI * radiusDEG * radiusDEG * (1 - Math.sin(lat));
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null)
      return false;
    return getClass().equals(obj.getClass());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public final double distance(Point from, double toX, double toY) {
    return DistanceUtils.toDegrees(distanceLatLonRAD(DistanceUtils.toRadians(from.getY()), 
    		DistanceUtils.toRadians(from.getX()), DistanceUtils.toRadians(toY), DistanceUtils.toRadians(toX)));
  }

  protected abstract double distanceLatLonRAD(double lat1, double lon1, double lat2, double lon2);

  public static class Haversine extends GeodesicSphereDistCalc {

    @Override
    protected double distanceLatLonRAD(double lat1, double lon1, double lat2, double lon2) {
      return DistanceUtils.distHaversineRAD(lat1,lon1,lat2,lon2);
    }

  }

  public static class LawOfCosines extends GeodesicSphereDistCalc {

    @Override
    protected double distanceLatLonRAD(double lat1, double lon1, double lat2, double lon2) {
      return DistanceUtils.distLawOfCosinesRAD(lat1, lon1, lat2, lon2);
    }

  }

  public static class Vincenty extends GeodesicSphereDistCalc {

    @Override
    protected double distanceLatLonRAD(double lat1, double lon1, double lat2, double lon2) {
      return DistanceUtils.distVincentyRAD(lat1, lon1, lat2, lon2);
    }
  }
}
