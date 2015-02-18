package org.javenstudio.hornet.search.collector;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.search.Collector;

/**
 * Just counts the total number of hits.
 */
public class TotalHitCountCollector extends Collector {
	
	private int mTotalHits;

	/** Returns how many hits matched the search. */
	public int getTotalHits() {
		return mTotalHits;
	}

	@Override
	public void setScorer(IScorer scorer) {
		// do nothing
	}

	@Override
	public void collect(int doc) {
		mTotalHits ++;
	}

	@Override
	public void setNextReader(IAtomicReaderRef context) {
		// do nothing
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		return true;
	}
	
}
