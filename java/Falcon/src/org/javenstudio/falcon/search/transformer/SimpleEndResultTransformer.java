package org.javenstudio.falcon.search.transformer;

import java.util.Map;

import org.javenstudio.common.indexdb.IScoreDoc;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.util.ResultList;
import org.javenstudio.hornet.grouping.GroupDocs;
import org.javenstudio.hornet.grouping.TopGroups;
import org.javenstudio.falcon.search.ResponseBuilder;

/**
 * Implementation of {@link EndResultTransformer} that transforms 
 * the grouped result into a single flat list.
 */
public class SimpleEndResultTransformer implements EndResultTransformer {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void transform(Map<String, ?> result, ResponseBuilder rb, 
			ResultSource documentSource) {
		NamedList<Object> commands = new NamedMap<Object>();
		
		for (Map.Entry<String, ?> entry : result.entrySet()) {
			Object value = entry.getValue();
			
			if (TopGroups.class.isInstance(value)) {
				@SuppressWarnings("unchecked")
				TopGroups<BytesRef> topGroups = (TopGroups<BytesRef>) value;
				
				NamedList<Object> command = new NamedMap<Object>();
				command.add("matches", rb.getTotalHitCount());
				if (topGroups.getTotalGroupCount() != null) 
					command.add("ngroups", topGroups.getTotalGroupCount());
				
				ResultList docList = new ResultList();
				docList.setStart(rb.getGroupingSpec().getOffset());
				docList.setNumFound(topGroups.getTotalHitCount());

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
        
				command.add("doclist", docList);
				commands.add(entry.getKey(), command);
			}
		}

		rb.getResponse().add("grouped", commands);
	}
	
}
