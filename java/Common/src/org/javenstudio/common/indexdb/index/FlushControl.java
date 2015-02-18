package org.javenstudio.common.indexdb.index;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.javenstudio.common.indexdb.ThreadInterruptedException;
import org.javenstudio.common.util.Logger;

public class FlushControl {
	private static final Logger LOG = Logger.getLogger(FlushControl.class);

	protected static class BlockedFlush {
		public final DocumentWriter mDwpt;
		public final long mBytes;
		
		public BlockedFlush(DocumentWriter dwpt, long bytes) {
			mDwpt = dwpt;
			mBytes = bytes;
		}
	}
	
	private final IdentityHashMap<DocumentWriter, Long> mFlushingWriters = 
			new IdentityHashMap<DocumentWriter, Long>();

	private final List<DocumentWriter> mFullFlushBuffer = 
			new ArrayList<DocumentWriter>();
	
	private final Queue<DocumentWriter> mFlushQueue = 
			new LinkedList<DocumentWriter>();
	
	// only for safety reasons if a DWPT is close to the RAM limit
	private final Queue<BlockedFlush> mBlockedFlushes = 
			new LinkedList<BlockedFlush>();
	
	private final AtomicBoolean mFlushDeletes = new AtomicBoolean(false);
	private final long mHardMaxBytesPerDWPT;
	
	private long mActiveBytes = 0;
	private long mFlushBytes = 0;
	private volatile int mNumPending = 0;
	private boolean mFullFlush = false;
	private boolean mClosed = false;
	
	private final SegmentWriter mSegmentWriter;
	private final StallControl mStallControl;
	
	public FlushControl(SegmentWriter writer) {
		mSegmentWriter = writer;
		mStallControl = new StallControl();
		mHardMaxBytesPerDWPT = writer.getIndexWriter().getIndexParams()
				.getRAMPerThreadHardLimitMB() * 1024 * 1024;
	}

	public final SegmentWriter getSegmentWriter() { 
		return mSegmentWriter;
	}
	
	public synchronized long getActiveBytes() { return mActiveBytes; }
	public synchronized long getFlushBytes() { return mFlushBytes; }
	public synchronized long getNetBytes() { return mFlushBytes + mActiveBytes; }
	
	public DeleteQueue newDeleteQueue(long generation) { 
		IndexWriter writer = getSegmentWriter().getIndexWriter();
		return writer.getIndexParams().newDeleteQueue(writer, generation);
	}
	
	public DocumentWriterState obtainAndLock() {
		final DocumentWriterState state = getSegmentWriter().getWriterPool().getAndLock();
		
		boolean success = false;
		try {
			if (state.isActive() && state.getDocumentWriter().getDeleteQueue() != 
					getSegmentWriter().getDeleteQueue()) {
				// There is a flush-all in process and this DWPT is
				// now stale -- enroll it for flush and try for
				// another DWPT:
				addFlushableState(state);
			}
			success = true;
			
			// simply return the ThreadState even in a flush all case sine we already hold the lock
			return state;
			
		} finally {
			if (!success) // make sure we unlock if this fails
				state.unlock();
		}
	}
	
	public synchronized DocumentWriter doAfterDocument(DocumentWriterState state, 
			boolean isUpdate) {
		try {
			commitWriterBytes(state);
			
			if (!state.isFlushPending()) {
				if (isUpdate) 
					getSegmentWriter().getFlushPolicy().onUpdate(this, state);
				else 
					getSegmentWriter().getFlushPolicy().onInsert(this, state);
				
				if (!state.isFlushPending() && state.getBytesUsed() > mHardMaxBytesPerDWPT) {
					// Safety check to prevent a single DWPT exceeding its RAM limit. This
					// is super important since we can not address more than 2048 MB per DWPT
					setFlushPending(state);
				}
			}
			
			DocumentWriter flushingDWPT = null;
			if (mFullFlush) {
				if (state.isFlushPending()) {
					checkoutAndBlock(state);
					flushingDWPT = nextPendingFlush();
				}
			} else {
				flushingDWPT = tryCheckoutForFlush(state);
			}
			
			return flushingDWPT;
			
		} finally {
			updateStallState();
		}
	}

	public synchronized void doAfterFlush(DocumentWriter dwpt) {
		assert mFlushingWriters.containsKey(dwpt);
		
		try {
			Long bytes = mFlushingWriters.remove(dwpt);
			mFlushBytes -= bytes.longValue();
			
			getSegmentWriter().getWriterPool().recycle(dwpt);
			
		} finally {
			try {
				updateStallState();
			} finally {
				notifyAll();
			}
		}
	}
	
	public synchronized void waitForFlush() {
		while (mFlushingWriters.size() != 0) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				throw new ThreadInterruptedException(e);
			}
		}
	}
	
	/**
	 * Sets flush pending state on the given {@link DocumentWriterState}. The
	 * {@link DocumentWriterState} must have indexed at least on Document and must not be
	 * already pending.
	 */
	public synchronized void setFlushPending(DocumentWriterState state) {
		assert !state.isFlushPending();
		
		if (state.getDocumentWriter().getNumDocsInRAM() > 0) {
			state.setFlushPending(true); // write access synced
			
			final long bytes = state.getBytesUsed();
			mFlushBytes += bytes;
			mActiveBytes -= bytes;
			mNumPending ++; // write access synced
		}
		
		// don't assert on numDocs since we could hit an abort excp. 
		// while selecting that dwpt for flushing
	}
	
	public synchronized void doOnAbort(DocumentWriterState state) {
		try {
			if (state.isFlushPending()) 
				mFlushBytes -= state.getBytesUsed();
			else 
				mActiveBytes -= state.getBytesUsed();
			
			// Take it out of the loop this DWPT is stale
			getSegmentWriter().getWriterPool().replaceForFlush(state, mClosed);
			
		} finally {
			updateStallState();
		}
	}
	
	public synchronized void doOnDelete() {
		// pass null this is a global delete no update
		getSegmentWriter().getFlushPolicy().onDelete(this, null);
	}
	
	public DocumentWriter nextPendingFlush() {
		int numPending;
		boolean fullFlush;
		
		synchronized (this) {
			final DocumentWriter poll;
			if ((poll = mFlushQueue.poll()) != null) {
				updateStallState();
				return poll;
			}
			
			fullFlush = mFullFlush;
			numPending = mNumPending;
		}
		
		if (numPending > 0 && !fullFlush) { // don't check if we are doing a full flush
			final int limit = getSegmentWriter().getWriterPool().getNumActiveWriters();
			for (int i = 0; i < limit && numPending > 0; i++) {
				
				final DocumentWriterState next = getSegmentWriter().getWriterPool().getWriterStateAt(i);
				if (next.isFlushPending()) {
					
					final DocumentWriter dwpt = tryCheckoutForFlush(next);
					if (dwpt != null) 
						return dwpt;
				}
			}
		}
		
		return null;
	}
	
	public void markForFullFlush() {
		final DeleteQueue flushingQueue;
		synchronized (this) {
			assert !mFullFlush : "called DWFC#markForFullFlush() while full flush is still running";
			assert mFullFlushBuffer.isEmpty() : "full flush buffer should be empty: "+ mFullFlushBuffer;
			
			mFullFlush = true;
			flushingQueue = getSegmentWriter().getDeleteQueue();
			
			// Set a new delete queue - all subsequent DWPT will use this queue until
			// we do another full flush
			DeleteQueue newQueue = newDeleteQueue(flushingQueue.getGeneration()+1);
			getSegmentWriter().setDeleteQueue(newQueue);
		}
		
		final int limit = getSegmentWriter().getWriterPool().getNumActiveWriters();
		for (int i = 0; i < limit; i++) {
			final DocumentWriterState next = getSegmentWriter().getWriterPool().getWriterStateAt(i);
			next.lock();
			try {
				if (!next.isActive()) 
					continue; 
				
				assert next.getDocumentWriter().getDeleteQueue() == flushingQueue || 
						next.getDocumentWriter().getDeleteQueue() == getSegmentWriter().getDeleteQueue() : 
						" flushingQueue: " + flushingQueue
						+ " currentqueue: " + getSegmentWriter().getDeleteQueue()
						+ " perThread queue: " + next.getDocumentWriter().getDeleteQueue()
						+ " numDocsInRam: " + next.getDocumentWriter().getNumDocsInRAM();
				
				if (next.getDocumentWriter().getDeleteQueue() != flushingQueue) {
					// this one is already a new DWPT
					continue;
				}
				
				addFlushableState(next);
				
			} finally {
				next.unlock();
			}
		}
		
		synchronized (this) {
			/** 
			 * make sure we move all DWPT that are where concurrently marked as
			 * pending and moved to blocked are moved over to the flushQueue. There is
			 * a chance that this happens since we marking DWPT for full flush without
			 * blocking indexing.
			 */
			pruneBlockedQueue(flushingQueue);
			
			mFlushQueue.addAll(mFullFlushBuffer);
			mFullFlushBuffer.clear();
			
			updateStallState();
		}
	}
	
	public synchronized void finishFullFlush() {
		assert mFullFlush;
		assert mFlushQueue.isEmpty();
		assert mFlushingWriters.isEmpty();
		
		try {
			if (!mBlockedFlushes.isEmpty()) {
				pruneBlockedQueue(getSegmentWriter().getDeleteQueue());
				
				assert mBlockedFlushes.isEmpty();
			}
		} finally {
			mFullFlush = false;
			updateStallState();
		}
	}
	
	public synchronized void abortFullFlushes() {
		try {
			abortPendingFlushes();
		} finally {
			mFullFlush = false;
		}
	}
	
	public synchronized void abortPendingFlushes() {
		try {
			for (DocumentWriter dwpt : mFlushQueue) {
				try {
					dwpt.abort();
					doAfterFlush(dwpt);
				} catch (Throwable ex) {
					// ignore - keep on aborting the flush queue
				}
			}
			
			for (BlockedFlush blockedFlush : mBlockedFlushes) {
				try {
					mFlushingWriters.put(blockedFlush.mDwpt, Long.valueOf(blockedFlush.mBytes));
					blockedFlush.mDwpt.abort();
					doAfterFlush(blockedFlush.mDwpt);
				} catch (Throwable ex) {
					// ignore - keep on aborting the blocked queue
				}
			}
		} finally {
			mFlushQueue.clear();
			mBlockedFlushes.clear();
			updateStallState();
		}
	}
	
	public synchronized void setClosed() {
		// set by DW to signal that we should not release new DWPT after close
		if (!mClosed) {
			mClosed = true;
			getSegmentWriter().getWriterPool().deactivateUnreleasedStates();
		}
	}
	
	/**
	 * Returns an iterator that provides access to all currently active {@link DocumentWriterState}s 
	 */
	public Iterator<DocumentWriterState> allActiveWriterStates() {
		return getPerWritersIterator(getSegmentWriter().getWriterPool().getNumActiveWriters());
	}
  
	private Iterator<DocumentWriterState> getPerWritersIterator(final int upto) {
		return new Iterator<DocumentWriterState>() {
				int i = 0;
				@Override
				public boolean hasNext() {
					return i < upto;
				}
				@Override
				public DocumentWriterState next() {
					return getSegmentWriter().getWriterPool().getWriterStateAt(i++);
				}
				@Override
				public void remove() {
					throw new UnsupportedOperationException("remove() not supported.");
				}
			};
	}
	
	/**
	 * Returns <code>true</code> if a full flush is currently running
	 */
	public synchronized boolean isFullFlush() {
		return mFullFlush;
	}

	/**
	 * Returns the number of flushes that are already checked out but not yet
	 * actively flushing
	 */
	public synchronized int getNumQueuedFlushes() {
		return mFlushQueue.size();
	}

	/**
	 * Returns the number of flushes that are checked out but not yet available
	 * for flushing. This only applies during a full flush if a DWPT needs
	 * flushing but must not be flushed until the full flush has finished.
	 */
	public synchronized int getNumBlockedFlushes() {
		return mBlockedFlushes.size();
	}

	/**
	 * This method will block if too many DWPT are currently flushing and no
	 * checked out DWPT are available
	 */
	public void waitIfStalled() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("waitIfStalled: numFlushesPending: " + mFlushQueue.size()
					+ " netBytes: " + getNetBytes() + " flushBytes: " + getFlushBytes()
					+ " fullFlush: " + mFullFlush);
		}
		mStallControl.waitIfStalled();
	}

	/**
	 * Returns <code>true</code> if stalled
	 */
	public boolean anyStalledWriters() {
		return mStallControl.anyStalledWriters();
	}
	
	/**
	 * Returns the number of delete terms in the global pool
	 */
	public int getNumGlobalTermDeletes() {
		return getSegmentWriter().getDeleteQueue().numGlobalTermDeletes() + 
				getSegmentWriter().getIndexWriter().getDeletesStream().getNumTerms();
	}
  
	public long getDeleteBytesUsed() {
		return getSegmentWriter().getDeleteQueue().getBytesUsed() + 
				getSegmentWriter().getIndexWriter().getDeletesStream().getBytesUsed();
	}

	public synchronized int getNumFlushingDWPT() {
		return mFlushingWriters.size();
	}
  
	public boolean doApplyAllDeletes() {	
		return mFlushDeletes.getAndSet(false);
	}

	public void setApplyAllDeletes() {	
		mFlushDeletes.set(true);
	}
  
	public int getNumActiveWriters() {
		return getSegmentWriter().getWriterPool().getNumActiveWriters();
	}
	
	/**
	 * Prunes the blockedQueue by removing all DWPT that are associated with the given flush queue. 
	 */
	private void pruneBlockedQueue(final DeleteQueue flushingQueue) {
		Iterator<BlockedFlush> iterator = mBlockedFlushes.iterator();
		while (iterator.hasNext()) {
			BlockedFlush blockedFlush = iterator.next();
			
			if (blockedFlush.mDwpt.getDeleteQueue() == flushingQueue) {
				iterator.remove();
				assert !mFlushingWriters.containsKey(blockedFlush.mDwpt) : "DWPT is already flushing";
				
				// Record the flushing DWPT to reduce flushBytes in doAfterFlush
				mFlushingWriters.put(blockedFlush.mDwpt, Long.valueOf(blockedFlush.mBytes));
				// don't decr pending here - its already done when DWPT is blocked
				mFlushQueue.add(blockedFlush.mDwpt);
			}
		}
	}
	
	private void addFlushableState(DocumentWriterState state) {
		if (LOG.isDebugEnabled()) 
			LOG.debug("addFlushableState " + state.getDocumentWriter());
		
		final DocumentWriter dwpt = state.getDocumentWriter();
		
		assert state.isHeldByCurrentThread();
		assert state.isActive();
		assert mFullFlush;
		assert dwpt.getDeleteQueue() != getSegmentWriter().getDeleteQueue();
		
		if (dwpt.getNumDocsInRAM() > 0) {
			synchronized(this) {
				if (!state.isFlushPending()) 
					setFlushPending(state);
				
				final DocumentWriter flushingDWPT = tryCheckOutForFlushInternal(state);
				
				assert flushingDWPT != null : "DWPT must never be null here since we hold the lock and it holds documents";
				assert dwpt == flushingDWPT : "flushControl returned different DWPT";
				
				mFullFlushBuffer.add(flushingDWPT);
			}
		} else {
			if (mClosed) {
				// make this state inactive
				getSegmentWriter().getWriterPool().deactivateWriterState(state); 
			} else 
				getSegmentWriter().getWriterPool().reinitWriterState(state);
		}
	}
	
	private void commitWriterBytes(DocumentWriterState state) {
		final long delta = state.getDocumentWriter().getBytesUsed() - state.getBytesUsed();
		state.increaseBytesUsed(delta);
		
		/**
		 * We need to differentiate here if we are pending since setFlushPending
		 * moves the perThread memory to the flushBytes and we could be set to
		 * pending during a delete
		 */
		if (state.isFlushPending()) 
			mFlushBytes += delta;
		else 
			mActiveBytes += delta;
	}
	
	private void checkoutAndBlock(DocumentWriterState state) {
		state.lock();
		try {
			assert state.isFlushPending() : "can not block non-pending threadstate";
			assert mFullFlush : "can not block if fullFlush == false";
			
			final long bytes = state.getBytesUsed();
			final DocumentWriter dwpt = getSegmentWriter().getWriterPool()
					.replaceForFlush(state, mClosed);
			
			mNumPending --;
			mBlockedFlushes.add(new BlockedFlush(dwpt, bytes));
			
		} finally {
			state.unlock();
		}
	}
	
	private synchronized DocumentWriter tryCheckoutForFlush(DocumentWriterState state) {
		return state.isFlushPending() ? tryCheckOutForFlushInternal(state) : null;
	}
  
	private DocumentWriter tryCheckOutForFlushInternal(DocumentWriterState state) {
		assert Thread.holdsLock(this);
		assert state.isFlushPending();
		
		try {
			// We are pending so all memory is already moved to flushBytes
			if (state.tryLock()) {
				try {
					if (state.isActive()) {
						assert state.isHeldByCurrentThread();
						final long bytes = state.getBytesUsed(); // do that before replace!
						
						final DocumentWriter dwpt = getSegmentWriter().getWriterPool()
								.replaceForFlush(state, mClosed);
						assert !mFlushingWriters.containsKey(dwpt) : "DWPT is already flushing";
						
						// Record the flushing DWPT to reduce flushBytes in doAfterFlush
						mFlushingWriters.put(dwpt, Long.valueOf(bytes));
						mNumPending --; // write access synced
						
						return dwpt;
					}
				} finally {
					state.unlock();
				}
			}
			
			return null;
			
		} finally {
			updateStallState();
		}
	}
	
	private final void updateStallState() {
		assert Thread.holdsLock(this);
		final long limit = getStallLimitBytes();
		
		/**
		 * we block indexing threads if net byte grows due to slow flushes
		 * yet, for small ram buffers and large documents we can easily
		 * reach the limit without any ongoing flushes. we need to ensure
		 * that we don't stall/block if an ongoing or pending flush can
		 * not free up enough memory to release the stall lock.
		 */
		final boolean stall = ((mActiveBytes + mFlushBytes) > limit)  &&
				(mActiveBytes < limit) && !mClosed;
		
		mStallControl.updateStalled(stall);
	}
	
	private long getStallLimitBytes() {
		final double maxRamMB = getSegmentWriter().getIndexWriter().getIndexParams().getRAMBufferSizeMB();
		return maxRamMB != IndexParams.DISABLE_AUTO_FLUSH ? 
				(long)(2 * (maxRamMB * 1024 * 1024)) : Long.MAX_VALUE;
	}
	
	@Override
	public String toString() {
		return "FlushControl{activeBytes=" + mActiveBytes + ", flushBytes=" + mFlushBytes + "}";
	}
	
}
