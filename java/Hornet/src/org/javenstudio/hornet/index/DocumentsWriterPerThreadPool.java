package org.javenstudio.hornet.index;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.javenstudio.common.indexdb.index.DocumentWriter;
import org.javenstudio.common.indexdb.index.DocumentWriterState;
import org.javenstudio.common.indexdb.index.IndexParams;
import org.javenstudio.common.indexdb.index.WriterPool;
import org.javenstudio.common.indexdb.index.field.FieldInfosBuilder;
import org.javenstudio.common.indexdb.index.field.FieldNumbers;

/**
 * {@link DocumentsWriterPerThreadPool} controls {@link DocumentWriterState} instances
 * and their thread assignments during indexing. Each {@link DocumentWriterState} holds
 * a reference to a {@link DocumentsWriterPerThread} that is once a
 * {@link DocumentWriterState} is obtained from the pool exclusively used for indexing a
 * single document by the obtaining thread. Each indexing thread must obtain
 * such a {@link DocumentWriterState} to make progress. Depending on the
 * {@link DocumentsWriterPerThreadPool} implementation {@link DocumentWriterState}
 * assignments might differ from document to document.
 * <p>
 * Once a {@link DocumentsWriterPerThread} is selected for flush the thread pool
 * is reusing the flushing {@link DocumentsWriterPerThread}s ThreadState with a
 * new {@link DocumentsWriterPerThread} instance.
 * </p>
 * 
 * A {@link DocumentsWriterPerThreadPool} implementation that tries to assign an
 * indexing thread to the same {@link DocumentWriterState} each time the thread tries to
 * obtain a {@link DocumentWriterState}. Once a new {@link DocumentWriterState} is created it is
 * associated with the creating thread. Subsequently, if the threads associated
 * {@link DocumentWriterState} is not in use it will be associated with the requesting
 * thread. Otherwise, if the {@link DocumentWriterState} is used by another thread
 * {@link ThreadAffinityDocumentsWriterThreadPool} tries to find the currently
 * minimal contended {@link DocumentWriterState}.
 * 
 */
final class DocumentsWriterPerThreadPool extends WriterPool {

	private Map<Thread, DocumentWriterState> mThreadBindings = 
			new ConcurrentHashMap<Thread, DocumentWriterState>();
	
	private final DocumentsWriter mDocumentsWriter;
	
	private final DocumentWriterState[] mThreadStates;
	private volatile int mNumThreadStatesActive = 0;
	
	public DocumentsWriterPerThreadPool(DocumentsWriter documentsWriter, FieldNumbers globalFieldMap) { 
		this(documentsWriter, globalFieldMap, IndexParams.DEFAULT_MAX_THREAD_STATES);
	}
	
	/**
	 * Creates a new {@link DocumentsWriterPerThreadPool} with 
	 *  a given maximum of {@link DocumentWriterState}s.
	 */
	public DocumentsWriterPerThreadPool(DocumentsWriter documentsWriter, 
			FieldNumbers globalFieldMap, int maxNumThreadStates) {
		super(globalFieldMap);
		
		if (maxNumThreadStates < 1) 
			throw new IllegalArgumentException("maxNumThreadStates must be >= 1 but was: " + maxNumThreadStates);
		
		mDocumentsWriter = documentsWriter; // thread pool is bound to DW
		mThreadStates = new DocumentWriterState[maxNumThreadStates];
		mNumThreadStatesActive = 0;
		
		for (int i = 0; i < mThreadStates.length; i++) {
			final FieldInfosBuilder infos = new FieldInfosBuilder(globalFieldMap);
			mThreadStates[i] = new DocumentWriterState(createDocumentWriter(infos));
		}
	}

	@Override
	protected DocumentWriter createDocumentWriter(FieldInfosBuilder fieldInfos) { 
		return new DocumentsWriterPerThread(
				mDocumentsWriter, fieldInfos, mDocumentsWriter.getIndexingChain());
	}
	
	/**
	 * Returns the max number of {@link DocumentWriterState} instances available in this
	 * {@link DocumentsWriterPerThreadPool}
	 */
	public int getMaxNumWriters() {
		return mThreadStates.length;
	}
  
	/**
	 * Returns the active number of {@link DocumentWriterState} instances.
	 */
	@Override
	public int getNumActiveWriters() {
		return mNumThreadStatesActive;
	}

	/**
	 * Returns a new {@link DocumentWriterState} iff any new state is available otherwise
	 * <code>null</code>.
	 * <p>
	 * NOTE: the returned {@link DocumentWriterState} is already locked iff non-
	 * <code>null</code>.
	 * 
	 * @return a new {@link DocumentWriterState} iff any new state is available otherwise
	 *         <code>null</code>
	 */
	private synchronized DocumentWriterState newThreadState() {
		if (mNumThreadStatesActive < mThreadStates.length) {
			final DocumentWriterState threadState = mThreadStates[mNumThreadStatesActive];
			threadState.lock(); // lock so nobody else will get this ThreadState
			
			boolean unlock = true;
			try {
				if (threadState.isActive()) {
					// unreleased thread states are deactivated during DW#close()
					mNumThreadStatesActive ++; // increment will publish the ThreadState
					assert threadState.getDocumentWriter() != null;
					
					threadState.getDocumentWriter().initialize();
					unlock = false;
					
					return threadState;
				}
				
				// unlock since the threadstate is not active anymore - we are closed!
				assert assertUnreleasedThreadStatesInactive();
				return null;
				
			} finally {
				if (unlock) {
					// in any case make sure we unlock if we fail 
					threadState.unlock();
				}
			}
		}
		
		return null;
	}
  
	private synchronized boolean assertUnreleasedThreadStatesInactive() {
		for (int i = mNumThreadStatesActive; i < mThreadStates.length; i++) {
			assert mThreadStates[i].tryLock() : "unreleased threadstate should not be locked";
			try {
				assert !mThreadStates[i].isActive() : "expected unreleased thread state to be inactive";
			} finally {
				mThreadStates[i].unlock();
			}
		}
		return true;
	}
  
	/**
	 * Deactivate all unreleased threadstates 
	 */
	@Override
	public synchronized void deactivateUnreleasedStates() {
		for (int i = mNumThreadStatesActive; i < mThreadStates.length; i++) {
			final DocumentWriterState threadState = mThreadStates[i];
			threadState.lock();
			try {
				threadState.resetWriter(null);
			} finally {
				threadState.unlock();
			}
		}
	}
  
	// you cannot subclass this without being in o.a.l.index package anyway, so
	// the class is already pkg-private... fix me: see LUCENE-4013
	@Override
	public DocumentWriterState getAndLock() { 
		Thread requestingThread = Thread.currentThread();
		DocumentWriterState threadState = mThreadBindings.get(requestingThread);
		if (threadState != null && threadState.tryLock()) 
			return threadState;
		
		/** 
		 * TODO -- another thread could lock the minThreadState we just got while 
     	 * we should somehow prevent this. 
    	 * Find the state that has minimum number of threads waiting
    	 */
		DocumentWriterState minThreadState = getMinContendedWriterState();
		
		if (minThreadState == null || minThreadState.hasQueuedThreads()) {
			// state is already locked if non-null
			final DocumentWriterState newState = newThreadState(); 
			if (newState != null) {
				assert newState.isHeldByCurrentThread();
				mThreadBindings.put(requestingThread, newState);
				return newState;
				
			} else if (minThreadState == null) {
				/**
				 * no new threadState available we just take the minContented one
				 * This must return a valid thread state since we accessed the 
				 * synced context in newThreadState() above.
				 */
				minThreadState = getMinContendedWriterState();
			}
		}
		
		assert minThreadState != null: "ThreadState is null";
    
		minThreadState.lock();
		return minThreadState;
	}

	/**
	 * Returns the <i>i</i>th active {@link DocumentWriterState} where <i>i</i> is the
	 * given ord.
	 * 
	 * @param ord
	 *          the ordinal of the {@link DocumentWriterState}
	 * @return the <i>i</i>th active {@link DocumentWriterState} where <i>i</i> is the
	 *         given ord.
	 */
	@Override
	public DocumentWriterState getWriterStateAt(int ord) {
		assert ord < mNumThreadStatesActive;
		return mThreadStates[ord];
	}

	/**
	 * Returns the ThreadState with the minimum estimated number of threads
	 * waiting to acquire its lock or <code>null</code> if no {@link DocumentWriterState}
	 * is yet visible to the calling thread.
	 */
	public DocumentWriterState getMinContendedWriterState() {
		DocumentWriterState minThreadState = null;
		final int limit = mNumThreadStatesActive;
		for (int i = 0; i < limit; i++) {
			final DocumentWriterState state = mThreadStates[i];
			if (minThreadState == null || state.getQueueLength() < minThreadState.getQueueLength()) 
				minThreadState = state;
		}
		return minThreadState;
	}
  
	/**
	 * Returns the number of currently deactivated {@link DocumentWriterState} instances.
	 * A deactivated {@link DocumentWriterState} should not be used for indexing anymore.
	 * 
	 * @return the number of currently deactivated {@link DocumentWriterState} instances.
	 */
	public int getNumDeactivatedWriters() {
		int count = 0;
		for (int i = 0; i < mThreadStates.length; i++) {
			final DocumentWriterState threadState = mThreadStates[i];
			threadState.lock();
			try {
				if (!threadState.isActive()) 
					count ++;
			} finally {
				threadState.unlock();
			}
		}
		return count;
	}
	
}
