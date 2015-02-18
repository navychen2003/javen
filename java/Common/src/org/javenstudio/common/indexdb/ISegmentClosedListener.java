package org.javenstudio.common.indexdb;

/**
 * Called when the shared core for this SegmentReader
 * is closed.
 * <p>
 * This listener is called only once all SegmentReaders 
 * sharing the same core are closed.  At this point it 
 * is safe for apps to evict this reader from any caches 
 * keyed on {@link #getCoreCacheKey}.  This is the same 
 * interface that {@link FieldCache} uses, internally, 
 * to evict entries.</p>
 */
public interface ISegmentClosedListener {

	public void onClose(ISegmentReader owner);
	
}
