package org.javenstudio.common.indexdb;

/**
 * A custom listener that's invoked when the IndexReader
 * is closed.
 */
public interface IIndexReaderClosedListener {

	public void onClose(IIndexReader reader);
	
}
