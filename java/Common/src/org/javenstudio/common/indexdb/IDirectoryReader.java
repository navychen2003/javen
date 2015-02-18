package org.javenstudio.common.indexdb;

import java.io.IOException;

public interface IDirectoryReader extends IIndexReader {

	/** Returns the directory this index resides in. */
	public IDirectory getDirectory();
	
	/**
	 * Version number when this IndexReader was opened.
	 *
	 * <p>This method
	 * returns the version recorded in the commit that the
	 * reader opened.  This version is advanced every time
	 * a change is made with {@link IndexWriter}.</p>
	 */
	public long getVersion();

	/**
	 * Check whether any new changes have occurred to the
	 * index since this reader was opened.
	 *
	 * <p>If this reader was created by calling {@link #open},  
	 * then this method checks if any further commits 
	 * (see {@link IndexWriter#commit}) have occurred in the 
	 * directory.</p>
	 *
	 * <p>If instead this reader is a near real-time reader
	 * (ie, obtained by a call to {@link
	 * DirectoryReader#open(IndexWriter,boolean)}, or by calling {@link #openIfChanged}
	 * on a near real-time reader), then this method checks if
	 * either a new commit has occurred, or any new
	 * uncommitted changes have taken place via the writer.
	 * Note that even if the writer has only performed
	 * merging, this method will still return false.</p>
	 *
	 * <p>In any event, if this returns false, you should call
	 * {@link #openIfChanged} to get a new reader that sees the
	 * changes.</p>
	 *
	 * @throws CorruptIndexException if the index is corrupt
	 * @throws IOException           if there is a low-level IO error
	 */
	public boolean isCurrent() throws CorruptIndexException, IOException;

	/**
	 * Expert: return the IndexCommit that this reader has opened.
	 */
	public IIndexCommit getIndexCommit() throws CorruptIndexException, IOException;
	
}
