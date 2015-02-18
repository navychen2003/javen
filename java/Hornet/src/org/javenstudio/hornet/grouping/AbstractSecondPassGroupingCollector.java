package org.javenstudio.hornet.grouping;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.ITopDocs;
import org.javenstudio.common.indexdb.search.Collector;
import org.javenstudio.common.indexdb.search.TopDocsCollector;
import org.javenstudio.hornet.search.collector.TopFieldCollector;
import org.javenstudio.hornet.search.collector.TopScoreDocCollector;

/**
 * SecondPassGroupingCollector is the second of two passes
 * necessary to collect grouped docs.  This pass gathers the
 * top N documents per top group computed from the
 * first pass. Concrete subclasses define what a group is and how it
 * is internally collected.
 *
 * <p>See {@link org.apache.lucene.search.grouping} for more
 * details including a full code example.</p>
 *
 */
public abstract class AbstractSecondPassGroupingCollector<GT> extends Collector {

	protected final Map<GT, SearchGroupDocs<GT>> mGroupMap;
	protected final int mMaxDocsPerGroup;
  
	protected final Collection<SearchGroup<GT>> mGroups;
	protected final ISort mWithinGroupSort;
	protected final ISort mGroupSort;

	protected SearchGroupDocs<GT>[] mGroupDocs;
	protected int mTotalHitCount;
	protected int mTotalGroupedHitCount;

	public AbstractSecondPassGroupingCollector(Collection<SearchGroup<GT>> groups, 
			ISort groupSort, ISort withinGroupSort, int maxDocsPerGroup, boolean getScores, 
			boolean getMaxScores, boolean fillSortFields) throws IOException {
		if (groups.size() == 0) 
			throw new IllegalArgumentException("no groups to collect (groups.size() is 0)");
		
		mGroupMap = new HashMap<GT, SearchGroupDocs<GT>>(groups.size());
		mGroupSort = groupSort;
		mWithinGroupSort = withinGroupSort;
		mGroups = groups;
		mMaxDocsPerGroup = maxDocsPerGroup;
		
		for (SearchGroup<GT> group : groups) {
			final TopDocsCollector<?> collector;
			
			if (withinGroupSort == null) {
				// Sort by score
				collector = TopScoreDocCollector.create(maxDocsPerGroup, true);
				
			} else {
				// Sort by fields
				collector = TopFieldCollector.create(withinGroupSort, 
						maxDocsPerGroup, fillSortFields, getScores, getMaxScores, true);
			}
			
			mGroupMap.put(group.getGroupValue(),
					new SearchGroupDocs<GT>(group.getGroupValue(), collector));
		}
	}

	@Override
	public void setScorer(IScorer scorer) throws IOException {
		for (SearchGroupDocs<GT> group : mGroupMap.values()) {
			group.mCollector.setScorer(scorer);
		}
	}

	@Override
	public void collect(int doc) throws IOException {
		mTotalHitCount ++;
		
		SearchGroupDocs<GT> group = retrieveGroup(doc);
		if (group != null) {
			mTotalGroupedHitCount ++;
			group.mCollector.collect(doc);
		}
	}

	/**
	 * Returns the group the specified doc belongs to or <code>null</code> 
	 * if no group could be retrieved.
	 *
	 * @param doc The specified doc
	 * @return the group the specified doc belongs to or <code>null</code> 
	 * if no group could be retrieved
	 * @throws IOException If an I/O related error occurred
	 */
	protected abstract SearchGroupDocs<GT> retrieveGroup(int doc) throws IOException;

	@Override
	public void setNextReader(IAtomicReaderRef readerContext) throws IOException {
		for (SearchGroupDocs<GT> group : mGroupMap.values()) {
			group.mCollector.setNextReader(readerContext);
		}
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		return false;
	}

	public TopGroups<GT> getTopGroups(int withinGroupOffset) {
		@SuppressWarnings("unchecked")
		final GroupDocs<GT>[] groupDocsResult = (GroupDocs<GT>[]) 
			new GroupDocs[mGroups.size()];

		float maxScore = Float.MIN_VALUE;
		int groupIDX = 0;
    
		for (SearchGroup<?> group : mGroups) {
			final SearchGroupDocs<GT> groupDocs = mGroupMap.get(group.getGroupValue());
			final ITopDocs topDocs = groupDocs.mCollector.getTopDocs(
					withinGroupOffset, mMaxDocsPerGroup);
			
			groupDocsResult[groupIDX++] = new GroupDocs<GT>(Float.NaN,
					topDocs.getMaxScore(),
					topDocs.getTotalHits(),
					topDocs.getScoreDocs(),
					groupDocs.getGroupValue(),
					group.getSortValues());
			
			maxScore = Math.max(maxScore, topDocs.getMaxScore());
		}

		return new TopGroups<GT>(mGroupSort.getSortFields(),
				mWithinGroupSort == null ? null : mWithinGroupSort.getSortFields(),
				mTotalHitCount, mTotalGroupedHitCount, groupDocsResult, maxScore);
	}

	// TODO: merge with SearchGroup or not?
	// ad: don't need to build a new hashmap
	// disad: blows up the size of SearchGroup if we need many of them, and couples implementations
	@SuppressWarnings("hiding")
	public class SearchGroupDocs<GT> {
		private final GT mGroupValue;
		private final TopDocsCollector<?> mCollector;

		public SearchGroupDocs(GT groupValue, TopDocsCollector<?> collector) {
			mGroupValue = groupValue;
			mCollector = collector;
		}
		
		public final GT getGroupValue() { return mGroupValue; }
		public final TopDocsCollector<?> getCollector() { return mCollector; }
	}
	
}
