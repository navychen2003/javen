package org.javenstudio.falcon.search.transformer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.indexdb.IDocument;
import org.javenstudio.common.indexdb.IScoreDoc;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.document.Fieldable;
import org.javenstudio.common.indexdb.index.field.StoredFieldVisitor;
import org.javenstudio.common.indexdb.search.FieldDoc;
import org.javenstudio.common.indexdb.search.ScoreDoc;
import org.javenstudio.common.indexdb.search.TopDocs;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.CharsRef;
import org.javenstudio.common.indexdb.util.UnicodeUtil;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.hornet.grouping.GroupDocs;
import org.javenstudio.hornet.grouping.TopGroups;
import org.javenstudio.falcon.search.ResponseBuilder;
import org.javenstudio.falcon.search.component.SearchCommand;
import org.javenstudio.falcon.search.hits.ShardDoc;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.search.schema.SchemaFieldType;
import org.javenstudio.falcon.search.shard.ShardTransformer;

/**
 * Implementation for transforming {@link TopGroups} and {@link TopDocs} 
 * into a {@link NamedList} structure and
 * visa versa.
 */
public class TopGroupsResultTransformer 
	implements ShardTransformer<List<SearchCommand<?>>, Map<String, ?>> {

	private final ResponseBuilder mBuilder;

	public TopGroupsResultTransformer(ResponseBuilder rb) {
		mBuilder = rb;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NamedList<?> transform(List<SearchCommand<?>> data) throws ErrorException {
		NamedList<NamedList<?>> result = new NamedList<NamedList<?>>();
		
		for (SearchCommand<?> command : data) {
			NamedList<?> commandResult;
			
			if (TopGroupsFieldCommand.class.isInstance(command)) {
				TopGroupsFieldCommand fieldCommand = (TopGroupsFieldCommand) command;
				SchemaField groupField = mBuilder.getSearchCore().getSchema().getField(fieldCommand.getKey());
				commandResult = serializeTopGroups(fieldCommand.getResult(), groupField);
				
			} else if (QueryFieldCommand.class.isInstance(command)) {
				QueryFieldCommand queryCommand = (QueryFieldCommand) command;
				commandResult = serializeTopDocs(queryCommand.getResult());
				
			} else {
				commandResult = null;
			}

			result.add(command.getKey(), commandResult);
		}
		
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, ?> transformToNative(NamedList<NamedList<?>> shardResponse, 
			ISort groupSort, ISort sortWithinGroup, String shard) throws ErrorException {
		Map<String, Object> result = new HashMap<String, Object>();

		for (Map.Entry<String, NamedList<?>> entry : shardResponse) {
			String key = entry.getKey();
			NamedList<?> commandResult = entry.getValue();
			
			Integer totalGroupedHitCount = (Integer) commandResult.get("totalGroupedHitCount");
			Integer totalHits = (Integer) commandResult.get("totalHits");
			
			if (totalHits != null) {
				Integer matches = (Integer) commandResult.get("matches");
				Float maxScore = (Float) commandResult.get("maxScore");
				if (maxScore == null) 
					maxScore = Float.NaN;

				@SuppressWarnings("unchecked")
				List<NamedList<Object>> documents = (List<NamedList<Object>>) commandResult.get("documents");
				ScoreDoc[] scoreDocs = new ScoreDoc[documents.size()];
				
				int j = 0;
				for (NamedList<Object> document : documents) {
					Object uniqueId = document.get("id").toString();
					Float score = (Float) document.get("score");
					if (score == null) 
						score = Float.NaN;
					
					Object[] sortValues = ((List<?>) document.get("sortValues")).toArray();
					scoreDocs[j++] = new ShardDoc(score, sortValues, uniqueId, shard);
				}
				
				result.put(key, new QueryFieldResult(
						new TopDocs(totalHits, scoreDocs, maxScore), matches));
				
				continue;
			}

			Integer totalHitCount = (Integer) commandResult.get("totalHitCount");

			List<GroupDocs<BytesRef>> groupDocs = new ArrayList<GroupDocs<BytesRef>>();
			
			for (int i = 2; i < commandResult.size(); i++) {
				String groupValue = commandResult.getName(i);
				
				@SuppressWarnings("unchecked")
				NamedList<Object> groupResult = (NamedList<Object>) commandResult.getVal(i);
				
				Integer totalGroupHits = (Integer) groupResult.get("totalHits");
				Float maxScore = (Float) groupResult.get("maxScore");
				if (maxScore == null) 
					maxScore = Float.NaN;
				
				@SuppressWarnings("unchecked")
				List<NamedList<Object>> documents = (List<NamedList<Object>>) groupResult.get("documents");
				ScoreDoc[] scoreDocs = new ScoreDoc[documents.size()];
				
				int j = 0;
				for (NamedList<Object> document : documents) {
					Object uniqueId = document.get("id").toString();
					Float score = (Float) document.get("score");
					if (score == null) 
						score = Float.NaN;
					
					Object[] sortValues = ((List<?>) document.get("sortValues")).toArray();
					scoreDocs[j++] = new ShardDoc(score, sortValues, uniqueId, shard);
				}

				BytesRef groupValueRef = groupValue != null ? new BytesRef(groupValue) : null;
				groupDocs.add(new GroupDocs<BytesRef>(Float.NaN, 
						maxScore, totalGroupHits, scoreDocs, groupValueRef, null));
			}

			@SuppressWarnings("unchecked")
			GroupDocs<BytesRef>[] groupDocsArr = groupDocs.toArray(new GroupDocs[groupDocs.size()]);
			TopGroups<BytesRef> topGroups = new TopGroups<BytesRef>(
					groupSort.getSortFields(), sortWithinGroup.getSortFields(), 
					totalHitCount, totalGroupedHitCount, groupDocsArr, Float.NaN
					);

			result.put(key, topGroups);
		}

		return result;
	}

	protected NamedList<?> serializeTopGroups(TopGroups<BytesRef> data, 
			SchemaField groupField) throws ErrorException {
		NamedList<Object> result = new NamedList<Object>();
		
		result.add("totalGroupedHitCount", data.getTotalGroupedHitCount());
		result.add("totalHitCount", data.getTotalHitCount());
		
		if (data.getTotalGroupCount() != null) 
			result.add("totalGroupCount", data.getTotalGroupCount());
		
		CharsRef spare = new CharsRef();
		SchemaField uniqueField = mBuilder.getSearchCore().getSchema().getUniqueKeyField();
		
		for (GroupDocs<BytesRef> searchGroup : data.getGroupDocs()) {
			NamedList<Object> groupResult = new NamedList<Object>();
			
			groupResult.add("totalHits", searchGroup.getTotalHits());
			if (!Float.isNaN(searchGroup.getMaxScore())) 
				groupResult.add("maxScore", searchGroup.getMaxScore());
			
			List<NamedList<Object>> documents = new ArrayList<NamedList<Object>>();
			
			for (int i = 0; i < searchGroup.getScoreDocsSize(); i++) {
				IScoreDoc scoreDoc = searchGroup.getScoreDocAt(i);
				NamedList<Object> document = new NamedList<Object>();
				documents.add(document);

				IDocument doc = retrieveDocument(uniqueField, scoreDoc.getDoc());
				document.add("id", uniqueField.getType().toExternal(
						(Fieldable)doc.getField(uniqueField.getName())));
				
				if (!Float.isNaN(scoreDoc.getScore())) 
					document.add("score", scoreDoc.getScore());
				
				if (!(scoreDoc instanceof FieldDoc)) 
					continue;
				
				FieldDoc fieldDoc = (FieldDoc) scoreDoc;
				Object[] convertedSortValues  = new Object[fieldDoc.getFieldSize()];
				
				for (int j = 0; j < fieldDoc.getFieldSize(); j++) {
					Object sortValue  = fieldDoc.getFieldAt(j);
					ISort sortWithinGroup = mBuilder.getGroupingSpec().getSortWithinGroup();
					SchemaField field = (sortWithinGroup.getSortFields()[j].getField() != null) ? 
							mBuilder.getSearchCore().getSchema().getFieldOrNull(
									sortWithinGroup.getSortFields()[j].getField()) : null;
					
					if (field != null) {
						SchemaFieldType fieldType = field.getType();
						
						if (sortValue instanceof BytesRef) {
							UnicodeUtil.UTF8toUTF16((BytesRef)sortValue, spare);
							String indexedValue = spare.toString();
							
							sortValue = fieldType.toObject(field.createField(
									fieldType.indexedToReadable(indexedValue), 1.0f));
							
						} else if (sortValue instanceof String) {
							sortValue = fieldType.toObject(field.createField(
									fieldType.indexedToReadable((String) sortValue), 1.0f));
						}
					}
					
					convertedSortValues[j] = sortValue;
				}
				
				document.add("sortValues", convertedSortValues);
			}
			
			groupResult.add("documents", documents);
			
			String groupValue = (searchGroup.getGroupValue() != null) ? 
					groupField.getType().indexedToReadable(
							searchGroup.getGroupValue().utf8ToString()) : null;
			
			result.add(groupValue, groupResult);
		}

		return result;
	}

	protected NamedList<?> serializeTopDocs(QueryFieldResult result) 
			throws ErrorException {
		NamedList<Object> queryResult = new NamedList<Object>();
		
		queryResult.add("matches", result.getMatches());
		queryResult.add("totalHits", result.getTopDocs().getTotalHits());
		
		if (mBuilder.getGroupingSpec().isNeedScore()) 
			queryResult.add("maxScore", result.getTopDocs().getMaxScore());
    
		List<NamedList<?>> documents = new ArrayList<NamedList<?>>();
		queryResult.add("documents", documents);

		SchemaField uniqueField = mBuilder.getSearchCore().getSchema().getUniqueKeyField();
		CharsRef spare = new CharsRef();
		
		for (IScoreDoc scoreDoc : result.getTopDocs().getScoreDocs()) {
			NamedList<Object> document = new NamedList<Object>();
			documents.add(document);

			IDocument doc = retrieveDocument(uniqueField, scoreDoc.getDoc());
			document.add("id", uniqueField.getType().toExternal(
					(Fieldable)doc.getField(uniqueField.getName())));
			
			if (mBuilder.getGroupingSpec().isNeedScore()) 
				document.add("score", scoreDoc.getScore());
			
			if (!FieldDoc.class.isInstance(scoreDoc)) 
				continue;
			
			FieldDoc fieldDoc = (FieldDoc) scoreDoc;
			Object[] convertedSortValues  = new Object[fieldDoc.getFieldSize()];
			
			for (int j = 0; j < fieldDoc.getFieldSize(); j++) {
				Object sortValue  = fieldDoc.getFieldAt(j);
				
				ISort groupSort = mBuilder.getGroupingSpec().getGroupSort();
				SchemaField field = (groupSort.getSortFields()[j].getField() != null) ? 
						mBuilder.getSearchCore().getSchema().getFieldOrNull(
								groupSort.getSortFields()[j].getField()) : null;
				
				if (field != null) {
					SchemaFieldType fieldType = field.getType();
					if (sortValue instanceof BytesRef) {
						UnicodeUtil.UTF8toUTF16((BytesRef)sortValue, spare);
						String indexedValue = spare.toString();
						
						sortValue = fieldType.toObject(field.createField(
								fieldType.indexedToReadable(indexedValue), 1.0f));
						
					} else if (sortValue instanceof String) {
						sortValue = fieldType.toObject(field.createField(
								fieldType.indexedToReadable((String) sortValue), 1.0f));
					}
				}
				
				convertedSortValues[j] = sortValue;
			}
			
			document.add("sortValues", convertedSortValues);
		}

		return queryResult;
	}

	private IDocument retrieveDocument(final SchemaField uniqueField, int doc) 
			throws ErrorException {
		StoredFieldVisitor visitor = new StoredFieldVisitor(uniqueField.getName());
		mBuilder.getRequest().getSearcher().document(doc, visitor);
		return visitor.getDocument();
	}

}
