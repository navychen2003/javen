package org.javenstudio.hornet.grouping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.search.Collector;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.PriorityQueue;

/**
 * Base class for computing grouped facets.
 *
 */
public abstract class AbstractGroupFacetCollector extends Collector {

	protected final List<SegmentResult> mSegmentResults;
	protected final String mGroupField;
	protected final String mFacetField;
	protected final BytesRef mFacetPrefix;
  
	protected int[] mSegmentFacetCounts;
	protected int mSegmentTotalCount;
	protected int mStartFacetOrd;
	protected int mEndFacetOrd;

	protected AbstractGroupFacetCollector(String groupField, 
			String facetField, BytesRef facetPrefix) {
		mGroupField = groupField;
		mFacetField = facetField;
		mFacetPrefix = facetPrefix;
		mSegmentResults = new ArrayList<SegmentResult>();
	}

	/**
	 * Returns grouped facet results that were computed over zero or more segments.
	 * Grouped facet counts are merged from zero or more segment results.
	 *
	 * @param size The total number of facets to include. This is typically offset + limit
	 * @param minCount The minimum count a facet entry should have to 
	 * be included in the grouped facet result
	 * @param orderByCount Whether to sort the facet entries by facet entry count. 
	 * If <code>false</code> then the facets are sorted lexicographically in ascending order.
	 * @return grouped facet results
	 * @throws IOException If I/O related errors occur during merging segment grouped facet counts.
	 */
	public GroupedFacetResult mergeSegmentResults(int size, int minCount, 
			boolean orderByCount) throws IOException {
		if (mSegmentFacetCounts != null) {
			mSegmentResults.add(createSegmentResult());
			mSegmentFacetCounts = null; // reset
		}

		int totalCount = 0;
		int missingCount = 0;
		
		SegmentResultPriorityQueue segments = 
				new SegmentResultPriorityQueue(mSegmentResults.size());
		
		for (SegmentResult segmentResult : mSegmentResults) {
			missingCount += segmentResult.mMissing;
			if (segmentResult.mMergePos >= segmentResult.mMaxTermPos) 
				continue;
      
			totalCount += segmentResult.mTotal;
			segments.add(segmentResult);
		}

		GroupedFacetResult facetResult = new GroupedFacetResult(size, minCount, 
				orderByCount, totalCount, missingCount);
		
		while (segments.size() > 0) {
			SegmentResult segmentResult = segments.top();
			BytesRef currentFacetValue = BytesRef.deepCopyOf(segmentResult.mMergeTerm);
			int count = 0;

			do {
				count += segmentResult.mCounts[segmentResult.mMergePos++];
				
				if (segmentResult.mMergePos < segmentResult.mMaxTermPos) {
					segmentResult.nextTerm();
					segmentResult = segments.updateTop();
					
				} else {
					segments.pop();
					segmentResult = segments.top();
					
					if (segmentResult == null) 
						break;
				}
			} while (currentFacetValue.equals(segmentResult.mMergeTerm));
			
			facetResult.addFacetCount(currentFacetValue, count);
		}
		
		return facetResult;
	}

	protected abstract SegmentResult createSegmentResult() throws IOException;

	public void setScorer(IScorer scorer) throws IOException {
		// do nothing
	}

	public boolean acceptsDocsOutOfOrder() {
		return true;
	}

	private static class SegmentResultPriorityQueue extends PriorityQueue<SegmentResult> {
		SegmentResultPriorityQueue(int maxSize) {
			super(maxSize);
		}

		@Override
		protected boolean lessThan(SegmentResult a, SegmentResult b) {
			return a.mMergeTerm.compareTo(b.mMergeTerm) < 0;
		}
	}

}
