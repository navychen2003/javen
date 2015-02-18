package org.javenstudio.falcon.search.update;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.DefaultThreadFactory;
import org.javenstudio.falcon.util.ModifiableParams;
import org.javenstudio.falcon.search.ISearchCore;
import org.javenstudio.falcon.search.ISearchRequest;

/**
 * Helper class for tracking autoCommit state.
 * 
 * Note: This is purely an implementation detail of autoCommit and will
 * definitely change in the future, so the interface should not be relied-upon
 * 
 * Note: all access must be synchronized.
 * 
 * Public for tests.
 */
public final class CommitTracker implements Runnable {
  	private static Logger LOG = Logger.getLogger(CommitTracker.class);
  
  	// scheduler delay for maxDoc-triggered autocommits
  	public final int DOC_COMMIT_DELAY_MS = 1;
  
  	// settings, not final so we can change them in testing
  	private int mDocsUpperBound;
  	private long mTimeUpperBound;
  
  	private final ScheduledExecutorService mScheduler = 
  			Executors.newScheduledThreadPool(1, 
  					new DefaultThreadFactory("commitScheduler"));
  	
  	@SuppressWarnings("rawtypes")
	private ScheduledFuture mPending;
  
  	// state
  	private AtomicLong mDocsSinceCommit = new AtomicLong(0);
  	private AtomicInteger mAutoCommitCount = new AtomicInteger(0);

  	private final ISearchCore mCore;

  	private final boolean mSoftCommit;
  	private final boolean mOpenSearcher;
  	private final boolean mWaitSearcher = true;

  	private String mName;
  
  	public CommitTracker(ISearchCore core, String name, 
  			int docsUpperBound, int timeUpperBound, 
  			boolean openSearcher, boolean softCommit) {
  		mCore = core;
  		mName = name;
  		mPending = null;
    
  		mDocsUpperBound = docsUpperBound;
  		mTimeUpperBound = timeUpperBound;
    
  		mSoftCommit = softCommit;
  		mOpenSearcher = openSearcher;

  		if (LOG.isDebugEnabled())
  			LOG.debug(name + " AutoCommit: " + getDescription());
  	}

  	public String getName() { 
  		return mName;
  	}
  	
  	public boolean isOpenSearcher() {
  		return mOpenSearcher;
  	}
  
  	public synchronized void close() {
  		if (mPending != null) {
  			mPending.cancel(true);
  			mPending = null;
  		}
  		mScheduler.shutdownNow();
  	}
  
  	/** schedule individual commits */
  	public void scheduleCommitWithin(long commitMaxTime) {
  		doScheduleCommitWithin(commitMaxTime);
  	}

  	public void cancelPendingCommit() {
  		synchronized (this) {
  			if (mPending != null) {
  				boolean canceled = mPending.cancel(false);
  				if (canceled) 
  					mPending = null;
  			}
  		}
  	}
  
  	private void doScheduleCommitWithinIfNeeded(long commitWithin) {
  		long ctime = (commitWithin > 0) ? commitWithin : mTimeUpperBound;
  		if (ctime > 0) 
  			doScheduleCommitWithin(ctime);
  	}

  	private void doScheduleCommitWithin(long commitMaxTime) {
  		if (commitMaxTime <= 0) 
  			return;
  		
  		synchronized (this) {
  			if (mPending != null && mPending.getDelay(TimeUnit.MILLISECONDS) <= commitMaxTime) {
  				// There is already a pending commit that will happen first, so
  				// nothing else to do here.
  				if (LOG.isDebugEnabled()) {
  					LOG.debug("### returning since getDelay()==" + mPending.getDelay(TimeUnit.MILLISECONDS) 
  							+ " less than " + commitMaxTime);
  				}

  				return;
  			}

  			if (mPending != null) {
  				// we need to schedule a commit to happen sooner than the existing one,
  				// so lets try to cancel the existing one first.
  				boolean canceled = mPending.cancel(false);
  				
  				if (!canceled) {
  					// It looks like we can't cancel... it must have just started running!
  					// this is possible due to thread scheduling delays and a low commitMaxTime.
  					// Nothing else to do since we obviously can't schedule our commit *before*
  					// the one that just started running (or has just completed).
  					if (LOG.isDebugEnabled()) 
  						LOG.debug("### returning since cancel failed");
  					
  					return;
  				}
  			}

  			if (LOG.isDebugEnabled()) 
  				LOG.debug("### scheduling for " + commitMaxTime);

  			// schedule our new commit
  			mPending = mScheduler.schedule(this, commitMaxTime, TimeUnit.MILLISECONDS);
  		}
  	}
  
  	/**
  	 * Indicate that documents have been added
  	 */
  	public void addedDocument(int commitWithin) {
  		// maxDocs-triggered autoCommit.  Use == instead of > so we only trigger once on the way up
  		if (mDocsUpperBound > 0) {
  			long docs = mDocsSinceCommit.incrementAndGet();
  			if (docs == mDocsUpperBound + 1) {
  				// reset the count here instead of run() so we don't miss other documents being added
  				mDocsSinceCommit.set(0);
  				doScheduleCommitWithin(DOC_COMMIT_DELAY_MS);
  			}
  		}
    
  		// maxTime-triggered autoCommit
  		doScheduleCommitWithinIfNeeded(commitWithin);
  	}
  
  	/** 
  	 * Indicate that documents have been deleted
  	 */
  	public void deletedDocument( int commitWithin ) {
  		doScheduleCommitWithinIfNeeded(commitWithin);
  	}
  
  	/** Inform tracker that a commit has occurred */
  	public void didCommit() {
  		// do nothing
  	}
  
  	/** Inform tracker that a rollback has occurred, cancel any pending commits */
  	public void didRollback() {
  		synchronized (this) {
  			if (mPending != null) {
  				mPending.cancel(false);
  				mPending = null; // let it start another one
  			}
  			mDocsSinceCommit.set(0);
  		}
  	}
  
  	/** This is the worker part for the ScheduledFuture **/
  	public void run() {
  		synchronized (this) {
  			if (LOG.isDebugEnabled()) 
  				LOG.debug("### start commit. pending=null");
  			
  			mPending = null;  // allow a new commit to be scheduled
  		}

  		ISearchRequest req = null;
  		
  		try {
  			req = mCore.createLocalRequest(null, 
  	  				new ModifiableParams());
  			
  			CommitCommand command = new CommitCommand(req, false);
  			
  			command.setOpenSearcher(mOpenSearcher);
  			command.setWaitSearcher(mWaitSearcher);
  			command.setSoftCommit(mSoftCommit);
  			// no need for command.maxOptimizeSegments = 1; since it is not optimizing

  			// we increment this *before* calling commit because it was causing a race
  			// in the tests (the new searcher was registered and the test proceeded
  			// to check the commit count before we had incremented it.)
  			mAutoCommitCount.incrementAndGet();

  			mCore.getUpdateIndexer().commit(command);
  			
  		} catch (Exception e) {
  			if (LOG.isErrorEnabled())
  				LOG.error("auto commit error: " + e.toString(), e);
  			
  		} finally {
  			if (LOG.isDebugEnabled()) 
  				LOG.debug("### done committing");
  			
  			try {
  				if (req != null)
  					req.close();
  			} catch (ErrorException ex) { 
  				if (LOG.isErrorEnabled())
  					LOG.error("close error: " + ex.toString(), ex);
  			}
  		}
  	}
  
  	// to facilitate testing: blocks if called during commit
  	public int getCommitCount() {
  		return mAutoCommitCount.get();
  	}
  
  	public long getTimeUpperBound() {
  		return mTimeUpperBound;
  	}

  	public int getDocsUpperBound() {
  		return mDocsUpperBound;
  	}

  	public void setDocsUpperBound(int docsUpperBound) {
  		mDocsUpperBound = docsUpperBound;
  	}

  	// only for testing - not thread safe
  	public void setTimeUpperBound(long timeUpperBound) {
  		mTimeUpperBound = timeUpperBound;
  	}
  	
  	@Override
  	public String toString() {
  		return mName + "AutoCommit{" + getDescription() + "}";
  	}
  	
  	private String getDescription() {
  		if (mTimeUpperBound > 0 || mDocsUpperBound > 0) {
  			return (mTimeUpperBound > 0 ? ("if uncommited for " + mTimeUpperBound + "ms; ") : "")
  				 + (mDocsUpperBound > 0 ? ("if " + mDocsUpperBound + " uncommited docs ") : "");
      
  		} else {
  			return "disabled";
  		}
  	}
  	
}
