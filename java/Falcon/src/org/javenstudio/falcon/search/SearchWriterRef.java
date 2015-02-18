package org.javenstudio.falcon.search;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.RefCounted;

public abstract class SearchWriterRef extends RefCounted<SearchWriter> {
	//private static final Logger LOG = Logger.getLogger(SearchWriterRef.class);

	public SearchWriterRef(SearchWriter writer) { 
		super(writer);
	}
	
	//@Override
	//protected void close() throws ErrorException { 
	//	if (LOG.isDebugEnabled())
	//		LOG.debug("close: " + get().getName());
	//	
	//	try {
	//		get().close();
	//	} catch (IOException ex) { 
	//		throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
	//	}
	//}
	
	@Override
	public void increaseRef() {
		super.increaseRef();
		
		//if (LOG.isDebugEnabled()) {
		//	LOG.debug("increaseRef: Writer: " + get().getName() 
		//			+ " refcount=" + getRefCount());
		//}
	}
	
	@Override
	public void decreaseRef() throws ErrorException {
		super.decreaseRef();
		
		//if (LOG.isDebugEnabled()) {
		//	LOG.debug("decreaseRef: Writer: " + get().getName() 
		//			+ " refcount=" + getRefCount());
		//}
	}
	
	@Override
	public String toString() { 
		return "SearchWriterRef{" + get() + ", refcount=" + getRefCount() + "}";
	}
	
}
