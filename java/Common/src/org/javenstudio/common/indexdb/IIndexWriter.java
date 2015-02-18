package org.javenstudio.common.indexdb;

import java.io.IOException;

public interface IIndexWriter {

	/**
	 * Specifies the open mode for {@link IndexWriter}.
	 */
	public static enum OpenMode {
	    /** 
	     * Creates a new index or overwrites an existing one. 
	     */
	    CREATE,
	    
	    /** 
	     * Opens an existing index. 
	     */
	    APPEND,
	    
	    /** 
	     * Creates a new index if one does not exist,
	     * otherwise it opens the index and documents will be appended. 
	     */
	    CREATE_OR_APPEND 
	}
	
	public IIndexContext getContext();
	public IDirectory getDirectory();
	
	/**
	 * Adds a document to this index.
	 *
	 * <p> Note that if an Exception is hit (for example disk full)
	 * then the index will be consistent, but this document
	 * may not have been added.  Furthermore, it's possible
	 * the index will have one segment in non-compound format
	 * even when using compound files (when a merge has
	 * partially succeeded).</p>
	 *
	 * <p> This method periodically flushes pending documents
	 * to the Directory (see <a href="#flush">above</a>), and
	 * also periodically triggers segment merges in the index
	 * according to the {@link MergePolicy} in use.</p>
	 *
	 * <p>Merges temporarily consume space in the
	 * directory. The amount of space required is up to 1X the
	 * size of all segments being merged, when no
	 * readers/searchers are open against the index, and up to
	 * 2X the size of all segments being merged when
	 * readers/searchers are open against the index (see
	 * {@link #forceMerge(int)} for details). The sequence of
	 * primitive merge operations performed is governed by the
	 * merge policy.
	 *
	 * <p>Note that each term in the document can be no longer
	 * than 16383 characters, otherwise an
	 * IllegalArgumentException will be thrown.</p>
	 *
	 * <p>Note that it's possible to create an invalid Unicode
	 * string in java if a UTF16 surrogate pair is malformed.
	 * In this case, the invalid characters are silently
	 * replaced with the Unicode replacement character
	 * U+FFFD.</p>
	 *
	 * <p><b>NOTE</b>: if this method hits an OutOfMemoryError
	 * you should immediately close the writer.  See <a
	 * href="#OOME">above</a> for details.</p>
	 *
	 * @throws CorruptIndexException if the index is corrupt
	 * @throws IOException if there is a low-level IO error
	 */
	public void addDocument(IDocument doc) throws IOException;
	
	/**
	 * Expert: returns a readonly reader, covering all
	 * committed as well as un-committed changes to the index.
	 * This provides "near real-time" searching, in that
	 * changes made during an IndexWriter session can be
	 * quickly made available for searching without closing
	 * the writer nor calling {@link #commit}.
	 *
	 * <p>Note that this is functionally equivalent to calling
	 * {#flush} and then opening a new reader.  But the turnaround time of this
	 * method should be faster since it avoids the potentially
	 * costly {@link #commit}.</p>
	 *
	 * <p>You must close the {@link IndexReader} returned by
	 * this method once you are done using it.</p>
	 *
	 * <p>It's <i>near</i> real-time because there is no hard
	 * guarantee on how quickly you can get a new reader after
	 * making changes with IndexWriter.  You'll have to
	 * experiment in your situation to determine if it's
	 * fast enough.  As this is a new and experimental
	 * feature, please report back on your findings so we can
	 * learn, improve and iterate.</p>
	 *
	 * <p>The resulting reader supports {@link
	 * DirectoryReader#openIfChanged}, but that call will simply forward
	 * back to this method (though this may change in the
	 * future).</p>
	 *
	 * <p>The very first time this method is called, this
	 * writer instance will make every effort to pool the
	 * readers that it opens for doing merges, applying
	 * deletes, etc.  This means additional resources (RAM,
	 * file descriptors, CPU time) will be consumed.</p>
	 *
	 * <p>For lower latency on reopening a reader, you should
	 * call {@link IndexWriterConfig#setMergedSegmentWarmer} to
	 * pre-warm a newly merged segment before it's committed
	 * to the index.  This is important for minimizing
	 * index-to-search delay after a large merge.  </p>
	 *
	 * <p>If an addIndexes* call is running in another thread,
	 * then this reader will only search those segments from
	 * the foreign index that have been successfully copied
	 * over, so far</p>.
	 *
	 * <p><b>NOTE</b>: Once the writer is closed, any
	 * outstanding readers may continue to be used.  However,
	 * if you attempt to reopen any of those readers, you'll
	 * hit an {@link AlreadyClosedException}.</p>
	 *
	 * @return IndexReader that covers entire index plus all
	 * changes made so far by this IndexWriter instance
	 *
	 * @throws IOException
	 */
	public IDirectoryReader getReader(boolean applyAllDeletes) 
			throws IOException;
	
	/**
	 * Commits all changes to an index and closes all
	 * associated files.  Note that this may be a costly
	 * operation, so, try to re-use a single writer instead of
	 * closing and opening a new one.  See {@link #commit()} for
	 * caveats about write caching done by some IO devices.
	 *
	 * <p> If an Exception is hit during close, eg due to disk
	 * full or some other reason, then both the on-disk index
	 * and the internal state of the IndexWriter instance will
	 * be consistent.  However, the close will not be complete
	 * even though part of it (flushing buffered documents)
	 * may have succeeded, so the write lock will still be
	 * held.</p>
	 *
	 * <p> If you can correct the underlying cause (eg free up
	 * some disk space) then you can call close() again.
	 * Failing that, if you want to force the write lock to be
	 * released (dangerous, because you may then lose buffered
	 * docs in the IndexWriter instance) then you can do
	 * something like this:</p>
	 *
	 * <pre>
	 * try {
	 *   writer.close();
	 * } finally {
	 *   if (IndexWriter.isLocked(directory)) {
	 *     IndexWriter.unlock(directory);
	 *   }
	 * }
	 * </pre>
	 *
	 * after which, you must be certain not to use the writer
	 * instance anymore.</p>
	 *
	 * <p><b>NOTE</b>: if this method hits an OutOfMemoryError
	 * you should immediately close the writer, again.  See <a
	 * href="#OOME">above</a> for details.</p>
	 *
	 * @throws CorruptIndexException if the index is corrupt
	 * @throws IOException if there is a low-level IO error
	 */
	public void close() throws CorruptIndexException, IOException;
	
	/** @see close() */
	public boolean isClosed();
	
}
