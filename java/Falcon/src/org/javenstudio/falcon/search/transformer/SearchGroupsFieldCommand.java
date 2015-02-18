package org.javenstudio.falcon.search.transformer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.javenstudio.common.indexdb.ICollector;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.hornet.grouping.SearchGroup;
import org.javenstudio.hornet.grouping.collector.TermAllGroupsCollector;
import org.javenstudio.hornet.grouping.collector.TermFirstPassGroupingCollector;
import org.javenstudio.falcon.search.component.SearchCommand;
import org.javenstudio.falcon.search.grouping.GroupingPair;
import org.javenstudio.falcon.search.schema.SchemaField;

/**
 * Creates all the collectors needed for the first phase and how to handle the results.
 */
public class SearchGroupsFieldCommand 
	implements SearchCommand<GroupingPair<Integer, Collection<SearchGroup<BytesRef>>>> {

	public static class Builder {

		private SchemaField mField;
		private ISort mGroupSort;
		private Integer mTopNGroups;
		
		private boolean mIncludeGroupCount = false;

		public Builder setField(SchemaField field) {
			mField = field;
			return this;
		}

		public Builder setGroupSort(ISort groupSort) {
			mGroupSort = groupSort;
			return this;
		}

		public Builder setTopNGroups(int topNGroups) {
			mTopNGroups = topNGroups;
			return this;
		}

		public Builder setIncludeGroupCount(boolean includeGroupCount) {
			mIncludeGroupCount = includeGroupCount;
			return this;
		}

		public SearchGroupsFieldCommand build() {
			if (mField == null || mGroupSort == null || mTopNGroups == null) 
				throw new IllegalStateException("All fields must be set");

			return new SearchGroupsFieldCommand(mField, 
					mGroupSort, mTopNGroups, mIncludeGroupCount);
		}
	}

	private TermFirstPassGroupingCollector mFirstPassGroupingCollector;
	private TermAllGroupsCollector mAllGroupsCollector;
	
	private final SchemaField mField;
	private final ISort mGroupSort;
	private final int mTopNGroups;
	private final boolean mIncludeGroupCount;

	private SearchGroupsFieldCommand(SchemaField field, ISort groupSort, 
			int topNGroups, boolean includeGroupCount) {
		mField = field;
		mGroupSort = groupSort;
		mTopNGroups = topNGroups;
		mIncludeGroupCount = includeGroupCount;
	}

	@Override
	public List<ICollector> createCollectors() throws ErrorException {
		List<ICollector> collectors = new ArrayList<ICollector>();
		
		try {
			if (mTopNGroups > 0) {
				mFirstPassGroupingCollector = new TermFirstPassGroupingCollector(
						mField.getName(), mGroupSort, mTopNGroups);
				collectors.add(mFirstPassGroupingCollector);
			}
			
			if (mIncludeGroupCount) {
				mAllGroupsCollector = new TermAllGroupsCollector(mField.getName());
				collectors.add(mAllGroupsCollector);
			}
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
		
		return collectors;
	}

	@Override
	public GroupingPair<Integer, Collection<SearchGroup<BytesRef>>> getResult() {
		final Collection<SearchGroup<BytesRef>> topGroups;
		if (mTopNGroups > 0) 
			topGroups = mFirstPassGroupingCollector.getTopGroups(0, true);
		else 
			topGroups = Collections.emptyList();
		
		final Integer groupCount;
		if (mIncludeGroupCount) 
			groupCount = mAllGroupsCollector.getGroupCount();
		else 
			groupCount = null;
		
		return new GroupingPair<Integer, Collection<SearchGroup<BytesRef>>>(groupCount, topGroups);
	}

	@Override
	public ISort getSortWithinGroup() {
		return null;
	}

	@Override
	public ISort getGroupSort() {
		return mGroupSort;
	}

	@Override
	public String getKey() {
		return mField.getName();
	}
	
}
