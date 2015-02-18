package org.javenstudio.common.indexdb.index;

import org.javenstudio.common.util.Logger;

/**
 * Default {@link FlushPolicy} implementation that flushes based on RAM used,
 * document count and number of buffered deletes depending on the IndexWriter's
 * {@link IndexWriterConfig}.
 * 
 * <ul>
 * <li>{@link #onDelete(FlushControl, DocumentWriterState)} - flushes
 * based on the global number of buffered delete terms iff
 * {@link IndexWriterConfig#getMaxBufferedDeleteTerms()} is enabled</li>
 * <li>{@link #onInsert(FlushControl, DocumentWriterState)} - flushes
 * either on the number of documents per {@link DocumentsWriterPerThread} (
 * {@link DocumentsWriterPerThread#getNumDocsInRAM()}) or on the global active
 * memory consumption in the current indexing session iff
 * {@link IndexWriterConfig#getMaxBufferedDocs()} or
 * {@link IndexWriterConfig#getRAMBufferSizeMB()} is enabled respectively</li>
 * <li>{@link #onUpdate(FlushControl, DocumentWriterState)} - calls
 * {@link #onInsert(FlushControl, DocumentWriterState)} and
 * {@link #onDelete(FlushControl, DocumentWriterState)} in order</li>
 * </ul>
 * All {@link IndexWriterConfig} settings are used to mark
 * {@link DocumentsWriterPerThread} as flush pending during indexing with
 * respect to their live updates.
 * <p>
 * If {@link IndexWriterConfig#setRAMBufferSizeMB(double)} is enabled, the
 * largest ram consuming {@link DocumentsWriterPerThread} will be marked as
 * pending if the global active RAM consumption is >= the configured max RAM
 * buffer.
 */
public class FlushByRamOrCountsPolicy extends FlushPolicy {
	private static final Logger LOG = Logger.getLogger(FlushByRamOrCountsPolicy.class);

	public FlushByRamOrCountsPolicy(SegmentWriter writer) { 
		super(writer);
	}
	
	@Override
	public void onDelete(FlushControl control, DocumentWriterState state) {
		if (flushOnDeleteTerms()) {
			// Flush this state by num del terms
			final int maxBufferedDeleteTerms = 
					getIndexParams().getMaxBufferedDeleteTerms();
			if (control.getNumGlobalTermDeletes() >= maxBufferedDeleteTerms) 
				control.setApplyAllDeletes();
		}
		
		if (flushOnRAM()) {
			final long limit = (long) (getIndexParams().getRAMBufferSizeMB() * 1024.d * 1024.d);
			if (control.getDeleteBytesUsed() > limit) { 
				control.setApplyAllDeletes();
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("FlushPolicy: force apply deletes bytesUsed=" 
							+ control.getDeleteBytesUsed() + " vs ramBuffer=" 
							+ limit);
				}
			}
		}
	}

	@Override
	public void onInsert(FlushControl control, DocumentWriterState state) {
		if (flushOnDocCount() && state.getDocumentWriter().getNumDocsInRAM() >= 
				getIndexParams().getMaxBufferedDocs()) {
			// Flush this state by num docs
			control.setFlushPending(state);
			
		} else if (flushOnRAM()) { // flush by RAM
			final long limit = (long) (getIndexParams().getRAMBufferSizeMB() * 1024.d * 1024.d);
			final long totalRam = control.getActiveBytes() + control.getDeleteBytesUsed();
			
			if (totalRam >= limit) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("FlushPolicy: flush: activeBytes=" + control.getActiveBytes() 
							+ " deleteBytes=" + control.getDeleteBytesUsed() 
							+ " vs limit=" + limit);
				}
				
				markLargestWriterPending(control, state, totalRam);
			}
		}
	}
  
	/**
	 * Marks the most ram consuming active {@link DocumentsWriterPerThread} flush
	 * pending
	 */
	protected void markLargestWriterPending(FlushControl control, 
			DocumentWriterState perThreadState, final long currentBytesPerThread) {
		control.setFlushPending(findLargestNonPendingWriter(control, perThreadState));
	}
  
	/**
	 * Returns <code>true</code> if this {@link FlushPolicy} flushes on
	 * {@link IndexWriterConfig#getMaxBufferedDocs()}, otherwise
	 * <code>false</code>.
	 */
	protected boolean flushOnDocCount() {
		return getIndexParams().getMaxBufferedDocs() != IndexParams.DISABLE_AUTO_FLUSH;
	}

	/**
	 * Returns <code>true</code> if this {@link FlushPolicy} flushes on
	 * {@link IndexWriterConfig#getMaxBufferedDeleteTerms()}, otherwise
	 * <code>false</code>.
	 */
	protected boolean flushOnDeleteTerms() {
		return getIndexParams().getMaxBufferedDeleteTerms() != IndexParams.DISABLE_AUTO_FLUSH;
	}

	/**
	 * Returns <code>true</code> if this {@link FlushPolicy} flushes on
	 * {@link IndexWriterConfig#getRAMBufferSizeMB()}, otherwise
	 * <code>false</code>.
	 */
	protected boolean flushOnRAM() {
		return getIndexParams().getRAMBufferSizeMB() != IndexParams.DISABLE_AUTO_FLUSH;
	}
	
}
