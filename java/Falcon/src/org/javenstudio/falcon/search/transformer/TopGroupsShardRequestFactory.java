package org.javenstudio.falcon.search.transformer;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.CommonParams;
import org.javenstudio.falcon.util.ModifiableParams;
import org.javenstudio.hornet.grouping.SearchGroup;
import org.javenstudio.falcon.search.Searcher;
import org.javenstudio.falcon.search.ResponseBuilder;
import org.javenstudio.falcon.search.grouping.Grouping;
import org.javenstudio.falcon.search.params.GroupParams;
import org.javenstudio.falcon.search.params.ShardParams;
import org.javenstudio.falcon.search.schema.SchemaFieldType;
import org.javenstudio.falcon.search.shard.ShardRequest;
import org.javenstudio.falcon.search.shard.ShardRequestFactory;
import org.javenstudio.panda.analysis.ReverseStringFilter;

/**
 * Concrete implementation of {@link ShardRequestFactory} that creates 
 * {@link ShardRequest} instances for getting the
 * top groups from all shards.
 */
public class TopGroupsShardRequestFactory implements ShardRequestFactory {

	/** Represents a string value for */
	public static final String GROUP_NULL_VALUE = "" + ReverseStringFilter.START_OF_HEADING_MARKER;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ShardRequest[] constructRequest(ResponseBuilder rb) throws ErrorException {
		// If we have a group.query we need to query all shards... 
		// Or we move this to the group first phase queries
		boolean containsGroupByQuery = rb.getGroupingSpec().getQueries().length > 0;
		
		// TODO: If groups.truncate=true we only have to query the specific 
		// shards even faceting and statistics are enabled
		if ((rb.getQueryCommand().getFlags() & Searcher.GET_DOCSET) != 0 || containsGroupByQuery) {
			// In case we need more results such as faceting and statistics we have to query all shards
			return createRequestForAllShards(rb);
			
		} else {
			// In case we only need top groups we only have to query the shards that contain these groups.
			return createRequestForSpecificShards(rb);
		}
	}

	private ShardRequest[] createRequestForSpecificShards(ResponseBuilder rb) 
			throws ErrorException {
		// Determine all unique shards to query for TopGroups
		Set<String> uniqueShards = new HashSet<String>();
		
		for (String command : rb.getSearchGroupToShardsKeySet()) {
			Map<SearchGroup<BytesRef>, Set<String>> groupsToShard = rb.getSearchGroupToShard(command);
			for (Set<String> shards : groupsToShard.values()) {
				uniqueShards.addAll(shards);
			}
		}

		return createRequest(rb, uniqueShards.toArray(new String[uniqueShards.size()]));
	}

	private ShardRequest[] createRequestForAllShards(ResponseBuilder rb) 
			throws ErrorException {
		return createRequest(rb, ShardRequest.ALL_SHARDS);
	}

	private ShardRequest[] createRequest(ResponseBuilder rb, String[] shards) 
			throws ErrorException {
		ShardRequest sreq = new ShardRequest();
		sreq.setShards(shards);
		sreq.setPurpose(ShardRequest.PURPOSE_GET_TOP_IDS);
		sreq.setParams(new ModifiableParams(rb.getRequest().getParams()));

		// If group.format=simple group.offset doesn't make sense
		Grouping.Format responseFormat = rb.getGroupingSpec().getResponseFormat();
		if (responseFormat == Grouping.Format.SIMPLE || rb.getGroupingSpec().isMain()) 
			sreq.getParams().remove(GroupParams.GROUP_OFFSET);
		
		sreq.getParams().remove(ShardParams.SHARDS);

		// set the start (offset) to 0 for each shard request so we can properly merge
		// results from the start.
		if (rb.getShardsStart() > -1) {
			// if the client set shards.start set this explicitly
			sreq.getParams().set(CommonParams.START, rb.getShardsStart());
			
		} else {
			sreq.getParams().set(CommonParams.START, "0");
		}
		
		if (rb.getShardsRows() > -1) {
			// if the client set shards.rows set this explicity
			sreq.getParams().set(CommonParams.ROWS, rb.getShardsRows());
			
		} else {
			sreq.getParams().set(CommonParams.ROWS, 
					rb.getSortSpec().getOffset() + rb.getSortSpec().getCount());
		}

		sreq.getParams().set(GroupParams.GROUP_DISTRIBUTED_SECOND, "true");
		
		for (String field : rb.getMergedSearchGroupsKeySet()) {
			Collection<SearchGroup<BytesRef>> groups = rb.getMergedSearchGroup(field);
			
			for (SearchGroup<BytesRef> searchGroup : groups) {
				String groupValue;
				
				if (searchGroup.getGroupValue() != null) {
					String rawGroupValue = searchGroup.getGroupValue().utf8ToString();
					SchemaFieldType fieldType = rb.getSearchCore().getSchema().getField(field).getType();
					groupValue = fieldType.indexedToReadable(rawGroupValue);
					
				} else {
					groupValue = GROUP_NULL_VALUE;
				}
				
				sreq.getParams().add(GroupParams.GROUP_DISTRIBUTED_TOPGROUPS_PREFIX + field, groupValue);
			}
		}

		if ((rb.getFieldFlags() & Searcher.GET_SCORES) != 0 || rb.getSortSpec().includesScore()) {
			sreq.getParams().set(CommonParams.FL, 
					rb.getSearchCore().getSchema().getUniqueKeyField().getName() + ",score");
			
		} else {
			sreq.getParams().set(CommonParams.FL, 
					rb.getSearchCore().getSchema().getUniqueKeyField().getName());
		}
    
		int origTimeAllowed = sreq.getParams().getInt(CommonParams.TIME_ALLOWED, -1);
		if (origTimeAllowed > 0) {
			sreq.getParams().set(CommonParams.TIME_ALLOWED, 
					Math.max(1,origTimeAllowed - rb.getFirstPhaseElapsedTime()));
		}

		return new ShardRequest[] {sreq};
	}

}
