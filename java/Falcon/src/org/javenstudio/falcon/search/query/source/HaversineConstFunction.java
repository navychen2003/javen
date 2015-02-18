package org.javenstudio.falcon.search.query.source;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.hornet.query.DoubleDocValues;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.hornet.query.source.ConstNumberSource;
import org.javenstudio.hornet.query.source.DoubleConstValueSource;
import org.javenstudio.hornet.query.source.MultiValueSource;
import org.javenstudio.hornet.query.source.VectorValueSource;
import org.javenstudio.falcon.search.params.SpatialParams;
import org.javenstudio.falcon.search.query.FunctionQueryBuilder;
import org.javenstudio.falcon.search.query.ValueSourceParser;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.search.schema.spatial.DistanceUtils;
import org.javenstudio.falcon.search.schema.spatial.InvalidShapeException;
import org.javenstudio.falcon.search.schema.spatial.SpatialUtils;

/**
 * Haversine function with one point constant
 */
public class HaversineConstFunction extends ValueSource {

	public static ValueSourceParser getDefaultParser() { 
		return sParser;
	}
	
	private static ValueSourceParser sParser = new ValueSourceParser() {
		@SuppressWarnings("unused")
		@Override
		public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
			// TODO: dispatch through SpatialQueryable in the future?
			List<ValueSource> sources = fp.parseValueSourceList();

			// "m" is a multi-value source, "x" is a single-value source
			// allow (m,m) (m,x,x) (x,x,m) (x,x,x,x)
			// if not enough points are present, "pt" will be checked first, followed by "sfield".      

			MultiValueSource mv1 = null;
			MultiValueSource mv2 = null;

			if (sources.size() == 0) {
				// nothing to do now
				
			} else if (sources.size() == 1) {
				ValueSource vs = sources.get(0);
				if (!(vs instanceof MultiValueSource)) {
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"geodist - invalid parameters:" + sources);
				}
				
				mv1 = (MultiValueSource)vs;
				
			} else if (sources.size() == 2) {
				ValueSource vs1 = sources.get(0);
				ValueSource vs2 = sources.get(1);

				if (vs1 instanceof MultiValueSource && vs2 instanceof MultiValueSource) {
					mv1 = (MultiValueSource)vs1;
					mv2 = (MultiValueSource)vs2;
					
				} else {
					mv1 = makeMV(sources, sources);
				}
				
			} else if (sources.size()==3) {
				ValueSource vs1 = sources.get(0);
				ValueSource vs2 = sources.get(1);
				
				if (vs1 instanceof MultiValueSource) {	// (m,x,x)
					mv1 = (MultiValueSource)vs1;
					mv2 = makeMV(sources.subList(1,3), sources);
					
				} else { 	// (x,x,m)
					mv1 = makeMV(sources.subList(0,2), sources);
					vs1 = sources.get(2);
					
					if (!(vs1 instanceof MultiValueSource)) {
						throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
								"geodist - invalid parameters:" + sources);
					}
					
					mv2 = (MultiValueSource)vs1;
				}
				
			} else if (sources.size()==4) {
				mv1 = makeMV(sources.subList(0,2), sources);
				mv2 = makeMV(sources.subList(2,4), sources);
				
			} else if (sources.size() > 4) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"geodist - invalid parameters:" + sources);
			}

			if (mv1 == null) {
				mv1 = parsePoint(fp);
				mv2 = parseSfield(fp);
				
			} else if (mv2 == null) {
				mv2 = parsePoint(fp);
				if (mv2 == null)
					mv2 = parseSfield(fp);
			}

			if (mv1 == null || mv2 == null) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"geodist - not enough parameters:" + sources);
			}

			// We have all the parameters at this point, now check if one of the points is constant
			double[] constants = getConstants(mv1);
			MultiValueSource other = mv2;
			
			if (constants == null) {
				constants = getConstants(mv2);
				other = mv1;
			}

			if (constants != null && other instanceof VectorValueSource) {
				return new HaversineConstFunction(constants[0], constants[1], 
						(VectorValueSource)other);
			}      

			return new HaversineFunction(mv1, mv2, 
					DistanceUtils.EARTH_MEAN_RADIUS_KM, true);
		}
	};

	/** make a MultiValueSource from two non MultiValueSources */
	private static VectorValueSource makeMV(List<ValueSource> sources, List<ValueSource> orig) 
			throws ErrorException {
		ValueSource vs1 = sources.get(0);
		ValueSource vs2 = sources.get(1);

		if (vs1 instanceof MultiValueSource || vs2 instanceof MultiValueSource) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"geodist - invalid parameters:" + orig);
		}
		
		return  new VectorValueSource(sources);
	}

	private static MultiValueSource parsePoint(FunctionQueryBuilder fp) throws ErrorException {
		String pt = fp.getParam(SpatialParams.POINT);
		if (pt == null) 
			return null;
		
		double[] point = null;
		try {
			point = SpatialUtils.parseLatitudeLongitude(pt);
		} catch (InvalidShapeException e) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Bad spatial pt:" + pt);
		}
		
		return new VectorValueSource(Arrays.<ValueSource>asList(
				new DoubleConstValueSource(point[0]), 
				new DoubleConstValueSource(point[1])));
	}

	private static double[] getConstants(MultiValueSource vs) {
		if (!(vs instanceof VectorValueSource)) 
			return null;
		
		List<ValueSource> sources = ((VectorValueSource)vs).getSources();
		
		if (sources.get(0) instanceof ConstNumberSource && 
			sources.get(1) instanceof ConstNumberSource) {
			return new double[] { 
					((ConstNumberSource) sources.get(0)).getDouble(), 
					((ConstNumberSource) sources.get(1)).getDouble()};
		}
		
		return null;
	}

	private static MultiValueSource parseSfield(FunctionQueryBuilder fp) throws ErrorException {
		String sfield = fp.getParam(SpatialParams.FIELD);
		if (sfield == null) 
			return null;
		
		SchemaField sf = fp.getSearchCore().getSchema().getField(sfield);
		ValueSource vs = sf.getType().getValueSource(sf, fp);
		
		if (!(vs instanceof MultiValueSource)) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Spatial field must implement MultiValueSource:" + sf);
		}
		
		return (MultiValueSource)vs;
	}

	private static final double EARTH_MEAN_DIAMETER = DistanceUtils.EARTH_MEAN_RADIUS_KM * 2;
	
	private final double mLatCenterRadCos; // cos(latCenter)
	
	private final double mLatCenter;
	private final double mLonCenter;
	private final VectorValueSource mSource2;  // lat+lon, just saved for display/debugging
	private final ValueSource mLatSource;
	private final ValueSource mLonSource;

	public HaversineConstFunction(double latCenter, double lonCenter, 
			VectorValueSource vs) {
		mLatCenter = latCenter;
		mLonCenter = lonCenter;
		mSource2 = vs;
		mLatSource = mSource2.getSources().get(0);
		mLonSource = mSource2.getSources().get(1);
		mLatCenterRadCos = Math.cos(latCenter * DistanceUtils.DEGREES_TO_RADIANS);
	}

	protected String getName() {
		return "geodist";
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		final FunctionValues latVals = mLatSource.getValues(context, readerContext);
		final FunctionValues lonVals = mLonSource.getValues(context, readerContext);
		final double latCenterRad = mLatCenter * DistanceUtils.DEGREES_TO_RADIANS;
		final double lonCenterRad = mLonCenter * DistanceUtils.DEGREES_TO_RADIANS;
		final double latCenterRad_cos = mLatCenterRadCos;

		return new DoubleDocValues(this) {
			@Override
			public double doubleVal(int doc) {
				double latRad = latVals.doubleVal(doc) * DistanceUtils.DEGREES_TO_RADIANS;
				double lonRad = lonVals.doubleVal(doc) * DistanceUtils.DEGREES_TO_RADIANS;
				
				double diffX = latCenterRad - latRad;
				double diffY = lonCenterRad - lonRad;
				
				double hsinX = Math.sin(diffX * 0.5);
				double hsinY = Math.sin(diffY * 0.5);
				
				double h = hsinX * hsinX +
						(latCenterRad_cos * Math.cos(latRad) * hsinY * hsinY);
				
				return (EARTH_MEAN_DIAMETER * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h)));
			}
			
			@Override
			public String toString(int doc) {
				return getName() + '(' + latVals.toString(doc) + ',' + lonVals.toString(doc) 
						+ ',' + mLatCenter + ',' + mLonCenter + ')';
			}
		};
	}

	@Override
	public void createWeight(ValueSourceContext context, 
			ISearcher searcher) throws IOException {
		mLatSource.createWeight(context, searcher);
		mLonSource.createWeight(context, searcher);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null || !(o instanceof HaversineConstFunction)) 
			return false;
		
		HaversineConstFunction other = (HaversineConstFunction) o;
		return this.mLatCenter == other.mLatCenter
				&& this.mLonCenter == other.mLonCenter
				&& this.mSource2.equals(other.mSource2);
	}

	@Override
	public int hashCode() {
		long temp;
		int result = mSource2.hashCode();
		temp = Double.doubleToRawLongBits(mLatCenter);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToRawLongBits(mLonCenter);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public String getDescription() {
		return getName() + '(' + mSource2 + ',' + mLatCenter + ',' + mLonCenter + ')';
	}
	
}
