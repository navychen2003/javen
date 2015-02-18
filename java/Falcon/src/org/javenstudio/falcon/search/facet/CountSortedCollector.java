package org.javenstudio.falcon.search.facet;

import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.CharsRef;
import org.javenstudio.common.indexdb.util.UnicodeUtil;
import org.javenstudio.falcon.util.BoundedTreeSet;
import org.javenstudio.falcon.util.NamedList;

//This collector expects facets to be collected in index order
final class CountSortedCollector extends FacetCollector {
	
	protected final CharsRef mSpare = new CharsRef();
	protected final BoundedTreeSet<FacetCountPair<String,Integer>> mQueue;

	protected final int mOffset;
	protected final int mLimit;
	protected final int mMaxSize;
	
	protected int mMin;  // the smallest value in the top 'N' values

	public CountSortedCollector(int offset, int limit, int mincount) {
		mOffset = offset;
		mLimit = limit;
		mMaxSize = limit>0 ? offset+limit : Integer.MAX_VALUE-1;
		mQueue = new BoundedTreeSet<FacetCountPair<String,Integer>>(mMaxSize);
		mMin = mincount-1;  // the smallest value in the top 'N' values
	}

	@Override
	public boolean collect(BytesRef term, int count) {
		if (count > mMin) {
			// NOTE: we use c>min rather than c>=min as an optimization because we are going in
			// index order, so we already know that the keys are ordered.  This can be very
			// important if a lot of the counts are repeated (like zero counts would be).
			UnicodeUtil.UTF8toUTF16(term, mSpare);
			mQueue.add(new FacetCountPair<String,Integer>(mSpare.toString(), count));
			
			if (mQueue.size() >= mMaxSize) 
				mMin = mQueue.last().getValue();
		}
		
		return false;
	}

	@Override
	public NamedList<Integer> getFacetCounts() {
		NamedList<Integer> res = new NamedList<Integer>();
		
		int off = mOffset;
		int lim = mLimit>=0 ? mLimit : Integer.MAX_VALUE;
		
		// now select the right page from the results
		for (FacetCountPair<String,Integer> p : mQueue) {
			if (--off >= 0) continue;
			if (--lim < 0) break;
			
			res.add(p.getKey(), p.getValue());
		}
		
		return res;
	}
	
}
