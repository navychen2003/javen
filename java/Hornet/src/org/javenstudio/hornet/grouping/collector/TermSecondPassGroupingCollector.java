package org.javenstudio.hornet.grouping.collector;

import java.io.IOException;
import java.util.Collection;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDocTermsIndex;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.grouping.AbstractSecondPassGroupingCollector;
import org.javenstudio.hornet.grouping.SearchGroup;
import org.javenstudio.hornet.search.cache.FieldCache;
import org.javenstudio.hornet.util.SentinelIntSet;

/**
 * Concrete implementation of {@link AbstractSecondPassGroupingCollector} that groups based on
 * field values and more specifically uses {@link DocTermsIndex}
 * to collect grouped docs.
 *
 */
public class TermSecondPassGroupingCollector 
		extends AbstractSecondPassGroupingCollector<BytesRef> {

	protected final BytesRef mSpareBytesRef = new BytesRef();
	protected final String mGroupField;
	protected final SentinelIntSet mOrdSet;
	protected IDocTermsIndex mIndex;

	@SuppressWarnings("unchecked")
	public TermSecondPassGroupingCollector(String groupField, 
			Collection<SearchGroup<BytesRef>> groups, ISort groupSort, ISort withinGroupSort,
			int maxDocsPerGroup, boolean getScores, boolean getMaxScores, boolean fillSortFields)
			throws IOException {
		super(groups, groupSort, withinGroupSort, maxDocsPerGroup, 
				getScores, getMaxScores, fillSortFields);
    
		mOrdSet = new SentinelIntSet(mGroupMap.size(), -1);
		mGroupField = groupField;
		mGroupDocs = (SearchGroupDocs<BytesRef>[]) new SearchGroupDocs[mOrdSet.getKeySize()];
	}

	@Override
	public void setNextReader(IAtomicReaderRef readerContext) throws IOException {
		super.setNextReader(readerContext);
		
		mIndex = FieldCache.DEFAULT.getTermsIndex(readerContext.getReader(), mGroupField);

		// Rebuild ordSet
		mOrdSet.clear();
		
		for (SearchGroupDocs<BytesRef> group : mGroupMap.values()) {
			int ord = group.getGroupValue() == null ? 0 : 
				mIndex.binarySearch(group.getGroupValue(), mSpareBytesRef);
			if (ord >= 0) 
				mGroupDocs[mOrdSet.put(ord)] = group;
		}
	}

	@Override
	protected SearchGroupDocs<BytesRef> retrieveGroup(int doc) throws IOException {
		int slot = mOrdSet.find(mIndex.getOrd(doc));
		if (slot >= 0) 
			return mGroupDocs[slot];
		
		return null;
	}
	
}
