package org.javenstudio.hornet.grouping.collector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDocTermsIndex;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.grouping.AbstractDistinctValuesCollector;
import org.javenstudio.hornet.grouping.SearchGroup;
import org.javenstudio.hornet.search.cache.FieldCache;
import org.javenstudio.hornet.util.SentinelIntSet;

/**
 * A term based implementation of {@link AbstractDistinctValuesCollector} that relies
 * on {@link DocTermsIndex} to count the distinct values per group.
 *
 */
public class TermDistinctValuesCollector 
		extends AbstractDistinctValuesCollector<TermDistinctValuesGroupCount> {

	protected final BytesRef mSpare = new BytesRef();
	protected final List<TermDistinctValuesGroupCount> mGroups;
	protected final TermDistinctValuesGroupCount mGroupCounts[];
	protected final SentinelIntSet mOrdSet;
	protected final String mGroupField;
	protected final String mCountField;
	
	protected IDocTermsIndex mGroupFieldTermIndex;
	protected IDocTermsIndex mCountFieldTermIndex;

	/**
	 * Constructs {@link TermDistinctValuesCollector} instance.
	 *
	 * @param groupField The field to group by
	 * @param countField The field to count distinct values for
	 * @param groups The top N groups, collected during the first phase search
	 */
	public TermDistinctValuesCollector(String groupField, String countField, 
			Collection<SearchGroup<BytesRef>> groups) {
		mGroupField = groupField;
		mCountField = countField;
		mGroups = new ArrayList<TermDistinctValuesGroupCount>(groups.size());
		
		for (SearchGroup<BytesRef> group : groups) {
			mGroups.add(new TermDistinctValuesGroupCount(group.getGroupValue()));
		}
		
		mOrdSet = new SentinelIntSet(groups.size(), -1);
		mGroupCounts = new TermDistinctValuesGroupCount[mOrdSet.getKeySize()];
	}

	@Override
	public void collect(int doc) throws IOException {
		int slot = mOrdSet.find(mGroupFieldTermIndex.getOrd(doc));
		if (slot < 0) 
			return;

		TermDistinctValuesGroupCount gc = mGroupCounts[slot];
		int countOrd = mCountFieldTermIndex.getOrd(doc);
		
		if (doesNotContainsOrd(countOrd, gc.mOrds)) {
			if (countOrd == 0) 
				gc.getUniqueValues().add(null);
			else 
				gc.getUniqueValues().add(mCountFieldTermIndex.lookup(countOrd, new BytesRef()));
			
			gc.mOrds = Arrays.copyOf(gc.mOrds, gc.mOrds.length + 1);
			gc.mOrds[gc.mOrds.length - 1] = countOrd;
			
			if (gc.mOrds.length > 1) 
				Arrays.sort(gc.mOrds);
		}
	}

	private boolean doesNotContainsOrd(int ord, int[] ords) {
		if (ords.length == 0) 
			return true;
		else if (ords.length == 1) 
			return ord != ords[0];
		else
			return Arrays.binarySearch(ords, ord) < 0;
	}

	@Override
	public List<TermDistinctValuesGroupCount> getGroups() {
		return mGroups;
	}

	@Override
	public void setNextReader(IAtomicReaderRef context) throws IOException {
		mGroupFieldTermIndex = FieldCache.DEFAULT.getTermsIndex(context.getReader(), mGroupField);
		mCountFieldTermIndex = FieldCache.DEFAULT.getTermsIndex(context.getReader(), mCountField);

		mOrdSet.clear();
		
		for (TermDistinctValuesGroupCount group : mGroups) {
			int groupOrd = (group.getGroupValue() == null) ? 0 : 
				mGroupFieldTermIndex.binarySearch(group.getGroupValue(), mSpare);
			if (groupOrd < 0) 
				continue;

			mGroupCounts[mOrdSet.put(groupOrd)] = group;
			
			group.mOrds = new int[group.getUniqueValues().size()];
			Arrays.fill(group.mOrds, -1);
			
			int i = 0;
			
			for (BytesRef value : group.getUniqueValues()) {
				int countOrd = value == null ? 0 : 
					mCountFieldTermIndex.binarySearch(value, new BytesRef());
				if (countOrd >= 0) 
					group.mOrds[i++] = countOrd;
			}
		}
	}

}
