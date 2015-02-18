package org.javenstudio.common.indexdb.index;

import java.io.Closeable;
import java.io.IOException;

import org.javenstudio.common.indexdb.CorruptIndexException;
import org.javenstudio.common.indexdb.IMergeOne;

/** 
 * <p>Expert: {@link IndexWriter} uses an instance
 *  implementing this interface to execute the merges
 *  selected by a {@link MergePolicy}.  The default
 *  MergeScheduler is {@link ConcurrentMergeScheduler}.</p>
 */
public abstract class MergeScheduler implements Closeable {

	/** Run the merges provided by {@link IndexWriter#getNextMerge()}. */
	public abstract void merge(IndexWriter writer) 
			throws CorruptIndexException, IOException;

	/** Close this MergeScheduler. */
	public void close() throws CorruptIndexException, IOException { 
		// do nothing
	}
	
	/** 
	 * A {@link MergeScheduler} that simply does each merge
	 *  sequentially, using the current thread. 
	 */
	public static class SerialMergeScheduler extends MergeScheduler {
		/** 
		 * Just do the merges in sequence. We do this
		 * "synchronized" so that even if the application is using
		 * multiple threads, only one merge may run at a time. 
		 */
		@Override
		public synchronized void merge(IndexWriter writer) 
				throws CorruptIndexException, IOException {
			while (true) {
				IMergeOne merge = writer.getMergeControl().getNextMerge();
				if (merge == null)
					break;
				writer.getMergeControl().merge(merge);
			}
		}
	}
	
}
