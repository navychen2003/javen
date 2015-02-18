package org.javenstudio.falcon.search.transformer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.indexdb.IScoreDoc;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.ITopDocs;
import org.javenstudio.common.indexdb.search.TopDocs;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.hornet.grouping.GroupDocs;
import org.javenstudio.hornet.grouping.ScoreMergeMode;
import org.javenstudio.hornet.grouping.TopGroups;
import org.javenstudio.hornet.search.TopDocsHelper;
import org.javenstudio.falcon.search.ResponseBuilder;
import org.javenstudio.falcon.search.grouping.Grouping;
import org.javenstudio.falcon.search.hits.ShardDoc;
import org.javenstudio.falcon.search.shard.ShardRequest;
import org.javenstudio.falcon.search.shard.ShardResponse;
import org.javenstudio.falcon.search.shard.ShardResponseProcessor;

/**
 * Concrete implementation for merging {@link TopGroups} instances from shard responses.
 */
public class TopGroupsShardResponseProcessor implements ShardResponseProcessor {

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void process(ResponseBuilder rb, ShardRequest shardRequest) throws ErrorException {
		String[] fields = rb.getGroupingSpec().getFields();
		String[] queries = rb.getGroupingSpec().getQueries();
		
		ISort groupSort = rb.getGroupingSpec().getGroupSort();
		ISort sortWithinGroup = rb.getGroupingSpec().getSortWithinGroup();

		// If group.format=simple group.offset doesn't make sense
		int groupOffsetDefault;
		if (rb.getGroupingSpec().getResponseFormat() == Grouping.Format.SIMPLE || 
			rb.getGroupingSpec().isMain()) {
			groupOffsetDefault = 0;
		} else {
			groupOffsetDefault = rb.getGroupingSpec().getGroupOffset();
		}
		
		int docsPerGroupDefault = rb.getGroupingSpec().getGroupLimit();

		Map<String, List<TopGroups<BytesRef>>> commandTopGroups = 
				new HashMap<String, List<TopGroups<BytesRef>>>();
		
		for (String field : fields) {
			commandTopGroups.put(field, new ArrayList<TopGroups<BytesRef>>());
		}

		Map<String, List<QueryFieldResult>> commandTopDocs = 
				new HashMap<String, List<QueryFieldResult>>();
		
		for (String query : queries) {
			commandTopDocs.put(query, new ArrayList<QueryFieldResult>());
		}

		TopGroupsResultTransformer serializer = new TopGroupsResultTransformer(rb);
		
		for (ShardResponse srsp : shardRequest.getResponses()) {
			NamedList<NamedList<?>> secondPhaseResult = (NamedList<NamedList<?>>) 
					srsp.getResponse().getValue("secondPhase");
			
			Map<String, ?> result = serializer.transformToNative(secondPhaseResult, 
					groupSort, sortWithinGroup, srsp.getShard());
			
			for (String field : commandTopGroups.keySet()) {
				TopGroups<BytesRef> topGroups = (TopGroups<BytesRef>) result.get(field);
				if (topGroups == null) 
					continue;
				
				commandTopGroups.get(field).add(topGroups);
			}
			
			for (String query : queries) {
				commandTopDocs.get(query).add((QueryFieldResult) result.get(query));
			}
		}
		
		try {
			for (String groupField : commandTopGroups.keySet()) {
				List<TopGroups<BytesRef>> topGroups = commandTopGroups.get(groupField);
				if (topGroups.isEmpty()) 
					continue;

				TopGroups<BytesRef>[] topGroupsArr = new TopGroups[topGroups.size()];
				rb.addMergedTopGroup(groupField, 
						TopGroups.merge(topGroups.toArray(topGroupsArr), 
								groupSort, sortWithinGroup, groupOffsetDefault, docsPerGroupDefault, 
								ScoreMergeMode.None));
			}

			for (String query : commandTopDocs.keySet()) {
				List<QueryFieldResult> queryCommandResults = commandTopDocs.get(query);
				List<ITopDocs> topDocs = new ArrayList<ITopDocs>(queryCommandResults.size());
				
				int mergedMatches = 0;
				
				for (QueryFieldResult queryCommandResult : queryCommandResults) {
					topDocs.add(queryCommandResult.getTopDocs());
					mergedMatches += queryCommandResult.getMatches();
				}

				int topN = rb.getGroupingSpec().getOffset() + rb.getGroupingSpec().getLimit();
				
				ITopDocs mergedTopDocs = TopDocsHelper.merge(sortWithinGroup, topN, 
						topDocs.toArray(new TopDocs[topDocs.size()]));
				
				rb.addMergedQueryCommandResult(query, 
						new QueryFieldResult(mergedTopDocs, mergedMatches));
			}

			Map<Object, ShardDoc> resultIds = new HashMap<Object, ShardDoc>();
			int i = 0;
			
			for (TopGroups<BytesRef> topGroups : rb.getMergedTopGroupsValues()) {
				for (GroupDocs<BytesRef> group : topGroups.getGroupDocs()) {
					for (IScoreDoc scoreDoc : group.getScoreDocs()) {
						ShardDoc shardDoc = (ShardDoc) scoreDoc;
						shardDoc.setPositionInResponse(i++);
						
						resultIds.put(shardDoc.getId(), shardDoc);
					}
				}
			}
			
			for (QueryFieldResult queryCommandResult : rb.getMergedQueryCommandResultsValues()) {
				for (IScoreDoc scoreDoc : queryCommandResult.getTopDocs().getScoreDocs()) {
					ShardDoc shardDoc = (ShardDoc) scoreDoc;
					shardDoc.setPositionInResponse(i++);
					
					resultIds.put(shardDoc.getId(), shardDoc);
				}
			}

			rb.setResultIds(resultIds);
		} catch (IOException e) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
}
