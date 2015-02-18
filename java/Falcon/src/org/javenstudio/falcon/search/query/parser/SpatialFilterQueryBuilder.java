package org.javenstudio.falcon.search.query.parser;

import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.params.SpatialParams;
import org.javenstudio.falcon.search.query.QueryBuilder;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.search.schema.SchemaFieldType;
import org.javenstudio.falcon.search.schema.SpatialOptions;
import org.javenstudio.falcon.search.schema.SpatialQueryable;
import org.javenstudio.falcon.search.schema.spatial.DistanceUtils;
import org.javenstudio.falcon.util.Params;

public class SpatialFilterQueryBuilder extends QueryBuilder {
	
	private boolean mBoundingBox;  // do bounding box only

	public SpatialFilterQueryBuilder(String qstr, Params localParams, Params params, 
			ISearchRequest req, boolean bbox) throws ErrorException {
		super(qstr, localParams, params, req);
		mBoundingBox = bbox;
	}

	@Override
	public IQuery parse() throws ErrorException {
		//if more than one, we need to treat them as a point...
		//TODO: Should we accept multiple fields
		String[] fields = mLocalParams.getParams("f");
		
		if (fields == null || fields.length == 0) {
			String field = getParam(SpatialParams.FIELD);
			if (field == null) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						" missing sfield for spatial request");
			}
			
			fields = new String[] {field};
		}
    
		String pointStr = getParam(SpatialParams.POINT);
		if (pointStr == null) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					SpatialParams.POINT + " missing.");
		}

		double dist = -1;
		String distS = getParam(SpatialParams.DISTANCE);
		if (distS != null) 
			dist = Double.parseDouble(distS);

		if (dist < 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					SpatialParams.DISTANCE + " must be >= 0");
		}

		String measStr = mLocalParams.get(SpatialParams.MEASURE);
		//TODO: Need to do something with Measures
		IQuery result = null;
		
		//fields is valid at this point
		if (fields.length == 1) {
			SchemaField sf = mRequest.getSearchCore().getSchema().getField(fields[0]);
			SchemaFieldType type = sf.getType();

			if (type instanceof SpatialQueryable) {
				double radius = mLocalParams.getDouble(SpatialParams.SPHERE_RADIUS, 
						DistanceUtils.EARTH_MEAN_RADIUS_KM);
				
				SpatialOptions opts = new SpatialOptions(pointStr, dist, sf, measStr, radius);
				opts.setBoundingBox(mBoundingBox);
				
				result = ((SpatialQueryable)type).createSpatialQuery(this, opts);
				
			} else {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"The field " + fields[0] + " does not support spatial filtering");
			}
			
		} else { 
			// fields.length > 1
			// TODO: Not sure about this just yet, is there a way to delegate, 
			// or do we just have a helper class?
			// Seems like we could just use FunctionQuery, but then what about scoring
			/*List<ValueSource> sources = new ArrayList<ValueSource>(fields.length);
      		for (String field : fields) {
        		SchemaField sf = schema.getField(field);
        		sources.add(sf.getType().getValueSource(sf, this));
      		}
      		MultiValueSource vs = new VectorValueSource(sources);
      		ValueSourceRangeFilter rf = new ValueSourceRangeFilter(vs, "0", 
      			String.valueOf(dist), true, true);
      		result = new SolrConstantScoreQuery(rf);*/
		}

		return result;
	}
	
}
