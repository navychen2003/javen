package org.javenstudio.falcon.search.transformer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.javenstudio.common.indexdb.IScoreDoc;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.CommonParams;
import org.javenstudio.falcon.util.ModifiableParams;
import org.javenstudio.falcon.util.StrHelper;
import org.javenstudio.hornet.grouping.GroupDocs;
import org.javenstudio.hornet.grouping.TopGroups;
import org.javenstudio.falcon.search.ResponseBuilder;
import org.javenstudio.falcon.search.hits.ShardDoc;
import org.javenstudio.falcon.search.params.GroupParams;
import org.javenstudio.falcon.search.params.ShardParams;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.search.shard.ShardRequest;
import org.javenstudio.falcon.search.shard.ShardRequestFactory;

/**
 *
 */
public class StoredFieldsShardRequestFactory implements ShardRequestFactory {

	@Override
	public ShardRequest[] constructRequest(ResponseBuilder rb) throws ErrorException {
		Map<String, Set<ShardDoc>> shardMap = new HashMap<String,Set<ShardDoc>>();
		
		for (TopGroups<BytesRef> topGroups : rb.getMergedTopGroupsValues()) {
			for (GroupDocs<BytesRef> group : topGroups.getGroupDocs()) {
				mapShardToDocs(shardMap, group.getScoreDocs());
			}
		}

		for (QueryFieldResult queryCommandResult : rb.getMergedQueryCommandResultsValues()) {
			mapShardToDocs(shardMap, queryCommandResult.getTopDocs().getScoreDocs());
		}

		ShardRequest[] shardRequests = new ShardRequest[shardMap.size()];
		SchemaField uniqueField = rb.getSearchCore().getSchema().getUniqueKeyField();
		
		int i = 0;
		for (Collection<ShardDoc> shardDocs : shardMap.values()) {
			ShardRequest sreq = new ShardRequest();
			
			sreq.setPurpose(ShardRequest.PURPOSE_GET_FIELDS);
			sreq.setShards(new String[] {shardDocs.iterator().next().getShard()});
			sreq.setParams(new ModifiableParams());
			sreq.getParams().add( rb.getRequest().getParams());
			sreq.getParams().remove(GroupParams.GROUP);
			sreq.getParams().remove(CommonParams.SORT);
			sreq.getParams().remove(ResponseBuilder.FIELD_SORT_VALUES);
			
			String fl = sreq.getParams().get(CommonParams.FL);
			if (fl != null) {
				fl = fl.trim();
				// currently, "score" is synonymous with "*,score" so
				// don't add "id" if the fl is empty or "score" or it would change the meaning.
				if (fl.length()!=0 && !"score".equals(fl) && !"*".equals(fl)) 
					sreq.getParams().set(CommonParams.FL, fl+','+uniqueField.getName());
			}

			List<String> ids = new ArrayList<String>(shardDocs.size());
			for (ShardDoc shardDoc : shardDocs) {
				ids.add(shardDoc.getId().toString());
			}
			
			sreq.getParams().add(ShardParams.IDS, StrHelper.join(ids, ','));
			shardRequests[i++] = sreq;
		}

		return shardRequests;
	}

	private void mapShardToDocs(Map<String, Set<ShardDoc>> shardMap, IScoreDoc[] scoreDocs) {
		for (IScoreDoc scoreDoc : scoreDocs) {
			ShardDoc shardDoc = (ShardDoc) scoreDoc;
			
			Set<ShardDoc> shardDocs = shardMap.get(shardDoc.getShard());
			if (shardDocs == null) 
				shardMap.put(shardDoc.getShard(), shardDocs = new HashSet<ShardDoc>());
			
			shardDocs.add(shardDoc);
		}
	}

}
