package org.javenstudio.hornet.grouping;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

import org.javenstudio.common.indexdb.util.BytesRef;

/**
 * The grouped facet result. Containing grouped facet entries, 
 * total count and total missing count.
 */
public class GroupedFacetResult {
	
	private final static Comparator<FacetEntry> sOrderByCountAndValue = 
		new Comparator<FacetEntry>() {
			public int compare(FacetEntry a, FacetEntry b) {
				int cmp = b.getCount() - a.getCount(); // Highest count first!
				if (cmp != 0) 
					return cmp;
				
				return a.getValue().compareTo(b.getValue());
			}
		};

	private final static Comparator<FacetEntry> sOrderByValue = 
		new Comparator<FacetEntry>() {
			public int compare(FacetEntry a, FacetEntry b) {
				return a.getValue().compareTo(b.getValue());
			}
		};

	private final NavigableSet<FacetEntry> mFacetEntries;
	private final int mMaxSize;
	private final int mTotalMissingCount;
	private final int mTotalCount;
	private int mCurrentMin;

	public GroupedFacetResult(int size, int minCount, boolean orderByCount, 
			int totalCount, int totalMissingCount) {
		mFacetEntries = new TreeSet<FacetEntry>(orderByCount ? sOrderByCountAndValue : sOrderByValue);
		mTotalMissingCount = totalMissingCount;
		mTotalCount = totalCount;
		mMaxSize = size;
		mCurrentMin = minCount;
	}

	public void addFacetCount(BytesRef facetValue, int count) {
		if (count < mCurrentMin) 
			return;

		FacetEntry facetEntry = new FacetEntry(facetValue, count);
		if (mFacetEntries.size() == mMaxSize) {
			if (mFacetEntries.higher(facetEntry) == null) 
				return;
			
			mFacetEntries.pollLast();
		}
		mFacetEntries.add(facetEntry);

		if (mFacetEntries.size() == mMaxSize) 
			mCurrentMin = mFacetEntries.last().getCount();
	}

	/**
	 * Returns a list of facet entries to be rendered based on the specified offset and limit.
	 * The facet entries are retrieved from the facet entries collected during merging.
	 *
	 * @param offset The offset in the collected facet entries during merging
	 * @param limit The number of facets to return starting from the offset.
	 * @return a list of facet entries to be rendered based on the specified offset and limit
	 */
	public List<FacetEntry> getFacetEntries(int offset, int limit) {
		List<FacetEntry> entries = new LinkedList<FacetEntry>();
		
		limit += offset;
		int i = 0;
		
		for (FacetEntry facetEntry : mFacetEntries) {
			if (i < offset) {
				i++;
				continue;
			}
			
			if (i++ >= limit) 
				break;
			
			entries.add(facetEntry);
		}
		
		return entries;
	}

	/**
	 * Returns the sum of all facet entries counts.
	 *
	 * @return the sum of all facet entries counts
	 */
	public int getTotalCount() {
		return mTotalCount;
	}

	/**
	 * Returns the number of groups that didn't have a facet value.
	 *
	 * @return the number of groups that didn't have a facet value
	 */
	public int getTotalMissingCount() {
		return mTotalMissingCount;
	}
	
}
