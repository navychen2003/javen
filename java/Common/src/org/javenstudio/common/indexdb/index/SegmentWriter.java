package org.javenstudio.common.indexdb.index;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.javenstudio.common.indexdb.AlreadyClosedException;
import org.javenstudio.common.indexdb.IAnalyzer;
import org.javenstudio.common.indexdb.IDocument;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.index.consumer.DocConsumer;
import org.javenstudio.common.util.Logger;

/**
 * This class accepts multiple added documents and directly
 * writes segment files.
 *
 * Each added document is passed to the {@link DocConsumer},
 * which in turn processes the document and interacts with
 * other consumers in the indexing chain.  Certain
 * consumers, like {@link StoredFieldsConsumer} and {@link
 * TermVectorsConsumer}, digest a document and
 * immediately write bytes to the "doc store" files (ie,
 * they do not consume RAM per document, except while they
 * are processing the document).
 *
 * Other consumers, eg {@link FreqProxTermsWriter} and
 * {@link NormsConsumer}, buffer bytes in RAM and flush only
 * when a new segment is produced.

 * Once we have used our allowed RAM buffer, or the number
 * of added docs is large enough (in the case we are
 * flushing by doc count instead of RAM usage), we create a
 * real segment and flush it to the Directory.
 *
 * Threads:
 *
 * Multiple threads are allowed into addDocument at once.
 * There is an initial synchronized call to getThreadState
 * which allocates a ThreadState for this thread.  The same
 * thread will get the same ThreadState over time (thread
 * affinity) so that if there are consistent patterns (for
 * example each thread is indexing a different content
 * source) then we make better use of RAM.  Then
 * processDocument is called on that ThreadState without
 * synchronization (most of the "heavy lifting" is in this
 * call).  Finally the synchronized "finishDocument" is
 * called to flush changes to the directory.
 *
 * When flush is called by IndexWriter we forcefully idle
 * all threads and flush only once they are all idle.  This
 * means you can call flush with a given thread even while
 * other threads are actively adding/deleting documents.
 *
 * Exceptions:
 *
 * Because this class directly updates in-memory posting
 * lists, and flushes stored fields and term vectors
 * directly to files in the directory, there are certain
 * limited times when an exception can corrupt this state.
 * For example, a disk full while flushing stored fields
 * leaves this file in a corrupt state.  Or, an OOM
 * exception while appending to the in-memory posting lists
 * can corrupt that posting list.  We call such exceptions
 * "aborting exceptions".  In these cases we must call
 * abort() to discard all docs added since the last flush.
 *
 * All other exceptions ("non-aborting exceptions") can
 * still partially update the index structures.  These
 * updates are consistent, but, they represent only a part
 * of the document seen up until the exception was hit.
 * When this happens, we immediately mark the document as
 * deleted so that the document is always atomically ("all
 * or none") added to the index.
 */
public abstract class SegmentWriter {
	private static final Logger LOG = Logger.getLogger(SegmentWriter.class);

	protected final IndexWriter mIndexWriter;
	protected final IndexingChain mIndexingChain;
	
	protected final AtomicInteger mNumDocsInRAM = new AtomicInteger(0);
	protected final FlushQueue mTicketQueue = new FlushQueue();
	
	// TODO: cut over to BytesRefHash in BufferedDeletes
	protected volatile DeleteQueue mDeleteQueue = null;
	
	protected final FlushControl mFlushControl;
	protected final FlushPolicy mFlushPolicy;
	
	/**
	 * we preserve changes during a full flush since IW might not checkout before
	 * we release all changes. NRT Readers otherwise suddenly return true from
	 * isCurrent while there are actually changes currently committed. See also
	 * #anyChanges() & #flushAllThreads
	 */
	protected volatile boolean mPendingChangesInCurrentFullFlush = false;
	
	protected volatile boolean mClosed = false;
	
	public SegmentWriter(IndexWriter indexWriter) throws IOException { 
		mIndexWriter = indexWriter;
		mIndexingChain = indexWriter.getIndexParams().getIndexingChain();
		mFlushControl = indexWriter.getIndexParams().newFlushControl(this);
		mFlushPolicy = indexWriter.getIndexParams().newFlushPolicy(this);
		mDeleteQueue = mFlushControl.newDeleteQueue(0);
	}
	
	public final IndexWriter getIndexWriter() { return mIndexWriter; }
	public final IndexingChain getIndexingChain() { return mIndexingChain; }
	public final DeleteQueue getDeleteQueue() { return mDeleteQueue; }
	public final FlushControl getFlushControl() { return mFlushControl; }
	public final FlushPolicy getFlushPolicy() { return mFlushPolicy; }
	
	public abstract WriterPool getWriterPool();
	
	public final void setDeleteQueue(DeleteQueue queue) { 
		mDeleteQueue = queue;
	}
	
	/** Returns how many docs are currently buffered in RAM. */
	public final int getNumDocs() {
		return mNumDocsInRAM.get();
	}

	public final void subtractFlushedNumDocs(int numFlushed) {
	    int oldValue = mNumDocsInRAM.get();
	    while (!mNumDocsInRAM.compareAndSet(oldValue, oldValue - numFlushed)) {
	    	oldValue = mNumDocsInRAM.get();
	    }
	}
	
	protected void ensureOpen() throws AlreadyClosedException {
	    if (mClosed) 
	    	throw new AlreadyClosedException("this IndexWriter is closed");
	}
	
	/**
	 * TODO: we could check w/ FreqProxTermsWriter: if the
	 * term doesn't exist, don't bother buffering into the
	 * per-DWPT map (but still must go into the global map)
	 */
	public synchronized void deleteTerms(final ITerm... terms) throws IOException {
	    final DeleteQueue deleteQueue = getDeleteQueue();
	    deleteQueue.addDelete(terms);
	    
	    getFlushControl().doOnDelete();
	    if (getFlushControl().doApplyAllDeletes()) 
	    	applyAllDeletes(deleteQueue);
	}
	
	public synchronized void deleteQueries(final IQuery... queries) throws IOException {
	    getDeleteQueue().addDelete(queries);
	    getFlushControl().doOnDelete();
	    if (getFlushControl().doApplyAllDeletes()) 
	    	applyAllDeletes(getDeleteQueue());
	}
	
	public boolean anyDeletions() {
		return getDeleteQueue().anyChanges();
	}
	
	public boolean anyChanges() {
	    if (LOG.isDebugEnabled()) {
	    	LOG.debug("anyChanges ? numDocsInRam=" + mNumDocsInRAM.get()
	    			+ " deletes=" + anyDeletions() + " hasTickets:"
	    			+ mTicketQueue.hasTickets() + " pendingChangesInFullFlush: "
	    			+ mPendingChangesInCurrentFullFlush);
	    }
	    
	    /**
	     * changes are either in a DWPT or in the deleteQueue.
	     * yet if we currently flush deletes and / or dwpt there
	     * could be a window where all changes are in the ticket queue
	     * before they are published to the IW. ie we need to check if the 
	     * ticket queue has any tickets.
	     */
	    return mNumDocsInRAM.get() != 0 || anyDeletions() || mTicketQueue.hasTickets() || 
	    		mPendingChangesInCurrentFullFlush;
	}
	
	protected void applyAllDeletes(DeleteQueue deleteQueue) throws IOException {
		if (deleteQueue != null && !getFlushControl().isFullFlush()) 
			mTicketQueue.addDeletesAndPurge(this, deleteQueue);
		
		getIndexWriter().applyAllDeletes();
		getIndexWriter().getFlushCount().incrementAndGet();
	}
	
	protected abstract boolean preUpdate() throws IOException;
	protected abstract boolean postUpdate(DocumentWriter flushingDWPT, 
			boolean maybeMerge) throws IOException;
	
	public final boolean updateDocuments(Iterable<? extends IDocument> docs, 
			IAnalyzer analyzer, ITerm delTerm) throws IOException { 
	    boolean maybeMerge = preUpdate();

	    final DocumentWriterState state = getFlushControl().obtainAndLock();
	    final DocumentWriter flushingDWPT;
	    
	    try {
	    	if (!state.isActive()) {
	    		ensureOpen();
	    		assert false: "writerState is not active but we are still open";
	    	}
	       
	    	final DocumentWriter dwpt = state.getDocumentWriter();
	    	try {
	    		final int docCount = dwpt.updateDocuments(docs, analyzer, delTerm);
	    		mNumDocsInRAM.addAndGet(docCount);
	    		
	    	} finally {
	    		if (dwpt.checkAndResetHasAborted()) 
	    			getFlushControl().doOnAbort(state);
	    	}
	    	
	    	final boolean isUpdate = delTerm != null;
	    	flushingDWPT = getFlushControl().doAfterDocument(state, isUpdate);
	    	
	    } finally {
	    	state.unlock();
	    }

	    return postUpdate(flushingDWPT, maybeMerge);
	}
	
	public final boolean updateDocument(IDocument doc, IAnalyzer analyzer, 
			ITerm delTerm) throws IOException { 
		boolean maybeMerge = preUpdate();
		
	    final DocumentWriterState state = getFlushControl().obtainAndLock();
	    final DocumentWriter flushingDWPT;
	    
	    try {
	    	if (!state.isActive()) {
	    		ensureOpen();
	    		assert false: "writerState is not active but we are still open";
	    	}
	       
	    	final DocumentWriter dwpt = state.getDocumentWriter();
	    	try {
	    		dwpt.updateDocument(doc, analyzer, delTerm); 
	    		mNumDocsInRAM.incrementAndGet();
	    		
	    	} finally {
	    		if (dwpt.checkAndResetHasAborted()) 
	    			getFlushControl().doOnAbort(state);
	    	}
	    	
	    	final boolean isUpdate = delTerm != null;
	    	flushingDWPT = getFlushControl().doAfterDocument(state, isUpdate);
	    	
	    } finally {
	    	state.unlock();
	    }

	    return postUpdate(flushingDWPT, maybeMerge);
	}
	
	public abstract void finishFlush(FlushedSegment newSegment, FrozenDeletes bufferedDeletes)
			throws IOException;
	
	public void setAborting() {}
	
	/** 
	 * Called if we hit an exception at a bad time (when
	 *  updating the index files) and must discard all
	 *  currently buffered docs.  This resets our state,
	 *  discarding any docs added since last flush. 
	 */
	public abstract void abort();
	
	public void close() throws IOException { 
	    mClosed = true;
	    getFlushControl().setClosed();
	}
	
}
