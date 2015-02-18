package org.javenstudio.falcon.search.shard;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.ResponseBuilder;

/**
 * Responsible for processing shard responses.
 *
 */
public interface ShardResponseProcessor {

	/**
	 * Processes the responses from the specified shardRequest. The result is put into specific
	 * fields in the specified rb.
	 *
	 * @param rb The ResponseBuilder to put the merge result into
	 * @param shardRequest The shard request containing the responses from all shards.
	 */
	public void process(ResponseBuilder rb, ShardRequest shardRequest) throws ErrorException ;

}
