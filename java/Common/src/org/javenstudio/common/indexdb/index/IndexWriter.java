package org.javenstudio.common.indexdb.index;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.javenstudio.common.indexdb.AlreadyClosedException;
import org.javenstudio.common.indexdb.Constants;
import org.javenstudio.common.indexdb.CorruptIndexException;
import org.javenstudio.common.indexdb.IAnalyzer;
import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IDocument;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.IPayloadProcessor;
import org.javenstudio.common.indexdb.ISegmentCommitInfo;
import org.javenstudio.common.indexdb.ISegmentInfo;
import org.javenstudio.common.indexdb.ISegmentInfos;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.Lock;
import org.javenstudio.common.indexdb.LockObtainFailedException;
import org.javenstudio.common.indexdb.ThreadInterruptedException;
import org.javenstudio.common.indexdb.codec.IIndexFormat;
import org.javenstudio.common.indexdb.index.field.FieldNumbers;
import org.javenstudio.common.indexdb.index.segment.SegmentInfos;
import org.javenstudio.common.indexdb.store.CompoundFileDirectory;
import org.javenstudio.common.indexdb.util.IOUtils;
import org.javenstudio.common.indexdb.util.JvmUtil;
import org.javenstudio.common.util.Logger;

public abstract class IndexWriter extends IndexWriterBase {
	private static final Logger LOG = Logger.getLogger(IndexWriter.class);
	
	protected final IndexParams mIndexParams;
	protected final IndexContext mContext;
	protected final IDirectory mDirectory;
	protected final IAnalyzer mAnalyzer;
	
	protected final MergeControl mMergeControl;
	
	protected final SegmentInfos mSegmentInfos; // the segments
	protected final DeletesStream mDeletesStream;
	protected IndexFileDeleter mDeleter;
	
	// list of segmentInfo we will fallback to if the commit fails
	protected List<ISegmentCommitInfo> mRollbackSegments;
	
	// increments every time a change is completed
	protected volatile long mChangeCount = 0; 
	// last changeCount that was committed
	protected long mLastCommitChangeCount = 0; 
	
	protected volatile boolean mClosed = false;
	protected volatile boolean mClosing = false;
	protected volatile boolean mHitOOM = false;
	
	protected Lock mWriteLock;
	
	// This is a "write once" variable (like the organic dye
	// on a DVD-R that may or may not be heated by a laser and
	// then cooled to permanently record the event): it's
	// false, until getReader() is called for the first time,
	// at which point it's switched to true and never changes
	// back to false.  Once this is true, we hold open and
	// reuse SegmentReader instances internally for applying
	// deletes, doing merges, and reopening near real-time
	// readers.
	protected volatile boolean mPoolReaders;
	
	protected final ReaderPool mReaderPool;
	
	// Ensures only one flush() is actually flushing segments
	// at a time:
	protected final Object mFullFlushLock = new Object();
	
	protected final AtomicInteger mFlushCount = new AtomicInteger();
	protected final AtomicInteger mFlushDeletesCount = new AtomicInteger();
	
	// Used only by commit and prepareCommit, below; lock
	// order is commitLock -> IW
	protected final Object mCommitLock = new Object();
	
	// set when a commit is pending (after prepareCommit() & before commit())
	protected volatile SegmentInfos mPendingCommit;
	protected volatile long mPendingCommitChangeCount;
	
	protected Collection<String> mFilesToCommit;
	
	// The PayloadProcessorProvider to use when segments are merged
	protected IPayloadProcessor.Provider mPayloadProcessorProvider;
	
	/**
	 * Constructs a new IndexWriter per the settings given in <code>conf</code>.
	 * Note that the passed in {@link IndexWriterConfig} is
	 * privately cloned; if you need to make subsequent "live"
	 * changes to the configuration use {@link #getConfig}.
	 * <p>
	 * 
	 * @param d
	 *          the index directory. The index is either created or appended
	 *          according <code>conf.getOpenMode()</code>.
	 * @param conf
	 *          the configuration settings according to which IndexWriter should
	 *          be initialized.
	 * @throws CorruptIndexException
	 *           if the index is corrupt
	 * @throws LockObtainFailedException
	 *           if another writer has this index open (<code>write.lock</code>
	 *           could not be obtained)
	 * @throws IOException
	 *           if the directory cannot be read/written to, or if it does not
	 *           exist and <code>conf.getOpenMode()</code> is
	 *           <code>OpenMode.APPEND</code> or if there is any other low-level
	 *           IO error
	 */
	public IndexWriter(IDirectory dir, IndexParams params) throws IOException { 
		mIndexParams = params;
		mContext = (IndexContext)params.getContext();
		mDirectory = dir;
		mAnalyzer = params.getAnalyzer();
		mPoolReaders = params.getReaderPooling();
		mMergeControl = params.newMergeControl(this);
		mDeletesStream = params.newDeletesStream(this);
		mReaderPool = new ReaderPool(this);
		mSegmentInfos = new SegmentInfos(mDirectory);
		
		onCreated(params);
	}
	
	public final IndexParams getIndexParams() { return mIndexParams; }
	public final IndexContext getContext() { return mContext; }
	public final IDirectory getDirectory() { return mDirectory; }
	public final IAnalyzer getAnalyzer() { return mAnalyzer; }
	
	public final MergeControl getMergeControl() { return mMergeControl; }
	public final ReaderPool getReaderPool() { return mReaderPool; }
	public final DeletesStream getDeletesStream() { return mDeletesStream; }
	public final IndexFileDeleter getDeleter() { return mDeleter; }
	
	public final AtomicInteger getFlushCount() { return mFlushCount; }
	public final SegmentInfos getSegmentInfos() { return mSegmentInfos; }
	public final boolean isPoolReaders() { return mPoolReaders; }
	
	public final IIndexFormat getIndexFormat() { 
		return getContext().getIndexFormat(); 
	}
	
	public final IPayloadProcessor.Provider getPayloadProcessorProvider() { 
		return mPayloadProcessorProvider; 
	}
	
	protected abstract void onInitSegmentWriter() throws IOException;
	
	@Override
	protected void handleOOM(OutOfMemoryError oom, String location) {
		if (LOG.isDebugEnabled())
			LOG.debug("hit OutOfMemoryError inside " + location);
		
		mHitOOM = true;
	    throw oom;
	}
	
	protected boolean isHitOOM() { return mHitOOM; }
	
	/**
	 * Used internally to throw an {@link
	 * AlreadyClosedException} if this IndexWriter has been
	 * closed.
	 * @throws AlreadyClosedException if this IndexWriter is closed
	 */
	protected final void ensureOpen(boolean includePendingClose) throws AlreadyClosedException {
		if (mClosed || (includePendingClose && mClosing)) 
			throw new AlreadyClosedException("this IndexWriter is closed");
	}

	@Override
	protected final void ensureOpen() throws AlreadyClosedException {
		ensureOpen(true);
	}
	
	public boolean isCurrent(ISegmentInfos infos) { 
		ensureOpen();
		return infos.getVersion() == getSegmentInfos().getVersion() && 
				!getSegmentWriter().anyChanges() && !getDeletesStream().any();
	}
	
	public synchronized boolean isClosed() { 
		return mClosed;
	}
	
	public synchronized boolean isClosing() { 
		return mClosing;
	}
	
	private final synchronized void onIndexChanged() { 
		mChangeCount ++;
		mSegmentInfos.increaseVersion();
	}
	
	/**
	 * Called whenever the SegmentInfos has been updated and
	 * the index files referenced exist (correctly) in the
	 * index directory.
	 */
	protected final synchronized void checkpoint() throws IOException {
		if (LOG.isDebugEnabled())
			LOG.debug("checkpoint");
		
		//mChangeCount ++;
		//mSegmentInfos.increaseVersion();
		onIndexChanged();
		mDeleter.checkpoint(mSegmentInfos, false);
	}
	
	/**
	 * Returns <code>true</code> iff the index in the named directory is
	 * currently locked.
	 * @param directory the directory to check for a lock
	 * @throws IOException if there is a low-level IO error
	 */
	public static boolean isLocked(IDirectory directory) throws IOException {
		return directory.makeLock(IndexFileNames.WRITE_LOCK_NAME).isLocked();
	}

	/**
	 * Forcibly unlocks the index in the named directory.
	 * <P>
	 * Caution: this should only be used by failure recovery code,
	 * when it is known that no other process nor thread is in fact
	 * currently accessing this index.
	 */
	public static void unlock(IDirectory directory) throws IOException {
		directory.makeLock(IndexFileNames.WRITE_LOCK_NAME).release();
	}
	
	protected void onCreated(IndexParams params) throws IOException {
		getMergeControl().getMergePolicy().setIndexWriter(this);
		mWriteLock = mDirectory.makeLock(IndexFileNames.WRITE_LOCK_NAME);
		
		if (!mWriteLock.obtain(params.getWriteLockTimeout())) // obtain write lock
			throw new LockObtainFailedException("Index locked for write: " + mWriteLock);
		
		boolean success = false;
	    try {
	    	OpenMode mode = params.getOpenMode();
	    	boolean create;
	    	if (mode == OpenMode.CREATE) {
	    		create = true;
	    	} else if (mode == OpenMode.APPEND) {
	    		create = false;
	    	} else {
	    		// CREATE_OR_APPEND - create only if an index does not exist
	    		create = !getIndexFormat().existsIndex(mDirectory);
	    	}

	    	// If index is too old, reading the segments will throw
	    	// IndexFormatTooOldException.
	    	//mSegmentInfos = new SegmentInfos(mDirectory);

	    	if (create) {
	    		// Try to read first.  This is to allow create
	    		// against an index that's currently open for
	    		// searching.  In this case we write the next
	    		// segments_N file with no segments:
	    		try {
	    			getIndexFormat().getSegmentInfosFormat().readSegmentInfos(mDirectory, mSegmentInfos);
	    			mSegmentInfos.clear();
	    		} catch (IOException e) {
	    			// Likely this means it's a fresh directory
	    		}

	    		// Record that we have a change (zero out all
	    		// segments) pending:
	    		onIndexChanged();
	    		
	    	} else {
	    		getIndexFormat().getSegmentInfosFormat().readSegmentInfos(mDirectory, mSegmentInfos);
	        
	            IndexCommit commit = params.getIndexCommit();
	            if (commit != null) {
	            	// Swap out all segments, but, keep metadata in
	            	// SegmentInfos, like version & generation, to
	            	// preserve write-once.  This is important if
	            	// readers are open against the future commit
	            	// points.
	            	if (commit.getDirectory() != mDirectory)
	            		throw new IllegalArgumentException("IndexCommit's directory doesn't match my directory");
	            	
	            	SegmentInfos oldInfos = new SegmentInfos(mDirectory);
	            	getIndexFormat().getSegmentInfosFormat().readSegmentInfos(
	            			mDirectory, oldInfos, commit.getSegmentsFileName());
	            	
	            	mSegmentInfos.replace(oldInfos);
	            	onIndexChanged();
	            	
	            	if (LOG.isDebugEnabled())
	            		LOG.debug("init: loaded commit \"" + commit.getSegmentsFileName() + "\"");
	            }
	    	}
	    	
	    	mRollbackSegments = mSegmentInfos.createBackupSegmentInfos();
	    	
	    	// start with previous field numbers, but new FieldInfos
	    	onInitSegmentWriter();
	    	
	        // Default deleter (for backwards compatibility) is
	        // KeepOnlyLastCommitDeleter:
	        synchronized(this) {
	        	mDeleter = new IndexFileDeleter(
	        			params.getIndexDeletionPolicy(), mSegmentInfos, getIndexFormat());
	        }
	        
	        if (mDeleter.isStartingCommitDeleted()) {
	        	// Deletion policy deleted the "head" commit point.
	        	// We have to mark ourself as changed so that if we
	        	// are closed w/o any further changes we write a new
	        	// segments_N file.
	        	onIndexChanged();
	        }
	    	
	        if (LOG.isDebugEnabled())
	        	LOG.debug("inited: create=" + create + " segmentInfos=" + mSegmentInfos);
	        
	    	success = true;
	    } finally { 
	        if (!success) {
	            try {
	            	mWriteLock.release();
	            } catch (Throwable t) {
	            	// don't mask the original exception
	            }
	            mWriteLock = null;
	        }
	    }
	}
	
  	protected IFieldInfos getFieldInfos(ISegmentInfo info) throws IOException {
	    IDirectory cfsDir = null;
	    try {
	    	if (info.getUseCompoundFile()) {
	    		cfsDir = new CompoundFileDirectory(getContext(), info.getDirectory(),
	    				getContext().getCompoundFileName(info.getName()), 
	    				false);
	    	} else {
	    		cfsDir = info.getDirectory();
	    	}
	    	
	    	return getIndexFormat().getFieldInfosFormat().createReader(
	    			info.getDirectory(), info.getName()).readFieldInfos();
	    	
	    } finally {
	    	if (info.getUseCompoundFile() && cfsDir != null) 
	    		cfsDir.close();
	    }
	}
	
	/**
     * Loads or returns the already loaded the global field number map for this {@link SegmentInfos}.
     * If this {@link SegmentInfos} has no global field number map the returned instance is empty
     */
    protected FieldNumbers getFieldNumberMap() throws IOException {
    	final FieldNumbers map  = new FieldNumbers();

    	ISegmentCommitInfo biggest = null;
    	for (ISegmentCommitInfo info : mSegmentInfos) {
    		if (biggest == null || (info.getSegmentInfo().getDocCount()-info.getDelCount()) > 
    		   (biggest.getSegmentInfo().getDocCount()-biggest.getDelCount())) {
    			biggest = info;
    		}
    	}

    	if (biggest != null) {
    		for (IFieldInfo fi : getFieldInfos(biggest.getSegmentInfo())) {
    			map.addOrGet(fi.getName(), fi.getNumber());
    		}
    	}

    	// TODO: we could also pull DV type of each field here,
    	// and use that to make sure new segment(s) don't change
    	// the type...
    	return map;
    }
	
    @Override
	public final String newSegmentName() {
		// Cannot synchronize on IndexWriter because that causes
		// deadlock
		synchronized(mSegmentInfos) {
			final int count = mSegmentInfos.getCounter();
			mSegmentInfos.increaseCounter(1);
			
			String name = "_" + Integer.toString(count, Character.MAX_RADIX);
			
			if (LOG.isDebugEnabled())
				LOG.debug("newSegmentName: name=" + name);
			
			// Important to increment changeCount so that the
			// segmentInfos is written on close.  Otherwise we
			// could close, re-open and re-return the same segment
			// name that was previously returned which can cause
			// problems at least with ConcurrentMergeScheduler.
			//mChangeCount ++;
			//mSegmentInfos.increaseVersion();
			onIndexChanged();
			
			return name;
		}
	}
    
	/**
	 * Obtain the number of deleted docs for a pooled reader.
	 * If the reader isn't being pooled, the segmentInfo's 
	 * delCount is returned.
	 */
	public int getNumDeletedDocs(ISegmentCommitInfo info) {
		ensureOpen(false);
		int delCount = info.getDelCount();

		final ReadersAndLiveDocs rld = getReaderPool().get(info, false);
		if (rld != null) 
			delCount += rld.getPendingDeleteCount();
		
		return delCount;
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
	@Override
	public void updateDocuments(ITerm delTerm, Iterable<? extends IDocument> docs, 
			IAnalyzer analyzer) throws IOException {
		ensureOpen();
		
		try {
			boolean success = false;
			boolean anySegmentFlushed = false;
			
			try {
				anySegmentFlushed = getSegmentWriter().updateDocuments(docs, analyzer, delTerm);
				success = true;
			} finally {
				if (!success) {
					if (LOG.isDebugEnabled())
						LOG.debug("hit exception updating document");
				}
			}
			
			if (anySegmentFlushed) 
				getMergeControl().maybeMerge();
			
		} catch (OutOfMemoryError oom) {
			handleOOM(oom, "updateDocuments");
		}
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
	@Override
	public void updateDocument(ITerm delTerm, IDocument doc, IAnalyzer analyzer) 
			throws IOException { 
		ensureOpen();
		
		try { 
			boolean success = false;
			boolean anySegmentFlushed = false;
			
			try { 
				anySegmentFlushed = getSegmentWriter().updateDocument(doc, analyzer, delTerm);
				success = true;
			} finally { 
				if (!success) {
					if (LOG.isDebugEnabled())
						LOG.debug("hit exception updating document");
				}
			}
			
			if (anySegmentFlushed) 
				getMergeControl().maybeMerge();
			
		} catch (OutOfMemoryError oom) {
			handleOOM(oom, "updateDocument");
	    }
	}
	
	/**
	 * Delete all documents in the index.
	 *
	 * <p>This method will drop all buffered documents and will
	 *    remove all segments from the index. This change will not be
	 *    visible until a {@link #commit()} has been called. This method
	 *    can be rolled back using {@link #rollback()}.</p>
	 *
	 * <p>NOTE: this method is much faster than using deleteDocuments( new MatchAllDocsQuery() ).</p>
	 *
	 * <p>NOTE: this method will forcefully abort all merges
	 *    in progress.  If other threads are running {@link
	 *    #forceMerge}, {@link #addIndexes(IndexReader[])} or
	 *    {@link #forceMergeDeletes} methods, they may receive
	 *    {@link MergePolicy.MergeAbortedException}s.
	 */
	public synchronized void deleteAll() throws IOException {
		ensureOpen();
		
		boolean success = false;
		try {

			// Abort any running merges
			getMergeControl().finishMerges(false);

			// Remove any buffered docs
			getSegmentWriter().abort();

			// Remove all segments
			getSegmentInfos().clear();

			// Ask deleter to locate unreferenced files & remove them:
			getDeleter().checkpoint(mSegmentInfos, false);
			getDeleter().refresh();

			// Don't bother saving any changes in our segmentInfos
			getReaderPool().dropAll(false);

			// Mark that the index has changed
			onIndexChanged();
			
			success = true;
		} catch (OutOfMemoryError oom) {
			handleOOM(oom, "deleteAll");
			
		} finally {
			if (!success) {
				if (LOG.isDebugEnabled())
					LOG.debug("hit exception during deleteAll");
			}
		}
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
	public final void close(boolean waitForMerges) throws CorruptIndexException, IOException {
		// Ensure that only one thread actually gets to do the
		// closing, and make sure no commit is also in progress:
		synchronized (mCommitLock) {
			if (shouldClose()) {
				// If any methods have hit OutOfMemoryError, then abort
				// on close, in case the internal state of IndexWriter
				// or DocumentsWriter is corrupt
				if (mHitOOM) {
					rollbackInternal();
				} else {
					closeInternal(waitForMerges, true);
				}
			}
		}
	}
	
	// Returns true if this thread should attempt to close, or
	// false if IndexWriter is now closed; else, waits until
	// another thread finishes closing
	protected final synchronized boolean shouldClose() {
		while (true) {
			if (!mClosed) {
				if (!mClosing) {
					mClosing = true;
					return true;
				} else {
					// Another thread is presently trying to close;
					// wait until it finishes one way (closes
					// successfully) or another (fails to close)
					doWait();
				}
			} else {
				return false;
			}
		}
	}
	
	protected synchronized void doWait() {
		// NOTE: the callers of this method should in theory
		// be able to do simply wait(), but, as a defense
		// against thread timing hazards where notifyAll()
		// fails to be called, we wait for at most 1 second
		// and then return so caller can check if wait
		// conditions are satisfied:
		try {
			wait(getIndexParams().getWriterWaitTimeout());
		} catch (InterruptedException ie) {
			throw new ThreadInterruptedException(ie);
		}
	}
	
	/**
	 * Close the <code>IndexWriter</code> without committing
	 * any changes that have occurred since the last commit
	 * (or since it was opened, if commit hasn't been called).
	 * This removes any temporary files that had been created,
	 * after which the state of the index will be the same as
	 * it was when commit() was last called or when this
	 * writer was first opened.  This also clears a previous
	 * call to {@link #prepareCommit}.
	 * @throws IOException if there is a low-level IO error
	 */
	public void rollback() throws IOException {
		ensureOpen();

		// Ensure that only one thread actually gets to do the
		// closing, and make sure no commit is also in progress:
		synchronized (mCommitLock) {
			if (shouldClose()) 
				rollbackInternal();
		}
	}
	
	protected abstract void rollbackInternal() throws IOException;
	
	protected abstract void closeInternal(boolean waitForMerges, boolean doFlush) 
			throws CorruptIndexException, IOException;
	
	/**
	 * Flush all in-memory buffered updates (adds and deletes)
	 * to the Directory.
	 * @param triggerMerge if true, we may merge segments (if
	 *  deletes or docs were flushed) if necessary
	 * @param applyAllDeletes whether pending deletes should also
	 */
	protected final void flush(boolean triggerMerge, boolean applyAllDeletes) 
			throws CorruptIndexException, IOException {
	    // NOTE: this method cannot be sync'd because
	    // maybeMerge() in turn calls mergeScheduler.merge which
	    // in turn can take a long time to run and we don't want
	    // to hold the lock for that.  In the case of
	    // ConcurrentMergeScheduler this can lead to deadlock
	    // when it stalls due to too many running merges.

	    // We can be called during close, when closing==true, so we must pass false to ensureOpen:
	    ensureOpen(false);
	    if (doFlush(applyAllDeletes) && triggerMerge) {
	    	getMergeControl().maybeMerge();
	    }
	}
	
	protected abstract boolean doFlush(boolean applyAllDeletes) 
			throws CorruptIndexException, IOException;
	
	protected final synchronized void maybeApplyDeletes(boolean applyAllDeletes) throws IOException {
		if (applyAllDeletes) {
			if (LOG.isDebugEnabled())
				LOG.debug("apply all deletes during flush");
			
			applyAllDeletes();
			
		} else if (LOG.isDebugEnabled()) { 
			LOG.debug("don't apply deletes now delTermCount=" + mDeletesStream.getNumTerms() + 
					" bytesUsed=" + mDeletesStream.getBytesUsed());
		}
	}
	
	/** 
	 * Expert: remove any index files that are no longer
	 *  used.
	 *
	 *  <p> IndexWriter normally deletes unused files itself,
	 *  during indexing.  However, on Windows, which disallows
	 *  deletion of open files, if there is a reader open on
	 *  the index then those files cannot be deleted.  This is
	 *  fine, because IndexWriter will periodically retry
	 *  the deletion.</p>
	 *
	 *  <p> However, IndexWriter doesn't try that often: only
	 *  on open, close, flushing a new segment, and finishing
	 *  a merge.  If you don't do any of these actions with your
	 *  IndexWriter, you'll see the unused files linger.  If
   	 *  that's a problem, call this method to delete them
   	 *  (once you've closed the open readers that were
   	 *  preventing their deletion). 
   	 *  
   	 *  <p> In addition, you can call this method to delete 
   	 *  unreferenced index commits. This might be useful if you 
   	 *  are using an {@link IndexDeletionPolicy} which holds
   	 *  onto index commits until some criteria are met, but those
   	 *  commits are no longer needed. Otherwise, those commits will
   	 *  be deleted the next time commit() is called.
   	 */
	public synchronized void deleteUnusedFiles() throws IOException {
		ensureOpen(false);
		mDeleter.deletePendingFiles();
		mDeleter.revisitPolicy();
	}

	// Called by DirectoryReader.doClose
	public synchronized void deletePendingFiles() throws IOException {
		mDeleter.deletePendingFiles();
	}
	
	protected synchronized String toSegmentString() throws IOException {
		return toSegmentString(mSegmentInfos);
	}

	protected synchronized String toSegmentString(Iterable<ISegmentCommitInfo> infos) 
			throws IOException {
		final StringBuilder buffer = new StringBuilder();
		for (final ISegmentCommitInfo info : infos) {
			if (buffer.length() > 0) 
				buffer.append(' ');
			buffer.append(toSegmentString(info));
		}
		return buffer.toString();
	}

	protected synchronized String toSegmentString(ISegmentCommitInfo info) throws IOException {
		return info.toString(info.getSegmentInfo().getDirectory(), 
				getNumDeletedDocs(info) - info.getDelCount());
	}
	
	// For infoStream output
	protected synchronized ISegmentInfos toLiveInfos(ISegmentInfos sis) {
		final SegmentInfos newSIS = new SegmentInfos(sis.getDirectory());
		final Map<ISegmentCommitInfo, ISegmentCommitInfo> liveSIS = 
				new HashMap<ISegmentCommitInfo, ISegmentCommitInfo>();        
		
		for (ISegmentCommitInfo info : mSegmentInfos) {
			liveSIS.put(info, info);
		}
		
		for (ISegmentCommitInfo info : sis) {
			ISegmentCommitInfo liveInfo = liveSIS.get(info);
			if (liveInfo != null) 
				info = liveInfo;
			
			newSIS.add(info);
		}

		return newSIS;
	}
	
	protected static void setDiagnostics(ISegmentInfo info, String source) {
		setDiagnostics(info, source, null);
	}

	protected static void setDiagnostics(ISegmentInfo info, String source, Map<String,String> details) {
		Map<String,String> diagnostics = new HashMap<String,String>();
		diagnostics.put("source", source);
		diagnostics.put("indexdb.version", Constants.INDEXDB_MAIN_VERSION);
		diagnostics.put("os", JvmUtil.OS_NAME);
		diagnostics.put("os.arch", JvmUtil.OS_ARCH);
		diagnostics.put("os.version", JvmUtil.OS_VERSION);
		diagnostics.put("java.version", JvmUtil.JAVA_VERSION);
		diagnostics.put("java.vendor", JvmUtil.JAVA_VENDOR);
		if (details != null) 
			diagnostics.putAll(details);
		
		info.setDiagnostics(diagnostics);
	}
	
	/**
	 * NOTE: this method creates a compound file for all files returned by
	 * info.files(). While, generally, this may include separate norms and
	 * deletion files, this SegmentInfo must not reference such files when this
	 * method is called, because they are not allowed within a compound file.
	 */
	protected static Collection<String> createCompoundFile(IIndexContext context, IDirectory directory, 
			CheckAbort checkAbort, final ISegmentInfo info) throws IOException { 
		final String fileName = context.getCompoundFileName(info.getName());
		if (LOG.isDebugEnabled())
			LOG.debug("create compound file " + fileName);
		
	    // Now merge all added files
	    Collection<String> files = info.getFileNames();
	    CompoundFileDirectory cfsDir = new CompoundFileDirectory(
	    		context, directory, fileName, true);
	    
	    IOException prior = null;
	    try {
	    	for (String file : files) {
	    		long length = directory.copy(context, cfsDir, file, file);
	    		//long length2 = directory.getFileLength(file);
	    		checkAbort.work(length);
	    		
	    		if (LOG.isDebugEnabled()) {
	    			LOG.debug("copied file: " + file + " (" + length 
	    					+ " bytes) to compound file: " + fileName);
	    		}
	    	}
	    } catch(IOException ex) {
	    	prior = ex;
	    } finally {
	    	IOUtils.closeWhileHandlingException(prior, cfsDir);
	    }
		
	    // Replace all previous files with the CFS/CFE files:
	    Set<String> siFiles = new HashSet<String>();
	    siFiles.add(fileName);
	    siFiles.add(IndexFileNames.getCompoundEntriesFileName(info.getName()));
	    info.setFileNames(siFiles);

	    return files;
	}
	
}
