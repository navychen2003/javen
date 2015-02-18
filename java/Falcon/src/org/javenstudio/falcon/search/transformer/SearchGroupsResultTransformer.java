package org.javenstudio.falcon.search.transformer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.CharsRef;
import org.javenstudio.common.indexdb.util.UnicodeUtil;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.hornet.grouping.SearchGroup;
import org.javenstudio.falcon.search.Searcher;
import org.javenstudio.falcon.search.component.SearchCommand;
import org.javenstudio.falcon.search.grouping.GroupingPair;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.search.schema.SchemaFieldType;
import org.javenstudio.falcon.search.shard.ShardTransformer;

/**
 * Implementation for transforming {@link SearchGroup} into 
 * a {@link NamedList} structure and visa versa.
 */
public class SearchGroupsResultTransformer 
	implements ShardTransformer<List<SearchCommand<?>>, 
		Map<String, GroupingPair<Integer, Collection<SearchGroup<BytesRef>>>>> {

	private final Searcher mSearcher;

	public SearchGroupsResultTransformer(Searcher searcher) {
		mSearcher = searcher;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NamedList<?> transform(List<SearchCommand<?>> data) throws ErrorException {
		NamedList<NamedList<?>> result = new NamedList<NamedList<?>>();
		
		for (SearchCommand<?> command : data) {
			final NamedList<Object> commandResult = new NamedList<Object>();
			
			if (SearchGroupsFieldCommand.class.isInstance(command)) {
				SearchGroupsFieldCommand fieldCommand = (SearchGroupsFieldCommand) command;
				GroupingPair<Integer, Collection<SearchGroup<BytesRef>>> pair = fieldCommand.getResult();
				
				Integer groupedCount = pair.getA();
				Collection<SearchGroup<BytesRef>> searchGroups = pair.getB();
				
				if (searchGroups != null) {
					commandResult.add("topGroups", 
							serializeSearchGroup(searchGroups, fieldCommand.getGroupSort()));
				}
				
				if (groupedCount != null) 
					commandResult.add("groupCount", groupedCount);
				
			} else 
				continue;

			result.add(command.getKey(), commandResult);
		}
		
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, GroupingPair<Integer, Collection<SearchGroup<BytesRef>>>> 
		transformToNative(NamedList<NamedList<?>> shardResponse, ISort groupSort, 
				ISort sortWithinGroup, String shard) {
		
		Map<String, GroupingPair<Integer, Collection<SearchGroup<BytesRef>>>> result = 
				new HashMap<String, GroupingPair<Integer, Collection<SearchGroup<BytesRef>>>>();
		
		for (Map.Entry<String, NamedList<?>> command : shardResponse) {
			List<SearchGroup<BytesRef>> searchGroups = new ArrayList<SearchGroup<BytesRef>>();
			NamedList<?> topGroupsAndGroupCount = command.getValue();
			
			@SuppressWarnings("unchecked")
			NamedList<List<Comparable<?>>> rawSearchGroups = (NamedList<List<Comparable<?>>>) 
				topGroupsAndGroupCount.get("topGroups");
			
			if (rawSearchGroups != null) {
				for (Map.Entry<String, List<Comparable<?>>> rawSearchGroup : rawSearchGroups){
					SearchGroup<BytesRef> searchGroup = new SearchGroup<BytesRef>();
					searchGroup.setGroupValue(rawSearchGroup.getKey() != null ? 
							new BytesRef(rawSearchGroup.getKey()) : null);
					searchGroup.setSortValues(rawSearchGroup.getValue().toArray(
							new Comparable[rawSearchGroup.getValue().size()]));
					searchGroups.add(searchGroup);
				}
			}

			Integer groupCount = (Integer) topGroupsAndGroupCount.get("groupCount");
			result.put(command.getKey(), new GroupingPair<Integer, 
					Collection<SearchGroup<BytesRef>>>(groupCount, searchGroups));
		}
		
		return result;
	}

	private NamedList<?> serializeSearchGroup(Collection<SearchGroup<BytesRef>> data, 
			ISort groupSort) throws ErrorException {
		NamedList<Comparable<?>[]> result = new NamedList<Comparable<?>[]>();
		CharsRef spare = new CharsRef();

		for (SearchGroup<BytesRef> searchGroup : data) {
			Comparable<?>[] convertedSortValues = new Comparable[searchGroup.getSortValueSize()];
			
			for (int i = 0; i < searchGroup.getSortValueSize(); i++) {
				Comparable<?> sortValue = (Comparable<?>) searchGroup.getSortValueAt(i);
				SchemaField field = (groupSort.getSortFields()[i].getField() != null) ? 
						mSearcher.getSchema().getFieldOrNull(groupSort.getSortFields()[i].getField()) : null;
				
				if (field != null) {
					SchemaFieldType fieldType = field.getType();
					if (sortValue instanceof BytesRef) {
						UnicodeUtil.UTF8toUTF16((BytesRef)sortValue, spare);
						String indexedValue = spare.toString();
						sortValue = (Comparable<?>) fieldType.toObject(
								field.createField(fieldType.indexedToReadable(indexedValue), 1.0f));
						
					} else if (sortValue instanceof String) {
						sortValue = (Comparable<?>) fieldType.toObject(
								field.createField(fieldType.indexedToReadable((String) sortValue), 1.0f));
					}
				}
				
				convertedSortValues[i] = sortValue;
			}
			
			String groupValue = (searchGroup.getGroupValue() != null) ? 
					searchGroup.getGroupValue().utf8ToString() : null;
			result.add(groupValue, convertedSortValues);
		}

		return result;
	}

}
