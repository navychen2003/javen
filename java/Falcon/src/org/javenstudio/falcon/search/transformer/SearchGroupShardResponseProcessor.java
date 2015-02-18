package org.javenstudio.falcon.search.transformer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.hornet.grouping.SearchGroup;
import org.javenstudio.falcon.search.ResponseBuilder;
import org.javenstudio.falcon.search.grouping.GroupingPair;
import org.javenstudio.falcon.search.hits.SortSpec;
import org.javenstudio.falcon.search.shard.ShardRequest;
import org.javenstudio.falcon.search.shard.ShardResponse;
import org.javenstudio.falcon.search.shard.ShardResponseProcessor;

/**
 * Concrete implementation for merging {@link SearchGroup} instances from shard responses.
 */
public class SearchGroupShardResponseProcessor implements ShardResponseProcessor {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void process(ResponseBuilder rb, ShardRequest shardRequest) throws ErrorException {
		SortSpec ss = rb.getSortSpec();
		ISort groupSort = rb.getGroupingSpec().getGroupSort();
		String[] fields = rb.getGroupingSpec().getFields();

		Map<String, List<Collection<SearchGroup<BytesRef>>>> commandSearchGroups = 
				new HashMap<String, List<Collection<SearchGroup<BytesRef>>>>();
		Map<String, Map<SearchGroup<BytesRef>, Set<String>>> tempSearchGroupToShards = 
				new HashMap<String, Map<SearchGroup<BytesRef>, Set<String>>>();
		
		for (String field : fields) {
			commandSearchGroups.put(field, new ArrayList<Collection<SearchGroup<BytesRef>>>(
					shardRequest.getResponses().size()));
			tempSearchGroupToShards.put(field, 
					new HashMap<SearchGroup<BytesRef>, Set<String>>());
			
			if (!rb.containsSearchGroupToShardsKey(field)) 
				rb.addSearchGroupToShard(field, new HashMap<SearchGroup<BytesRef>, Set<String>>());
		}

		SearchGroupsResultTransformer serializer = new SearchGroupsResultTransformer(
				rb.getRequest().getSearcher());
		
		try {
			int maxElapsedTime = 0;
			int hitCountDuringFirstPhase = 0;
			
			for (ShardResponse srsp : shardRequest.getResponses()) {
				maxElapsedTime = (int) Math.max(maxElapsedTime, srsp.getResponse().getElapsedTime());
				
				@SuppressWarnings("unchecked")
				NamedList<NamedList<?>> firstPhaseResult = (NamedList<NamedList<?>>) 
						srsp.getResponse().getValue("firstPhase");
				
				Map<String, GroupingPair<Integer, Collection<SearchGroup<BytesRef>>>> result = 
						serializer.transformToNative(firstPhaseResult, groupSort, null, srsp.getShard());
				
				for (String field : commandSearchGroups.keySet()) {
					GroupingPair<Integer, Collection<SearchGroup<BytesRef>>> firstPhaseCommandResult = 
							result.get(field);
					
					Integer groupCount = firstPhaseCommandResult.getA();
					if (groupCount != null) {
						Integer existingGroupCount = rb.getMergedGroupCount(field);
						// Assuming groups don't cross shard boundary...
						rb.addMergedGroupCount(field, existingGroupCount != null ? 
								existingGroupCount + groupCount : groupCount);
					}

					Collection<SearchGroup<BytesRef>> searchGroups = firstPhaseCommandResult.getB();
					if (searchGroups == null) 
						continue;

					commandSearchGroups.get(field).add(searchGroups);
					
					for (SearchGroup<BytesRef> searchGroup : searchGroups) {
						Map<SearchGroup<BytesRef>, java.util.Set<String>> map = 
								tempSearchGroupToShards.get(field);
						
						Set<String> shards = map.get(searchGroup);
						if (shards == null) {
							shards = new HashSet<String>();
							map.put(searchGroup, shards);
						}
						
						shards.add(srsp.getShard());
					}
				}
				
				hitCountDuringFirstPhase += (Integer) srsp.getResponse().getValue("totalHitCount");
			}
			
			rb.setTotalHitCount(hitCountDuringFirstPhase);
			rb.setFirstPhaseElapsedTime(maxElapsedTime);
			
			for (String groupField : commandSearchGroups.keySet()) {
				List<Collection<SearchGroup<BytesRef>>> topGroups = commandSearchGroups.get(groupField);
				Collection<SearchGroup<BytesRef>> mergedTopGroups = 
						SearchGroup.merge(topGroups, ss.getOffset(), ss.getCount(), groupSort);
				
				if (mergedTopGroups == null) 
					continue;

				rb.addMergedSearchGroup(groupField, mergedTopGroups);
				
				for (SearchGroup<BytesRef> mergedTopGroup : mergedTopGroups) {
					rb.getSearchGroupToShard(groupField).put(mergedTopGroup, 
							tempSearchGroupToShards.get(groupField).get(mergedTopGroup));
				}
			}
		} catch (IOException e) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}

}
