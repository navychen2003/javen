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
import org.javenstudio.hornet.grouping.GroupDocs;
import org.javenstudio.hornet.grouping.SearchGroup;
import org.javenstudio.hornet.grouping.TopGroups;
import org.javenstudio.hornet.grouping.collector.TermSecondPassGroupingCollector;
import org.javenstudio.falcon.search.component.SearchCommand;
import org.javenstudio.falcon.search.schema.SchemaField;

/**
 * Defines all collectors for retrieving the second phase 
 * and how to handle the collector result.
 */
public class TopGroupsFieldCommand implements SearchCommand<TopGroups<BytesRef>> {

	public static class Builder {

		private Collection<SearchGroup<BytesRef>> mFirstPhaseGroups;
		private SchemaField mField;
		
		private ISort mGroupSort;
		private ISort mSortWithinGroup;
		
		private Integer mMaxDocPerGroup;
		private boolean mNeedScores = false;
		private boolean mNeedMaxScore = false;

		public Builder setField(SchemaField field) {
			mField = field;
			return this;
		}

		public Builder setGroupSort(ISort groupSort) {
			mGroupSort = groupSort;
			return this;
		}

		public Builder setSortWithinGroup(ISort sortWithinGroup) {
			mSortWithinGroup = sortWithinGroup;
			return this;
		}

		public Builder setFirstPhaseGroups(Collection<SearchGroup<BytesRef>> firstPhaseGroups) {
			mFirstPhaseGroups = firstPhaseGroups;
			return this;
		}

		public Builder setMaxDocPerGroup(int maxDocPerGroup) {
			mMaxDocPerGroup = maxDocPerGroup;
			return this;
		}

		public Builder setNeedScores(Boolean needScores) {
			mNeedScores = needScores;
			return this;
		}

		public Builder setNeedMaxScore(Boolean needMaxScore) {
			mNeedMaxScore = needMaxScore;
			return this;
		}

		public TopGroupsFieldCommand build() {
			if (mField == null || mGroupSort == null || mSortWithinGroup == null || 
				mFirstPhaseGroups == null || mMaxDocPerGroup == null) 
				throw new IllegalStateException("All required fields must be set");

			return new TopGroupsFieldCommand(mField, mGroupSort, mSortWithinGroup, 
					mFirstPhaseGroups, mMaxDocPerGroup, mNeedScores, mNeedMaxScore);
		}

	}

	private final Collection<SearchGroup<BytesRef>> mFirstPhaseGroups;
	private TermSecondPassGroupingCollector mSecondPassCollector;
	
	private final SchemaField mField;
	private final ISort mGroupSort;
	private final ISort mSortWithinGroup;
	
	private final int mMaxDocPerGroup;
	private final boolean mNeedScores;
	private final boolean mNeedMaxScore;
	
	private TopGroupsFieldCommand(SchemaField field, ISort groupSort, ISort sortWithinGroup,
			Collection<SearchGroup<BytesRef>> firstPhaseGroups, int maxDocPerGroup,
			boolean needScores, boolean needMaxScore) {
		mField = field;
		mGroupSort = groupSort;
		mSortWithinGroup = sortWithinGroup;
		mFirstPhaseGroups = firstPhaseGroups;
		mMaxDocPerGroup = maxDocPerGroup;
		mNeedScores = needScores;
		mNeedMaxScore = needMaxScore;
	}

	@Override
	public List<ICollector> createCollectors() throws ErrorException {
		if (mFirstPhaseGroups.isEmpty()) 
			return Collections.emptyList();

		List<ICollector> collectors = new ArrayList<ICollector>();
		
		try {
			mSecondPassCollector = new TermSecondPassGroupingCollector(
					mField.getName(), mFirstPhaseGroups, mGroupSort, mSortWithinGroup, 
					mMaxDocPerGroup, mNeedScores, mNeedMaxScore, true);
			
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
		
		collectors.add(mSecondPassCollector);
		
		return collectors;
	}

	@SuppressWarnings("unchecked")
	public TopGroups<BytesRef> getResult() {
		if (mFirstPhaseGroups.isEmpty()) {
			return new TopGroups<BytesRef>(mGroupSort.getSortFields(), 
					mSortWithinGroup.getSortFields(), 0, 0, new GroupDocs[0], Float.NaN);
		}

		return mSecondPassCollector.getTopGroups(0);
	}

	public String getKey() { return mField.getName(); }
	public ISort getGroupSort() { return mGroupSort; }
	public ISort getSortWithinGroup() { return mSortWithinGroup; }
	
}
