package org.javenstudio.falcon.search.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.hornet.query.DoubleDocValues;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.hornet.query.source.MultiValueSource;
import org.javenstudio.falcon.search.schema.spatial.DistanceUtils;

/**
 * Calculate the Haversine formula (distance) between any two points on a sphere
 * Takes in four value sources: (latA, lonA); (latB, lonB).
 * <p/>
 * Assumes the value sources are in radians unless
 * <p/>
 * See http://en.wikipedia.org/wiki/Great-circle_distance and
 * http://en.wikipedia.org/wiki/Haversine_formula for the actual formula and
 * also http://www.movable-type.co.uk/scripts/latlong.html
 */
public class HaversineFunction extends ValueSource {

	private MultiValueSource mSource1;
	private MultiValueSource mSource2;
	private boolean mConvertToRadians = false;
	private double mRadius;

	public HaversineFunction(MultiValueSource p1, MultiValueSource p2, 
			double radius) throws ErrorException {
		this(p1, p2, radius, false);
	}

	public HaversineFunction(MultiValueSource p1, MultiValueSource p2, 
			double radius, boolean convertToRads) throws ErrorException {
		mSource1 = p1;
		mSource2 = p2;
		
		if (p1.dimension() != 2 || p2.dimension() != 2) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Illegal dimension for value sources");
		}
		
		mRadius = radius;
		mConvertToRadians = convertToRads;
	}

	protected String getName() {
		return "hsin";
	}

	/**
	 * @param doc  The doc to score
	 * @return The haversine distance formula
	 */
	protected double distance(int doc, FunctionValues p1DV, FunctionValues p2DV) {
		double[] p1D = new double[2];
		double[] p2D = new double[2];
		
		p1DV.doubleVal(doc, p1D);
		p2DV.doubleVal(doc, p2D);
		
		double y1;
		double x1;
		double y2;
		double x2;
		
		if (mConvertToRadians) {
			y1 = p1D[0] * DistanceUtils.DEGREES_TO_RADIANS;
			x1 = p1D[1] * DistanceUtils.DEGREES_TO_RADIANS;
			y2 = p2D[0] * DistanceUtils.DEGREES_TO_RADIANS;
			x2 = p2D[1] * DistanceUtils.DEGREES_TO_RADIANS;
			
		} else {
			y1 = p1D[0];
			x1 = p1D[1];
			y2 = p2D[0];
			x2 = p2D[1];
		}
		
		return DistanceUtils.distHaversineRAD(y1,x1,y2,x2) * mRadius;
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		final FunctionValues vals1 = mSource1.getValues(context, readerContext);
		final FunctionValues vals2 = mSource2.getValues(context, readerContext);
		
		return new DoubleDocValues(this) {
				@Override
				public double doubleVal(int doc) {
					return distance(doc, vals1, vals2);
				}
				
				@Override
				public String toString(int doc) {
					StringBuilder sb = new StringBuilder();
					sb.append(getName()).append('(');
					sb.append(vals1.toString(doc)).append(',').append(vals2.toString(doc));
					sb.append(')');
					return sb.toString();
				}
			};
	}

	@Override
	public void createWeight(ValueSourceContext context, 
			ISearcher searcher) throws IOException {
		mSource1.createWeight(context, searcher);
		mSource2.createWeight(context, searcher);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null || this.getClass() != o.getClass()) 
			return false;
		
		HaversineFunction other = (HaversineFunction) o;
		return this.getName().equals(other.getName()) &&
            mSource1.equals(other.mSource1) &&
            mSource2.equals(other.mSource2) && mRadius == other.mRadius;
	}

	@Override
	public int hashCode() {
		int result = mSource1.hashCode();
		result = 31 * result + mSource2.hashCode();
		result = 31 * result + getName().hashCode();
		long temp = Double.doubleToRawLongBits(mRadius);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public String getDescription() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append('(');
		sb.append(mSource1).append(',').append(mSource2);
		sb.append(')');
		return sb.toString();
	}
	
}
