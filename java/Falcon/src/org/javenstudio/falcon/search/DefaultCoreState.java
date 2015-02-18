package org.javenstudio.falcon.search;

import java.io.IOException;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.SearchWriterCloser;
import org.javenstudio.falcon.search.SearchWriter;
import org.javenstudio.falcon.search.SearchWriterRef;
import org.javenstudio.falcon.search.ISearchCore;
import org.javenstudio.falcon.search.SearchCoreHolder;
import org.javenstudio.falcon.search.SearchCoreState;
import org.javenstudio.falcon.search.store.DirectoryFactory;

public class DefaultCoreState extends SearchCoreState {
	static Logger LOG = Logger.getLogger(DefaultCoreState.class);
  
	private final boolean SKIP_AUTO_RECOVERY = 
			Boolean.getBoolean("lightningcloud.skip.autorecovery");
  
	private final Object mRecoveryLock = new Object();
  
	// protects pauseWriter and writerFree
	private final Object mWriterPauseLock = new Object();
  
	private SearchWriter mIndexWriter = null;
	private SearchWriterRef mIndexWriterRef = null;
	private DirectoryFactory mDirectoryFactory;
	private int mRefCount = 1;

	private volatile boolean mRecoveryRunning;
	//private RecoveryStrategy recoveryStrat;
	private boolean mClosed = false;

	private boolean mPauseWriter;
	private boolean mWriterFree = true;
  
	public DefaultCoreState(ISearchCore core, DirectoryFactory directoryFactory) {
		super(core);
		mDirectoryFactory = directoryFactory;
	}
  
	@Override
	public synchronized SearchWriterRef getIndexWriterRef() throws ErrorException {
		final ISearchCore core = getSearchCore();
		
		synchronized (mWriterPauseLock) {
			if (core == null) {
				// core == null is a signal to just return the current writer, or null
				// if none.
				if (mIndexWriterRef != null) 
					mIndexWriterRef.increaseRef();
				
				return mIndexWriterRef;
			}
      
			while (mPauseWriter) {
				try {
					mWriterPauseLock.wait();
				} catch (InterruptedException e) {}
			}
      
			if (mIndexWriter == null) 
				mIndexWriter = createIndexWriter(core, false);
			
			if (mIndexWriterRef == null) {
				mIndexWriterRef = new SearchWriterRef(mIndexWriter) {
						@Override
						public void close() {
							synchronized (mWriterPauseLock) {
								mWriterFree = true;
								mWriterPauseLock.notifyAll();
							}
						}
					};
			}
			
			mWriterFree = false;
			mWriterPauseLock.notifyAll();
			mIndexWriterRef.increaseRef();
			
			return mIndexWriterRef;
		}
	}

	@Override
	public synchronized void newIndexWriter(boolean rollback) throws ErrorException {
		final ISearchCore core = getSearchCore();
		String coreName = core.getName();
		
		if (LOG.isInfoEnabled())
			LOG.info("Creating new IndexWriter...");
		
		synchronized (mWriterPauseLock) {
			// we need to wait for the Writer to fall out of use
			// first lets stop it from being lent out
			mPauseWriter = true;
			
			// then lets wait until its out of use
			if (LOG.isInfoEnabled())
				LOG.info("Waiting until IndexWriter is unused... core=" + coreName);
			
			while (!mWriterFree) {
				try {
					mWriterPauseLock.wait();
				} catch (InterruptedException e) {}
			}

			try {
				if (mIndexWriter != null) {
					if (!rollback) {
						try {
							if (LOG.isInfoEnabled())
								LOG.info("Closing old IndexWriter... core=" + coreName);
							
							mIndexWriter.close();
						} catch (Throwable t) {
							if (LOG.isErrorEnabled())
								LOG.error("Error closing old IndexWriter. core=" + coreName, t);
						}
					} else {
						try {
							if (LOG.isInfoEnabled())
								LOG.info("Rollback old IndexWriter... core=" + coreName);
							
							mIndexWriter.rollback();
						} catch (Throwable t) {
							if (LOG.isErrorEnabled())
								LOG.error("Error rolling back old IndexWriter. core=" + coreName, t);
						}
					}
				}
				
				mIndexWriter = createIndexWriter(core, true);
				
				if (LOG.isInfoEnabled())
					LOG.info("New IndexWriter is ready to be used.");
				
				// we need to null this so it picks up the new writer next get call
				mIndexWriterRef = null;
				
			} finally {
				mPauseWriter = false;
				mWriterPauseLock.notifyAll();
			}
		}
	}

	@Override
	public void decreaseRef(SearchWriterCloser closer) {
		synchronized (this) {
			mRefCount --;
			
			if (LOG.isDebugEnabled())
				LOG.debug("decreaseRef: refcount=" + mRefCount);
			
			if (mRefCount == 0) {
				try {
					if (LOG.isInfoEnabled())
						LOG.info("CoreState ref count has reached 0 - closing IndexWriter");
					
					if (closer != null) {
						closer.closeWriter(mIndexWriter);
					} else if (mIndexWriter != null) {
						mIndexWriter.close();
					}
				} catch (Throwable t) {
					if (LOG.isErrorEnabled())
						LOG.error("Error during shutdown of writer.", t);
				}
				
				try {
					mDirectoryFactory.close();
				} catch (Throwable t) {
					if (LOG.isErrorEnabled())
						LOG.error("Error during shutdown of directory factory.", t);
				}
				
				try {
					if (LOG.isInfoEnabled())
						LOG.info("Closing CoreState - canceling any ongoing recovery");
					
					cancelRecovery();
				} catch (Throwable t) {
					if (LOG.isErrorEnabled())
						LOG.error("Error cancelling recovery", t);
				}

				mClosed = true;
			}
		}
	}

	@Override
	public synchronized void increaseRef() {
		if (mRefCount <= 0) 
			throw new IllegalStateException("IndexWriter has been closed");
    
		mRefCount ++;
		
		if (LOG.isDebugEnabled())
			LOG.debug("increaseRef: refcount=" + mRefCount);
	}

	@Override
	public synchronized void rollbackIndexWriter() throws ErrorException {
		newIndexWriter(true);
	}
  
	protected SearchWriter createIndexWriter(ISearchCore core, 
			boolean forceNewDirectory) throws ErrorException {
		try {
			SearchWriter writer = SearchWriter.create(core.getName(), 
					core.getNewIndexDir(), core.getDirectoryFactory(), 
					core.createIndexParams());
			
			core.registerInfoMBean(writer);
			return writer;
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
	}

	@Override
	public DirectoryFactory getDirectoryFactory() {
		return mDirectoryFactory;
	}

	public void doRecovery(SearchCoreHolder holder, String name) {
		if (SKIP_AUTO_RECOVERY) {
			if (LOG.isWarnEnabled())
				LOG.warn("Skipping recovery according to sys prop lightningcloud.skip.autorecovery");
			return;
		}
    
		if (holder.isShutdown()) {
			if (LOG.isWarnEnabled())
				LOG.warn("Skipping recovery because Lightning is shutdown");
			return;
		}
    
		synchronized (mRecoveryLock) {
			if (LOG.isInfoEnabled())
				LOG.info("Running recovery - first canceling any ongoing recovery");
			
			cancelRecovery();
      
			while (mRecoveryRunning) {
				try {
					mRecoveryLock.wait(1000);
				} catch (InterruptedException e) {
					// ignore
				}
				
				// check again for those that were waiting
				if (holder.isShutdown()) {
					if (LOG.isWarnEnabled())
						LOG.warn("Skipping recovery because Lightning is shutdown");
					
					return;
				}
				
				if (mClosed) return;
			}

			// if true, we are recovering after startup and shouldn't have 
			// (or be receiving) additional updates (except for local tlog recovery)
			//boolean recoveringAfterStartup = recoveryStrat == null;

			//recoveryStrat = new RecoveryStrategy(cc, name, this);
			//recoveryStrat.setRecoveringAfterStartup(recoveringAfterStartup);
			//recoveryStrat.start();
			
			mRecoveryRunning = true;
		}
	}
  
	@Override
	public void cancelRecovery() {
		// TODO: for cloud
	}

}
