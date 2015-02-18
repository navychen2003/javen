package org.javenstudio.falcon.search.schema;

import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.query.QueryBuilder;

/**
 * Indicate that the implementing class is capable of generating a Query against spatial resources.
 * For example, the LatLonType is capable of creating a query that restricts the document space down
 * to documents that are within a certain distance of a given point on Earth. *
 *
 **/
public interface SpatialQueryable {

	public IQuery createSpatialQuery(QueryBuilder parser, SpatialOptions options) 
			throws ErrorException;
	
}
