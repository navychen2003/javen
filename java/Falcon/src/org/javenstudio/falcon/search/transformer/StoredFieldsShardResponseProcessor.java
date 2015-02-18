package org.javenstudio.falcon.search.transformer;

import org.javenstudio.common.indexdb.search.FieldDoc;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.Searcher;
import org.javenstudio.falcon.search.ResponseBuilder;
import org.javenstudio.falcon.search.hits.ShardDoc;
import org.javenstudio.falcon.search.shard.ShardRequest;
import org.javenstudio.falcon.search.shard.ShardResponse;
import org.javenstudio.falcon.search.shard.ShardResponseProcessor;
import org.javenstudio.falcon.util.ResultItem;
import org.javenstudio.falcon.util.ResultList;

/**
 * Concrete implementation for processing the stored field values from shard responses.
 */
public class StoredFieldsShardResponseProcessor implements ShardResponseProcessor {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void process(ResponseBuilder rb, ShardRequest shardRequest) throws ErrorException {
		boolean returnScores = (rb.getFieldFlags() & Searcher.GET_SCORES) != 0;
		ShardResponse srsp = shardRequest.getResponses().get(0);
		
		ResultList docs = (ResultList)srsp.getResponse().getValue("response");
		String uniqueIdFieldName = rb.getSearchCore().getSchema().getUniqueKeyField().getName();

		for (ResultItem doc : docs) {
			Object id = doc.getFieldValue(uniqueIdFieldName).toString();
			
			ShardDoc shardDoc = rb.getResultIds().get(id);
			FieldDoc fieldDoc = (FieldDoc) shardDoc;
			
			if (shardDoc != null) {
				if (returnScores && !Float.isNaN(fieldDoc.getScore())) 
					doc.setField("score", fieldDoc.getScore());
				
				rb.addRetrievedDocument(id, doc);
			}
		}
	}
	
}
