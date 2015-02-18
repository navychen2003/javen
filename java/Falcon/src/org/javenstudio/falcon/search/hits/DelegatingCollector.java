package org.javenstudio.falcon.search.hits;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ICollector;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.search.Collector;

/** A simple delegating collector where one can set the delegate after creation */
public class DelegatingCollector extends Collector {

	/** 
	 * for internal testing purposes only to determine the number of times 
	 * a delegating collector chain was used 
	 */
	public static int sSetLastDelegateCount;

	protected ICollector mDelegate;
	protected IScorer mScorer;
	protected IAtomicReaderRef mContext;
	protected int mDocBase;

	public ICollector getDelegate() {
		return mDelegate;
	}

	public void setDelegate(ICollector delegate) {
		mDelegate = delegate;
	}

	/** Sets the last delegate in a chain of DelegatingCollectors */
	public void setLastDelegate(ICollector delegate) {
		DelegatingCollector ptr = this;
		for (; ptr.getDelegate() instanceof DelegatingCollector; ptr = (DelegatingCollector)ptr.getDelegate());
		ptr.setDelegate(delegate);
		sSetLastDelegateCount ++;
	}

	@Override
	public void setScorer(IScorer scorer) throws IOException {
		mScorer = scorer;
		mDelegate.setScorer(scorer);
	}

	@Override
	public void collect(int doc) throws IOException {
		mDelegate.collect(doc);
	}

	@Override
	public void setNextReader(IAtomicReaderRef context) throws IOException {
		mContext = context;
		mDocBase = context.getDocBase();
		mDelegate.setNextReader(context);
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		return mDelegate.acceptsDocsOutOfOrder();
	}
	
}

