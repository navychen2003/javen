package org.javenstudio.common.indexdb.index;

import java.util.Iterator;

/**
 * {@link FlushPolicy} controls when segments are flushed from a RAM resident
 * internal data-structure to the {@link IndexWriter}s {@link Directory}.
 * <p>
 * Segments are traditionally flushed by:
 * <ul>
 * <li>RAM consumption - configured via
 * {@link IndexWriterConfig#setRAMBufferSizeMB(double)}</li>
 * <li>Number of RAM resident documents - configured via
 * {@link IndexWriterConfig#setMaxBufferedDocs(int)}</li>
 * <li>Number of buffered delete terms/queries - configured via
 * {@link IndexWriterConfig#setMaxBufferedDeleteTerms(int)}</li>
 * </ul>
 * 
 * The {@link IndexWriter} consults a provided {@link FlushPolicy} to control the
 * flushing process. The policy is informed for each added or
 * updated document as well as for each delete term. Based on the
 * {@link FlushPolicy}, the information provided via {@link DocumentWriterState} and
 * {@link FlushControl}, the {@link FlushPolicy} decides if a
 * {@link DocumentsWriterPerThread} needs flushing and mark it as
 * flush-pending via
 * {@link FlushControl#setFlushPending(DocumentWriterState.ThreadState)}.
 * 
 * @see DocumentWriterState
 * @see FlushControl
 */
public abstract class FlushPolicy {
	
	protected final SegmentWriter mWriter;

	public FlushPolicy(SegmentWriter writer) { 
		mWriter = writer;
	}
	
	protected final IndexParams getIndexParams() { 
		return mWriter.getIndexWriter().getIndexParams();
	}
	
	/**
	 * Called for each delete term. If this is a delete triggered due to an update
	 * the given {@link DocumentWriterState} is non-null.
	 * <p>
	 * Note: This method is called synchronized on the given
	 * {@link FlushControl} and it is guaranteed that the calling
	 * thread holds the lock on the given {@link DocumentWriterState}
	 */
	public abstract void onDelete(FlushControl control, DocumentWriterState state);

	/**
	 * Called for each document update on the given {@link DocumentWriterState}'s
	 * {@link DocumentsWriterPerThread}.
	 * <p>
	 * Note: This method is called  synchronized on the given
	 * {@link FlushControl} and it is guaranteed that the calling
	 * thread holds the lock on the given {@link DocumentWriterState}
	 */
	public void onUpdate(FlushControl control, DocumentWriterState state) {
		onInsert(control, state);
		onDelete(control, state);
	}

	/**
	 * Called for each document addition on the given {@link DocumentWriterState}s
	 * {@link DocumentsWriterPerThread}.
	 * <p>
	 * Note: This method is synchronized by the given
	 * {@link FlushControl} and it is guaranteed that the calling
	 * thread holds the lock on the given {@link DocumentWriterState}
	 */
	public abstract void onInsert(FlushControl control, DocumentWriterState state);

	/**
	 * Returns the current most RAM consuming non-pending {@link DocumentWriterState} with
	 * at least one indexed document.
	 * <p>
	 * This method will never return <code>null</code>
	 */
	protected DocumentWriterState findLargestNonPendingWriter(FlushControl control, 
			DocumentWriterState state) {
		assert state.getDocumentWriter().getNumDocsInRAM() > 0;
		long maxRamSoFar = state.getBytesUsedPerThread();
		
		// the dwpt which needs to be flushed eventually
		DocumentWriterState maxRamUsingWriterState = state;
		assert !state.isFlushPending() : "DWPT should have flushed";
		
		Iterator<DocumentWriterState> activePerWritersIterator = control.allActiveWriterStates();
		while (activePerWritersIterator.hasNext()) {
			DocumentWriterState next = activePerWritersIterator.next();
			if (!next.isFlushPending()) {
				final long nextRam = next.getBytesUsedPerThread();
				if (nextRam > maxRamSoFar && next.getDocumentWriter().getNumDocsInRAM() > 0) {
					maxRamSoFar = nextRam;
					maxRamUsingWriterState = next;
				}
			}
		}
		
		//assert assertMessage("set largest ram consuming thread pending on lower watermark");
		return maxRamUsingWriterState;
	}
  
}
