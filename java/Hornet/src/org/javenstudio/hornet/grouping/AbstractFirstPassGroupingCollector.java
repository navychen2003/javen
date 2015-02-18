package org.javenstudio.hornet.grouping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IFieldComparator;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.search.Collector;

/** 
 * FirstPassGroupingCollector is the first of two passes necessary
 *  to collect grouped hits.  This pass gathers the top N sorted
 *  groups. Concrete subclasses define what a group is and how it
 *  is internally collected.
 *
 *  <p>See {@link org.apache.lucene.search.grouping} for more
 *  details including a full code example.</p>
 *
 */
public abstract class AbstractFirstPassGroupingCollector<GT> 
		extends Collector {

	private final Map<GT, CollectedSearchGroup<GT>> mGroupMap;
	
	// Set once we reach topNGroups unique groups:
	private TreeSet<CollectedSearchGroup<GT>> mOrderedGroups;
	
	private final ISort mGroupSort;
	private final IFieldComparator<?>[] mComparators;
	private final int[] mReversed;
	private final int mTopNGroups;
	private final int mCompIDXEnd;

	private int mDocBase;
	private int mSpareSlot;

	/**
	 * Create the first pass collector.
	 *
	 *  @param groupSort The {@link Sort} used to sort the
	 *    groups.  The top sorted document within each group
	 *    according to groupSort, determines how that group
	 *    sorts against other groups.  This must be non-null,
	 *    ie, if you want to groupSort by relevance use
	 *    Sort.RELEVANCE.
	 *  @param topNGroups How many top groups to keep.
	 *  @throws IOException If I/O related errors occur
	 */
	public AbstractFirstPassGroupingCollector(ISort groupSort, 
			int topNGroups) throws IOException {
		if (topNGroups < 1) 
			throw new IllegalArgumentException("topNGroups must be >= 1 (got " + topNGroups + ")");

		// TODO: allow null groupSort to mean "by relevance",
		// and specialize it?
		mGroupSort = groupSort;
		mTopNGroups = topNGroups;

		final ISortField[] sortFields = groupSort.getSortFields();
		
		if (sortFields == null || sortFields.length == 0) {
			throw new IllegalArgumentException("sortFields must not be empty, collector is " 
					+ getClass().getName());
		}
		
		mComparators = new IFieldComparator[sortFields.length];
		mCompIDXEnd = mComparators.length - 1;
		mReversed = new int[sortFields.length];
		
		for (int i = 0; i < sortFields.length; i++) {
			final ISortField sortField = sortFields[i];

			// use topNGroups + 1 so we have a spare slot to use for comparing (tracked by this.spareSlot):
			mComparators[i] = sortField.getComparator(topNGroups + 1, i);
			mReversed[i] = sortField.getReverse() ? -1 : 1;
		}

		mSpareSlot = topNGroups;
		mGroupMap = new HashMap<GT, CollectedSearchGroup<GT>>(topNGroups);
	}

	/**
	 * Returns top groups, starting from offset.  This may
	 * return null, if no groups were collected, or if the
	 * number of unique groups collected is <= offset.
	 *
	 * @param groupOffset The offset in the collected groups
	 * @param fillFields Whether to fill to {@link SearchGroup#sortValues}
	 * @return top groups, starting from offset
	 */
	public Collection<SearchGroup<GT>> getTopGroups(int groupOffset, boolean fillFields) {
		if (groupOffset < 0) 
			throw new IllegalArgumentException("groupOffset must be >= 0 (got " + groupOffset + ")");
		
		if (mGroupMap.size() <= groupOffset) 
			return null;

		if (mOrderedGroups == null) 
			buildSortedSet();

		final Collection<SearchGroup<GT>> result = new ArrayList<SearchGroup<GT>>();
		final int sortFieldCount = mGroupSort.getSortFields().length;
		
		int upto = 0;
    	
		for (CollectedSearchGroup<GT> group : mOrderedGroups) {
			if (upto++ < groupOffset) 
				continue;
      
			SearchGroup<GT> searchGroup = new SearchGroup<GT>();
			searchGroup.setGroupValue(group.getGroupValue());
			
			if (fillFields) {
				searchGroup.setSortValues(new Object[sortFieldCount]);
				
				for (int sortFieldIDX=0; sortFieldIDX < sortFieldCount; sortFieldIDX++) {
					searchGroup.setSortValueAt(sortFieldIDX, 
							mComparators[sortFieldIDX].getValue(group.getComparatorSlot()));
				}
			}
			
			result.add(searchGroup);
		}
		
		return result;
	}

	@Override
	public void setScorer(IScorer scorer) throws IOException {
		for (IFieldComparator<?> comparator : mComparators) {
			comparator.setScorer(scorer);
		}
	}

	@Override
	public void collect(int doc) throws IOException {
		// If orderedGroups != null we already have collected N groups and
		// can short circuit by comparing this document to the bottom group,
		// without having to find what group this document belongs to.
    
		// Even if this document belongs to a group in the top N, we'll know that
		// we don't have to update that group.

		// Downside: if the number of unique groups is very low, this is
		// wasted effort as we will most likely be updating an existing group.
		if (mOrderedGroups != null) {
			for (int compIDX = 0;; compIDX++) {
				final int c = mReversed[compIDX] * mComparators[compIDX].compareBottom(doc);
				
				if (c < 0) {
					// Definitely not competitive. So don't even bother to continue
					return;
					
				} else if (c > 0) {
					// Definitely competitive.
					break;
					
				} else if (compIDX == mCompIDXEnd) {
					// Here c=0. If we're at the last comparator, this doc is not
					// competitive, since docs are visited in doc Id order, which means
					// this doc cannot compete with any other document in the queue.
					return;
				}
			}
		}

		// TODO: should we add option to mean "ignore docs that
		// don't have the group field" (instead of stuffing them
		// under null group)?
		final GT groupValue = getDocGroupValue(doc);
		final CollectedSearchGroup<GT> group = mGroupMap.get(groupValue);

		if (group == null) {
			// First time we are seeing this group, or, we've seen
			// it before but it fell out of the top N and is now
			// coming back

			if (mGroupMap.size() < mTopNGroups) {
				// Still in startup transient: we have not
				// seen enough unique groups to start pruning them;
				// just keep collecting them

				// Add a new CollectedSearchGroup:
				CollectedSearchGroup<GT> sg = new CollectedSearchGroup<GT>();
				sg.setGroupValue(copyDocGroupValue(groupValue, null));
				sg.setComparatorSlot(mGroupMap.size());
				sg.setTopDoc(mDocBase + doc);
				
				for (IFieldComparator<?> fc : mComparators) {
					fc.copy(sg.getComparatorSlot(), doc);
				}
				
				mGroupMap.put(sg.getGroupValue(), sg);

				if (mGroupMap.size() == mTopNGroups) {
					// End of startup transient: we now have max
					// number of groups; from here on we will drop
					// bottom group when we insert new one:
					buildSortedSet();
				}

				return;
			}

			// We already tested that the document is competitive, so replace
			// the bottom group with this new group.
			final CollectedSearchGroup<GT> bottomGroup = mOrderedGroups.pollLast();
			assert mOrderedGroups.size() == mTopNGroups -1;

			mGroupMap.remove(bottomGroup.getGroupValue());

			// reuse the removed CollectedSearchGroup
			bottomGroup.setGroupValue(copyDocGroupValue(groupValue, bottomGroup.getGroupValue()));
			bottomGroup.setTopDoc(mDocBase + doc);

			for (IFieldComparator<?> fc : mComparators) {
				fc.copy(bottomGroup.getComparatorSlot(), doc);
			}

			mGroupMap.put(bottomGroup.getGroupValue(), bottomGroup);
			mOrderedGroups.add(bottomGroup);
			assert mOrderedGroups.size() == mTopNGroups;

			final int lastComparatorSlot = mOrderedGroups.last().getComparatorSlot();
			for (IFieldComparator<?> fc : mComparators) {
				fc.setBottom(lastComparatorSlot);
			}

			return;
		}

		// Update existing group:
		for (int compIDX = 0;; compIDX++) {
			final IFieldComparator<?> fc = mComparators[compIDX];
			fc.copy(mSpareSlot, doc);

			final int c = mReversed[compIDX] * fc.compare(group.getComparatorSlot(), mSpareSlot);
			if (c < 0) {
				// Definitely not competitive.
				return;
				
			} else if (c > 0) {
				// Definitely competitive; set remaining comparators:
				for (int compIDX2=compIDX+1; compIDX2 < mComparators.length; compIDX2++) {
					mComparators[compIDX2].copy(mSpareSlot, doc);
				}
				break;
				
			} else if (compIDX == mCompIDXEnd) {
				// Here c=0. If we're at the last comparator, this doc is not
				// competitive, since docs are visited in doc Id order, which means
				// this doc cannot compete with any other document in the queue.
				return;
			}
		}

		// Remove before updating the group since lookup is done via comparators
		// TODO: optimize this
		final CollectedSearchGroup<GT> prevLast;
		
		if (mOrderedGroups != null) {
			prevLast = mOrderedGroups.last();
			mOrderedGroups.remove(group);
			assert mOrderedGroups.size() == mTopNGroups-1;
			
		} else {
			prevLast = null;
		}

		group.setTopDoc(mDocBase + doc);

		// Swap slots
		final int tmp = mSpareSlot;
		mSpareSlot = group.getComparatorSlot();
		group.setComparatorSlot(tmp);

		// Re-add the changed group
		if (mOrderedGroups != null) {
			mOrderedGroups.add(group);
			assert mOrderedGroups.size() == mTopNGroups;
			
			final CollectedSearchGroup<?> newLast = mOrderedGroups.last();
			
			// If we changed the value of the last group, or changed which group was last, then update bottom:
			if (group == newLast || prevLast != newLast) {
				for (IFieldComparator<?> fc : mComparators) {
					fc.setBottom(newLast.getComparatorSlot());
				}
			}
		}
	}

	private void buildSortedSet() {
		final Comparator<CollectedSearchGroup<?>> comparator = new Comparator<CollectedSearchGroup<?>>() {
			public int compare(CollectedSearchGroup<?> o1, CollectedSearchGroup<?> o2) {
				for (int compIDX = 0;; compIDX++) {
					IFieldComparator<?> fc = mComparators[compIDX];
					final int c = mReversed[compIDX] * fc.compare(o1.getComparatorSlot(), o2.getComparatorSlot());
					if (c != 0) 
						return c;
					else if (compIDX == mCompIDXEnd) 
						return o1.getTopDoc() - o2.getTopDoc();
				}
			}
		};

		mOrderedGroups = new TreeSet<CollectedSearchGroup<GT>>(comparator);
		mOrderedGroups.addAll(mGroupMap.values());
		assert mOrderedGroups.size() > 0;

		for (IFieldComparator<?> fc : mComparators) {
			fc.setBottom(mOrderedGroups.last().getComparatorSlot());
		}
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		return false;
	}

	@Override
	public void setNextReader(IAtomicReaderRef readerContext) throws IOException {
		mDocBase = readerContext.getDocBase();
		for (int i=0; i < mComparators.length; i++) {
			mComparators[i] = mComparators[i].setNextReader(readerContext);
		}
	}

	/**
	 * Returns the group value for the specified doc.
	 *
	 * @param doc The specified doc
	 * @return the group value for the specified doc
	 */
	protected abstract GT getDocGroupValue(int doc);

	/**
	 * Returns a copy of the specified group value by creating a new instance 
	 * and copying the value from the specified
	 * groupValue in the new instance. Or optionally the reuse argument 
	 * can be used to copy the group value in.
	 *
	 * @param groupValue The group value to copy
	 * @param reuse Optionally a reuse instance to prevent a new instance creation
	 * @return a copy of the specified group value
	 */
	protected abstract GT copyDocGroupValue(GT groupValue, GT reuse);

}

