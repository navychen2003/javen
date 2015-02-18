package org.javenstudio.falcon.search;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.RefCounted;

public abstract class SearcherRef extends RefCounted<Searcher> {
	//private static final Logger LOG = Logger.getLogger(SearcherRef.class);

	public SearcherRef(Searcher searcher) { 
		super(searcher);
	}
	
	//@Override
	//protected void close() throws ErrorException { 
	//	if (LOG.isDebugEnabled())
	//		LOG.debug("close: " + get().getName());
	//	
	//	get().close();
	//}
	
	@Override
	public void increaseRef() {
		super.increaseRef();
		
		//if (LOG.isDebugEnabled()) {
		//	LOG.debug("increaseRef: Searcher: " + get().getName() 
		//			+ " refcount=" + getRefCount());
		//}
	}
	
	@Override
	public void decreaseRef() throws ErrorException {
		super.decreaseRef();
		
		//if (LOG.isDebugEnabled()) {
		//	LOG.debug("decreaseRef: Searcher: " + get().getName() 
		//			+ " refcount=" + getRefCount());
		//}
	}
	
	@Override
	public String toString() { 
		return "SearcherRef{" + get() + ", refcount=" + getRefCount() + "}";
	}
	
}
