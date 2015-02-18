package org.javenstudio.common.indexdb.index;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.javenstudio.common.indexdb.AlreadyClosedException;
import org.javenstudio.common.indexdb.CorruptIndexException;
import org.javenstudio.common.indexdb.IAnalyzer;
import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IDirectoryReader;
import org.javenstudio.common.indexdb.IDocument;
import org.javenstudio.common.indexdb.IIndexWriter;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ISegmentCommitInfo;
import org.javenstudio.common.indexdb.ISegmentInfos;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.codec.IIndexFormat;
import org.javenstudio.common.indexdb.index.field.FieldNumbers;

abstract class IndexWriterBase implements IIndexWriter {

	public abstract IIndexContext getContext();
	public abstract IndexParams getIndexParams();
	public abstract IIndexFormat getIndexFormat();
	public abstract IAnalyzer getAnalyzer();
	public abstract IDirectory getDirectory();
	
	public abstract ISegmentInfos getSegmentInfos();
	public abstract SegmentWriter getSegmentWriter();
	public abstract IndexFileDeleter getDeleter();
	public abstract DeletesStream getDeletesStream();
	public abstract MergeControl getMergeControl();
	
	public abstract AtomicInteger getFlushCount();
	public abstract ReaderPool getReaderPool();
	public abstract boolean isPoolReaders();
	
	public abstract FieldNumbers getGlobalFieldNumbers();
	public abstract String newSegmentName();
	
	protected abstract void ensureOpen() throws AlreadyClosedException;
	protected abstract void handleOOM(OutOfMemoryError oom, String location);
	
    public synchronized boolean hasDeletions() {
        ensureOpen();
        
        if (getDeletesStream().any()) 
        	return true;
        
        if (getSegmentWriter().anyDeletions()) 
        	return true;
        
        for (ISegmentCommitInfo info : getSegmentInfos()) {
        	if (info.hasDeletions()) 
        		return true;
        }
        
        return false;
	}
	
	/** 
     * Returns total number of docs in this index, including
     *  docs not yet flushed (still in the RAM buffer),
     *  not counting deletions.
     *  @see #getNumDocs 
     */
    public synchronized int getMaxDoc() {
    	ensureOpen();
    	
    	int count = 0;
    	
    	SegmentWriter writer = getSegmentWriter();
    	if (writer != null)
    		count = writer.getNumDocs();

    	count += getSegmentInfos().getTotalDocCount();
    	
    	return count;
    }

    /** 
     * Returns total number of docs in this index, including
     *  docs not yet flushed (still in the RAM buffer), and
     *  including deletions.  <b>NOTE:</b> buffered deletions
     *  are not counted.  If you really need these to be
     *  counted you should call {@link #commit()} first.
     *  @see #getNumDocs 
     */
    public synchronized int getNumDocs() {
    	ensureOpen();
    	
    	int count = 0;
    	
    	SegmentWriter writer = getSegmentWriter();
    	if (writer != null)
    		count = writer.getNumDocs();

    	for (ISegmentCommitInfo info : getSegmentInfos()) {
    		count += info.getSegmentInfo().getDocCount() - getNumDeletedDocs(info);
    	}
    	
    	return count;
    }
	
    /** 
	 * Expert:  Return the total size of all index files currently cached in memory.
	 * Useful for size management with flushRamDocs()
	 */
	public final long getRamSizeInBytes() {
		ensureOpen();
		return getSegmentWriter().getFlushControl().getNetBytes() 
				+ getDeletesStream().getBytesUsed();
	}
	
	/** 
	 * Expert:  Return the number of documents currently
	 *  buffered in RAM. 
	 */
	public final synchronized int getNumRamDocs() {
		ensureOpen();
		return getSegmentWriter().getNumDocs();
	}
    
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
	public abstract IDirectoryReader getReader(boolean applyAllDeletes) 
			throws IOException;
	
	/**
	 * Obtain the number of deleted docs for a pooled reader.
	 * If the reader isn't being pooled, the segmentInfo's 
	 * delCount is returned.
	 */
	public abstract int getNumDeletedDocs(ISegmentCommitInfo info);
	
	public abstract void publishFrozenDeletes(FrozenDeletes packet);
	
	public abstract void applyAllDeletes() throws IOException;
	
	/**
	 * Prepares the {@link SegmentInfo} for the new flushed segment and persists
	 * the deleted documents {@link MutableBits}. Use
   	 * {@link #publishFlushedSegment(SegmentCommitInfo, FrozenBufferedDeletes, FrozenBufferedDeletes)} to
   	 * publish the returned {@link SegmentInfo} together with its segment private
   	 * delete packet.
   	 * 
   	 * @see #publishFlushedSegment(SegmentCommitInfo, FrozenBufferedDeletes, FrozenBufferedDeletes)
   	 */
	public abstract ISegmentCommitInfo prepareFlushedSegment(
			FlushedSegment flushedSegment) throws IOException;
	
	/**
	 * Atomically adds the segment private delete packet and publishes the flushed
	 * segments SegmentInfo to the index writer. NOTE: use
	 * {@link #prepareFlushedSegment(FlushedSegment)} to obtain the
	 * {@link SegmentInfo} for the flushed segment.
	 * 
	 * @see #prepareFlushedSegment(FlushedSegment)
	 */
	public abstract void publishFlushedSegment(ISegmentCommitInfo newSegment,
			FrozenDeletes packet, FrozenDeletes globalPacket) throws IOException;
	
	/**
	 * A hook for extending classes to execute operations after pending added and
	 * deleted documents have been flushed to the Directory but before the change
	 * is committed (new segments_N file written).
	 */
	public void doAfterFlush() throws IOException {}

	/**
	 * A hook for extending classes to execute operations before pending added and
	 * deleted documents are flushed to the Directory.
	 */
	public void doBeforeFlush() throws IOException {}
	
	/**
	 * Atomically adds a block of documents with sequentially
	 * assigned document IDs, such that an external reader
	 * will see all or none of the documents.
	 *
	 * <p><b>WARNING</b>: the index does not currently record
	 * which documents were added as a block.  Today this is
	 * fine, because merging will preserve a block. The order of
	 * documents within a segment will be preserved, even when child
	 * documents within a block are deleted. Most search features
	 * (like result grouping and block joining) require you to
	 * mark documents; when these documents are deleted these
	 * search features will not work as expected. Obviously adding
	 * documents to an existing block will require you the reindex
	 * the entire block.
	 *
	 * <p>However it's possible that in the future Indexdb may
	 * merge more aggressively re-order documents (for example,
	 * perhaps to obtain better index compression), in which case
	 * you may need to fully re-index your documents at that time.
	 *
	 * <p>See {@link #addDocument(Iterable)} for details on
	 * index and IndexWriter state after an Exception, and
	 * flushing/merging temporary free space requirements.</p>
	 *
	 * <p><b>NOTE</b>: tools that do offline splitting of an index
	 * (for example, IndexSplitter in contrib) or
	 * re-sorting of documents (for example, IndexSorter in
	 * contrib) are not aware of these atomically added documents
	 * and will likely break them up.  Use such tools at your
	 * own risk!
	 *
	 * <p><b>NOTE</b>: if this method hits an OutOfMemoryError
	 * you should immediately close the writer.  See <a
	 * href="#OOME">above</a> for details.</p>
	 *
	 * @throws CorruptIndexException if the index is corrupt
	 * @throws IOException if there is a low-level IO error
	 */
	public void addDocuments(Iterable<? extends IDocument> docs) 
			throws IOException {
		addDocuments(docs, getAnalyzer());
	}
	
	/**
	 * Atomically adds a block of documents, analyzed using the
	 * provided analyzer, with sequentially assigned document
	 * IDs, such that an external reader will see all or none
	 * of the documents. 
	 *
	 * @throws CorruptIndexException if the index is corrupt
	 * @throws IOException if there is a low-level IO error
	 */
	public void addDocuments(Iterable<? extends IDocument> docs, IAnalyzer analyzer) 
			throws IOException {
		updateDocuments(null, docs, analyzer);
	}
	
	/**
	 * Atomically deletes documents matching the provided
	 * delTerm and adds a block of documents with sequentially
	 * assigned document IDs, such that an external reader
	 * will see all or none of the documents. 
	 *
	 * See {@link #addDocuments(Iterable)}.
	 *
	 * @throws CorruptIndexException if the index is corrupt
	 * @throws IOException if there is a low-level IO error
	 */
	public void updateDocuments(ITerm delTerm, Iterable<? extends IDocument> docs) 
			throws IOException {
		updateDocuments(delTerm, docs, getAnalyzer());
	}
	
	/**
	 * Atomically deletes documents matching the provided
	 * delTerm and adds a block of documents, analyzed  using
	 * the provided analyzer, with sequentially
	 * assigned document IDs, such that an external reader
	 * will see all or none of the documents. 
	 *
	 * See {@link #addDocuments(Iterable)}.
	 *
	 * @throws CorruptIndexException if the index is corrupt
	 * @throws IOException if there is a low-level IO error
	 */
	public abstract void updateDocuments(ITerm delTerm, Iterable<? extends IDocument> docs, 
			IAnalyzer analyzer) throws IOException;
	
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
	public void addDocument(IDocument doc) throws IOException {
		addDocument(doc, getAnalyzer());
	}
	
	/**
	 * Adds a document to this index, using the provided analyzer instead of the
	 * value of {@link #getAnalyzer()}.
	 *
	 * <p>See {@link #addDocument(Iterable)} for details on
	 * index and IndexWriter state after an Exception, and
	 * flushing/merging temporary free space requirements.</p>
	 *
	 * <p><b>NOTE</b>: if this method hits an OutOfMemoryError
	 * you should immediately close the writer.  See <a
	 * href="#OOME">above</a> for details.</p>
	 *
	 * @throws CorruptIndexException if the index is corrupt
	 * @throws IOException if there is a low-level IO error
	 */
	public void addDocument(IDocument doc, IAnalyzer analyzer) throws IOException {
		updateDocument(null, doc, analyzer);
	}
	
	/**
	 * Updates a document by first deleting the document(s)
	 * containing <code>term</code> and then adding the new
	 * document.  The delete and then add are atomic as seen
	 * by a reader on the same index (flush may happen only after
	 * the add).
	 *
	 * <p><b>NOTE</b>: if this method hits an OutOfMemoryError
	 * you should immediately close the writer.  See <a
	 * href="#OOME">above</a> for details.</p>
	 *
	 * @param term the term to identify the document(s) to be
	 * deleted
	 * @param doc the document to be added
	 * @throws CorruptIndexException if the index is corrupt
	 * @throws IOException if there is a low-level IO error
	 */
	public void updateDocument(ITerm term, IDocument doc) throws IOException {
		updateDocument(term, doc, getAnalyzer());
	}
	
	/**
	 * Atomically deletes documents matching the provided
	 * delTerm and adds a block of documents, analyzed  using
	 * the provided analyzer, with sequentially
	 * assigned document IDs, such that an external reader
	 * will see all or none of the documents. 
	 *
	 * See {@link #addDocuments(Iterable)}.
	 *
	 * @throws CorruptIndexException if the index is corrupt
	 * @throws IOException if there is a low-level IO error
	 */
	public abstract void updateDocument(ITerm delTerm, IDocument doc, IAnalyzer analyzer) 
			throws IOException;
	
	/**
	 * Deletes the document(s) containing <code>term</code>.
	 *
	 * <p><b>NOTE</b>: if this method hits an OutOfMemoryError
	 * you should immediately close the writer.  See <a
	 * href="#OOME">above</a> for details.</p>
	 *
	 * @param term the term to identify the documents to be deleted
	 * @throws CorruptIndexException if the index is corrupt
	 * @throws IOException if there is a low-level IO error
	 */
	public void deleteDocuments(ITerm term) throws IOException {
		ensureOpen();
		try {
			getSegmentWriter().deleteTerms(term);
		} catch (OutOfMemoryError oom) {
			handleOOM(oom, "deleteDocuments(Term)");
		}
	}
	
	/**
	 * Deletes the document(s) containing any of the
	 * terms. All given deletes are applied and flushed atomically
	 * at the same time.
	 *
	 * <p><b>NOTE</b>: if this method hits an OutOfMemoryError
	 * you should immediately close the writer.  See <a
	 * href="#OOME">above</a> for details.</p>
	 *
	 * @param terms array of terms to identify the documents
	 * to be deleted
	 * @throws CorruptIndexException if the index is corrupt
	 * @throws IOException if there is a low-level IO error
	 */
	public void deleteDocuments(ITerm... terms) throws IOException {
		ensureOpen();
		try {
			getSegmentWriter().deleteTerms(terms);
		} catch (OutOfMemoryError oom) {
			handleOOM(oom, "deleteDocuments(Term..)");
		}
	}
	
	/**
	 * Deletes the document(s) matching the provided query.
	 *
	 * <p><b>NOTE</b>: if this method hits an OutOfMemoryError
	 * you should immediately close the writer.  See <a
	 * href="#OOME">above</a> for details.</p>
	 *
	 * @param query the query to identify the documents to be deleted
	 * @throws CorruptIndexException if the index is corrupt
	 * @throws IOException if there is a low-level IO error
	 */
	public void deleteDocuments(IQuery query) throws IOException {
		ensureOpen();
		try {
			getSegmentWriter().deleteQueries(query);
		} catch (OutOfMemoryError oom) {
			handleOOM(oom, "deleteDocuments(Query)");
		}
	}

	/**
	 * Deletes the document(s) matching any of the provided queries.
	 * All given deletes are applied and flushed atomically at the same time.
	 *
	 * <p><b>NOTE</b>: if this method hits an OutOfMemoryError
	 * you should immediately close the writer.  See <a
	 * href="#OOME">above</a> for details.</p>
	 *
	 * @param queries array of queries to identify the documents
	 * to be deleted
	 * @throws CorruptIndexException if the index is corrupt
	 * @throws IOException if there is a low-level IO error
	 */
	public void deleteDocuments(IQuery... queries) throws IOException {
		ensureOpen();
		try {
			getSegmentWriter().deleteQueries(queries);
		} catch (OutOfMemoryError oom) {
			handleOOM(oom, "deleteDocuments(Query..)");
		}
	}
	
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
	public void close() throws CorruptIndexException, IOException {
		close(true);
	}
	
	/**
	 * Closes the index with or without waiting for currently
	 * running merges to finish.  This is only meaningful when
	 * using a MergeScheduler that runs merges in background
	 * threads.
	 *
	 * <p><b>NOTE</b>: if this method hits an OutOfMemoryError
	 * you should immediately close the writer, again.  See <a
	 * href="#OOME">above</a> for details.</p>
	 *
	 * <p><b>NOTE</b>: it is dangerous to always call
	 * close(false), especially when IndexWriter is not open
	 * for very long, because this can result in "merge
	 * starvation" whereby long merges will never have a
	 * chance to finish.  This will cause too many segments in
	 * your index over time.</p>
	 *
	 * @param waitForMerges if true, this call will block
	 * until all merges complete; else, it will ask all
	 * running merges to abort, wait until those merges have
	 * finished (which should be at most a few seconds), and
	 * then return.
	 */
	public abstract void close(boolean waitForMerges) 
			throws CorruptIndexException, IOException;
	
	/**
	 * <p>Commits all pending changes (added & deleted
	 * documents, segment merges, added
	 * indexes, etc.) to the index, and syncs all referenced
	 * index files, such that a reader will see the changes
	 * and the index updates will survive an OS or machine
	 * crash or power loss.  Note that this does not wait for
	 * any running background merges to finish.  This may be a
	 * costly operation, so you should test the cost in your
	 * application and do it only when really necessary.</p>
	 *
	 * <p> Note that this operation calls Directory.sync on
	 * the index files.  That call should not return until the
	 * file contents & metadata are on stable storage.  For
	 * FSDirectory, this calls the OS's fsync.  But, beware:
	 * some hardware devices may in fact cache writes even
	 * during fsync, and return before the bits are actually
	 * on stable storage, to give the appearance of faster
	 * performance.  If you have such a device, and it does
	 * not have a battery backup (for example) then on power
	 * loss it may still lose data.  Indexdb cannot guarantee
	 * consistency on such devices.  </p>
	 *
	 * <p><b>NOTE</b>: if this method hits an OutOfMemoryError
	 * you should immediately close the writer.  See <a
	 * href="#OOME">above</a> for details.</p>
	 *
	 * @see #prepareCommit
	 * @see #commit(Map)
	 */
	public final void commit() throws IOException {
		commit(null);
	}
	
	/** 
	 * Commits all changes to the index, specifying a
	 *  commitUserData Map (String -> String).  This just
	 *  calls {@link #prepareCommit(Map)} (if you didn't
	 *  already call it) and then {@link #commit}.
	 *
	 * <p><b>NOTE</b>: if this method hits an OutOfMemoryError
	 * you should immediately close the writer.  See <a
	 * href="#OOME">above</a> for details.</p>
	 */
	public final void commit(Map<String,String> commitUserData) throws IOException {
		ensureOpen();
		commitInternal(commitUserData);
	}
	
	protected abstract void commitInternal(Map<String,String> commitUserData) 
			throws CorruptIndexException, IOException;
	
	/** 
	 * Expert: prepare for commit.
	 *
	 * <p><b>NOTE</b>: if this method hits an OutOfMemoryError
	 * you should immediately close the writer.  See <a
	 * href="#OOME">above</a> for details.</p>
	 *
	 * @see #prepareCommit(Map) 
	 */
	public final void prepareCommit() throws IOException {
		ensureOpen();
		prepareCommit(null);
	}
	
	/** 
	 * <p>Expert: prepare for commit, specifying
	 *  commitUserData Map (String -> String).  This does the
	 *  first phase of 2-phase commit. This method does all
	 *  steps necessary to commit changes since this writer
	 *  was opened: flushes pending added and deleted docs,
	 *  syncs the index files, writes most of next segments_N
	 *  file.  After calling this you must call either {@link
	 *  #commit()} to finish the commit, or {@link
	 *  #rollback()} to revert the commit and undo all changes
	 *  done since the writer was opened.</p>
	 *
	 *  <p>You can also just call {@link #commit(Map)} directly
	 *  without prepareCommit first in which case that method
	 *  will internally call prepareCommit.
	 *
	 *  <p><b>NOTE</b>: if this method hits an OutOfMemoryError
	 *  you should immediately close the writer.  See <a
	 *  href="#OOME">above</a> for details.</p>
	 *
	 *  @param commitUserData Opaque Map (String->String)
	 *  that's recorded into the segments file in the index,
	 *  and retrievable by {@link
	 *  IndexCommit#getUserData}.  Note that when
	 *  IndexWriter commits itself during {@link #close}, the
	 *  commitUserData is unchanged (just carried over from
	 *  the prior commit).  If this is null then the previous
	 *  commitUserData is kept.  Also, the commitUserData will
	 *  only "stick" if there are actually changes in the
	 *  index to commit.
	 */
	public abstract void prepareCommit(Map<String,String> commitUserData)
			throws CorruptIndexException, IOException;
	
}
