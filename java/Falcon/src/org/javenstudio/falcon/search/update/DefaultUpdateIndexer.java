package org.javenstudio.falcon.search.update;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.document.Document;
import org.javenstudio.common.indexdb.index.term.Term;
import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ModifiableParams;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.util.PluginHolder;
import org.javenstudio.falcon.util.PluginInfo;
import org.javenstudio.hornet.index.segment.DirectoryReader;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.search.query.BooleanClause;
import org.javenstudio.hornet.search.query.BooleanQuery;
import org.javenstudio.hornet.search.query.MatchAllDocsQuery;
import org.javenstudio.hornet.search.query.TermQuery;
import org.javenstudio.falcon.search.DefaultCoreState;
import org.javenstudio.falcon.search.SearchWriterCloser;
import org.javenstudio.falcon.search.SearcherRef;
import org.javenstudio.falcon.search.SearchWriter;
import org.javenstudio.falcon.search.SearchWriterRef;
import org.javenstudio.falcon.search.ISearchCore;
import org.javenstudio.falcon.search.SearchCoreState;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.SearchRequestInfo;
import org.javenstudio.falcon.search.ISearchResponse;
import org.javenstudio.falcon.search.filter.ValueSourceRangeFilter;
import org.javenstudio.falcon.search.query.FunctionRangeQuery;
import org.javenstudio.falcon.search.query.QueryBuilder;
import org.javenstudio.falcon.search.query.QueryUtils;
import org.javenstudio.falcon.search.schema.SchemaField;

/**
 *  TODO: add soft commitWithin support
 * 
 * <code>DirectUpdateHandler</code> implements an UpdateHandler where documents are added
 * directly to the main index as opposed to adding to a separate smaller index.
 * 
 */
public class DefaultUpdateIndexer extends UpdateIndexer 
		implements SearchWriterCloser {
	private static Logger LOG = Logger.getLogger(DefaultUpdateIndexer.class);
	
	private final Lock mCommitLock = new ReentrantLock();
	private final SearchCoreState mCoreState;
	
	// tracks when auto-commit should occur
	private final CommitTracker mCommitTracker;
	private final CommitTracker mSoftCommitTracker;
	
	// stats
	private AtomicLong mAddCommands = new AtomicLong();
	private AtomicLong mAddCommandsCumulative = new AtomicLong();
	private AtomicLong mDeleteByIdCommands= new AtomicLong();
	private AtomicLong mDeleteByIdCommandsCumulative= new AtomicLong();
	private AtomicLong mDeleteByQueryCommands= new AtomicLong();
	private AtomicLong mDeleteByQueryCommandsCumulative= new AtomicLong();
	private AtomicLong mExpungeDeleteCommands = new AtomicLong();
	private AtomicLong mMergeIndexesCommands = new AtomicLong();
	private AtomicLong mCommitCommands= new AtomicLong();
	private AtomicLong mOptimizeCommands= new AtomicLong();
	private AtomicLong mRollbackCommands= new AtomicLong();
	private AtomicLong mNumDocsPending= new AtomicLong();
	private AtomicLong mNumErrors = new AtomicLong();
	private AtomicLong mNumErrorsCumulative = new AtomicLong();

	public DefaultUpdateIndexer(PluginHolder core) 
			throws ErrorException {
		this((ISearchCore)core);
	}
	
	public DefaultUpdateIndexer(ISearchCore core) throws ErrorException {
		this(core, (UpdateIndexer)null);
	}
  
	public DefaultUpdateIndexer(ISearchCore core, UpdateIndexer updateHandler) 
			throws ErrorException {
		super(core);
		
		if (LOG.isDebugEnabled()) { 
			LOG.debug("Param updateHandler is " + ((updateHandler != null) ? 
					updateHandler.toString() : "null"));
		}
		
		if (updateHandler != null && updateHandler instanceof DefaultUpdateIndexer) {
			mCoreState = ((DefaultUpdateIndexer) updateHandler).mCoreState;
			
		} else {
			// the impl has changed, so we cannot use the old state - decref it
			if (updateHandler != null)
				updateHandler.decreaseRef();
			
			mCoreState = new DefaultCoreState(core, core.getDirectoryFactory());
		}
    
		UpdateIndexerConfig indexerConf = core.getSearchConfig().getIndexerConfig();
		
		int docsUpperBound = indexerConf.getAutoCommitMaxDocs(); 
		int timeUpperBound = indexerConf.getAutoCommitMaxTime(); 
		
		mCommitTracker = new CommitTracker(core, "Hard", 
				docsUpperBound, timeUpperBound, indexerConf.isOpenSearcher(), false);
    
		int softCommitDocsUpperBound = indexerConf.getAutoSoftCommitMaxDocs(); 
		int softCommitTimeUpperBound = indexerConf.getAutoSoftCommitMaxTime(); 
		
		mSoftCommitTracker = new CommitTracker(core, "Soft", 
				softCommitDocsUpperBound, softCommitTimeUpperBound, 
				indexerConf.isOpenSearcher(), true);
    
		mUpdateLog = (updateHandler != null) ? updateHandler.getUpdateLog() : null;
		if (mUpdateLog != null) 
			mUpdateLog.init(this);
	}

	@Override
  	protected void initUpdateLog() throws ErrorException {
  		PluginInfo info = mCore.getSearchConfig().getPluginInfo(UpdateLog.class.getName());
  		if (info != null && info.isEnabled()) {
  			mUpdateLog = new DefaultUpdateLog();
  			mUpdateLog.init(info);
  			mUpdateLog.init(this);
  		}
  	}
	
	private void deleteAll() throws ErrorException {
		if (LOG.isInfoEnabled())
			LOG.info("REMOVING ALL DOCUMENTS FROM INDEX");
		
		SearchWriterRef iw = mCoreState.getIndexWriterRef();
		try {
			iw.get().deleteAll();
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		} finally {
			iw.decreaseRef();
		}
	}

	protected void rollbackWriter() throws ErrorException {
		mNumDocsPending.set(0);
		mCoreState.rollbackIndexWriter();
	}

	@Override
	public int addDoc(AddCommand cmd) throws ErrorException {
		//if (LOG.isDebugEnabled())
		//	LOG.debug("addDoc: " + cmd);
		
		SearchWriterRef iw = mCoreState.getIndexWriterRef();
		int rc = -1;
		
		try {
			SearchWriter writer = iw.get();
			
			mAddCommands.incrementAndGet();
			mAddCommandsCumulative.incrementAndGet();
      
			// if there is no ID field, don't overwrite
			if (mIdField == null) 
				cmd.setOverwrite(false);
      
			try {
				if (cmd.isOverwrite()) {
					// Check for delete by query commands newer (i.e. reordered). This
					// should always be null on a leader
					List<UpdateLog.DBQ> deletesAfter = null;
					if (mUpdateLog != null && cmd.getVersion() > 0) 
						deletesAfter = mUpdateLog.getDBQNewer(cmd.getVersion());
					
					if (deletesAfter != null) {
						if (LOG.isInfoEnabled())
							LOG.info("addDoc: overwrited, reordered DBQs detected: " + deletesAfter);
						
						List<IQuery> dbqList = new ArrayList<IQuery>(deletesAfter.size());
						for (UpdateLog.DBQ dbq : deletesAfter) {
							try {
								DeleteCommand tmpDel = new DeleteCommand(cmd.getRequest(), getSchema());
								
								tmpDel.setQueryString(dbq.getQueryString());
								tmpDel.setVersion(-dbq.getVersion());
								
								dbqList.add(getQuery(tmpDel));
							} catch (Exception e) {
								if (LOG.isErrorEnabled())
									LOG.error("Exception parsing reordered query : " + dbq, e);
							}
						}
            
						addAndDelete(cmd, dbqList);
						
					} else {
						//if (LOG.isDebugEnabled())
						//	LOG.debug("addDoc: overwrited, normal update");
						
						// normal update
						Term idTerm = new Term(mIdField.getName(), cmd.getIndexedId());
						
						Term updateTerm;
						boolean del = false;
						
						if (cmd.getUpdateTerm() == null) {
							updateTerm = idTerm;
						} else {
							del = true;
							updateTerm = cmd.getUpdateTerm();
						}
            
						Document indexDoc = cmd.getIndexDocument();
						try {
							writer.updateDocument(updateTerm, indexDoc, getSchema().getAnalyzer());
						} catch (IOException ex) { 
							throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
						}
            
						if (del) { // ensure id remains unique
							BooleanQuery bq = new BooleanQuery();
							
							bq.add(new BooleanClause(new TermQuery(updateTerm),
									BooleanClause.Occur.MUST_NOT));
							bq.add(new BooleanClause(new TermQuery(idTerm), 
									BooleanClause.Occur.MUST));
							
							try {
								writer.deleteDocuments(bq);
							} catch (IOException ex) { 
								throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
							}
						}
            
						// Add to the transaction log *after* successfully adding to the
						// index, if there was no error.
						// This ordering ensures that if we log it, it's definitely been
						// added to the the index.
						// This also ensures that if a commit sneaks in-between, that we
						// know everything in a particular
						// log version was definitely committed.
						if (mUpdateLog != null) 
							mUpdateLog.add(cmd);
					}
          
				} else {
					if (LOG.isDebugEnabled())
						LOG.debug("addDoc: duplicated update");
					
					try {
						// allow duplicates
						writer.addDocument(cmd.getIndexDocument(), getSchema().getAnalyzer());
					} catch (IOException ex) { 
						throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
					}
					
					if (mUpdateLog != null) 
						mUpdateLog.add(cmd);
				}
        
				if ((cmd.getFlags() & UpdateCommand.IGNORE_AUTOCOMMIT) == 0) {
					mCommitTracker.addedDocument(-1);
					mSoftCommitTracker.addedDocument(cmd.getCommitWithin());
				}
        
				rc = 1;
			} finally {
				if (rc != 1) {
					mNumErrors.incrementAndGet();
					mNumErrorsCumulative.incrementAndGet();
				} else {
					mNumDocsPending.incrementAndGet();
				}
			}
      
		} finally {
			iw.decreaseRef();
		}
    
		return rc;
	}
  
	private void updateDeleteTrackers(DeleteCommand cmd) {
		if ((cmd.getFlags() & UpdateCommand.IGNORE_AUTOCOMMIT) == 0) {
			mSoftCommitTracker.deletedDocument(cmd.getCommitWithin());
      
			if (mCommitTracker.getTimeUpperBound() > 0) 
				mCommitTracker.scheduleCommitWithin(mCommitTracker.getTimeUpperBound());
      
			if (mSoftCommitTracker.getTimeUpperBound() > 0) 
				mSoftCommitTracker.scheduleCommitWithin(mSoftCommitTracker.getTimeUpperBound());
		}
	}

	/** 
	 * we don't return the number of docs deleted because it's not always possible 
	 * to quickly know that info.
	 */
	@Override
	public void delete(DeleteCommand cmd) throws ErrorException {
		if (LOG.isDebugEnabled())
			LOG.debug("delete: " + cmd);
		
		mDeleteByIdCommands.incrementAndGet();
		mDeleteByIdCommandsCumulative.incrementAndGet();

		Term deleteTerm = new Term(mIdField.getName(), cmd.getIndexedId());
		SearchWriterRef iw = mCoreState.getIndexWriterRef();
		try {
			iw.get().deleteDocuments(deleteTerm);
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		} finally {
			iw.decreaseRef();
		}

		if (mUpdateLog != null) 
			mUpdateLog.delete(cmd);

		updateDeleteTrackers(cmd);
	}

	public void clearIndex() throws ErrorException {
		deleteAll();
		if (mUpdateLog != null) 
			mUpdateLog.deleteAll();
	}

	private IQuery getQuery(DeleteCommand cmd) throws ErrorException {
		IQuery q = null;
		try {
			// move this higher in the stack?
			QueryBuilder parser = getSearchCore().getQueryFactory().getQueryBuilder(
					cmd.getQueryString(), "default", cmd.getRequest());
			
			q = parser.getQuery();
			q = QueryUtils.makeQueryable(q);

			// Make sure not to delete newer versions
			if (mUpdateLog != null && cmd.getVersion() != 0 && cmd.getVersion() != -Long.MAX_VALUE) {
				BooleanQuery bq = new BooleanQuery();
				bq.add(q, BooleanClause.Occur.MUST);
				
				SchemaField sf = mUpdateLog.getVersionInfo().getVersionField();
				ValueSource vs = sf.getType().getValueSource(sf, null);
				ValueSourceRangeFilter filt = new ValueSourceRangeFilter(vs, null, 
						Long.toString(Math.abs(cmd.getVersion())), true, true);
				
				FunctionRangeQuery range = new FunctionRangeQuery(filt);
				bq.add(range, BooleanClause.Occur.MUST);
				
				q = bq;
			}

			return q;
		} catch (Exception e) {
			if (e instanceof ErrorException) 
				throw (ErrorException)e; 
			else 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, e);
		}
	}

	/** 
	 * we don't return the number of docs deleted because it's not always possible to 
	 * quickly know that info.
	 */
	@Override
 	public void deleteByQuery(DeleteCommand cmd) throws ErrorException {
		if (LOG.isDebugEnabled())
			LOG.debug("deleteByQuery: " + cmd);
		
		mDeleteByQueryCommands.incrementAndGet();
		mDeleteByQueryCommandsCumulative.incrementAndGet();
		
		boolean madeIt = false;
		try {
			IQuery q = getQuery(cmd);
			boolean delAll = (MatchAllDocsQuery.class == q.getClass());

			// currently for testing purposes. 
			// Do a delete of complete index w/o worrying about versions, don't log, 
			// clean up most state in update log, etc
			if (delAll && cmd.getVersion() == -Long.MAX_VALUE) {
				synchronized (mCoreState.getUpdateLock()) {
					deleteAll();
					mUpdateLog.deleteAll();
					return;
				}
			}

			//
			// synchronized to prevent deleteByQuery from running during the "open new searcher"
			// part of a commit.  DBQ needs to signal that a fresh reader will be needed for
			// a realtime view of the index.  When a new searcher is opened after a DBQ, that
			// flag can be cleared.  If those thing happen concurrently, it's not thread safe.
			//
			synchronized (mCoreState.getUpdateLock()) {
				if (delAll) {
					deleteAll();
					
				} else {
					SearchWriterRef iw = mCoreState.getIndexWriterRef();
					try {
						iw.get().deleteDocuments(q);
					} catch (IOException ex) { 
						throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
					} finally {
						iw.decreaseRef();
					}
				}

				if (mUpdateLog != null) 
					mUpdateLog.deleteByQuery(cmd);
			}

			madeIt = true;
			updateDeleteTrackers(cmd);

		} finally {
			if (!madeIt) {
				mNumErrors.incrementAndGet();
				mNumErrorsCumulative.incrementAndGet();
			}
		}
	}

	/** Add a document execute the deletes as atomically as possible */
	private void addAndDelete(AddCommand cmd, List<IQuery> dbqList) throws ErrorException {
		Document indexDoc = cmd.getIndexDocument();
		Term idTerm = new Term(mIdField.getName(), cmd.getIndexedId());
    
		// see comment in deleteByQuery
		synchronized (mCoreState.getUpdateLock()) {
			SearchWriterRef iw = mCoreState.getIndexWriterRef();
			try {
				SearchWriter writer = iw.get();
				writer.updateDocument(idTerm, indexDoc, getSchema().getAnalyzer());
        
				for (IQuery q : dbqList) {
					writer.deleteDocuments(q);
				}
			} catch (IOException ex) { 
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
			} finally {
				iw.decreaseRef();
			}
      
			if (mUpdateLog != null) 
				mUpdateLog.add(cmd, true);
		}
	}

	@Override
	public int mergeIndexes(MergeCommand cmd) throws ErrorException {
		if (LOG.isDebugEnabled())
			LOG.debug("mergeIndexes: " + cmd);
		
		mMergeIndexesCommands.incrementAndGet();
		
		if (LOG.isInfoEnabled())
			LOG.info("start mergeIndexes " + cmd);
    
		DirectoryReader[] readers = cmd.getReaders();
		int rc;
    
		if (readers != null && readers.length > 0) {
			SearchWriterRef iw = mCoreState.getIndexWriterRef();
			try {
				iw.get().addIndexes(readers);
			} catch (IOException ex) { 
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
			} finally {
				iw.decreaseRef();
			}
			
			rc = 1;
		} else {
			rc = 0;
		}
		
		if (LOG.isInfoEnabled())
			LOG.info("end mergeIndexes");

		// TODO: consider soft commit issues
		if (rc == 1 && mCommitTracker.getTimeUpperBound() > 0) {
			mCommitTracker.scheduleCommitWithin(
					mCommitTracker.getTimeUpperBound());
			
		} else if (rc == 1 && mSoftCommitTracker.getTimeUpperBound() > 0) {
			mSoftCommitTracker.scheduleCommitWithin(
					mSoftCommitTracker.getTimeUpperBound());
		}

		return rc;
	}

	public void prepareCommit(CommitCommand cmd) throws ErrorException {
		boolean error = true;
		try {
			if (LOG.isInfoEnabled())
				LOG.info("start prepareCommit " + cmd);
			
			SearchWriterRef iw = mCoreState.getIndexWriterRef();
			try {
				iw.get().prepareCommit();
			} catch (IOException ex) { 
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
			} finally {
				iw.decreaseRef();
			}

			if (LOG.isInfoEnabled())
				LOG.info("end prepareCommit");

			error = false;
		} finally {
			if (error) 
				mNumErrors.incrementAndGet();
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void commit(CommitCommand cmd) throws ErrorException {
		if (LOG.isDebugEnabled())
			LOG.debug("commit: " + cmd);
		
		if (cmd.isPrepareCommit()) {
			prepareCommit(cmd);
			return;
		}

		if (cmd.isOptimize()) {
			mOptimizeCommands.incrementAndGet();
			
		} else {
			mCommitCommands.incrementAndGet();
			if (cmd.isExpungeDeletes()) 
				mExpungeDeleteCommands.incrementAndGet();
		}

		Future[] waitSearcher = null;
		if (cmd.isWaitSearcher()) 
			waitSearcher = new Future[1];

		boolean error = true;
		try {
			// only allow one hard commit to proceed at once
			if (!cmd.isSoftCommit()) 
				mCommitLock.lock();

			if (LOG.isInfoEnabled())
				LOG.info("start commit " + cmd);

			// We must cancel pending commits *before* we actually execute the commit.
			if (cmd.isOpenSearcher()) {
				// we can cancel any pending soft commits if this commit will open a new searcher
				mSoftCommitTracker.cancelPendingCommit();
			}
			
			if (!cmd.isSoftCommit() && (cmd.isOpenSearcher() || !mCommitTracker.isOpenSearcher())) {
				// cancel a pending hard commit if this commit is of equal or greater "strength"...
				// If the autoCommit has openSearcher=true, then this commit must have openSearcher=true
				// to cancel.
				mCommitTracker.cancelPendingCommit();
			}

			SearchWriterRef iw = mCoreState.getIndexWriterRef();
			try {
				SearchWriter writer = iw.get();
				if (cmd.isOptimize()) {
					writer.forceMerge(cmd.getMaxOptimizeSegments());
				} else if (cmd.isExpungeDeletes()) {
					writer.forceMergeDeletes();
				}
        
				if (!cmd.isSoftCommit()) {
					synchronized (mCoreState.getUpdateLock()) { 
						// sync is currently needed to prevent preCommit
						// from being called between preSoft and
						// postSoft... see postSoft comments.
						if (mUpdateLog != null) 
							mUpdateLog.preCommit(cmd);
					}
          
					final Map<String,String> commitData = new HashMap<String,String>();
					commitData.put(SearchWriter.COMMIT_TIME_MSEC_KEY,
							String.valueOf(System.currentTimeMillis()));
					
					writer.commit(commitData);
					
					mNumDocsPending.set(0);
					callPostCommitCallbacks();
					
				} else {
					callPostSoftCommitCallbacks();
				}
			} catch (IOException ex) { 
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
			} finally {
				iw.decreaseRef();
			}

			if (cmd.isOptimize()) 
				callPostOptimizeCallbacks();

			if (cmd.isSoftCommit()) {
				synchronized (mCoreState.getUpdateLock()) {
					if (mUpdateLog != null) 
						mUpdateLog.preSoftCommit(cmd);
					
					getSearchCore().getSearchControl().getSearcherRef(true, false, waitSearcher, true);
					
					if (mUpdateLog != null) 
						mUpdateLog.postSoftCommit(cmd);
				}
			} else {
				synchronized (mCoreState.getUpdateLock()) {
					if (mUpdateLog != null) 
						mUpdateLog.preSoftCommit(cmd);
					
					if (cmd.isOpenSearcher()) {
						getSearchCore().getSearchControl().getSearcherRef(true, false, waitSearcher);
						
					} else {
						// force open a new realtime searcher so realtime-get and versioning code 
						// can see the latest
						SearcherRef searchHolder = getSearchCore().getSearchControl()
								.openNewSearcherRef(true, true);
						
						searchHolder.decreaseRef();
					}
					
					if (mUpdateLog != null) 
						mUpdateLog.postSoftCommit(cmd);
				}
				
				// postCommit currently means new searcher has also been opened
				if (mUpdateLog != null) 
					mUpdateLog.postCommit(cmd);
			}

			// reset commit tracking
			if (cmd.isSoftCommit()) 
				mSoftCommitTracker.didCommit();
			else 
				mCommitTracker.didCommit();
      
			if (LOG.isInfoEnabled())
				LOG.info("end commit flush");

			error = false;
		} finally {
			if (!cmd.isSoftCommit()) 
				mCommitLock.unlock();

			mAddCommands.set(0);
			mDeleteByIdCommands.set(0);
			mDeleteByQueryCommands.set(0);
			
			if (error) 
				mNumErrors.incrementAndGet();
		}

		// if we are supposed to wait for the searcher to be registered, then we should do it
		// outside any synchronized block so that other update operations can proceed.
		if (waitSearcher != null && waitSearcher[0] != null) {
			try {
				waitSearcher[0].get();
			} catch (Exception e) {
				if (LOG.isErrorEnabled())
					LOG.error(e.toString(), e);
			}
		}
	}

	@Override
	public void newIndexWriter(boolean rollback) throws ErrorException {
		mCoreState.newIndexWriter(rollback);
	}
  
	@Override
	public void rollback(RollbackCommand cmd) throws ErrorException {
		if (LOG.isDebugEnabled())
			LOG.debug("rollback: " + cmd);
		
		mRollbackCommands.incrementAndGet();
		boolean error = true;
		
		try {
			if (LOG.isInfoEnabled())
				LOG.info("start rollback " + cmd);

			rollbackWriter();
			//callPostRollbackCallbacks();

			// reset commit tracking
			mCommitTracker.didRollback();
			mSoftCommitTracker.didRollback();
      
			if (LOG.isInfoEnabled())
				LOG.info("end rollback");

			error = false;
		} finally {
			mAddCommandsCumulative.set(
					mAddCommandsCumulative.get() - mAddCommands.getAndSet(0));
			mDeleteByIdCommandsCumulative.set(
					mDeleteByIdCommandsCumulative.get() - mDeleteByIdCommands.getAndSet(0));
			mDeleteByQueryCommandsCumulative.set(
					mDeleteByQueryCommandsCumulative.get() - mDeleteByQueryCommands.getAndSet(0));
			
			if (error) 
				mNumErrors.incrementAndGet();
		}
	}

	@Override
	public UpdateLog getUpdateLog() {
		return mUpdateLog;
	}

	@Override
	public void finish(boolean changesSinceCommit) throws ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("finish: changesSinceCommit=" + changesSinceCommit);
		
		if (changesSinceCommit) { 
			if (mUpdateLog != null) {
				mUpdateLog.finish(null);
				return;
			}
			
			// TODO: ...
		}
	}
	
	@Override
	public void close() throws ErrorException {
		if (LOG.isInfoEnabled())
			LOG.info("Closing " + this);
    
		mCommitTracker.close();
		mSoftCommitTracker.close();

		mNumDocsPending.set(0);

		mCoreState.decreaseRef(this);
	}

	private static boolean sCommitOnClose = true;  // TODO: make this a real config option?

	// IndexWriterCloser interface method - called from mCoreState.decref(this)
	@Override
	public void closeWriter(SearchWriter writer) throws ErrorException {
		if (LOG.isDebugEnabled())
			LOG.debug("closeWriter: " + writer);
		
		boolean clearRequestInfo = false;
		mCommitLock.lock();
		
		try {
			ISearchRequest req = getSearchCore().createLocalRequest(null, 
					new ModifiableParams());
			ISearchResponse rsp = getSearchCore().createLocalResponse(null, req);
			
			if (SearchRequestInfo.getRequestInfo() == null) {
				clearRequestInfo = true;
				// important for debugging
				SearchRequestInfo.setRequestInfo(new SearchRequestInfo(req, rsp)); 
			}

			if (!sCommitOnClose) {
				try { 
					if (writer != null) 
						writer.rollback();
				} catch (IOException ex) { 
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
				}

				// we shouldn't close the transaction logs either, but leaving them open
				// means we can't delete them on windows (needed for tests)
				if (mUpdateLog != null) 
					mUpdateLog.close(false);

				return;
			}

			// do a commit before we quit?     
			boolean tryToCommit = (writer != null) && (mUpdateLog != null) && 
					mUpdateLog.hasUncommittedChanges() && mUpdateLog.getState() == UpdateLog.State.ACTIVE;

			try {
				if (tryToCommit) {
					CommitCommand cmd = new CommitCommand(req, false);
					cmd.setOpenSearcher(false);
					cmd.setWaitSearcher(false);
					cmd.setSoftCommit(false);

					// TODO: keep other commit callbacks from being called?
					// too many test failures using this method... is it because of callbacks?
					//  this.commit(cmd); 

					synchronized (mCoreState.getUpdateLock()) {
						mUpdateLog.preCommit(cmd);
					}

					// todo: refactor this shared code (or figure out why a real CommitUpdateCommand can't be used)
					final Map<String,String> commitData = new HashMap<String,String>();
					commitData.put(SearchWriter.COMMIT_TIME_MSEC_KEY, 
							String.valueOf(System.currentTimeMillis()));
					
					writer.commit(commitData);

					synchronized (mCoreState.getUpdateLock()) {
						mUpdateLog.postCommit(cmd);
					}
				}
			} catch (Throwable th) {
				if (LOG.isErrorEnabled())
					LOG.error("Error in final commit", th);
			}

			// we went through the normal process to commit, so we don't have to artificially
			// cap any mUpdateLog files.
			try {
				if (mUpdateLog != null) 
					mUpdateLog.close(false);
			}  catch (Throwable th) {
				if (LOG.isErrorEnabled())
					LOG.error("Error closing log files", th);
			}

			try { 
				if (writer != null) 
					writer.close();
			} catch (IOException ex) { 
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
			}
		} finally {
			mCommitLock.unlock();
			if (clearRequestInfo) 
				SearchRequestInfo.clearRequestInfo();
		}
	}

	@Override
	public SearchCoreState getCoreState() {
		return mCoreState;
	}

	@Override
	public void decreaseRef() throws ErrorException {
		mCoreState.decreaseRef(this);
	}

	@Override
	public void increaseRef() throws ErrorException {
		mCoreState.increaseRef();
	}

	// allow access for tests
	public CommitTracker getCommitTracker() {
		return mCommitTracker;
	}

	// allow access for tests
	public CommitTracker getSoftCommitTracker() {
		return mSoftCommitTracker;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "{" + getMBeanStatistics() + "}";
	}

	@Override
	public String getMBeanDescription() {
		return "Update handler that efficiently directly updates the on-disk main index";
	}

	@Override
	public NamedList<?> getMBeanStatistics() {
	    NamedList<Object> lst = new NamedMap<Object>();
	    lst.add("commits", mCommitCommands.get());
	    lst.add("autocommits", mCommitTracker.getCommitCount());
	    
	    if (mCommitTracker.getDocsUpperBound() > 0) 
	    	lst.add("autocommit maxDocs", mCommitTracker.getDocsUpperBound());
	    if (mCommitTracker.getTimeUpperBound() > 0) 
	    	lst.add("autocommit maxTime", "" + mCommitTracker.getTimeUpperBound() + "ms");
	    
	    lst.add("soft autocommits", mSoftCommitTracker.getCommitCount());
	    
	    if (mSoftCommitTracker.getDocsUpperBound() > 0) 
	    	lst.add("soft autocommit maxDocs", mSoftCommitTracker.getDocsUpperBound());
	    if (mSoftCommitTracker.getTimeUpperBound() > 0) 
	    	lst.add("soft autocommit maxTime", "" + mSoftCommitTracker.getTimeUpperBound() + "ms");
	    
	    lst.add("optimizes", mOptimizeCommands.get());
	    lst.add("rollbacks", mRollbackCommands.get());
	    lst.add("expungeDeletes", mExpungeDeleteCommands.get());
	    lst.add("docsPending", mNumDocsPending.get());
	    
	    // pset.size() not synchronized, but it should be fine to access.
	    // lst.add("deletesPending", pset.size());
	    
	    lst.add("adds", mAddCommands.get());
	    lst.add("deletesById", mDeleteByIdCommands.get());
	    lst.add("deletesByQuery", mDeleteByQueryCommands.get());
	    lst.add("errors", mNumErrors.get());
	    
	    lst.add("cumulative adds", mAddCommandsCumulative.get());
	    lst.add("cumulative deletesById", mDeleteByIdCommandsCumulative.get());
	    lst.add("cumulative deletesByQuery", mDeleteByQueryCommandsCumulative.get());
	    lst.add("cumulative errors", mNumErrorsCumulative.get());
	    
	    return lst;
	}
	
}
