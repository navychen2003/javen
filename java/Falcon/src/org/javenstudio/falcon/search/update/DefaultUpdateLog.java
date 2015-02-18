package org.javenstudio.falcon.search.update;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.SearcherRef;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.SearchRequestInfo;
import org.javenstudio.falcon.search.ISearchResponse;
import org.javenstudio.falcon.util.DefaultThreadFactory;
import org.javenstudio.falcon.util.ModifiableParams;
import org.javenstudio.falcon.util.PluginInfo;

final class DefaultUpdateLog extends UpdateLog {
	
	public static String LOG_FILENAME_PATTERN = "%s.%019d";
	public static String TLOG_NAME="tlog";

	public static class LogPtr {
		private final long mPointer;
	    private final long mVersion;

	    public LogPtr(long pointer, long version) {
	    	mPointer = pointer;
	    	mVersion = version;
	    }

	    @Override
	    public String toString() {
	    	return "LogPtr(" + mPointer + ")";
	    }
	}
	
	private long mId = -1;
	@SuppressWarnings("unused")
	private State mState = State.ACTIVE;
	// flags to write in the transaction log with operations (i.e. FLAG_GAP)
	private int mOperationFlags; 

	private TransactionLog mTransLog;
	private TransactionLog mPrevLog;
	
	// list of recent logs, newest first
	private Deque<TransactionLog> mLogs = new LinkedList<TransactionLog>(); 
	private LinkedList<TransactionLog> mNewestLogsOnStartup = new LinkedList<TransactionLog>();
	private int mNumOldRecords;  // number of records in the recent logs

	private Map<BytesRef,LogPtr> mMap = new HashMap<BytesRef, LogPtr>();
	private Map<BytesRef,LogPtr> mPrevMap;  // used while committing/reopening is happening
	private Map<BytesRef,LogPtr> mPrevMap2; // used while committing/reopening is happening
	private TransactionLog mPrevMapLog;  	// the transaction log used to look up entries found in prevMap
	private TransactionLog mPrevMapLog2;  	// the transaction log used to look up entries found in prevMap

	private final int mNumDeletesToKeep = 1000;
	private final int mNumDeletesByQueryToKeep = 100;
	private final int mNumRecordsToKeep = 100;

	// keep track of deletes only... this is not updated on an add
	@SuppressWarnings("serial")
	private LinkedHashMap<BytesRef, LogPtr> mOldDeletes = 
		new LinkedHashMap<BytesRef, LogPtr>(mNumDeletesToKeep) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<BytesRef, LogPtr> eldest) {
				return size() > mNumDeletesToKeep;
			}
		};
	
	private LinkedList<DBQ> mDeleteByQueries = new LinkedList<DBQ>();

	private String[] mLogFiles;
	private File mLogDir;
	private Collection<String> mGlobalStrings;

	private String mDataDir;
	private String mLastDataDir;

	private VersionInfo mVersionInfo;
	private SyncLevel mDefaultSyncLevel = SyncLevel.FLUSH;

	// a core reload can change this reference!
	private volatile UpdateIndexer mUpdateIndexer; 
	private volatile boolean cancelApplyBufferUpdate;
	
	@SuppressWarnings("unused")
	private List<Long> mStartingVersions;
	@SuppressWarnings("unused")
	private int mStartingOperation; // last operation in the logs on startup
	
	private RecoveryInfo mRecoveryInfo;
		
	private ThreadPoolExecutor mRecoveryExecutor = new ThreadPoolExecutor(0,
			Integer.MAX_VALUE, 1, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
			new DefaultThreadFactory("recoveryExecutor"));
	
	  
	@Override
	public void init(PluginInfo info) throws ErrorException {
	    mDataDir = (String)info.getInitArgs().get("dir");
	    mDefaultSyncLevel = SyncLevel.getSyncLevel(
	    		(String)info.getInitArgs().get("syncLevel"));
	}

	@Override
	public void init(UpdateIndexer handler) throws ErrorException {
		if (mDataDir == null || mDataDir.length() == 0) 
			mDataDir = handler.getSearchCore().getDataDir();

      	mUpdateIndexer = handler;

      	if (mDataDir.equals(mLastDataDir)) {
      		if (LOG.isDebugEnabled()) {
      			LOG.debug("UpdateIndexer init: tlogDir=" + mLogDir + ", next id=" + mId 
      					+ ", this is a reopen... nothing else to do.");
      		}

      		mVersionInfo.reload();

      		// on a normal reopen, we currently shouldn't have to do anything
      		return;
      	}
      	
      	mLastDataDir = mDataDir;
      	mLogDir = new File(mDataDir, TLOG_NAME);
      	mLogDir.mkdirs();
      	mLogFiles = getLogList(mLogDir);
      	mId = getLastLogId() + 1; // add 1 since we will create a new log for the next update

      	if (LOG.isDebugEnabled()) {
      		LOG.debug("UpdateIndexer init: tlogDir=" + mLogDir + ", existing tlogs=" 
      				+ Arrays.asList(mLogFiles) + ", next id=" + mId);
      	}
      
      	TransactionLog oldLog = null;
      	
      	for (String oldLogName : mLogFiles) {
      		File f = new File(mLogDir, oldLogName);
      		try {
      			oldLog = new TransactionLog(f, null, true);
      			// don't remove old logs on startup since more than one may be uncapped.
      			addOldLog(oldLog, false); 
      			
      		} catch (Exception e) {
      			if (LOG.isErrorEnabled())
      				LOG.error("Failure to open existing log file (non fatal) " + f, e);
      			
      			deleteFile(f);
      		}
      	}

      	// Record first two logs (oldest first) at startup for potential tlog recovery.
      	// It's possible that at abnormal shutdown both "tlog" and "prevTlog" were uncapped.
      	for (TransactionLog ll : mLogs) {
      		mNewestLogsOnStartup.addFirst(ll);
      		if (mNewestLogsOnStartup.size() >= 2) 
      			break;
      	}

      	mVersionInfo = new VersionInfo(this, 256);

      	// TODO: these startingVersions assume that we successfully 
      	// recover from all non-complete tlogs.
      	RecentUpdates startingUpdates = getRecentUpdates();
      	
      	try {
      		mStartingVersions = startingUpdates.getVersions(mNumRecordsToKeep);
      		mStartingOperation = startingUpdates.getLatestOperation();

      		// populate recent deletes list (since we can't get that info from the index)
      		for (int i = startingUpdates.mDeleteList.size()-1; i >= 0; i--) {
      			DeleteUpdate du = startingUpdates.mDeleteList.get(i);
      			mOldDeletes.put(new BytesRef(du.mId), new LogPtr(-1, du.mVersion));
      		}

      		// populate recent deleteByQuery commands
      		for (int i = startingUpdates.mDeleteByQueryList.size()-1; i >= 0; i--) {
      			Update update = startingUpdates.mDeleteByQueryList.get(i);
      			
      			@SuppressWarnings("unchecked")
				List<Object> dbq = (List<Object>) update.mLog.lookup(update.mPointer);
      			long version = (Long) dbq.get(1);
      			String q = (String) dbq.get(2);
      			
      			trackDeleteByQuery(q, version);
      		}
      	} finally {
      		startingUpdates.close();
      	}
	}

	@Override
	public UpdateIndexer getUpdateIndexer() {
		return mUpdateIndexer;
	}

	@Override
	public VersionInfo getVersionInfo() {
		return mVersionInfo;
	}

	@Override
	public State getState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasUncommittedChanges() {
		return mTransLog != null;
	}

	@Override
	public List<DBQ> getDBQNewer(long version) throws ErrorException {
	    synchronized (this) {
	        if (mDeleteByQueries.isEmpty() || mDeleteByQueries.getFirst().getVersion() < version) {
	        	// fast common case
	        	return null;
	        }

	        List<DBQ> dbqList = new ArrayList<DBQ>();
	        for (DBQ dbq : mDeleteByQueries) {
	        	if (dbq.getVersion() <= version) break;
	        	dbqList.add(dbq);
	        }
	        
	        return dbqList;
	    }
	}

	// This method works like realtime-get... it only guarantees to return the latest
	// version of the *completed* update.  There can be updates in progress concurrently
	// that have already grabbed higher version numbers.  Higher level coordination or
	// synchronization is needed for stronger guarantees (as VersionUpdateProcessor does).
	@Override
	public Long lookupVersion(BytesRef indexedId) throws ErrorException {
	    LogPtr entry;
	    @SuppressWarnings("unused")
	    TransactionLog lookupLog;

	    synchronized (this) {
	    	entry = mMap.get(indexedId);
	    	lookupLog = mTransLog;  // something found in "map" will always be in "tlog"
	    	
	    	if (entry == null && mPrevMap != null) {
	    		entry = mPrevMap.get(indexedId);
	    		// something found in prevMap will always be found in preMapLog 
	    		// (which could be tlog or prevTlog)
	    		lookupLog = mPrevMapLog;
	    	}
	    	
	    	if (entry == null && mPrevMap2 != null) {
	    		entry = mPrevMap2.get(indexedId);
	    		// something found in prevMap2 will always be found in preMapLog2 
	    		// (which could be tlog or prevTlog)
	    		lookupLog = mPrevMapLog2;
	    	}
	    }

	    if (entry != null) 
	    	return entry.mVersion;

	    // Now check real index
	    Long version = mVersionInfo.getVersionFromIndex(indexedId);

	    if (version != null) 
	    	return version;

	    // We can't get any version info for deletes from the index, so if the doc
	    // wasn't found, check a cache of recent deletes.

	    synchronized (this) {
	    	entry = mOldDeletes.get(indexedId);
	    }

	    if (entry != null) 
	    	return entry.mVersion;

	    return null;
	}

	@Override
	public void add(AddCommand cmd, boolean clearCaches) throws ErrorException {
	    // don't log if we are replaying from another log
	    // TODO: we currently need to log to maintain correct versioning, rtg, etc
	    // if ((cmd.getFlags() & UpdateCommand.REPLAY) != 0) return;

	    synchronized (this) {
	    	long pos = -1;

	    	// don't log if we are replaying from another log
	    	if ((cmd.getFlags() & UpdateCommand.REPLAY) == 0) {
	    		ensureLog();
	    		pos = mTransLog.write(cmd, mOperationFlags);
	    	}

	    	if (!clearCaches) {
	    		// TODO: in the future we could support a real position for a REPLAY update.
	    		// Only currently would be useful for RTG while in recovery mode though.
	    		LogPtr ptr = new LogPtr(pos, cmd.getVersion());

	    		// only update our map if we're not buffering
	    		if ((cmd.getFlags() & UpdateCommand.BUFFERING) == 0) 
	    			mMap.put(cmd.getIndexedId(), ptr);

	    		if (LOG.isDebugEnabled()) {
	    			LOG.debug("added id " + cmd.getPrintableId() + " to " + mTransLog + " " 
	    					+ ptr + " map=" + System.identityHashCode(mMap));
	    		}

	    	} else {
	    		// replicate the deleteByQuery logic.  See deleteByQuery for comments.

	    		if (mMap != null) mMap.clear();
	    		if (mPrevMap != null) mPrevMap.clear();
	    		if (mPrevMap2 != null) mPrevMap2.clear();

	    		try {
	    			SearcherRef holder = mUpdateIndexer.getSearchCore()
	    					.getSearchControl().openNewSearcherRef(true, true);
	    			
	    			holder.decreaseRef();
	    		} catch (Throwable e) {
	    			if (LOG.isErrorEnabled())
	    				LOG.error("Error opening realtime searcher for deleteByQuery", e);
	    		}

	    		if (LOG.isDebugEnabled()) {
	    			LOG.debug("added id " + cmd.getPrintableId() + " to " + mTransLog 
	    					+ " clearCaches=true");
	    		}
	    	}
	    }
	}

	@Override
	public void delete(DeleteCommand cmd) throws ErrorException {
	    BytesRef br = cmd.getIndexedId();

	    synchronized (this) {
	    	long pos = -1;

	    	// don't log if we are replaying from another log
	    	if ((cmd.getFlags() & UpdateCommand.REPLAY) == 0) {
	    		ensureLog();
	    		pos = mTransLog.writeDelete(cmd, mOperationFlags);
	    	}

	    	LogPtr ptr = new LogPtr(pos, cmd.getVersion());

	    	// only update our map if we're not buffering
	    	if ((cmd.getFlags() & UpdateCommand.BUFFERING) == 0) {
	    		mMap.put(br, ptr);

	    		mOldDeletes.put(br, ptr);
	    	}

	    	if (LOG.isDebugEnabled()) {
	    		LOG.debug("added delete for id " + cmd.getId() + " to " + mTransLog + " " 
	    				+ ptr + " map=" + System.identityHashCode(mMap));
	    	}
	    }
	}

	@Override
	public void deleteAll() throws ErrorException {
	    synchronized (this) {
	        try {
	        	SearcherRef holder = mUpdateIndexer.getSearchCore()
	        			.getSearchControl().openNewSearcherRef(true, true);
	        	
	        	holder.decreaseRef();
	        } catch (Throwable e) {
	        	if (LOG.isErrorEnabled())
	        		LOG.error("Error opening realtime searcher for deleteByQuery", e);
	        }

	        if (mMap != null) mMap.clear();
	        if (mPrevMap != null) mPrevMap.clear();
	        if (mPrevMap2 != null) mPrevMap2.clear();

	        mOldDeletes.clear();
	        mDeleteByQueries.clear();
	    }
	}

	@Override
	public void deleteByQuery(DeleteCommand cmd) throws ErrorException {
	    synchronized (this) {
	        long pos = -1;
	        // don't log if we are replaying from another log
	        if ((cmd.getFlags() & UpdateCommand.REPLAY) == 0) {
	        	ensureLog();
	        	pos = mTransLog.writeDeleteByQuery(cmd, mOperationFlags);
	        }

	        // only change our caches if we are not buffering
	        if ((cmd.getFlags() & UpdateCommand.BUFFERING) == 0) {
	        	// given that we just did a delete-by-query, we don't know what documents were
	        	// affected and hence we must purge our caches.
	        	if (mMap != null) mMap.clear();
	        	if (mPrevMap != null) mPrevMap.clear();
	        	if (mPrevMap2 != null) mPrevMap2.clear();

	        	trackDeleteByQuery(cmd.getQueryString(), cmd.getVersion());
	        	// oldDeletes.clear();

	        	// We must cause a new IndexReader to be opened before anything 
	        	// looks at these caches again
	        	// so that a cache miss will read fresh data.
	        	//
	        	// TODO: FUTURE: open a new searcher lazily for better throughput 
	        	// with delete-by-query commands
	        	try {
	        		SearcherRef holder = mUpdateIndexer.getSearchCore()
	        				.getSearchControl().openNewSearcherRef(true, true);
	        		
	        		holder.decreaseRef();
	        	} catch (Throwable e) {
	        		if (LOG.isErrorEnabled())
	        			LOG.error("Error opening realtime searcher for deleteByQuery", e);
	        	}
	        }

	        LogPtr ptr = new LogPtr(pos, cmd.getVersion());
	        if (LOG.isDebugEnabled()) {
	        	LOG.debug("added deleteByQuery \"" + cmd.getQueryString() + "\" to " 
	        			+ mTransLog + " " + ptr + " map=" + System.identityHashCode(mMap));
	        }
	    }
	}

	@Override
	public void preCommit(CommitCommand cmd) throws ErrorException {
	    synchronized (this) {
	        if (LOG.isDebugEnabled()) 
	          	LOG.debug("preCommit: " + cmd);

	        if (getState() != State.ACTIVE && (cmd.getFlags() & UpdateCommand.REPLAY) == 0) {
	        	// if we aren't in the active state, and this isn't a replay
	        	// from the recovery process, then we shouldn't mess with
	        	// the current transaction log.  This normally shouldn't happen
	        	// as DistributedUpdateProcessor will prevent this.  Commits
	        	// that don't use the processor are possible though.
	        	return;
	        }

        	// since we're changing the log, we must change the map.
        	newMap();

        	if (mPrevLog != null) 
        		mGlobalStrings = mPrevLog.getGlobalStrings();
        
        	// since document additions can happen concurrently with commit, create
        	// a new transaction log first so that we know the old one is definitely
        	// in the index.
        	mPrevLog = mTransLog;
        	mTransLog = null;
        	mId ++;
      	}
	}

	@Override
	public void postCommit(CommitCommand cmd) throws ErrorException {
	    synchronized (this) {
	        if (LOG.isDebugEnabled()) 
	          	LOG.debug("postCommit: " + cmd);
	        
	        if (mPrevLog != null) {
	        	// if we made it through the commit, write a commit command to the log
	        	// TODO: check that this works to cap a tlog we were using to buffer 
	        	// so we don't replay on startup.
	        	mPrevLog.writeCommit(cmd, mOperationFlags);

	        	addOldLog(mPrevLog, true);
	        	// the old log list will decref when no longer needed
	        	// prevTlog.decref();
	        	mPrevLog = null;
	        }
	    }
	}

	@Override
	public void preSoftCommit(CommitCommand cmd) throws ErrorException {
	    synchronized (this) {
	    	if (!cmd.isSoftCommit()) 
	    		return;  // already handled this at the start of the hard commit
	      
	    	newMap();

	    	// start adding documents to a new map since we won't know if
	    	// any added documents will make it into this commit or not.
	    	// But we do know that any updates already added will definitely
	    	// show up in the latest reader after the commit succeeds.
	    	mMap = new HashMap<BytesRef, LogPtr>();

	    	if (LOG.isDebugEnabled()) {
	    		LOG.debug("preSoftCommit: prevMap=" + System.identityHashCode(mPrevMap) 
	    				+ " new map=" + System.identityHashCode(mMap));
	    	}
	    }
	}

	@Override
	public void postSoftCommit(CommitCommand cmd) throws ErrorException {
	    synchronized (this) {
	        // We can clear out all old maps now that a new searcher has been opened.
	        // This currently only works since DUH2 synchronizes around preCommit to avoid
	        // it being called in the middle of a preSoftCommit, postSoftCommit sequence.
	        // If this DUH2 synchronization were to be removed, preSoftCommit should
	        // record what old maps were created and only remove those.

	        if (LOG.isDebugEnabled()) {
	        	LOG.debug("postSoftCommit: disposing of prevMap=" 
	        			+ System.identityHashCode(mPrevMap) + ", prevMap2=" 
	        			+ System.identityHashCode(mPrevMap2));
	        }
	        
	        clearOldMaps();
	    }
	}

	@Override
	public void finish(SyncLevel syncLevel) throws ErrorException {
	    if (syncLevel == null) 
	        syncLevel = mDefaultSyncLevel;
	      
	    if (syncLevel == SyncLevel.NONE) 
	        return;

	    TransactionLog currLog;
	    synchronized (this) {
	        currLog = mTransLog;
	        if (currLog == null) return;
	        currLog.increaseRef();
	    }

	    try {
	        currLog.finish(syncLevel);
	    } finally {
	        currLog.decreaseRef();
	    }
	}

	@Override
	public void close(boolean committed) throws ErrorException {
	    synchronized (this) {
	        try {
	        	mRecoveryExecutor.shutdownNow();
	        } catch (Exception e) {
	        	if (LOG.isErrorEnabled())
	        		LOG.error(e.toString(), e);
	        }

	        // Don't delete the old tlogs, we want to be able to replay from them and retrieve old versions

	        doClose(mPrevLog, committed);
	        doClose(mTransLog, committed);

	        for (TransactionLog log : mLogs) {
	        	if (log == mPrevLog || log == mTransLog) continue;
	          
	        	log.setDeleteOnClose(false);
	        	log.decreaseRef();
	          	log.forceClose();
	        }
	    }
	}

	public Future<RecoveryInfo> recoverFromLog() throws ErrorException {
	    mRecoveryInfo = new RecoveryInfo();

	    List<TransactionLog> recoverLogs = new ArrayList<TransactionLog>(1);
	    
	    for (TransactionLog ll : mNewestLogsOnStartup) {
	    	if (!ll.try_increaseRef()) continue;

	    	try {
	    		if (ll.endsWithCommit()) {
	    			ll.decreaseRef();
	    			continue;
	    		}
	    	} catch (IOException e) {
	    		if (LOG.isErrorEnabled())
	    			LOG.error("Error inspecting tlog " + ll);
	    		
	    		ll.decreaseRef();
	    		continue;
	    	}

	    	recoverLogs.add(ll);
	    }

	    if (recoverLogs.isEmpty()) 
	    	return null;

	    ExecutorCompletionService<RecoveryInfo> cs = 
	    		new ExecutorCompletionService<RecoveryInfo>(mRecoveryExecutor);
	    
	    LogReplayer replayer = new LogReplayer(recoverLogs, false);
	    mVersionInfo.blockUpdates();
	    
	    try {
	    	mState = State.REPLAYING;
	    } finally {
	    	mVersionInfo.unblockUpdates();
	    }

	    // At this point, we are guaranteed that any new updates coming in 
	    // will see the state as "replaying"

	    return cs.submit(replayer, mRecoveryInfo);
	}

  	private void ensureLog() throws ErrorException {
  		if (mTransLog == null) {
  			String newLogName = String.format(Locale.ROOT, 
  					LOG_FILENAME_PATTERN, TLOG_NAME, mId);
  			mTransLog = new TransactionLog(
  					new File(mLogDir, newLogName), mGlobalStrings);
  		}
  	}

  	private void doClose(TransactionLog theLog, boolean writeCommit) 
  			throws ErrorException {
  		if (theLog != null) {
  			if (writeCommit) {
  				// record a commit
  				if (LOG.isInfoEnabled()) {
  					LOG.info("Recording current closed for " + mUpdateIndexer.getSearchCore() 
  							+ " log=" + theLog);
  				}
  				
  				CommitCommand cmd = new CommitCommand(
  						mUpdateIndexer.getSearchCore().createLocalRequest(null, 
  							new ModifiableParams()), false);
  				
  				theLog.writeCommit(cmd, mOperationFlags);
  			}

  			theLog.setDeleteOnClose(false);
  			theLog.decreaseRef();
  			theLog.forceClose();
  		}
  	}
	
	public Object lookup(BytesRef indexedId) throws ErrorException {
	    LogPtr entry;
	    TransactionLog lookupLog;

	    synchronized (this) {
	    	entry = mMap.get(indexedId);
	    	lookupLog = mTransLog; // something found in "map" will always be in "tlog"
	      
	    	if (entry == null && mPrevMap != null) {
	    		entry = mPrevMap.get(indexedId);
	    		// something found in prevMap will always be found in preMapLog 
	    		// (which could be tlog or prevTlog)
	    		lookupLog = mPrevMapLog;
	    	}
	    	
	    	if (entry == null && mPrevMap2 != null) {
	    		entry = mPrevMap2.get(indexedId);
	    		// something found in prevMap2 will always be found in preMapLog2 
	    		// (which could be tlog or prevTlog)
	    		lookupLog = mPrevMapLog2;
	    	}

	    	if (entry == null) 
	    		return null;
	      
	    	lookupLog.increaseRef();
	    }

	    try {
	    	// now do the lookup outside of the sync block for concurrency
	    	return lookupLog.lookup(entry.mPointer);
	    } finally {
	    	lookupLog.decreaseRef();
	    }
	}
	
	private void newMap() {
	    mPrevMap2 = mPrevMap;
	    mPrevMapLog2 = mPrevMapLog;

	    mPrevMap = mMap;
	    mPrevMapLog = mTransLog;

	    mMap = new HashMap<BytesRef, LogPtr>();
	}

	private void clearOldMaps() {
	    mPrevMap = null;
	    mPrevMap2 = null;
	}
	
	private void trackDeleteByQuery(String q, long version) {
		version = Math.abs(version);
	    DBQ dbq = new DBQ(q, version);

	    synchronized (this) {
	    	if (mDeleteByQueries.isEmpty() || mDeleteByQueries.getFirst().getVersion() < version) {
	    		// common non-reordered case
	    		mDeleteByQueries.addFirst(dbq);
	    		
	    	} else {
	    		// find correct insertion point
	    		ListIterator<DBQ> iter = mDeleteByQueries.listIterator();
	    		// we already checked the first element in the previous "if" clause
	    		iter.next(); 
	    		
	    		while (iter.hasNext()) {
	    			DBQ oldDBQ = iter.next();
	    			if (oldDBQ.getVersion() < version) {
	    				iter.previous();
	    				break;
	    				
	    			} else if (oldDBQ.getVersion() == version && 
	    					oldDBQ.getQueryString().equals(q)) {
	    				// a duplicate
	    				return;
	    			}
	    		}
	    		
	    		// this also handles the case of adding at the end when hasNext() == false
	    		iter.add(dbq); 
	    	}

	    	if (mDeleteByQueries.size() > mNumDeletesByQueryToKeep) 
	    		mDeleteByQueries.removeLast();
	    }
	}
	
  	public static String[] getLogList(File directory) {
  		final String prefix = TLOG_NAME+'.';
  		String[] names = directory.list(new FilenameFilter() {
  				@Override
	  			public boolean accept(File dir, String name) {
	  				return name.startsWith(prefix);
	  			}
	  		});
  		Arrays.sort(names);
  		return names;
  	}

  	public long getLastLogId() {
  		if (mId != -1) return mId;
  		if (mLogFiles.length == 0) return -1;
  		
  		String last = mLogFiles[mLogFiles.length-1];
  		return Long.parseLong(last.substring(TLOG_NAME.length()+1));
  	}
	
	/** 
	 * Takes over ownership of the log, keeping it until no longer needed
	 * and then decrementing it's reference and dropping it.
	 */
  	private void addOldLog(TransactionLog oldLog, boolean removeOld) 
  			throws ErrorException {
  		if (oldLog == null) return;

  		mNumOldRecords += oldLog.getNumRecords();
  		int currRecords = mNumOldRecords;

  		if (oldLog != mTransLog && mTransLog != null) 
  			currRecords += mTransLog.getNumRecords();
   
  		while (removeOld && mLogs.size() > 0) {
  			TransactionLog log = mLogs.peekLast();
  			int nrec = log.getNumRecords();
  			
  			// remove oldest log if we don't need it to keep at least numRecordsToKeep, or if
  			// we already have the limit of 10 log files.
  			if (currRecords - nrec >= mNumRecordsToKeep || mLogs.size() >= 10) {
  				currRecords -= nrec;
  				mNumOldRecords -= nrec;
  				// dereference so it will be deleted when no longer in use
  				mLogs.removeLast().decreaseRef(); 
  				continue;
  			}

  			break;
  		}

  		// don't incref... we are taking ownership from the caller.
  		mLogs.addFirst(oldLog);
  	}
	
	public RecentUpdates getRecentUpdates() throws ErrorException {
		Deque<TransactionLog> logList;
		
	    synchronized (this) {
	    	logList = new LinkedList<TransactionLog>(mLogs);
	    	
	    	for (TransactionLog log : logList) {
	    		log.increaseRef();
	    	}
	    	
	    	if (mPrevLog != null) {
	    		mPrevLog.increaseRef();
	    		logList.addFirst(mPrevLog);
	    	}
	    	
	    	if (mTransLog != null) {
	    		mTransLog.increaseRef();
	    		logList.addFirst(mTransLog);
	    	}
	    }

	    // TODO: what if I hand out a list of updates, then do an update, 
	    // then hand out another list (and
	    // one of the updates I originally handed out fell off the list).  
	    // Over-request?
	    RecentUpdates recentUpdates = new RecentUpdates();
	    recentUpdates.mLogList = logList;
	    recentUpdates.update();

	    return recentUpdates;
	}
	
	static class Update {
		private TransactionLog mLog;
	    private long mVersion;
	    private long mPointer;
	}

	static class DeleteUpdate {
	    private long mVersion;
	    private byte[] mId;

	    public DeleteUpdate(long version, byte[] id) {
	    	mVersion = version;
	    	mId = id;
	    }
	}
  
	public static class RecoveryInfo {
	    private long mPositionOfStart;

	    private int mAdds;
	    private int mDeletes;
	    private int mDeleteByQuery;
	    private int mErrors;

	    @SuppressWarnings("unused")
		private boolean mFailed;

	    @Override
	    public String toString() {
	    	return "RecoveryInfo{adds=" + mAdds + ",deletes=" + mDeletes 
	    			+ ",deleteByQuery=" + mDeleteByQuery + ",errors=" + mErrors 
	    			+ ",positionOfStart=" + mPositionOfStart + "}";
	    }
	}
	
	public class RecentUpdates {
		private Deque<TransactionLog> mLogList; // newest first
		private List<List<Update>> mUpdateList;
		private Map<Long, Update> mUpdates;
		private List<Update> mDeleteByQueryList;
		private List<DeleteUpdate> mDeleteList;
		private int mLatestOperation;

		public List<Long> getVersions(int n) {
			List<Long> ret = new ArrayList<Long>(n);
      
			for (List<Update> singleList : mUpdateList) {
				for (Update ptr : singleList) {
					ret.add(ptr.mVersion);
					if (--n <= 0) 
						return ret;
				}
			}
      
			return ret;
		}
    
		public Object lookup(long version) throws ErrorException {
			Update update = mUpdates.get(version);
			if (update == null) 
				return null;

			return update.mLog.lookup(update.mPointer);
		}

		/** Returns the list of deleteByQueries that happened after the given version */
		public List<Object> getDeleteByQuery(long afterVersion) throws ErrorException {
			List<Object> result = new ArrayList<Object>(mDeleteByQueryList.size());
			
			for (Update update : mDeleteByQueryList) {
				if (Math.abs(update.mVersion) > afterVersion) {
					Object dbq = update.mLog.lookup(update.mPointer);
					result.add(dbq);
				}
			}
			
			return result;
		}

		public int getLatestOperation() {
			return mLatestOperation;
		}

		private void update() throws ErrorException {
			int numUpdates = 0;
			
			mUpdateList = new ArrayList<List<Update>>(mLogList.size());
			mDeleteByQueryList = new ArrayList<Update>();
			mDeleteList = new ArrayList<DeleteUpdate>();
			mUpdates = new HashMap<Long,Update>(mNumRecordsToKeep);

			for (TransactionLog oldLog : mLogList) {
				List<Update> updatesForLog = new ArrayList<Update>();
				TransactionLog.ReverseReader reader = null;
				
				try {
					reader = oldLog.getReverseReader();

					while (numUpdates < mNumRecordsToKeep) {
						Object o = reader.next();
						if (o == null) break;
						
						try {
							// should currently be a List<Oper,Ver,Doc/Id>
							List<?> entry = (List<?>)o;

							// TODO: refactor this out so we get common error handling
							int opAndFlags = (Integer)entry.get(0);
							if (mLatestOperation == 0) 
								mLatestOperation = opAndFlags;
              
							int oper = opAndFlags & UpdateLog.OPERATION_MASK;
							long version = (Long) entry.get(1);

							switch (oper) {
							case UpdateLog.ADD:
							case UpdateLog.DELETE:
							case UpdateLog.DELETE_BY_QUERY:
								Update update = new Update();
								update.mLog = oldLog;
								update.mPointer = reader.position();
								update.mVersion = version;

								updatesForLog.add(update);
								mUpdates.put(version, update);
                  
								if (oper == UpdateLog.DELETE_BY_QUERY) {
									mDeleteByQueryList.add(update);
								} else if (oper == UpdateLog.DELETE) {
									mDeleteList.add(new DeleteUpdate(version, (byte[])entry.get(2)));
								}
								break;

							case UpdateLog.COMMIT:
								break;
							default:
								throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
										"Unknown Operation! " + oper);
							}
						} catch (ClassCastException cl) {
							if (LOG.isWarnEnabled())
								LOG.warn("Unexpected log entry or corrupt log. Entry=" + o, cl);
							
							// would be caused by a corrupt transaction log
						} catch (Exception ex) {
							if (LOG.isWarnEnabled())
								LOG.warn("Exception reverse reading log", ex);
							
							break;
						}
					}

				//} catch (IOException e) {
				//	// failure to read a log record isn't fatal
				//	if (LOG.isErrorEnabled())
				//		LOG.error("Exception reading versions from log", e);
					
				} finally {
					if (reader != null) 
						reader.close();
				}

				mUpdateList.add(updatesForLog);
			}
		}
    
		public void close() throws ErrorException {
			for (TransactionLog log : mLogList) {
				log.decreaseRef();
			}
		}
	}
	
	/**
	 * Internal param used to specify the current phase of a distributed update, 
	 * not intended for use by clients.  Any non-blank value can be used to 
	 * indicate to the <code>UpdateRequestProcessorChain</code> that factories 
	 * prior to the <code>DistributingUpdateProcessorFactory</code> can be skipped.
	 * Implementations of this interface may use the non-blank values any way 
	 * they wish.
	 */
	public static final String DISTRIB_UPDATE_PARAM = "update.distrib";
	
	/**
	 * Values this processor supports for the <code>DISTRIB_UPDATE_PARAM</code>.
	 * This is an implementation detail exposed solely for tests.
	 * 
	 * @see DistributingUpdateProcessorFactory#DISTRIB_UPDATE_PARAM
	 */
	public static enum DistribPhase {
	    NONE, TOLEADER, FROMLEADER;
	}
	  
	public static final String LOG_REPLAY = "log_replay";
	
	public static Runnable sTestingLogReplayHook;  // called before each log read
	public static Runnable sTestingLogReplayFinishHook;  // called when log replay has finished
	
	private class LogReplayer implements Runnable {
		
		private List<TransactionLog> mTranslogs;
		private TransactionLog.LogReader mLogReader;
		
		private ISearchRequest mRequest;
		private ISearchResponse mResponse;
		
		private boolean mActiveLog;
		// state where we lock out other updates and finish those updates that snuck in before we locked
		private boolean mFinishing = false; 

		public LogReplayer(List<TransactionLog> translogs, boolean activeLog) {
			mTranslogs = translogs;
			mActiveLog = activeLog;
		}

		@Override
		public void run() {
			try {
				ModifiableParams params = new ModifiableParams();
				params.set(DISTRIB_UPDATE_PARAM, DistribPhase.FROMLEADER.toString());
				params.set(LOG_REPLAY, "true");
				
				mRequest = mUpdateIndexer.getSearchCore().createLocalRequest(null, params);
				mResponse = mUpdateIndexer.getSearchCore().createLocalResponse(null, mRequest);
				
				// setting request info will help logging
				SearchRequestInfo.setRequestInfo(new SearchRequestInfo(mRequest, mResponse));
				
				for (TransactionLog translog : mTranslogs) {
					doReplay(translog);
				}
				
			} catch (ErrorException e) {
				if (LOG.isErrorEnabled())
					LOG.error(e.toString(), e);
				
				if (e.getCode() == ErrorException.ErrorCode.SERVICE_UNAVAILABLE.getCode()) 
					mRecoveryInfo.mFailed = true;
				else 
					mRecoveryInfo.mErrors ++;
				
			} catch (Throwable e) {
				if (LOG.isErrorEnabled())
					LOG.error(e.toString(), e);
				
				mRecoveryInfo.mErrors ++;
				
			} finally {
				// change the state while updates are still blocked to prevent races
				mState = State.ACTIVE;
				if (mFinishing) 
					mVersionInfo.unblockUpdates();
			}

			if (LOG.isWarnEnabled())
				LOG.warn("Log replay finished. recoveryInfo=" + mRecoveryInfo);

			if (sTestingLogReplayFinishHook != null) 
				sTestingLogReplayFinishHook.run();

			SearchRequestInfo.clearRequestInfo();
		}

		public void doReplay(TransactionLog translog) throws ErrorException {
			try {
				if (LOG.isWarnEnabled()) {
					LOG.warn("Starting log replay " + translog + " active=" + mActiveLog 
							+ " starting pos=" + mRecoveryInfo.mPositionOfStart);
				}
				
				mLogReader = translog.getReader(mRecoveryInfo.mPositionOfStart);

				// NOTE: we don't currently handle a core reload during recovery. 
				// This would cause the core to change underneath us.

				// TODO: use the standard request factory?  
				// We won't get any custom configuration instantiating this way.
				//RunUpdateProcessorFactory runFac = new RunUpdateProcessorFactory();
				//DistributedUpdateProcessorFactory magicFac = new DistributedUpdateProcessorFactory();
				
				//runFac.init(new NamedList());
				//magicFac.init(new NamedList());

				//UpdateRequestProcessor proc = magicFac.getInstance(mRequest, mResponse, 
				//		runFac.getInstance(mRequest, mResponse, null));

				long commitVersion = 0;
				int operationAndFlags = 0;

				for (;;) {
					Object o = null;
					if (cancelApplyBufferUpdate) break;
					
					try {
						if (sTestingLogReplayHook != null) 
							sTestingLogReplayHook.run();
						
						o = null;
						o = mLogReader.next();
						
						if (o == null && mActiveLog) {
							if (!mFinishing) {
								// block to prevent new adds, but don't immediately unlock since
								// we could be starved from ever completing recovery.  Only unlock
								// after we've finished this recovery.
								// NOTE: our own updates won't be blocked since the thread holding 
								// a write lock can lock a read lock.
								mVersionInfo.blockUpdates();
								mFinishing = true;
								o = mLogReader.next();
								
							} else {
								// we had previously blocked updates, so this "null" from the log is final.

								// Wait until our final commit to change the state and unlock.
								// This is only so no new updates are written to the current log file, and is
								// only an issue if we crash before the commit (and we are paying attention
								// to incomplete log files).
								//
								// versionInfo.unblockUpdates();
							}
						}
					} catch (Throwable e) {
						if (LOG.isErrorEnabled())
							LOG.error(e.toString(), e);
					}

					if (o == null) break;

					try {
						// should currently be a List<Oper,Ver,Doc/Id>
						List<?> entry = (List<?>)o;

						operationAndFlags = (Integer)entry.get(0);
						int oper = operationAndFlags & OPERATION_MASK;
						long version = (Long) entry.get(1);

						switch (oper) {
						case UpdateLog.ADD: {
							mRecoveryInfo.mAdds ++;
							
							// byte[] idBytes = (byte[]) entry.get(2);
							InputDocument sdoc = (InputDocument)entry.get(entry.size()-1);
							
							AddCommand cmd = new AddCommand(mRequest, mUpdateIndexer.getSchema());
							// cmd.setIndexedId(new BytesRef(idBytes));
							cmd.setInputDocument(sdoc);
							cmd.setVersion(version);
							cmd.setFlags(UpdateCommand.REPLAY | UpdateCommand.IGNORE_AUTOCOMMIT);
							
							if (LOG.isDebugEnabled()) 
								LOG.debug("add " +  cmd);

							//proc.processAdd(cmd);
							break;
						}
						
						case UpdateLog.DELETE: {
							mRecoveryInfo.mDeletes ++;
							
							byte[] idBytes = (byte[]) entry.get(2);
							
							DeleteCommand cmd = new DeleteCommand(mRequest, mUpdateIndexer.getSchema());
							cmd.setIndexedId(new BytesRef(idBytes));
							cmd.setVersion(version);
							cmd.setFlags(UpdateCommand.REPLAY | UpdateCommand.IGNORE_AUTOCOMMIT);
							
							if (LOG.isDebugEnabled()) 
								LOG.debug("delete " +  cmd);
							
							//proc.processDelete(cmd);
							break;
						}

						case UpdateLog.DELETE_BY_QUERY: {
							mRecoveryInfo.mDeleteByQuery ++;
							
							String query = (String)entry.get(2);
							
							DeleteCommand cmd = new DeleteCommand(mRequest, mUpdateIndexer.getSchema());
							cmd.setQueryString(query);
							cmd.setVersion(version);
							cmd.setFlags(UpdateCommand.REPLAY | UpdateCommand.IGNORE_AUTOCOMMIT);
							
							if (LOG.isDebugEnabled()) 
								LOG.debug("deleteByQuery " +  cmd);
							
							//proc.processDelete(cmd);
							break;
						}

						case UpdateLog.COMMIT: {
							commitVersion = version;
							break;
						}

						default:
							throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
									"Unknown Operation! " + oper);
						}

						if (mResponse.getException() != null) {
							if (LOG.isDebugEnabled())
								LOG.error("REPLAY_ERR: Exception replaying log", mResponse.getException());
							
							throw mResponse.getException();
						}
						
					} catch (IOException ex) {
						if (LOG.isWarnEnabled())
							LOG.warn("REYPLAY_ERR: IOException reading log", ex);
						
						mRecoveryInfo.mErrors ++;
						// could be caused by an incomplete flush if recovering from log
						
					} catch (ClassCastException cl) {
						if (LOG.isWarnEnabled())
							LOG.warn("REPLAY_ERR: Unexpected log entry or corrupt log.  Entry=" + o, cl);
						
						mRecoveryInfo.mErrors ++;
						// would be caused by a corrupt transaction log
						
					} catch (ErrorException ex) {
						if (ex.getCode() == ErrorException.ErrorCode.SERVICE_UNAVAILABLE.getCode()) 
							throw ex;
            
						if (LOG.isWarnEnabled())
							LOG.warn("REYPLAY_ERR: IOException reading log", ex);
						
						mRecoveryInfo.mErrors ++;
						// could be caused by an incomplete flush if recovering from log
						
					} catch (Throwable ex) {
						if (LOG.isWarnEnabled())
							LOG.warn("REPLAY_ERR: Exception replaying log", ex);
						
						mRecoveryInfo.mErrors ++;
						// something wrong with the request?
					}
				}

				CommitCommand cmd = new CommitCommand(mRequest, false);
				cmd.setVersion(commitVersion);
				cmd.setSoftCommit(false);
				cmd.setWaitSearcher(true);
				cmd.setFlags(UpdateCommand.REPLAY);
				
				try {
					if (LOG.isDebugEnabled()) 
						LOG.debug("commit " +  cmd);
					
					// this should cause a commit to be added to the incomplete log 
					// and avoid it being replayed again after a restart.
					mUpdateIndexer.commit(cmd); 
					
				} catch (Throwable ex) {
					if (LOG.isErrorEnabled())
						LOG.error("Replay exception: final commit.", ex);
					
					mRecoveryInfo.mErrors ++;
				}

				if (!mActiveLog) {
					// if we are replaying an old tlog file, we need to add a commit to the end
					// so we don't replay it again if we restart right after.

					// if the last operation we replayed had FLAG_GAP set, we want to use that 
					// again so we don't lose it as the flag on the last operation.
					translog.writeCommit(cmd, mOperationFlags | (operationAndFlags & ~OPERATION_MASK));
				}

				/*try {
					//proc.finish();
				} catch (IOException ex) {
					if (LOG.isErrorEnabled())
						LOG.error("Replay exception: finish()", ex);
					
					mRecoveryInfo.mErrors ++;
				}*/

			} finally {
				if (mLogReader != null) mLogReader.close();
				translog.decreaseRef();
			}
		}
	}
	
	public static void deleteFile(File file) {
	    boolean success = false;
	    try {
	    	success = file.delete();
	    	if (!success) 
	    		LOG.error("Error deleting file: " + file);
	    	
	    } catch (Exception e) {
	    	LOG.error("Error deleting file: " + file, e);
	    }

	    if (!success) {
	    	try {
	    		file.deleteOnExit();
	    	} catch (Exception e) {
	    		LOG.error("Error deleting file on exit: " + file, e);
	    	}
	    }
	}
	
}
