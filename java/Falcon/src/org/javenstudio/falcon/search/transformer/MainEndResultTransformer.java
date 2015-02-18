package org.javenstudio.falcon.search.transformer;

import java.util.Map;

import org.javenstudio.common.indexdb.IScoreDoc;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.grouping.GroupDocs;
import org.javenstudio.hornet.grouping.TopGroups;
import org.javenstudio.falcon.search.ResponseBuilder;
import org.javenstudio.falcon.util.ResultList;

/**
 * Implementation of {@link EndResultTransformer} that transforms 
 * the grouped result into the main result list in the
 * response.
 */
public class MainEndResultTransformer implements EndResultTransformer {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void transform(Map<String, ?> result, ResponseBuilder rb, 
			ResultSource documentSource) {
		Object value = result.get(rb.getGroupingSpec().getFields()[0]);
		
		if (TopGroups.class.isInstance(value)) {
			@SuppressWarnings("unchecked")
			TopGroups<BytesRef> topGroups = (TopGroups<BytesRef>) value;
			
			ResultList docList = new ResultList();
			docList.setStart(rb.getGroupingSpec().getOffset());
			docList.setNumFound(rb.getTotalHitCount());

			Float maxScore = Float.NEGATIVE_INFINITY;
			
			for (GroupDocs<BytesRef> group : topGroups.getGroupDocs()) {
				for (IScoreDoc scoreDoc : group.getScoreDocs()) {
					if (maxScore < scoreDoc.getScore()) 
						maxScore = scoreDoc.getScore();
          
					docList.add(documentSource.retrieve(scoreDoc));
				}
			}
			
			if (maxScore != Float.NEGATIVE_INFINITY) 
				docList.setMaxScore(maxScore);
      
			rb.getResponse().add("response", docList);
		}
	}
	
}
