package org.javenstudio.falcon.search.schema;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.document.FieldType;
import org.javenstudio.common.indexdb.document.Fieldable;
import org.javenstudio.common.indexdb.search.SortField;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.TextWriter;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.search.query.BooleanClause;
import org.javenstudio.hornet.search.query.BooleanQuery;
import org.javenstudio.falcon.search.query.QueryBuilder;
import org.javenstudio.falcon.search.schema.spatial.DistanceUtils;
import org.javenstudio.falcon.search.schema.spatial.InvalidShapeException;
import org.javenstudio.falcon.search.schema.spatial.Rectangle;
import org.javenstudio.falcon.search.schema.spatial.SpatialContext;
import org.javenstudio.falcon.search.schema.spatial.SpatialUtils;

/**
 * Represents a Latitude/Longitude as a 2 dimensional point. 
 * Latitude is <b>always</b> specified first.
 */
public class LatLonFieldType extends AbstractSubTypeFieldType 
		implements SpatialQueryable {
	
	public static final int LAT = 0;
	public static final int LON = 1;

	@Override
	public void init(IndexSchema schema, Map<String, String> args) 
			throws ErrorException {
		super.init(schema, args);
		
		// TODO: refactor this, as we are creating the suffix cache twice, 
		// since the super.init does it too
		// we need three extra fields: one for the storage field, 
		// two for the lat/lon
		createSuffixCache(3); 
	}

	@Override
	public Fieldable[] createFields(SchemaField field, Object value, 
			float boost) throws ErrorException {
		String externalVal = value.toString();
		
		//we could have tileDiff + 3 fields (two for the lat/lon, one for storage)
		Fieldable[] f = new Fieldable[(field.isIndexed() ? 2 : 0) + (field.isStored() ? 1 : 0)];
		if (field.isIndexed()) {
			try {
				int i = 0;
				double[] latLon = SpatialUtils.parseLatitudeLongitude(null, externalVal);
				
				//latitude
				SchemaField lat = subField(field, i);
				f[i] = lat.createField(String.valueOf(latLon[LAT]), 
						lat.isIndexed() && !lat.isOmitNorms() ? boost : 1f);
				
				i++;
				
				//longitude
				SchemaField lon = subField(field, i);
				f[i] = lon.createField(String.valueOf(latLon[LON]), 
						lon.isIndexed() && !lon.isOmitNorms() ? boost : 1f);
				
			} catch (InvalidShapeException e) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, e);
			}
		}

		if (field.isStored()) {
			FieldType customType = new FieldType();
			customType.setStored(true);
			
			f[f.length - 1] = createField(field.getName(), externalVal, customType, 1f);
		}
		
		return f;
	}

	@Override
	public IQuery getRangeQuery(QueryBuilder parser, SchemaField field, String part1, String part2, 
			boolean minInclusive, boolean maxInclusive) throws ErrorException {
		int dimension = 2;
		try {
			String[] p1 = SpatialUtils.parsePoint(null, part1, dimension);
			String[] p2 = SpatialUtils.parsePoint(null, part2, dimension);
			
		    BooleanQuery result = new BooleanQuery(true);
		    for (int i = 0; i < dimension; i++) {
		    	SchemaField subSF = subField(field, i);
		    	
		    	// points must currently be ordered... should we support 
		    	// specifying any two opposite corner points?
		    	result.add(subSF.getType().getRangeQuery(parser, subSF, p1[i], p2[i], 
		    			minInclusive, maxInclusive), BooleanClause.Occur.MUST);
		    }
			
		    return result;
		    
		} catch (InvalidShapeException e) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, e);
		}
	}

	@Override
	public IQuery getFieldQuery(QueryBuilder parser, SchemaField field, String externalVal) 
			throws ErrorException {
		int dimension = 2;
		try {
			String[] p1 = SpatialUtils.parsePoint(null, externalVal, dimension);
			
			BooleanQuery bq = new BooleanQuery(true);
			for (int i = 0; i < dimension; i++) {
				SchemaField sf = subField(field, i);
				IQuery tq = sf.getType().getFieldQuery(parser, sf, p1[i]);
				bq.add(tq, BooleanClause.Occur.MUST);
			}
			
			return bq;
			
		} catch (InvalidShapeException e) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, e);
		}
	}

	@Override
	public IQuery createSpatialQuery(QueryBuilder parser, SpatialOptions options) 
			throws ErrorException {
		try {
			double[] point = SpatialUtils.parseLatitudeLongitude(options.getPointString());
			
			// lat & lon in degrees
			double latCenter = point[LAT];
			double lonCenter = point[LON];
	    
			double distDeg = DistanceUtils.dist2Degrees(options.getDistance(), options.getRadius());
			Rectangle bbox = DistanceUtils.calcBoxByDistFromPtDEG(latCenter, lonCenter, 
					distDeg, SpatialContext.GEO, null);
			
			double latMin = bbox.getMinY();
			double latMax = bbox.getMaxY();
			double lonMin, lonMax, lon2Min, lon2Max;
			
			if (bbox.getCrossesDateLine()) {
				lonMin = -180;
				lonMax = bbox.getMaxX();
				lon2Min = bbox.getMinX();
				lon2Max = 180;
				
			} else {
				lonMin = bbox.getMinX();
				lonMax = bbox.getMaxX();
				lon2Min = -180;
				lon2Max = 180;
			}
	    
			// Now that we've figured out the ranges, build them!
			SchemaField latField = subField(options.getSchemaField(), LAT);
			SchemaField lonField = subField(options.getSchemaField(), LON);

			SpatialDistanceQuery spatial = new SpatialDistanceQuery();

			if (options.isBoundingBox()) {
				BooleanQuery result = new BooleanQuery();

				IQuery latRange = latField.getType().getRangeQuery(
						parser, latField,
						String.valueOf(latMin),
						String.valueOf(latMax),
						true, true);
				
				result.add(latRange, BooleanClause.Occur.MUST);

				if (lonMin != -180 || lonMax != 180) {
					IQuery lonRange = lonField.getType().getRangeQuery(
							parser, lonField,
							String.valueOf(lonMin),
							String.valueOf(lonMax),
							true, true);
					
					if (lon2Min != -180 || lon2Max != 180) {
						// another valid longitude range
						BooleanQuery bothLons = new BooleanQuery();
						bothLons.add(lonRange, BooleanClause.Occur.SHOULD);

						lonRange = lonField.getType().getRangeQuery(
								parser, lonField,
								String.valueOf(lon2Min),
								String.valueOf(lon2Max),
								true, true);
						
						bothLons.add(lonRange, BooleanClause.Occur.SHOULD);

						lonRange = bothLons;
					}

					result.add(lonRange, BooleanClause.Occur.MUST);
				}

				spatial.mBBoxQuery = result;
			}

			spatial.mOrigField = options.getSchemaField().getName();
			spatial.mLatSource = latField.getType().getValueSource(latField, parser);
			spatial.mLonSource = lonField.getType().getValueSource(lonField, parser);
			spatial.mLatMin = latMin;
			spatial.mLatMax = latMax;
			spatial.mLonMin = lonMin;
			spatial.mLonMax = lonMax;
			spatial.mLon2Min = lon2Min;
			spatial.mLon2Max = lon2Max;
			spatial.mLon2 = lon2Min != -180 || lon2Max != 180;

			spatial.mLatCenter = latCenter;
			spatial.mLonCenter = lonCenter;
			spatial.mDist = options.getDistance();
			spatial.mPlanetRadius = options.getRadius();

			spatial.mCalcDist = !options.isBoundingBox();

			return spatial;
		} catch (InvalidShapeException e) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, e);
		}
	}

	@Override
	public ValueSource getValueSource(SchemaField field, QueryBuilder parser) 
			throws ErrorException {
		List<ValueSource> vs = new ArrayList<ValueSource>(2);
		for (int i = 0; i < 2; i++) {
			SchemaField sub = subField(field, i);
			vs.add(sub.getType().getValueSource(sub, parser));
		}
		return new LatLonValueSource(field, vs);
	}

	@Override
	public boolean isPolyField() {
		return true;
	}

	@Override
	public void write(TextWriter writer, String name, Fieldable f) throws ErrorException {
		try {
			writer.writeString(name, f.getStringValue(), false);
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
	}

	@Override
	public SortField getSortField(SchemaField field, boolean top) throws ErrorException {
		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
				"Sorting not supported on LatLonType " + field.getName());
	}

	//It never makes sense to create a single field, so make it impossible to happen

	@Override
	public Fieldable createField(SchemaField field, Object value, float boost) {
		throw new UnsupportedOperationException("LatLonType uses multiple fields. field=" + field.getName());
	}

}
