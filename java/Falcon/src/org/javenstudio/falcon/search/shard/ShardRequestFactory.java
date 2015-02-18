package org.javenstudio.falcon.search.shard;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.ResponseBuilder;

/**
 * Responsible for creating shard requests to the shards 
 * in the cluster to perform distributed grouping.
 *
 */
public interface ShardRequestFactory {

	/**
	 * Returns {@link ShardRequest} instances.
	 * Never returns <code>null</code>. If no {@link ShardRequest} instances 
	 * are constructed an empty array is returned.
	 *
	 * @param rb The response builder
	 * @return {@link ShardRequest} instances
	 */
	public ShardRequest[] constructRequest(ResponseBuilder rb) throws ErrorException;

}
