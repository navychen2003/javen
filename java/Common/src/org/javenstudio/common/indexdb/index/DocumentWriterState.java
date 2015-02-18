package org.javenstudio.common.indexdb.index;

import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link DocumentWriterState} references and guards a
 * {@link DocumentsWriterPerThread} instance that is used during indexing to
 * build a in-memory index segment. {@link DocumentWriterState} also holds all flush
 * related per-thread data controlled by {@link AdvancedFlushControl}.
 * <p>
 * A {@link DocumentWriterState}, its methods and members should only accessed by one
 * thread a time. Users must acquire the lock via {@link DocumentWriterState#lock()}
 * and release the lock in a finally block via {@link DocumentWriterState#unlock()}
 * before accessing the state.
 */
@SuppressWarnings("serial")
public class DocumentWriterState extends ReentrantLock {
	
	private DocumentWriter mDwpt;
	
    // TODO this should really be part of DocumentsWriterFlushControl
    // write access guarded by DocumentsWriterFlushControl
	private volatile boolean mFlushPending = false;
	
    // TODO this should really be part of DocumentsWriterFlushControl
    // write access guarded by DocumentsWriterFlushControl
	private long mBytesUsed = 0;
	
    // guarded by Reentrant lock
    private boolean mIsActive = true;

    public DocumentWriterState(DocumentWriter dpwt) {
    	mDwpt = dpwt;
    }
    
    public final long getBytesUsed() { return mBytesUsed; }
    
    public void increaseBytesUsed(long count) { 
    	mBytesUsed += count;
    }
    
    /**
     * Resets the internal {@link DocumentsWriterPerThread} with the given one. 
     * if the given DWPT is <code>null</code> this ThreadState is marked as inactive and should not be used
     * for indexing anymore.
     * @see #isActive()  
     */
    public void resetWriter(DocumentWriter dwpt) {
    	assert this.isHeldByCurrentThread();
    	if (dwpt == null) 
    		mIsActive = false;
    	
    	mDwpt = dwpt;
    	mBytesUsed = 0;
    	mFlushPending = false;
    }
    
    /**
     * Returns <code>true</code> if this ThreadState is still open. This will
     * only return <code>false</code> iff the DW has been closed and this
     * ThreadState is already checked out for flush.
     */
    public boolean isActive() {
    	assert this.isHeldByCurrentThread();
    	return mIsActive;
    }
    
    /**
     * Returns the number of currently active bytes in this ThreadState's
     * {@link DocumentsWriterPerThread}
     */
    public long getBytesUsedPerThread() {
    	assert this.isHeldByCurrentThread();
    	// public for FlushPolicy
    	return mBytesUsed;
    }
    
    /**
     * Returns this {@link DocumentWriterState}s {@link DocumentsWriterPerThread}
     */
    public DocumentWriter getDocumentWriter() {
    	assert this.isHeldByCurrentThread();
    	// public for FlushPolicy
    	return mDwpt;
    }
    
    /**
     * Returns <code>true</code> iff this {@link DocumentWriterState} is marked as flush
     * pending otherwise <code>false</code>
     */
    public boolean isFlushPending() {
    	return mFlushPending;
    }
    
    public void setFlushPending(boolean pending) { 
    	mFlushPending = pending;
    }
    
}
