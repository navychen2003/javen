package org.javenstudio.falcon.search.transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.indexdb.IScoreDoc;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.util.ResultList;
import org.javenstudio.hornet.grouping.GroupDocs;
import org.javenstudio.hornet.grouping.TopGroups;
import org.javenstudio.falcon.search.Searcher;
import org.javenstudio.falcon.search.ResponseBuilder;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.search.schema.SchemaFieldType;

/**
 * Implementation of {@link EndResultTransformer} that keeps 
 * each grouped result separate in the final response.
 */
public class GroupedEndResultTransformer implements EndResultTransformer {

	private final Searcher mSearcher;

	public GroupedEndResultTransformer(Searcher searcher) {
		mSearcher = searcher;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void transform(Map<String, ?> result, ResponseBuilder rb, 
			ResultSource documentSource) throws ErrorException {
		NamedList<Object> commands = new NamedList<Object>();
		
		for (Map.Entry<String, ?> entry : result.entrySet()) {
			Object value = entry.getValue();
			if (TopGroups.class.isInstance(value)) {
				@SuppressWarnings("unchecked")
				TopGroups<BytesRef> topGroups = (TopGroups<BytesRef>) value;
				
				NamedList<Object> command = new NamedMap<Object>();
				command.add("matches", rb.getTotalHitCount());
				
				Integer totalGroupCount = rb.getMergedGroupCount(entry.getKey());
				if (totalGroupCount != null) 
					command.add("ngroups", totalGroupCount);

				List<NamedList<?>> groups = new ArrayList<NamedList<?>>();
				
				SchemaField groupField = mSearcher.getSchema().getField(entry.getKey());
				SchemaFieldType groupFieldType = groupField.getType();
				
				for (GroupDocs<BytesRef> group : topGroups.getGroupDocs()) {
					NamedMap<Object> groupResult = new NamedMap<Object>();
					if (group.getGroupValue() != null) {
						groupResult.add("groupValue", groupFieldType.toObject(
								groupField.createField(group.getGroupValue().utf8ToString(), 1.0f)));
					} else {
						groupResult.add("groupValue", null);
					}
					
					ResultList docList = new ResultList();
					docList.setNumFound(group.getTotalHits());
					
					if (!Float.isNaN(group.getMaxScore())) 
						docList.setMaxScore(group.getMaxScore());
					
					docList.setStart(rb.getGroupingSpec().getGroupOffset());
					
					for (IScoreDoc scoreDoc : group.getScoreDocs()) {
						docList.add(documentSource.retrieve(scoreDoc));
					}
					
					groupResult.add("doclist", docList);
					groups.add(groupResult);
				}
				
				command.add("groups", groups);
				commands.add(entry.getKey(), command);
				
			} else if (QueryFieldResult.class.isInstance(value)) {
				QueryFieldResult queryCommandResult = (QueryFieldResult) value;
				
				NamedList<Object> command = new NamedMap<Object>();
				command.add("matches", queryCommandResult.getMatches());
				
				ResultList docList = new ResultList();
				docList.setNumFound(queryCommandResult.getTopDocs().getTotalHits());
				
				if (!Float.isNaN(queryCommandResult.getTopDocs().getMaxScore())) 
					docList.setMaxScore(queryCommandResult.getTopDocs().getMaxScore());
				
				docList.setStart(rb.getGroupingSpec().getGroupOffset());
				
				for (IScoreDoc scoreDoc :queryCommandResult.getTopDocs().getScoreDocs()){
					docList.add(documentSource.retrieve(scoreDoc));
				}
				
				command.add("doclist", docList);
				commands.add(entry.getKey(), command);
			}
		}
		
		rb.getResponse().add("grouped", commands);
	}

}
