package org.javenstudio.falcon.search.schema;


/**
 *
 */
public class SpatialOptions {
	
	private SchemaField mField;
	
	private String mPointStr;
	private double mDistance;
	private String mMeasStr;
	
	//(planetRadius) effectively establishes the units
	private double mRadius;

	/** 
	 * Just do a "bounding box" - or any other quicker method / shape that
	 * still encompasses all of the points of interest, but may also encompass
	 * points outside.
	 */ 
	private boolean mBoundingBox;

	public SpatialOptions() {}

	public SpatialOptions(String pointStr, double dist, 
			SchemaField sf, String measStr, double radius) {
		mPointStr = pointStr;
		mDistance = dist;
		mField = sf;
		mMeasStr = measStr;
		mRadius = radius;
	}
	
	public SchemaField getSchemaField() { return mField; }
	
	public boolean isBoundingBox() { return mBoundingBox; }
	public void setBoundingBox(boolean bbox) { mBoundingBox = bbox; }
	
	public String getPointString() { return mPointStr; }
	public String getMeasString() { return mMeasStr; }
	
	public double getDistance() { return mDistance; }
	public double getRadius() { return mRadius; }
	
}
