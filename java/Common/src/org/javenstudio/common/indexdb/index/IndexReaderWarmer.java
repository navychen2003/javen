package org.javenstudio.common.indexdb.index;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReader;

/** 
 * If {@link DirectoryReader#open(IndexWriter,boolean)} has
 *  been called (ie, this writer is in near real-time
 *  mode), then after a merge completes, this class can be
 *  invoked to warm the reader on the newly merged
 *  segment, before the merge commits.  This is not
 *  required for near real-time search, but will reduce
 *  search latency on opening a new near real-time reader
 *  after a merge completes.
 *
 * <p><b>NOTE</b>: warm is called before any deletes have
 * been carried over to the merged segment. 
 */
public abstract class IndexReaderWarmer {

	public abstract void warm(IAtomicReader reader) throws IOException;
	
}
