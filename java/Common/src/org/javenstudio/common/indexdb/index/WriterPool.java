package org.javenstudio.common.indexdb.index;

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
 */
public abstract class WriterPool {

	private final FieldNumbers mGlobalFieldMap;
	
	public WriterPool(FieldNumbers globalFieldMap) { 
		mGlobalFieldMap = globalFieldMap;
	}
	
	public abstract DocumentWriterState getAndLock();
	public abstract void deactivateUnreleasedStates();
	
	public abstract int getNumActiveWriters();
	public abstract DocumentWriterState getWriterStateAt(int ord);
	
	protected abstract DocumentWriter createDocumentWriter(FieldInfosBuilder fieldInfos);
	
	public DocumentWriter replaceForFlush(DocumentWriterState state, boolean closed) {
		assert state.isHeldByCurrentThread();
		
		final DocumentWriter dwpt = state.getDocumentWriter();
		if (!closed) {
			final FieldInfosBuilder infos = new FieldInfosBuilder(mGlobalFieldMap);
			final DocumentWriter newDwpt = createDocumentWriter(infos);
			
			newDwpt.initialize();
			state.resetWriter(newDwpt);
			
		} else {
			state.resetWriter(null);
		}
		
		return dwpt;
	}
	
	/**
	 * Deactivates an active {@link DocumentWriterState}. Inactive {@link DocumentWriterState} can
	 * not be used for indexing anymore once they are deactivated. This method should only be used
	 * if the parent {@link DocumentsWriter} is closed or aborted.
	 * 
	 * @param threadState the state to deactivate
	 */
	public void deactivateWriterState(DocumentWriterState state) {
		assert state.isActive();
		state.resetWriter(null);
	}

	/**
	 * Reinitialized an active {@link DocumentWriterState}. A {@link DocumentWriterState} should
	 * only be reinitialized if it is active without any pending documents.
	 * 
	 * @param threadState the state to reinitialize
	 */
	public void reinitWriterState(DocumentWriterState state) {
		assert state.isActive();
		assert state.getDocumentWriter().getNumDocsInRAM() == 0;
		state.getDocumentWriter().initialize();
	}
	
	public void recycle(DocumentWriter dwpt) {
		// don't recycle DWPT by default
	}
	
}
