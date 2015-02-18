package org.javenstudio.hornet.grouping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;

import org.javenstudio.common.indexdb.IFieldComparator;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.ISortField;

/**
 * Represents a group that is found during the first pass search.
 *
 */
public class SearchGroup<GT> {

	/** The value that defines this group  */
	private GT mGroupValue;

	/** 
	 * The sort values used during sorting. These are the
	 *  groupSort field values of the highest rank document
	 *  (by the groupSort) within the group.  Can be
	 * <code>null</code> if <code>fillFields=false</code> had
	 * been passed to {@link AbstractFirstPassGroupingCollector#getTopGroups} 
	 */
	private Object[] mSortValues;

	public void setGroupValue(GT value) { mGroupValue = value; }
	public GT getGroupValue() { return mGroupValue; }
	
	public void setSortValues(Object[] values) { mSortValues = values; }
	public void setSortValueAt(int index, Object value) { mSortValues[index] = value; }
	
	public Object[] getSortValues() { return mSortValues; }
	public Object getSortValueAt(int index) { return mSortValues[index]; }
	public int getSortValueSize() { return mSortValues != null ? mSortValues.length : 0; }
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) 
			return false;

		SearchGroup<?> that = (SearchGroup<?>) o;

		if (mGroupValue == null) {
			if (that.mGroupValue != null) 
				return false;
			
		} else if (!mGroupValue.equals(that.mGroupValue)) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return mGroupValue != null ? mGroupValue.hashCode() : 0;
	}

	@Override
	public String toString() {
		return("SearchGroup(groupValue=" + mGroupValue + " sortValues=" 
				+ Arrays.toString(mSortValues) + ")");
	}
	
	private static class ShardIter<T> {
		private final Iterator<SearchGroup<T>> mIter;
		private final int mShardIndex;

		public ShardIter(Collection<SearchGroup<T>> shard, int shardIndex) {
			mShardIndex = shardIndex;
			mIter = shard.iterator();
			assert mIter.hasNext();
		}

		public SearchGroup<T> next() {
			assert mIter.hasNext();
			final SearchGroup<T> group = mIter.next();
			
			if (group.mSortValues == null) {
				throw new IllegalArgumentException("group.sortValues is null; " 
						+ "you must pass fillFields=true to the first pass collector");
			}
			
			return group;
		}
    
		@Override
		public String toString() {
			return "ShardIter(shard=" + mShardIndex + ")";
		}
	}

	// Holds all shards currently on the same group
	private static class MergedGroup<T> {

		private final List<ShardIter<T>> mShards = new ArrayList<ShardIter<T>>();
		
		// groupValue may be null!
		private final T mGroupValue;

		private Object[] mTopValues;
		
		private int mMinShardIndex;
		
		private boolean mProcessed;
		private boolean mInQueue;

		public MergedGroup(T groupValue) {
			mGroupValue = groupValue;
		}

		// Only for assert
		private boolean neverEquals(Object _other) {
			if (_other instanceof MergedGroup) {
				MergedGroup<?> other = (MergedGroup<?>) _other;
				if (mGroupValue == null) {
					assert other.mGroupValue != null;
				} else {
					assert !mGroupValue.equals(other.mGroupValue);
				}
			}
			return true;
		}

		@Override
		public boolean equals(Object other) {
			// We never have another MergedGroup instance with
			// same groupValue
			assert neverEquals(other);

			if (other instanceof MergedGroup) {
				MergedGroup<?> that = (MergedGroup<?>) other;
				if (mGroupValue == null) 
					return that == null;
				else 
					return mGroupValue.equals(that);
			}
			
			return false;
		}

		@Override
		public int hashCode() {
			if (mGroupValue == null) 
				return 0;
			else 
				return mGroupValue.hashCode();
		}
	}

	private static class GroupComparator<T> implements Comparator<MergedGroup<T>> {

		@SuppressWarnings("rawtypes")
		private final IFieldComparator[] mComparators;
		private final int[] mReversed;

		public GroupComparator(ISort groupSort) throws IOException {
			final ISortField[] sortFields = groupSort.getSortFields();
			
			mComparators = new IFieldComparator<?>[sortFields.length];
			mReversed = new int[sortFields.length];
			
			for (int compIDX = 0; compIDX < sortFields.length; compIDX++) {
				final ISortField sortField = sortFields[compIDX];
				
				mComparators[compIDX] = sortField.getComparator(1, compIDX);
				mReversed[compIDX] = sortField.getReverse() ? -1 : 1;
			}
		}

		@SuppressWarnings("unchecked")
		public int compare(MergedGroup<T> group, MergedGroup<T> other) {
			if (group == other) 
				return 0;
			
			final Object[] groupValues = group.mTopValues;
			final Object[] otherValues = other.mTopValues;
			
			for (int compIDX = 0;compIDX < mComparators.length; compIDX++) {
				final int c = mReversed[compIDX] * mComparators[compIDX].compareValues(
						groupValues[compIDX], otherValues[compIDX]);
				
				if (c != 0) 
					return c;
			}

			// Tie break by min shard index:
			assert group.mMinShardIndex != other.mMinShardIndex;
			
			return group.mMinShardIndex - other.mMinShardIndex;
		}
	}

	private static class GroupMerger<T> {

		private final GroupComparator<T> mGroupComp;
		private final NavigableSet<MergedGroup<T>> mQueue;
		private final Map<T,MergedGroup<T>> mGroupsSeen;

		public GroupMerger(ISort groupSort) throws IOException {
			mGroupComp = new GroupComparator<T>(groupSort);
			mQueue = new TreeSet<MergedGroup<T>>(mGroupComp);
			mGroupsSeen = new HashMap<T,MergedGroup<T>>();
		}

		@SuppressWarnings("unchecked")
		private void updateNextGroup(int topN, ShardIter<T> shard) {
			while (shard.mIter.hasNext()) {
				final SearchGroup<T> group = shard.next();
				MergedGroup<T> mergedGroup = mGroupsSeen.get(group.mGroupValue);
				
				final boolean isNew = mergedGroup == null;
				if (isNew) {
					// Start a new group:
					mergedGroup = new MergedGroup<T>(group.mGroupValue);
					mergedGroup.mMinShardIndex = shard.mShardIndex;
					
					assert group.mSortValues != null;
					mergedGroup.mTopValues = group.mSortValues;
					
					mGroupsSeen.put(group.mGroupValue, mergedGroup);
					
					mergedGroup.mInQueue = true;
					mQueue.add(mergedGroup);
					
				} else if (mergedGroup.mProcessed) {
					// This shard produced a group that we already
					// processed; move on to next group...
					continue;
					
				} else {
					boolean competes = false;
					
					for (int compIDX=0; compIDX < mGroupComp.mComparators.length; compIDX++) {
						final int cmp = mGroupComp.mReversed[compIDX] * 
								mGroupComp.mComparators[compIDX].compareValues(
										group.mSortValues[compIDX], mergedGroup.mTopValues[compIDX]);
						
						if (cmp < 0) {
							// Definitely competes
							competes = true;
							break;
							
						} else if (cmp > 0) {
							// Definitely does not compete
							break;
							
						} else if (compIDX == mGroupComp.mComparators.length-1) {
							if (shard.mShardIndex < mergedGroup.mMinShardIndex) 
								competes = true;
						}
					}

					if (competes) {
						// Group's sort changed -- remove & re-insert
						if (mergedGroup.mInQueue) 
							mQueue.remove(mergedGroup);
            
						mergedGroup.mTopValues = group.mSortValues;
						mergedGroup.mMinShardIndex = shard.mShardIndex;
						
						mQueue.add(mergedGroup);
						mergedGroup.mInQueue = true;
					}
				}

				mergedGroup.mShards.add(shard);
				break;
			}

			// Prune un-competitive groups:
			while (mQueue.size() > topN) {
				final MergedGroup<T> group = mQueue.pollLast();
				group.mInQueue = false;
			}
		}

		public Collection<SearchGroup<T>> merge(List<Collection<SearchGroup<T>>> shards, 
				int offset, int topN) {
			final int maxQueueSize = offset + topN;

			// Init queue:
			for (int shardIDX=0; shardIDX < shards.size(); shardIDX++) {
				final Collection<SearchGroup<T>> shard = shards.get(shardIDX);
				if (!shard.isEmpty()) 
					updateNextGroup(maxQueueSize, new ShardIter<T>(shard, shardIDX));
			}

			// Pull merged topN groups:
			final List<SearchGroup<T>> newTopGroups = new ArrayList<SearchGroup<T>>();

			int count = 0;
			while (mQueue.size() != 0) {
				final MergedGroup<T> group = mQueue.pollFirst();
				group.mProcessed = true;
				
				if (count++ >= offset) {
					final SearchGroup<T> newGroup = new SearchGroup<T>();
					newGroup.mGroupValue = group.mGroupValue;
					newGroup.mSortValues = group.mTopValues;
					
					newTopGroups.add(newGroup);
					
					if (newTopGroups.size() == topN) 
						break;
				}

				// Advance all iters in this group:
				for (ShardIter<T> shardIter : group.mShards) {
					updateNextGroup(maxQueueSize, shardIter);
				}
			}

			if (newTopGroups.size() == 0) 
				return null;
			else 
				return newTopGroups;
		}
	}

	/** 
	 * Merges multiple collections of top groups, for example
	 *  obtained from separate index shards.  The provided
	 *  groupSort must match how the groups were sorted, and
	 *  the provided SearchGroups must have been computed
	 *  with fillFields=true passed to {@link
	 *  AbstractFirstPassGroupingCollector#getTopGroups}.
	 *
	 * <p>NOTE: this returns null if the topGroups is empty.
	 */
	public static <T> Collection<SearchGroup<T>> merge(
			List<Collection<SearchGroup<T>>> topGroups, int offset, int topN, ISort groupSort)
			throws IOException {
		if (topGroups.size() == 0) 
			return null;
		
		return new GroupMerger<T>(groupSort).merge(topGroups, offset, topN);
	}
	
}
