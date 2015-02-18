package org.javenstudio.falcon.search.schema.type;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.document.FieldType;
import org.javenstudio.common.indexdb.document.Fieldable;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.MapParams;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.util.TextWriter;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.search.query.BooleanClause;
import org.javenstudio.hornet.search.query.BooleanQuery;
import org.javenstudio.falcon.search.query.QueryBuilder;
import org.javenstudio.falcon.search.schema.CoordinateFieldType;
import org.javenstudio.falcon.search.schema.IndexSchema;
import org.javenstudio.falcon.search.schema.PointValueSource;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.search.schema.SpatialOptions;
import org.javenstudio.falcon.search.schema.SpatialQueryable;
import org.javenstudio.falcon.search.schema.spatial.DistanceUtils;
import org.javenstudio.falcon.search.schema.spatial.InvalidShapeException;
import org.javenstudio.falcon.search.schema.spatial.SpatialUtils;

/**
 * A point type that indexes a point in an n-dimensional space as separate fields 
 * and supports range queries.
 * See {@link LatLonType} for geo-spatial queries.
 */
public class PointFieldType extends CoordinateFieldType implements SpatialQueryable {

	@Override
	public void init(IndexSchema schema, Map<String, String> args) throws ErrorException {
		Params p = new MapParams(args);
		
		mDimension = p.getInt(DIMENSION, DEFAULT_DIMENSION);
		if (mDimension < 1) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
					"The dimension must be > 0: " + mDimension);
		}
		
		args.remove(DIMENSION);
		
    	mSchema = schema;
    	super.init(schema, args);

    	// cache suffixes
    	createSuffixCache(mDimension);
	}

	@Override
	public boolean isPolyField() {
		return true; // really only true if the field is indexed
	}

	@Override
	public Fieldable[] createFields(SchemaField field, Object value, float boost) 
			throws ErrorException {
		String externalVal = value.toString();
		String[] point = new String[0];
		try {
			point = SpatialUtils.parsePoint(null, externalVal, mDimension);
		} catch (InvalidShapeException e) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, e);
		}

		// TODO: this doesn't currently support polyFields as sub-field types
		int size = (field.isIndexed() ? mDimension : 0) + (field.isStored() ? 1 : 0);
		Fieldable[] f = new Fieldable[size];

		if (field.isIndexed()) {
			for (int i=0; i < mDimension; i++) {
				SchemaField sf = subField(field, i);
				f[i] = sf.createField(point[i], sf.isIndexed() && !sf.isOmitNorms() ? boost : 1f);
			}
		}

		if (field.isStored()) {
			String storedVal = externalVal; // normalize or not?
			FieldType customType = new FieldType();
			customType.setStored(true);
			f[f.length - 1] = createField(field.getName(), storedVal, customType, 1f);
		}
    
		return f;
	}

	/**
	 * It never makes sense to create a single field, so make it impossible to happen by
	 * throwing UnsupportedOperationException
	 */
	@Override
	public Fieldable createField(SchemaField field, Object value, float boost) {
		throw new UnsupportedOperationException("PointType uses multiple fields. field=" + field.getName());
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
	public ISortField getSortField(SchemaField field, boolean top) throws ErrorException {
		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
				"Sorting not supported on PointType " + field.getName());
	}

	@Override
	public ValueSource getValueSource(SchemaField field, QueryBuilder parser) 
			throws ErrorException {
		ArrayList<ValueSource> vs = new ArrayList<ValueSource>(mDimension);
		for (int i=0; i < mDimension; i++) {
			SchemaField sub = subField(field, i);
			vs.add(sub.getType().getValueSource(sub, parser));
		}
		return new PointValueSource(field, vs);
	}
	
	/**
	 * Care should be taken in calling this with higher order dimensions 
	 * for performance reasons.
	 */
	@Override
	public IQuery getRangeQuery(QueryBuilder parser, SchemaField field, 
			String part1, String part2, boolean minInclusive, boolean maxInclusive) 
			throws ErrorException {
		// Query could look like: [x1,y1 TO x2,y2] for 2 dimension, but could look like: 
		// [x1,y1,z1 TO x2,y2,z2], and can be extrapolated to n-dimensions
		// thus, this query essentially creates a box, cube, etc.
		final String[] p1;
		final String[] p2;
		try {
			p1 = SpatialUtils.parsePoint(null, part1, mDimension);
			p2 = SpatialUtils.parsePoint(null, part2, mDimension);
		} catch (InvalidShapeException e) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, e);
		}
		
		BooleanQuery result = new BooleanQuery(true);
		for (int i = 0; i < mDimension; i++) {
			SchemaField subSF = subField(field, i);
			// points must currently be ordered... should we support specifying 
			// any two opposite corner points?
			result.add(subSF.getType().getRangeQuery(parser, subSF, p1[i], p2[i], 
					minInclusive, maxInclusive), BooleanClause.Occur.MUST);
		}
		
		return result;
	}

	@Override
	public IQuery getFieldQuery(QueryBuilder parser, SchemaField field, 
			String externalVal) throws ErrorException {
		String[] p1 = new String[0];
		try {
			p1 = SpatialUtils.parsePoint(null, externalVal, mDimension);
		} catch (InvalidShapeException e) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, e);
		}
		
		//TODO: should we assert that p1.length == dimension?
		BooleanQuery bq = new BooleanQuery(true);
		for (int i = 0; i < mDimension; i++) {
			SchemaField sf = subField(field, i);
			IQuery tq = sf.getType().getFieldQuery(parser, sf, p1[i]);
			bq.add(tq, BooleanClause.Occur.MUST);
		}
		
		return bq;
	}

	/**
	 * Calculates the range and creates a RangeQuery (bounding box) wrapped in a BooleanQuery 
	 * (unless the dimension is 1, one range for every dimension, AND'd together by a Boolean
	 * @param parser The parser
	 * @param options The {@link SpatialOptions} for this filter.
	 * @return The Query representing the bounding box around the point.
	 */
	public IQuery createSpatialQuery(QueryBuilder parser, SpatialOptions options) 
			throws ErrorException {
		IQuery result = null;
		double [] point = new double[0];
		try {
			point = SpatialUtils.parsePointDouble(null, options.getPointString(), mDimension);
		} catch (InvalidShapeException e) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, e);
		}
		
		if (mDimension == 1){
			//TODO: Handle distance measures
			String lower = String.valueOf(point[0] - options.getDistance());
			String upper = String.valueOf(point[0] + options.getDistance());
			
			SchemaField subSF = subField(options.getSchemaField(), 0);
			
			// points must currently be ordered... should we support specifying 
			// any two opposite corner points?
			result = subSF.getType().getRangeQuery(parser, subSF, lower, upper, true, true);
			
		} else {
			BooleanQuery tmp = new BooleanQuery();
			
			//TODO: Handle distance measures, as this assumes Euclidean
			double[] ur = DistanceUtils.vectorBoxCorner(point, null, options.getDistance(), true);
			double[] ll = DistanceUtils.vectorBoxCorner(point, null, options.getDistance(), false);
			
			for (int i = 0; i < ur.length; i++) {
				SchemaField subSF = subField(options.getSchemaField(), i);
				IQuery range = subSF.getType().getRangeQuery(parser, subSF, 
						String.valueOf(ll[i]), String.valueOf(ur[i]), true, true);
				tmp.add(range, BooleanClause.Occur.MUST);
			}
			
			result = tmp;
		}
		
		return result;
	}
	
}
