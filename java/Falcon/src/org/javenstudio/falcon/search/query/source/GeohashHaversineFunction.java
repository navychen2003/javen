package org.javenstudio.falcon.search.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.hornet.query.DoubleDocValues;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.falcon.search.schema.spatial.DistanceUtils;
import org.javenstudio.falcon.search.schema.spatial.GeodesicSphereDistCalc;
import org.javenstudio.falcon.search.schema.spatial.GeohashUtils;
import org.javenstudio.falcon.search.schema.spatial.InvalidShapeException;
import org.javenstudio.falcon.search.schema.spatial.Point;
import org.javenstudio.falcon.search.schema.spatial.SpatialContext;

/**
 *  Calculate the Haversine distance between two geo hash codes.
 *
 * <p/>
 * Ex: ghhsin(ValueSource, ValueSource, radius)
 * <p/>
 *
 * @see HaversineFunction for more details on the implementation
 */
public class GeohashHaversineFunction extends ValueSource {

	private final ValueSource mGeoHash1, mGeoHash2;
	private final SpatialContext mContext;
	private final double mDegreesToDist;

	public GeohashHaversineFunction(ValueSource geoHash1, ValueSource geoHash2, double radius) {
		mGeoHash1 = geoHash1;
		mGeoHash2 = geoHash2;
		mDegreesToDist = DistanceUtils.degrees2Dist(1, radius);
		mContext = SpatialContext.GEO;
		assert mContext.getDistCalc() instanceof GeodesicSphereDistCalc.Haversine;
	}

	protected String getName() {
		return "ghhsin";
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		final FunctionValues gh1DV = mGeoHash1.getValues(context, readerContext);
		final FunctionValues gh2DV = mGeoHash2.getValues(context, readerContext);

		return new DoubleDocValues(this) {
				@Override
				public double doubleVal(int doc) {
					return distance(doc, gh1DV, gh2DV);
				}
				
				@Override
				public String toString(int doc) {
					StringBuilder sb = new StringBuilder();
					sb.append(getName()).append('(');
					sb.append(gh1DV.toString(doc)).append(',').append(gh2DV.toString(doc));
					sb.append(')');
					return sb.toString();
				}
			};
	}

	protected double distance(int doc, FunctionValues gh1DV, FunctionValues gh2DV) {
		double result = 0;
		
		try {
			String h1 = gh1DV.stringVal(doc);
			String h2 = gh2DV.stringVal(doc);
			
			if (h1 != null && h2 != null && h1.equals(h2) == false){
				//TODO: If one of the hashes is a literal value source, seems like we could cache it
				//and avoid decoding every time
				Point p1 = GeohashUtils.decode(h1, mContext);
				Point p2 = GeohashUtils.decode(h2, mContext);
				
				result = mContext.getDistCalc().distance(p1, p2) * mDegreesToDist;
				
			} else if (h1 == null || h2 == null){
				result = Double.MAX_VALUE;
			}
		} catch (InvalidShapeException ex) { 
			throw new RuntimeException(ex);
		}
		
		return result;
	}

	@Override
	public void createWeight(ValueSourceContext context, ISearcher searcher) 
			throws IOException {
		mGeoHash1.createWeight(context, searcher);
		mGeoHash2.createWeight(context, searcher);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null || this.getClass() != o.getClass()) 
			return false;
		
		GeohashHaversineFunction other = (GeohashHaversineFunction) o;
		return this.getName().equals(other.getName()) &&
				this.mGeoHash1.equals(other.mGeoHash1) &&
				this.mGeoHash2.equals(other.mGeoHash2) &&
				this.mDegreesToDist == other.mDegreesToDist;
	}

	@Override
	public int hashCode() {
		int result = mGeoHash1.hashCode();
		result = 31 * result + mGeoHash2.hashCode();
		result = 31 * result + getName().hashCode();
		long temp = Double.doubleToRawLongBits(mDegreesToDist);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public String getDescription() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append('(');
		sb.append(mGeoHash1).append(',').append(mGeoHash2);
		sb.append(')');
		return sb.toString();
	}
	
}
