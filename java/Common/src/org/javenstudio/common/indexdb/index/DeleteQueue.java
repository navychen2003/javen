package org.javenstudio.common.indexdb.index;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.ReentrantLock;

import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ITerm;

/**
 * {@link DeleteQueue} is a non-blocking linked pending deletes
 * queue. In contrast to other queue implementation we only maintain the
 * tail of the queue. A delete queue is always used in a context of a set of
 * DWPTs and a global delete pool. Each of the DWPT and the global pool need to
 * maintain their 'own' head of the queue (as a DeleteSlice instance per DWPT).
 * The difference between the DWPT and the global pool is that the DWPT starts
 * maintaining a head once it has added its first document since for its segments
 * private deletes only the deletes after that document are relevant. The global
 * pool instead starts maintaining the head once this instance is created by
 * taking the sentinel instance as its initial head.
 * <p>
 * Since each {@link DeleteSlice} maintains its own head and the list is only
 * single linked the garbage collector takes care of pruning the list for us.
 * All nodes in the list that are still relevant should be either directly or
 * indirectly referenced by one of the DWPT's private {@link DeleteSlice} or by
 * the global {@link BufferedDeletes} slice.
 * <p>
 * Each DWPT as well as the global delete pool maintain their private
 * DeleteSlice instance. In the DWPT case updating a slice is equivalent to
 * atomically finishing the document. The slice update guarantees a "happens
 * before" relationship to all other updates in the same indexing session. When a
 * DWPT updates a document it:
 * 
 * <ol>
 * <li>consumes a document and finishes its processing</li>
 * <li>updates its private {@link DeleteSlice} either by calling
 * {@link #updateSlice(DeleteSlice)} or {@link #add(Term, DeleteSlice)} (if the
 * document has a delTerm)</li>
 * <li>applies all deletes in the slice to its private {@link BufferedDeletes}
 * and resets it</li>
 * <li>increments its internal document id</li>
 * </ol>
 * 
 * The DWPT also doesn't apply its current documents delete term until it has
 * updated its delete slice which ensures the consistency of the update. If the
 * update fails before the DeleteSlice could have been updated the deleteTerm
 * will also not be added to its private deletes neither to the global deletes.
 * 
 */
public abstract class DeleteQueue {
	private volatile DeleteNode<?> mTail;
	  
	@SuppressWarnings("rawtypes")
	private static final AtomicReferenceFieldUpdater<DeleteQueue, DeleteNode> mTailUpdater = 
		AtomicReferenceFieldUpdater.newUpdater(DeleteQueue.class, DeleteNode.class, "mTail");

	private final DeleteSlice mGlobalSlice;
	private final BufferedDeletes mGlobalBufferedDeletes;
	/** only acquired to update the global deletes */
	private final ReentrantLock mGlobalBufferLock = new ReentrantLock();

	private final long mGeneration;
  
	public DeleteQueue() {
		this(0);
	}
  
	public DeleteQueue(long generation) {
		this(new BufferedDeletes(), generation);
	}

	public DeleteQueue(BufferedDeletes globalBufferedDeletes, long generation) {
		mGlobalBufferedDeletes = globalBufferedDeletes;
		mGeneration = generation;
		/**
		 * we use a sentinel instance as our initial tail. No slice will ever try to
		 * apply this tail since the head is always omitted.
		 */
		mTail = new DeleteNode<Object>(null); // sentinel
		mGlobalSlice = new DeleteSlice(mTail);
	}

	public void addDelete(IQuery... queries) {
		add(new DeleteNode.QueryArrayDeleteNode(queries));
		tryApplyGlobalSlice();
	}

	public void addDelete(ITerm... terms) {
		add(new DeleteNode.TermArrayDeleteNode(terms));
		tryApplyGlobalSlice();
	}

	/**
	 * invariant for document update
	 */
	public void add(ITerm term, DeleteSlice slice) {
		final DeleteNode.TermDeleteNode termNode = new DeleteNode.TermDeleteNode(term);
		add(termNode);
		
		/**
		 * this is an update request where the term is the updated documents
		 * delTerm. in that case we need to guarantee that this insert is atomic
		 * with regards to the given delete slice. This means if two threads try to
		 * update the same document with in turn the same delTerm one of them must
		 * win. By taking the node we have created for our del term as the new tail
		 * it is guaranteed that if another thread adds the same right after us we
		 * will apply this delete next time we update our slice and one of the two
		 * competing updates wins!
		 */
		slice.setSliceTail(termNode);
		assert slice.getSliceHead() != slice.getSliceTail() : "slice head and tail must differ after add";
		
		tryApplyGlobalSlice(); // TODO doing this each time is not necessary maybe
		// we can do it just every n times or so?
	}

	public void add(DeleteNode<?> item) {
		/**
		 * this non-blocking / 'wait-free' linked list add was inspired by 
		 * Harmony's ConcurrentLinkedQueue Implementation.
		 */
		while (true) {
			final DeleteNode<?> currentTail = mTail;
			final DeleteNode<?> tailNext = currentTail.next();
			
			if (mTail == currentTail) {
				if (tailNext != null) {
					/*
					 * we are in intermediate state here. the tails next pointer has been
					 * advanced but the tail itself might not be updated yet. help to
					 * advance the tail and try again updating it.
					 */
					mTailUpdater.compareAndSet(this, currentTail, tailNext); // can fail
					
				} else {
					/**
					 * we are in quiescent state and can try to insert the item to the
					 * current tail if we fail to insert we just retry the operation since
					 * somebody else has already added its item
					 */
					if (currentTail.casNext(null, item)) {
						/**
						 * now that we are done we need to advance the tail while another
						 * thread could have advanced it already so we can ignore the return
						 * type of this CAS call
						 */
						mTailUpdater.compareAndSet(this, currentTail, item);
						return;
					}
				}
			}
		}
	}

	public boolean anyChanges() {
		mGlobalBufferLock.lock();
		try {
			/**
			 * check if all items in the global slice were applied 
			 * and if the global slice is up-to-date
			 * and if globalBufferedDeletes has changes
			 */
			return mGlobalBufferedDeletes.any() || !mGlobalSlice.isEmpty() || 
					mGlobalSlice.getSliceTail() != mTail || mTail.next() != null;
			
		} finally {
			mGlobalBufferLock.unlock();
		}
	}

	public void tryApplyGlobalSlice() {
		if (mGlobalBufferLock.tryLock()) {
			/**
			 * The global buffer must be locked but we don't need to update them if
			 * there is an update going on right now. It is sufficient to apply the
			 * deletes that have been added after the current in-flight global slices
			 * tail the next time we can get the lock!
			 */
			try {
				if (updateSlice(mGlobalSlice)) 
					mGlobalSlice.apply(mGlobalBufferedDeletes, BufferedDeletes.MAX_INT);
        
			} finally {
				mGlobalBufferLock.unlock();
			}
		}
	}

	public FrozenDeletes freezeGlobalBuffer(DeleteSlice callerSlice) {
		mGlobalBufferLock.lock();
		
		/**
		 * Here we freeze the global buffer so we need to lock it, apply all
		 * deletes in the queue and reset the global slice to let the GC prune the
		 * queue.
		 */
		final DeleteNode<?> currentTail = mTail; // take the current tail make this local any
		
		// Changes after this call are applied later
		// and not relevant here
		if (callerSlice != null) {
			// Update the callers slices so we are on the same page
			callerSlice.setSliceTail(currentTail);
		}
		
		try {
			if (mGlobalSlice.getSliceTail() != currentTail) {
				mGlobalSlice.setSliceTail(currentTail);
				mGlobalSlice.apply(mGlobalBufferedDeletes, BufferedDeletes.MAX_INT);
			}

			final FrozenDeletes packet = newFrozenDeletes(mGlobalBufferedDeletes, false);
			mGlobalBufferedDeletes.clear();
			
			return packet;
			
		} finally {
			mGlobalBufferLock.unlock();
		}
	}

	protected abstract FrozenDeletes newFrozenDeletes(BufferedDeletes deletes, 
			boolean isSegmentPrivate);
	
	public DeleteSlice newSlice() {
		return new DeleteSlice(mTail);
	}

	public boolean updateSlice(DeleteSlice slice) {
		if (slice.getSliceTail() != mTail) { // If we are the same just
			slice.setSliceTail(mTail);
			return true;
		}
		return false;
	}

	public int numGlobalTermDeletes() {
		return mGlobalBufferedDeletes.getNumTermDeletes().get();
	}

	public void clear() {
		mGlobalBufferLock.lock();
		try {
			final DeleteNode<?> currentTail = mTail;
			
			mGlobalSlice.setSliceHead(currentTail);
			mGlobalSlice.setSliceTail(currentTail);
			
			mGlobalBufferedDeletes.clear();
		} finally {
			mGlobalBufferLock.unlock();
		}
	}
  
	private boolean forceApplyGlobalSlice() {
		mGlobalBufferLock.lock();
		final DeleteNode<?> currentTail = mTail;
		try {
			if (mGlobalSlice.getSliceTail() != currentTail) {
				mGlobalSlice.setSliceTail(currentTail);
				mGlobalSlice.apply(mGlobalBufferedDeletes, BufferedDeletes.MAX_INT);
			}
			
			return mGlobalBufferedDeletes.any();
		} finally {
			mGlobalBufferLock.unlock();
		}
	}	

	public int getBufferedDeleteTermsSize() {
		mGlobalBufferLock.lock();
		try {
			forceApplyGlobalSlice();
			return mGlobalBufferedDeletes.getTerms().size();
		} finally {
			mGlobalBufferLock.unlock();
		}
	}
  
	public long getBytesUsed() {
		return mGlobalBufferedDeletes.getBytesUsed().get();
	}

	public long getGeneration() { 
		return mGeneration;
	}
	
	@Override
	public String toString() {
		return "DeleteQueue{generation=" + mGeneration + "}";
	}
  
}
