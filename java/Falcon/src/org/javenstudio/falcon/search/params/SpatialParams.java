package org.javenstudio.falcon.search.params;

/**
 *
 */
public interface SpatialParams {
	
  public static final String POINT = "pt";
  public static final String DISTANCE = "d";
  //the field that contains the points we are measuring from "pt"
  public static final String FIELD = "sfield"; 
  
  /**
   * km - kilometers
   * mi - miles
   */
  public static final String UNITS = "units";
  
  /**
   * The distance measure to use.
   */
  public static final String MEASURE = "meas";
  
  /**
   * The radius of the sphere to use to in calculating 
   * spherical distances like Haversine
   */
  public static final String SPHERE_RADIUS = "sphere_radius";
  
}
