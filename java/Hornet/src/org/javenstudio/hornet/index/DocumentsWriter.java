package org.javenstudio.hornet.index;

import java.io.IOException;

import org.javenstudio.common.indexdb.ISegmentCommitInfo;
import org.javenstudio.common.indexdb.index.BufferedDeletes;
import org.javenstudio.common.indexdb.index.DeleteQueue;
import org.javenstudio.common.indexdb.index.DocumentWriter;
import org.javenstudio.common.indexdb.index.FlushTicket;
import org.javenstudio.common.indexdb.index.FrozenDeletes;
import org.javenstudio.common.indexdb.index.IndexWriter;
import org.javenstudio.common.indexdb.index.SegmentWriter;
import org.javenstudio.common.indexdb.index.FlushedSegment;
import org.javenstudio.common.indexdb.index.DocumentWriterState;
import org.javenstudio.common.indexdb.index.consumer.DocConsumer;
import org.javenstudio.common.indexdb.index.field.FieldNumbers;
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
final class DocumentsWriter extends SegmentWriter {
	private static final Logger LOG = Logger.getLogger(DocumentsWriter.class);

	private final DocumentsWriterPerThreadPool mPerThreadPool;
	
	public DocumentsWriter(IndexWriter indexWriter, FieldNumbers fieldNumbers) 
			throws IOException { 
		super(indexWriter);
		mPerThreadPool = new DocumentsWriterPerThreadPool(this, fieldNumbers);
	}
	
	public final DocumentsWriterPerThreadPool getWriterPool() { 
		return mPerThreadPool; 
	}
	
	@Override
	protected boolean preUpdate() throws IOException {
	    ensureOpen();
	    boolean maybeMerge = false;
	    
	    if (getFlushControl().anyStalledWriters() || getFlushControl().getNumQueuedFlushes() > 0) {
	    	// Help out flushing any queued DWPTs so we can un-stall:
	    	if (LOG.isDebugEnabled()) 
	    		LOG.debug("has queued dwpt; will hijack this thread to flush pending segment(s)");
	    	
	    	do {
	    		// Try pick up pending threads here if possible
	    		DocumentWriter flushingDWPT;
	    		while ((flushingDWPT = getFlushControl().nextPendingFlush()) != null) {
	    			// Don't push the delete here since the update could fail!
	    			maybeMerge |= doFlush(flushingDWPT);
	    		}
	  
	    		if (LOG.isDebugEnabled()) {
	    			if (getFlushControl().anyStalledWriters()) 
	    				LOG.debug("WARNING DocumentsWriter has stalled threads; waiting");
	    		}
	        
	    		getFlushControl().waitIfStalled(); // block if stalled
	    	} while (getFlushControl().getNumQueuedFlushes() != 0); // still queued DWPTs try help flushing

	    	if (LOG.isDebugEnabled()) 
	    		LOG.debug("continue indexing after helping out flushing DocumentsWriter is healthy");
	    }
	    
	    return maybeMerge;
	}
	
	/** 
	 * Called if we hit an exception at a bad time (when
	 *  updating the index files) and must discard all
	 *  currently buffered docs.  This resets our state,
	 *  discarding any docs added since last flush. 
	 */
	@Override
	public synchronized void abort() { 
	    boolean success = false;
	    try {
	    	getDeleteQueue().clear();
	    	if (LOG.isDebugEnabled()) 
	    		LOG.debug("abort");

	    	final int limit = mPerThreadPool.getNumActiveWriters();
	    	for (int i = 0; i < limit; i++) {
	    		final DocumentWriterState perThread = mPerThreadPool.getWriterStateAt(i);
	    		perThread.lock();
	    		
	    		try {
	    			if (perThread.isActive()) { // we might be closed
	    				try {
	    					perThread.getDocumentWriter().abort();
	    				} finally {
	    					perThread.getDocumentWriter().checkAndResetHasAborted();
	    					getFlushControl().doOnAbort(perThread);
	    				}
	    			} else 
	    				assert mClosed;
	    		} finally {
	    			perThread.unlock();
	    		}
	    	}
	    	
	    	getFlushControl().abortPendingFlushes();
	    	getFlushControl().waitForFlush();
	    	
	    	success = true;
	    } finally {
	    	if (LOG.isDebugEnabled()) 
	    		LOG.debug("done abort; success=" + success);
	    }
	}
	
	/**
	 * FlushAllThreads is synced by IW fullFlushLock. Flushing all threads is a
	 * two stage operation; the caller must ensure (in try/finally) that finishFlush
	 * is called after this method, to release the flush lock in DWFlushControl
	 */
	public final boolean flushAllWriters() throws IOException {
	    if (LOG.isDebugEnabled()) 
	    	LOG.debug(Thread.currentThread().getName() + " startFullFlush");
	    
	    final DeleteQueue flushingDeleteQueue;
	    synchronized (this) {
	    	mPendingChangesInCurrentFullFlush = anyChanges();
	    	flushingDeleteQueue = mDeleteQueue;
	    	/** 
	    	 * Cutover to a new delete queue.  This must be synced on the flush control
	    	 * otherwise a new DWPT could sneak into the loop with an already flushing
	    	 * delete queue 
	    	 */
	    	getFlushControl().markForFullFlush(); // swaps the delQueue synced on FlushControl
	    	//assert setFlushingDeleteQueue(flushingDeleteQueue);
	    }
	    
	    boolean anythingFlushed = false;
	    try {
	    	DocumentWriter flushingDWPT;
	    	// Help out with flushing:
	    	while ((flushingDWPT = getFlushControl().nextPendingFlush()) != null) {
	    		anythingFlushed |= doFlush(flushingDWPT);
	    	}
	    	
	    	// If a concurrent flush is still in flight wait for it
	    	getFlushControl().waitForFlush();  
	    	// apply deletes if we did not flush any document
	    	if (!anythingFlushed && flushingDeleteQueue.anyChanges()) {
	    		if (LOG.isDebugEnabled()) 
	    			LOG.debug(Thread.currentThread().getName() + ": flush naked frozen global deletes");
	    		
	    		mTicketQueue.addDeletesAndPurge(this, flushingDeleteQueue);
	    	} else {
	    		mTicketQueue.forcePurge(this);
	    	}
	    	
	    	assert !flushingDeleteQueue.anyChanges() && !mTicketQueue.hasTickets();
	    } finally {
	    	//assert flushingDeleteQueue == currentFullFlushDelQueue;
	    }
	    
	    return anythingFlushed;
	}
	
	public final void finishFullFlush(boolean success) {
	    try {
	    	if (LOG.isDebugEnabled()) 
	    		LOG.debug(Thread.currentThread().getName() + " finishFullFlush success=" + success);
	        //assert setFlushingDeleteQueue(null);
	    	
	        if (success) {
	        	// Release the flush lock
	        	getFlushControl().finishFullFlush();
	        } else {
	        	getFlushControl().abortFullFlushes();
	        }
	    } finally {
	        mPendingChangesInCurrentFullFlush = false;
	    }
	}
	
	@Override
	public void finishFlush(FlushedSegment newSegment, FrozenDeletes bufferedDeletes)
			throws IOException {
	    // Finish the flushed segment and publish it to IndexWriter
	    if (newSegment == null) {
	    	assert bufferedDeletes != null;
	    	if (bufferedDeletes != null && bufferedDeletes.any()) {
	    		getIndexWriter().publishFrozenDeletes(bufferedDeletes);
	    		
	    		if (LOG.isDebugEnabled()) 
	    			LOG.debug("flush: push buffered deletes: " + bufferedDeletes);
	    	}
	    } else {
	    	publishFlushedSegment(newSegment, bufferedDeletes);  
	    }
	}
	
	private boolean doFlush(DocumentWriter flushingDWPT) throws IOException {
		boolean maybeMerge = false;
		
	    while (flushingDWPT != null) {
	    	maybeMerge = true;
	    	boolean success = false;
	    	FlushTicket ticket = null;
	    	
	    	try {
	    		/**
	    		 * Since with DWPT the flush process is concurrent and several DWPT
	    		 * could flush at the same time we must maintain the order of the
	    		 * flushes before we can apply the flushed segment and the frozen global
	    		 * deletes it is buffering. The reason for this is that the global
	    		 * deletes mark a certain point in time where we took a DWPT out of
	    		 * rotation and freeze the global deletes.
	    		 * 
	    		 * Example: A flush 'A' starts and freezes the global deletes, then
	    		 * flush 'B' starts and freezes all deletes occurred since 'A' has
	    		 * started. if 'B' finishes before 'A' we need to wait until 'A' is done
	    		 * otherwise the deletes frozen by 'B' are not applied to 'A' and we
	    		 * might miss to deletes documents in 'A'.
	    		 */
	    		try {
	    			// Each flush is assigned a ticket in the order they acquire the ticketQueue lock
	    			ticket = mTicketQueue.addFlushTicket(flushingDWPT);
	  
	    			// flush concurrently without locking
	    			final FlushedSegment newSegment = flushingDWPT.flush();
	    			mTicketQueue.addSegment(ticket, newSegment);
	    			
	    			// flush was successful once we reached this point - new seg. 
	    			// has been assigned to the ticket!
	    			success = true;
	    		} finally {
	    			if (!success && ticket != null) {
	    				// In the case of a failure make sure we are making progress and
	    				// apply all the deletes since the segment flush failed since the flush
	    				// ticket could hold global deletes see FlushTicket#canPublish()
	    				mTicketQueue.markTicketFailed(ticket);
	    			}
	    		}
	    		
	    		/**
	    		 * Now we are done and try to flush the ticket queue if the head of the
	    		 * queue has already finished the flush.
	    		 */
	    		mTicketQueue.tryPurge(this);
	    		
	    	} finally {
	    		mFlushControl.doAfterFlush(flushingDWPT);
	    		flushingDWPT.checkAndResetHasAborted();
	    		mIndexWriter.getFlushCount().incrementAndGet();
	    		mIndexWriter.doAfterFlush();
	    	}
	     
	    	flushingDWPT = mFlushControl.nextPendingFlush();
	    }
	
	    // If deletes alone are consuming > 1/2 our RAM
	    // buffer, force them all to apply now. This is to
	    // prevent too-frequent flushing of a long tail of
	    // tiny segments:
	    final double ramBufferSizeMB = mIndexWriter.getIndexParams().getRAMBufferSizeMB();
	    
	    if (ramBufferSizeMB != AdvancedIndexParams.DISABLE_AUTO_FLUSH &&
	    	getFlushControl().getDeleteBytesUsed() > (1024*1024*ramBufferSizeMB/2)) {
	    	
	    	if (LOG.isDebugEnabled()) {
	    		LOG.debug("force apply deletes bytesUsed=" 
	    				+ getFlushControl().getDeleteBytesUsed() + " vs ramBuffer=" 
	    				+ (1024*1024*ramBufferSizeMB));
	    	}
	    	
	    	applyAllDeletes(mDeleteQueue);
	    }

	    return maybeMerge;
	}

	@Override
	protected boolean postUpdate(DocumentWriter flushingDWPT, 
			boolean maybeMerge) throws IOException {
	    if (getFlushControl().doApplyAllDeletes()) 
	    	applyAllDeletes(getDeleteQueue());
	    
	    if (flushingDWPT != null) {
	    	maybeMerge |= doFlush(flushingDWPT);
	    	
	    } else {
	    	final DocumentWriter nextPendingFlush = getFlushControl().nextPendingFlush();
	    	if (nextPendingFlush != null) 
	    		maybeMerge |= doFlush(nextPendingFlush);
	    }

	    return maybeMerge;
	}
	  
	/**
	 * Publishes the flushed segment, segment private deletes (if any) and its
	 * associated global delete (if present) to IndexWriter.  The actual
	 * publishing operation is synced on IW -> BDS so that the {@link SegmentInfo}'s
	 * delete generation is always GlobalPacket_deleteGeneration + 1
	 */
	private void publishFlushedSegment(FlushedSegment newSegment, FrozenDeletes globalPacket)
			throws IOException {
	    assert newSegment != null;
	    assert newSegment.getCommitInfo() != null;
	    
	    final ISegmentCommitInfo segInfo = mIndexWriter.prepareFlushedSegment(newSegment);
	    final BufferedDeletes deletes = newSegment.getSegmentDeletes();
	    
	    if (LOG.isDebugEnabled()) {
	    	LOG.debug(Thread.currentThread().getName() 
	    			+ ": publishFlushedSegment seg-private deletes=" + deletes);  
	    }
	    
	    FrozenBufferedDeletes packet = null;
	    if (deletes != null && deletes.any()) {
	    	// Segment private delete
	    	packet = new FrozenBufferedDeletes(mIndexWriter.getContext(), deletes, true);
	    	
	    	if (LOG.isDebugEnabled()) 
	    		LOG.debug("flush: push buffered seg private deletes: " + packet);
	    }

	    // now publish!
	    mIndexWriter.publishFlushedSegment(segInfo, packet, globalPacket);
	}
	
}
