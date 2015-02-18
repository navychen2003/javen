package org.javenstudio.falcon.search;

import java.io.IOException;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.store.DirectoryFactory;

/**
 * The state in this class can be easily shared between SearchCores across
 * SearchCore reloads.
 * 
 */
public abstract class SearchCoreState {

	private final Object mDeleteLock = new Object();
	private final ISearchCore mCore;
  
	protected SearchCoreState(ISearchCore core) { 
		mCore = core;
	}
	
	public Object getUpdateLock() {
		return mDeleteLock;
	}
  
	public ISearchCore getSearchCore() { 
		return mCore;
	}
	
	/**
	 * Force the creation of a new IndexWriter using the settings from the given
	 * SearchCore.
	 * 
	 * @param rollback close IndexWriter if false, else rollback
	 * @throws IOException If there is a low-level I/O error.
	 */
	public abstract void newIndexWriter(boolean rollback) throws ErrorException;
  
	/**
	 * Get the current IndexWriter. If a new IndexWriter must be created, use the
	 * settings from the given {@link SearchCore}.
	 * 
	 * @throws IOException If there is a low-level I/O error.
	 */
	public abstract SearchWriterRef getIndexWriterRef() throws ErrorException;
  
	/**
	 * Decrement the number of references to this state. When then number of
	 * references hits 0, the state will close.  If an optional closer is
	 * passed, that will be used to close the writer.
	 * 
	 * @throws IOException If there is a low-level I/O error.
	 */
	public abstract void decreaseRef(SearchWriterCloser closer) 
			throws ErrorException;
  
	/**
	 * Increment the number of references to this state.
	 */
	public abstract void increaseRef() throws ErrorException;
  
	/**
	 * Rollback the current IndexWriter. When creating the new IndexWriter use the
	 * settings from the given {@link SearchCore}.
	 * 
	 * @throws IOException If there is a low-level I/O error.
	 */
	public abstract void rollbackIndexWriter() throws ErrorException;
  
	/**
	 * @return the {@link DirectoryFactory} that should be used.
	 */
	public abstract DirectoryFactory getDirectoryFactory();

	//public abstract void doRecovery(CoreContainer cc, String name);
  
	public abstract void cancelRecovery();
	
}
