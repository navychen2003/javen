package org.javenstudio.falcon.search.transformer;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.CommonParams;
import org.javenstudio.falcon.util.ModifiableParams;
import org.javenstudio.falcon.search.Searcher;
import org.javenstudio.falcon.search.ResponseBuilder;
import org.javenstudio.falcon.search.grouping.GroupingSpecification;
import org.javenstudio.falcon.search.params.GroupParams;
import org.javenstudio.falcon.search.params.ShardParams;
import org.javenstudio.falcon.search.shard.ShardRequest;
import org.javenstudio.falcon.search.shard.ShardRequestFactory;

/**
 * Concrete implementation of {@link ShardRequestFactory} that creates 
 * {@link ShardRequest} instances for getting the
 * search groups from all shards.
 */
public class SearchGroupsRequestFactory implements ShardRequestFactory {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ShardRequest[] constructRequest(ResponseBuilder rb) throws ErrorException {
		ShardRequest sreq = new ShardRequest();
		GroupingSpecification groupingSpecification = rb.getGroupingSpec();
		if (groupingSpecification.getFields().length == 0) 
			return new ShardRequest[0];

		sreq.setPurpose(ShardRequest.PURPOSE_GET_TOP_GROUPS);
		sreq.setParams(new ModifiableParams(rb.getRequest().getParams()));
		// TODO: base on current params or original params?

		// don't pass through any shards param
		sreq.getParams().remove(ShardParams.SHARDS);

		// set the start (offset) to 0 for each shard request so we can properly merge
		// results from the start.
		if(rb.getShardsStart() > -1) {
			// if the client set shards.start set this explicitly
			sreq.getParams().set(CommonParams.START,rb.getShardsStart());
		} else {
			sreq.getParams().set(CommonParams.START, "0");
		}
		
		// TODO: should we even use the SortSpec?  That's obtained from the QParser, and
		// perhaps we shouldn't attempt to parse the query at this level?
		// Alternate Idea: instead of specifying all these things at the upper level,
		// we could just specify that this is a shard request.
		if (rb.getShardsRows() > -1) {
			// if the client set shards.rows set this explicity
			sreq.getParams().set(CommonParams.ROWS, rb.getShardsRows());
		} else {
			sreq.getParams().set(CommonParams.ROWS, rb.getSortSpec().getOffset() + 
					rb.getSortSpec().getCount());
		}

		// in this first phase, request only the unique key field
		// and any fields needed for merging.
		sreq.getParams().set(GroupParams.GROUP_DISTRIBUTED_FIRST, "true");

		if ((rb.getFieldFlags() & Searcher.GET_SCORES) != 0 || rb.getSortSpec().includesScore()) {
			sreq.getParams().set(CommonParams.FL, 
					rb.getSearchCore().getSchema().getUniqueKeyField().getName() + ",score");
		} else {
			sreq.getParams().set(CommonParams.FL, 
					rb.getSearchCore().getSchema().getUniqueKeyField().getName());
		}
		
		return new ShardRequest[] {sreq};
	}

}
