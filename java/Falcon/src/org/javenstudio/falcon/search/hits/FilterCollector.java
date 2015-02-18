package org.javenstudio.falcon.search.hits;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ICollector;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.search.Collector;

/**
 * A collector that filters incoming doc ids that are not in the filter.
 *
 */
public class FilterCollector extends Collector {

	private final DocSet mFilter;
	private final ICollector mDelegate;
	private int mDocBase;
	private int mMatches;

	public FilterCollector(DocSet filter, ICollector delegate) {
		mFilter = filter;
		mDelegate = delegate;
	}

	public void setScorer(IScorer scorer) throws IOException {
		mDelegate.setScorer(scorer);
	}

	public void collect(int doc) throws IOException {
		mMatches ++;
		if (mFilter.exists(doc + mDocBase)) 
			mDelegate.collect(doc);
	}

	public void setNextReader(IAtomicReaderRef context) throws IOException {
		mDocBase = context.getDocBase();
		mDelegate.setNextReader(context);
	}

	public boolean acceptsDocsOutOfOrder() {
		return mDelegate.acceptsDocsOutOfOrder();
	}

	public int getMatches() {
		return mMatches;
	}

	/**
	 * Returns the delegate collector
	 *
	 * @return the delegate collector
	 */
	public ICollector getDelegate() {
		return mDelegate;
	}
	
}
