package org.javenstudio.falcon.search;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IDirectoryReader;
import org.javenstudio.common.indexdb.LockObtainFailedException;
import org.javenstudio.common.indexdb.index.IndexDeletionPolicy;
import org.javenstudio.common.indexdb.index.IndexWriter;
import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.DefaultThreadFactory;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedListPlugin;
import org.javenstudio.falcon.util.PluginInfo;
import org.javenstudio.falcon.util.RefCounted;
import org.javenstudio.hornet.index.segment.DirectoryReader;
import org.javenstudio.falcon.search.store.DirectoryFactory;
import org.javenstudio.falcon.search.store.NRTCachingDirectoryFactory;

public class SearchControl {
	private static final Logger LOG = Logger.getLogger(SearchControl.class);

	private static final Set<String> sDirs = new HashSet<String>();
	
	private final ISearchCore mCore;

	private final DirectoryFactory mDirectoryFactory;
	private final DeletionPolicyWrapper mDeletionPolicy;
	private final IndexReaderFactory mIndexReaderFactory;
	
	private final List<SearcherListener> mFirstSearcherListeners = 
			new ArrayList<SearcherListener>();
	private final List<SearcherListener> mNewSearcherListeners = 
			new ArrayList<SearcherListener>();
	
	// All of the normal open searchers.  Don't access this directly.
	// protected by synchronizing on searcherLock.
	private final LinkedList<SearcherRef> mSearcherRefs = 
			new LinkedList<SearcherRef>();
	private final LinkedList<SearcherRef> mRealtimeSearcherRefs = 
			new LinkedList<SearcherRef>();

	private final ExecutorService mSearcherExecutor = Executors.newSingleThreadExecutor(
			new DefaultThreadFactory("searcherExecutor"));
	
	private final int mMaxWarmingSearchers;  // max number of on-deck searchers allowed
	
	// The current searcher used to service queries.
	// Don't access this directly!!!! use getSearcher() to
	// get it (and it will increment the ref count at the same time).
	// This reference is protected by searcherLock.
	private SearcherRef mSearcherRef = null;
	
	private int mOnDeckSearchers = 0;  // number of searchers preparing
	// Lock ordering: one can acquire the openSearcherLock and then the searcherLock, but not vice-versa.
	private Object mSearcherLock = new Object();  // the sync object for the searcher
	// used to serialize opens/reopens for absolute ordering
	private ReentrantLock mOpenSearcherLock = new ReentrantLock(true); 
	
	private SearcherRef mRealtimeSearcherRef = null;
	private Callable<IDirectoryReader> mNewReaderCreator = null;
	
	public SearchControl(ISearchCore core, ISearchCore prev) 
			throws ErrorException { 
		mCore = core;
		mMaxWarmingSearchers = 
				core.getSearchConfig().getMaxWarmingSearchers();
		
		initListeners();
		
		mDirectoryFactory = loadDirectoryFactory();
		mDeletionPolicy = loadDeletionPolicy();
		mIndexReaderFactory = loadIndexReaderFactory();
		
		initIndex(prev != null);
		
	    // Handle things that should eventually go away
	    //initDeprecatedSupport();

	    final CountDownLatch latch = new CountDownLatch(1);
	    try { 
	    	
	        // cause the executor to stall so firstSearcher events won't fire
	        // until after inform() has been called for all components.
	        // searchExecutor must be single-threaded for this to work
	        mSearcherExecutor.submit(new Callable<Object>() {
	        		@Override
		          	public Object call() throws Exception {
		          		latch.await();
		          		return null;
		          	}
		        });
	    	
	        // use the (old) writer to open the first searcher
	        SearchWriterRef iwRef = null;
	        if (prev != null) {
	        	iwRef = prev.getUpdateIndexer().getCoreState().getIndexWriterRef();
	        	if (iwRef != null) {
	        		final SearchWriter iw = iwRef.get();
	        		mNewReaderCreator = new Callable<IDirectoryReader>() {
		        			@Override
		        			public IDirectoryReader call() throws Exception {
		        				return DirectoryReader.open(iw, true);
		        			}
		        		};
	        	}
	        }

	        // Open the searcher *before* the update handler so we don't end up opening
	        // one in the middle.
	        // With lockless commits in indexdb now, this probably shouldn't be an issue anymore

	        try {
	        	getSearcherRef(false, false, null, true);
	        } finally {
	        	mNewReaderCreator = null;
	        	if (iwRef != null) 
	        		iwRef.decreaseRef();
	        }
	        
	    } catch (Throwable e) { 
	    	// release the latch, otherwise we block trying to do the close. 
	    	// This should be fine, since counting down on a latch of 0 is still fine
	    	latch.countDown();
	    	
	        // close down the searcher and any other resources, 
	    	// if it exists, as this is not recoverable
	        core.close();
	        
	        if (e instanceof ErrorException) 
	        	throw (ErrorException)e;
	        else 
	        	throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
	        
	    } finally { 
	    	// allow firstSearcher events to fire and make sure it is released
	        latch.countDown();
	    }
	}
	
	public final ISearchCore getSearchCore() { 
		return mCore;
	}
	
	public DirectoryFactory getDirectoryFactory() { 
		return mDirectoryFactory;
	}
	
	public DeletionPolicyWrapper getIndexDeletionPolicy() { 
		return mDeletionPolicy; 
	}
	
	public IndexReaderFactory getIndexReaderFactory() { 
		return mIndexReaderFactory;
	}
	
	public SearcherRef getRealtimeSearcher() { 
		return mRealtimeSearcherRef;
	}
	
	private void initListeners() throws ErrorException {
	    final Class<SearcherListener> clazz = SearcherListener.class;
	    
	    for (PluginInfo info : getSearchCore().getSearchConfig().getPluginInfos(
	    		SearcherListener.class.getName())) {
	    	String event = info.getAttribute("event");
	    	
	    	if ("firstSearcher".equals(event) ){
	    		SearcherListener obj = getSearchCore().createPlugin(info, clazz);
	    		mFirstSearcherListeners.add(obj);
	    		
	    		if (LOG.isDebugEnabled())
	    			LOG.debug("Added SearcherListener for firstSearcher: " + obj);
	    		
	    	} else if("newSearcher".equals(event) ){
	    		SearcherListener obj = getSearchCore().createPlugin(info, clazz);
	    		mNewSearcherListeners.add(obj);
	    		
	    		if (LOG.isDebugEnabled())
	    			LOG.debug("Added SearcherListener for newSearcher: " + obj);
	    	}
	    }
	}
	
	private DirectoryFactory loadDirectoryFactory() throws ErrorException {
	    DirectoryFactory dirFactory;
	    PluginInfo info = getSearchCore().getSearchConfig().getPluginInfo(
	    		DirectoryFactory.class.getName());
	    
	    if (info != null) {
	    	dirFactory = getSearchCore().createPlugin(info, DirectoryFactory.class);
		    
	    } else {
	    	dirFactory = new NRTCachingDirectoryFactory();
	    	getSearchCore().registerInfoMBean(dirFactory);
	    	
	    	dirFactory.init(NamedList.EMPTY);
	    }
	    
	    // And set it
	    dirFactory.setContext(getSearchCore().getIndexContext());
	    return dirFactory;
	}
	
	private DeletionPolicyWrapper loadDeletionPolicy() throws ErrorException {
		PluginInfo info = getSearchCore().getSearchConfig().getPluginInfo(
				IndexDeletionPolicy.class.getName());
		
		IndexDeletionPolicy delPolicy = null;
		if (info != null) {
			delPolicy = getSearchCore().createPlugin(info, IndexDeletionPolicy.class);
			
		} else {
			delPolicy = new SearchDeletionPolicy();
			getSearchCore().registerInfoMBean(delPolicy);
			
			if (delPolicy instanceof NamedListPlugin) 
				((NamedListPlugin) delPolicy).init(NamedList.EMPTY);
		}
		
		return new DeletionPolicyWrapper(delPolicy);
	}
	
	private void initIndex(boolean reload) throws ErrorException {
	    try {
	    	String indexDir = getSearchCore().getNewIndexDir();
	    	boolean indexExists = getDirectoryFactory().exists(indexDir);
	    	
	    	boolean firstTime;
	    	synchronized (sDirs) {
	    		firstTime = sDirs.add(new File(indexDir).getCanonicalPath());
	    	}
	    	
	    	boolean removeLocks = getSearchCore().getSearchConfig().isUnlockOnStartup();
	    	

	    	if (indexExists && firstTime && !reload) {
	    		// to remove locks, the directory must already exist... so we create it
	    		// if it didn't exist already...
	    		IDirectory dir = getDirectoryFactory().get(indexDir, 
	    				getSearchCore().getSearchConfig().getIndexLockType());
	    		
	    		if (dir != null)  {
	    			if (IndexWriter.isLocked(dir)) {
	    				if (removeLocks) {
	    					if (LOG.isWarnEnabled()) {
	    						LOG.warn("WARNING: index directory '" + indexDir 
	    								+ "' is locked.  Unlocking...");
	    					}
	    					IndexWriter.unlock(dir);
	    					
	    				} else {
	    					if (LOG.isErrorEnabled()) {
	    						LOG.error("index directory '" + indexDir 
	    								+ "' is locked. Throwing exception");
	    					}
	    					throw new LockObtainFailedException("Index locked for write for core " 
	    							+ getSearchCore().getName());
	    				}
	    			}
	    			
	    			getDirectoryFactory().release(dir);
	    		}
	    	}

	    	// Create the index if it doesn't exist.
	    	if (!indexExists) {
	    		if (LOG.isInfoEnabled()) {
	    			LOG.info("index directory '" + new File(indexDir) + "' doesn't exist."
	    					+ " Creating new index...");
	    		}

	    		SearchWriter writer = SearchWriter.create(
	    				getSearchCore().getName(), indexDir, getDirectoryFactory(), 
	    				getSearchCore().createIndexParams());
	    	  
	    		writer.close();
	    	}

	    } catch (IOException e) {
	    	throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
	    }
	}
	
	private IndexReaderFactory loadIndexReaderFactory() throws ErrorException {
	    PluginInfo info = getSearchCore().getSearchConfig().getPluginInfo(
	    		IndexReaderFactory.class.getName());
	    
	    IndexReaderFactory indexReaderFactory;
	    if (info != null) {
	    	indexReaderFactory = getSearchCore().createPlugin(info, IndexReaderFactory.class);
	    	
	    } else {
	    	indexReaderFactory = new DefaultReaderFactory();
	    	getSearchCore().registerInfoMBean(indexReaderFactory);
	    	
	    	indexReaderFactory.init(NamedList.EMPTY);
	    }
	    
	    return indexReaderFactory;
	}
	
	/**
	 * Return a registered {@link RefCounted}&lt;{@link Searcher}&gt; with
	 * the reference count incremented.  It <b>must</b> be decremented when no longer needed.
	 * This method should not be called from CoreAware.inform() since it can result
	 * in a deadlock if useColdSearcher==false.
	 * If handling a normal request, the searcher should be obtained from
	 * {@link QueryRequest#getSearcher()} instead.
	 */
	public SearcherRef getSearcherRef() throws ErrorException {
		if (LOG.isDebugEnabled())
			LOG.debug("getSearcherRef");
		
		return getSearcherRef(false, true, null);
	}

	/**
	 * Returns the current registered searcher with its reference count incremented, 
	 * or null if none are registered.
	 */
	public SearcherRef getRegisteredSearcherRef() {
		if (LOG.isDebugEnabled())
			LOG.debug("getRegisteredSearcherRef");
		
		synchronized (mSearcherLock) {
			if (mSearcherRef != null) 
				mSearcherRef.increaseRef();
			
			return mSearcherRef;
		}
	}

	/**
	 * Return the newest normal {@link RefCounted}&lt;{@link Searcher}&gt; with
	 * the reference count incremented.  It <b>must</b> be decremented when no longer needed.
	 * If no searcher is currently open, then if openNew==true a new searcher will be opened,
	 * or null is returned if openNew==false.
	 */
	public SearcherRef getNewestSearcherRef(boolean openNew) 
			throws ErrorException {
		if (LOG.isDebugEnabled())
			LOG.debug("getNewestSearcherRef: openNew=" + openNew);
		
		synchronized (mSearcherLock) {
			if (!mSearcherRefs.isEmpty()) {
				SearcherRef newest = mSearcherRefs.getLast();
				newest.increaseRef();
				return newest;
			}
		}

		return openNew ? getRealtimeSearcherRef() : null;
	}

	/** 
	 * Gets the latest real-time searcher w/o forcing open a new searcher if one already exists.
	 * The reference count will be incremented.
	 */
	public SearcherRef getRealtimeSearcherRef() throws ErrorException {
		if (LOG.isDebugEnabled())
			LOG.debug("getRealtimeSearcherRef");
		
		synchronized (mSearcherLock) {
			if (mRealtimeSearcherRef != null) {
				mRealtimeSearcherRef.increaseRef();
				return mRealtimeSearcherRef;
			}
		}

		// use the searcher lock to prevent multiple people from trying to open at once
		mOpenSearcherLock.lock();
		try {
			// try again
			synchronized (mSearcherLock) {
				if (mRealtimeSearcherRef != null) {
					mRealtimeSearcherRef.increaseRef();
					return mRealtimeSearcherRef;
				}
			}

			// force a new searcher open
			return openNewSearcherRef(true, true);
		} finally {
			mOpenSearcherLock.unlock();
		}
	}

	@SuppressWarnings("rawtypes")
	public SearcherRef getSearcherRef(boolean forceNew, boolean returnSearcher, 
			final Future[] waitSearcher) throws ErrorException {
		return getSearcherRef(forceNew, returnSearcher, waitSearcher, false);
	}

	/** 
	 * Opens a new searcher and returns a RefCounted&lt;Searcher&gt; 
	 * with it's reference incremented.
	 *
	 * "realtime" means that we need to open quickly for a realtime view 
	 * of the index, hence don't do any
	 * autowarming and add to the _realtimeSearchers queue rather than 
	 * the _searchers queue (so it won't
	 * be used for autowarming by a future normal searcher).  
	 * A "realtime" searcher will currently never
	 * become "registered" (since it currently lacks caching).
	 *
	 * realtimeSearcher is updated to the latest opened searcher, 
	 * regardless of the value of "realtime".
	 *
	 * This method acquires openSearcherLock - do not call with searckLock held!
	 */
	public SearcherRef openNewSearcherRef(boolean updateHandlerReopens, 
			boolean realtime) throws ErrorException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("openNewSearcherRef: updateHandlerReopens=" + updateHandlerReopens 
					+ " realtime=" + realtime);
		}
		
		SearcherRef newestSearcher = null;
		Searcher tmp = null;
		boolean nrt = getSearchCore().getSearchConfig().isReopenReaders() 
				&& updateHandlerReopens;

		mOpenSearcherLock.lock();
		try {
			String newIndexDir = getSearchCore().getNewIndexDir();
			
			File indexDirFile = null;
			File newIndexDirFile = null;

			// if it's not a normal near-realtime update, check that paths haven't changed.
			if (!nrt) {
				indexDirFile = new File(getSearchCore().getIndexDir()).getCanonicalFile();
				newIndexDirFile = new File(newIndexDir).getCanonicalFile();
			}

			synchronized (mSearcherLock) {
				newestSearcher = mRealtimeSearcherRef;
				if (newestSearcher != null) 
					newestSearcher.increaseRef(); // the matching decref is in the finally block
			}

			if (newestSearcher != null && getSearchCore().getSearchConfig().isReopenReaders() && 
				(nrt || indexDirFile.equals(newIndexDirFile))) {

				DirectoryReader newReader = null;
				DirectoryReader currentReader = (DirectoryReader)newestSearcher.get().getDirectoryReader();

				if (updateHandlerReopens) {
					SearchWriterRef writer = getSearchCore().getUpdateIndexer()
							.getCoreState().getIndexWriterRef();
					try {
						newReader = DirectoryReader.openIfChanged(currentReader, writer.get(), true);
					} finally {
						writer.decreaseRef();
					}

				} else {
					newReader = DirectoryReader.openIfChanged(currentReader);
				}

				if (newReader == null) {
					// if this is a request for a realtime searcher, 
					// just return the same searcher if there haven't been any changes.
					if (realtime) {
						newestSearcher.increaseRef();
						return newestSearcher;
					}

					currentReader.increaseRef();
					newReader = currentReader;
				}

				// for now, turn off caches if this is for a realtime reader 
				// (caches take a little while to instantiate)
				tmp = new Searcher(this, mDirectoryFactory, getSearchCore().getSchema(), 
						newReader, true, !realtime, true, realtime);

			} else {
				// newestSearcher == null at this point

				if (mNewReaderCreator != null) {
					// this is set in the constructor if there is a currently open index writer
					// so that we pick up any uncommitted changes and so we don't go backwards
					// in time on a core reload
					IDirectoryReader newReader = mNewReaderCreator.call();
					
					tmp = new Searcher(this, mDirectoryFactory, getSearchCore().getSchema(), 
							newReader, true, !realtime, true, realtime);
					
				} else {
					String lockType = getSearchCore().getSearchConfig().getIndexLockType();
					
					if (LOG.isDebugEnabled()) { 
						LOG.debug("Opening directory by " + mDirectoryFactory.getClass().getName() 
								+ " with indexDir=" + newIndexDir + " lockType=" + lockType);
					}
					
					IDirectory directory = mDirectoryFactory.get(newIndexDir, 
							lockType);
					
					if (LOG.isDebugEnabled()) { 
						LOG.debug("Creating directoryReader by " + mIndexReaderFactory.getClass().getName() 
								+ " with directory=" + directory);
					}
					
					// normal open that happens at startup
					IDirectoryReader newReader = mIndexReaderFactory.newReader(
							directory, getSearchCore());
					
					tmp = new Searcher(this, mDirectoryFactory, getSearchCore().getSchema(), 
							newReader, true, true, false, realtime);
				}
			}

			List<SearcherRef> searcherList = realtime ? mRealtimeSearcherRefs : mSearcherRefs;
			SearcherRef newSearcher = newHolder(tmp, searcherList); // refcount now at 1

			// Increment reference again for "realtimeSearcher" variable.  
			// It should be at 2 after.
			// When it's decremented by both the caller of this method, 
			// and by realtimeSearcher being replaced, it will be closed.
			newSearcher.increaseRef();

			synchronized (mSearcherLock) {
				if (mRealtimeSearcherRef != null) 
					mRealtimeSearcherRef.decreaseRef();
				
				mRealtimeSearcherRef = newSearcher;
				searcherList.add(mRealtimeSearcherRef);
			}

			return newSearcher;

		} catch (Exception e) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Error opening new searcher", e);
			
		} finally {
			mOpenSearcherLock.unlock();
			if (newestSearcher != null) 
				newestSearcher.decreaseRef();
			
			if (LOG.isDebugEnabled())
				LOG.debug("openNewSearcherRef end.");
		}
	}
  
	/**
	 * Get a {@link Searcher} or start the process of creating a new one.
	 * <p>
	 * The registered searcher is the default searcher used to service queries.
	 * A searcher will normally be registered after all of the warming
	 * and event handlers (newSearcher or firstSearcher events) have run.
	 * In the case where there is no registered searcher, the newly created searcher will
	 * be registered before running the event handlers (a slow searcher is 
	 * better than no searcher).
	 *
	 * <p>
	 * These searchers contain read-only IndexReaders. To access a non read-only IndexReader,
	 * see newSearcher(String name, boolean readOnly).
	 *
	 * <p>
	 * If <tt>forceNew==true</tt> then
	 *  A new searcher will be opened and registered regardless of whether there is already
	 *    a registered searcher or other searchers in the process of being created.
	 * <p>
	 * If <tt>forceNew==false</tt> then:<ul>
	 *   <li>If a searcher is already registered, that searcher will be returned</li>
	 *   <li>If no searcher is currently registered, but at least one is in 
	 *   the process of being created, then
	 * this call will block until the first searcher is registered</li>
	 *   <li>If no searcher is currently registered, and no searchers in 
	 *   the process of being registered, a new
	 * searcher will be created.</li>
	 * </ul>
	 * <p>
	 * If <tt>returnSearcher==true</tt> then a {@link RefCounted}&lt;{@link Searcher}&gt; 
	 * will be returned with
	 * the reference count incremented.  It <b>must</b> be decremented when no longer needed.
	 * <p>
	 * If <tt>waitSearcher!=null</tt> and a new {@link Searcher} was created,
	 * then it is filled in with a Future that will return after the searcher is registered.  
	 * The Future may be set to
	 * <tt>null</tt> in which case the Searcher created has already been registered at the time
	 * this method returned.
	 * <p>
	 * @param forceNew             if true, force the open of a new index searcher 
	 * regardless if there is already one open.
	 * @param returnSearcher       if true, returns a {@link Searcher} holder with 
	 * the refcount already incremented.
	 * @param waitSearcher         if non-null, will be filled in with a {@link Future} 
	 * that will return after the new searcher is registered.
	 * @param updateHandlerReopens if true, the UpdateHandler will be used 
	 * when reopening a {@link Searcher}.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public SearcherRef getSearcherRef(boolean forceNew, boolean returnSearcher, 
			final Future[] waitSearcher, boolean updateHandlerReopens) throws ErrorException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("getSearcherRef: forceNew=" + forceNew + " returnSearcher=" + returnSearcher 
					+ " waitSearcher=" + waitSearcher + " updateHandlerReopens=" + updateHandlerReopens);
		}
		
		// it may take some time to open an index.... we may need to make
		// sure that two threads aren't trying to open one at the same time
		// if it isn't necessary.

		synchronized (mSearcherLock) {
			// see if we can return the current searcher
			if (mSearcherRef != null && !forceNew) {
				if (returnSearcher) {
					mSearcherRef.increaseRef();
					return mSearcherRef;
				} else {
					return null;
				}
			}

			// check to see if we can wait for someone else's searcher to be set
			if (mOnDeckSearchers > 0 && !forceNew && mSearcherRef == null) {
				try {
					mSearcherLock.wait();
				} catch (InterruptedException e) {
					LOG.warn(e.toString(), e);
				}
			}

			// check again: see if we can return right now
			if (mSearcherRef != null && !forceNew) {
				if (returnSearcher) {
					mSearcherRef.increaseRef();
					return mSearcherRef;
				} else {
					return null;
				}
			}

			// At this point, we know we need to open a new searcher...
			// first: increment count to signal other threads that we are
			//        opening a new searcher.
			mOnDeckSearchers ++;
			if (mOnDeckSearchers < 1) {
				if (LOG.isErrorEnabled())
					LOG.error("ERROR!!! onDeckSearchers is " + mOnDeckSearchers);
				
				// should never happen... just a sanity check
				mOnDeckSearchers = 1; // reset
				
			} else if (mOnDeckSearchers > mMaxWarmingSearchers) {
				mOnDeckSearchers --;
				
				// HTTP 503==service unavailable, or 409==Conflict
				throw new ErrorException(ErrorException.ErrorCode.SERVICE_UNAVAILABLE, 
						"Error opening new searcher. exceeded limit of maxWarmingSearchers=" 
						+ mMaxWarmingSearchers + ", try again later.");
				
			} else if (mOnDeckSearchers > 1) {
				LOG.warn("PERFORMANCE WARNING: Overlapping onDeckSearchers=" + mOnDeckSearchers);
			}
		}

		// a signal to decrement onDeckSearchers if something goes wrong.
		final boolean[] decrementOnDeckCount = new boolean[]{true};
		
		SearcherRef currSearcherHolder = null; // searcher we are autowarming from
		SearcherRef searchHolder = null;
		boolean success = false;

		mOpenSearcherLock.lock();
		try {
			searchHolder = openNewSearcherRef(updateHandlerReopens, false);
			
			// the searchHolder will be incremented once already (and it will eventually 
			// be assigned to _searcher when registered)
			// increment it again if we are going to return it to the caller.
			if (returnSearcher) 
				searchHolder.increaseRef();

			final SearcherRef newSearchHolder = searchHolder;
			final Searcher newSearcher = newSearchHolder.get();

			boolean alreadyRegistered = false;
			synchronized (mSearcherLock) {
				if (mSearcherRef == null) {
					// if there isn't a current searcher then we may
					// want to register this one before warming is complete instead of waiting.
					if (getSearchCore().getSearchConfig().useColdSearcher()) {
						registerSearcher(newSearchHolder);
						decrementOnDeckCount[0] = false;
						alreadyRegistered = true;
					}
				} else {
					// get a reference to the current searcher for purposes of autowarming.
					currSearcherHolder = mSearcherRef;
					currSearcherHolder.increaseRef();
				}
			}

			final Searcher currSearcher = 
					(currSearcherHolder == null) ? null : currSearcherHolder.get();
			Future future = null;

			// warm the new searcher based on the current searcher.
			// should this go before the other event handlers or after?
			if (currSearcher != null) {
				future = mSearcherExecutor.submit(
						new Callable() {
							public Object call() throws Exception {
								try {
									newSearcher.warm(currSearcher);
								} catch (Throwable e) {
									if (LOG.isErrorEnabled())
										LOG.error(e.toString(), e);
								}
								return null;
							}
						}
					);
			}

			if (currSearcher == null && mFirstSearcherListeners.size() > 0) {
				future = mSearcherExecutor.submit(
						new Callable() {
							public Object call() throws Exception {
								try {
									for (SearcherListener listener : mFirstSearcherListeners) {
										listener.newSearcher(newSearcher, null);
									}
								} catch (Throwable e) {
									if (LOG.isErrorEnabled())
										LOG.error(e.toString(), e);
								}
								return null;
							}
						}
					);
			}

			if (currSearcher != null && mNewSearcherListeners.size() > 0) {
				future = mSearcherExecutor.submit(
						new Callable() {
							public Object call() throws Exception {
								try {
									for (SearcherListener listener : mNewSearcherListeners) {
										listener.newSearcher(newSearcher, currSearcher);
									}
								} catch (Throwable e) {
									if (LOG.isErrorEnabled())
										LOG.error(e.toString(), e);
								}
								return null;
							}
						}
					);
			}

			// WARNING: this code assumes a single threaded executor (that all tasks
			// queued will finish first).
			final SearcherRef currSearcherHolderF = currSearcherHolder;
			
			if (!alreadyRegistered) {
				future = mSearcherExecutor.submit(
						new Callable() {
							public Object call() throws Exception {
								try {
									// registerSearcher will decrement onDeckSearchers and
									// do a notify, even if it fails.
									registerSearcher(newSearchHolder);
								} catch (Throwable e) {
									if (LOG.isErrorEnabled())
										LOG.error(e.toString(), e);
								} finally {
									// we are all done with the old searcher we used
									// for warming...
									if (currSearcherHolderF != null) 
										currSearcherHolderF.decreaseRef();
								}
								return null;
							}
						}
					);
			}

			if (waitSearcher != null) 
				waitSearcher[0] = future;

			success = true;

			// Return the searcher as the warming tasks run in parallel
			// callers may wait on the waitSearcher future returned.
			return returnSearcher ? newSearchHolder : null;

		} catch (Exception e) {
			if (e instanceof ErrorException) 
				throw (ErrorException)e;
			else 
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
			
		} finally {
			if (!success) {
				synchronized (mSearcherLock) {
					mOnDeckSearchers --;

					if (mOnDeckSearchers < 0) {
						if (LOG.isErrorEnabled())
							LOG.error("ERROR!!! onDeckSearchers after decrement=" + mOnDeckSearchers);
						
						// sanity check... should never happen
						mOnDeckSearchers = 0; // try and recover
					}
					// if we failed, we need to wake up at least one waiter to continue the process
					mSearcherLock.notify();
				}

				if (currSearcherHolder != null) 
					currSearcherHolder.decreaseRef();

				if (searchHolder != null) {
					// decrement 1 for _searcher (searchHolder will never become _searcher now)
					searchHolder.decreaseRef(); 
					
					if (returnSearcher) {
						// decrement 1 because we won't be returning the searcher to the user
						searchHolder.decreaseRef(); 
					}
				}
			}

			// we want to do this after we decrement onDeckSearchers so another thread
			// doesn't increment first and throw a false warning.
			mOpenSearcherLock.unlock();
			
			if (LOG.isDebugEnabled())
				LOG.debug("getSearcherRef end.");
		}
	}

	private SearcherRef newHolder(Searcher newSearcher, 
			final List<SearcherRef> searcherList) {
		SearcherRef holder = new SearcherRef(newSearcher) {
				@Override
				public void close() {
					try {
						synchronized (mSearcherLock) {
							// it's possible for someone to get a reference via the _searchers queue
							// and increment the refcount while RefCounted.close() is being called.
							// we check the refcount again to see if this has happened and abort the close.
							// This relies on the RefCounted class allowing close() to be called every
							// time the counter hits zero.
							if (getRefCount() > 0) return;
							searcherList.remove(this);
						}
						get().close();
					} catch (Throwable e) {
						// do not allow decref() operations to fail since they are typically called in finally blocks
						// and throwing another exception would be very unexpected.
						if (LOG.isErrorEnabled())
							LOG.error("Error closing searcher:" + this, e);
					}
				}
			};
		
		// set ref count to 1 to account for this._searcher
		holder.increaseRef(); 
		
		return holder;
	}

	// Take control of newSearcherHolder (which should have a reference count of at
	// least 1 already.  If the caller wishes to use the newSearcherHolder directly
	// after registering it, then they should increment the reference count *before*
	// calling this method.
	//
	// onDeckSearchers will also be decremented (it should have been incremented
	// as a result of opening a new searcher).
	private void registerSearcher(SearcherRef newSearcherHolder) {
		synchronized (mSearcherLock) {
			try {
				if (mSearcherRef != null) {
					if (LOG.isDebugEnabled()) 
						LOG.debug("remove current searcher: " + mSearcherRef);
					
					mSearcherRef.decreaseRef(); // dec refcount for this._searcher
					mSearcherRef = null;
				}

				mSearcherRef = newSearcherHolder;
				Searcher newSearcher = newSearcherHolder.get();

				/***
        		// a searcher may have been warming asynchronously while the core was being closed.
        		// if this happens, just close the searcher.
        		if (isClosed()) {
          			// NOTE: this should not happen now - see close() for details.
          			// *BUT* if we left it enabled, this could still happen before
          			// close() stopped the executor - so disable this test for now.
          			log.error("Ignoring searcher register on closed core: " + newSearcher);
          			mSearcherRef.decreaseRef();
        		}
				 ***/

				newSearcher.register(); // register subitems (caches)
				
				if (LOG.isInfoEnabled())
					LOG.info("Registered new searcher " + newSearcher);

			} catch (Throwable e) {
				// an exception in register() shouldn't be fatal.
				if (LOG.isErrorEnabled())
					LOG.error(e.toString(), e);
				
			} finally {
				// wake up anyone waiting for a searcher
				// even in the face of errors.
				mOnDeckSearchers --;
				mSearcherLock.notifyAll();
			}
		}
	}

	private void closeSearcher() throws ErrorException {
		if (LOG.isInfoEnabled())
			LOG.info("Closing main searcher on request.");
		
		synchronized (mSearcherLock) {
			if (mRealtimeSearcherRef != null) {
				mRealtimeSearcherRef.decreaseRef();
				mRealtimeSearcherRef = null;
			}
			
			if (mSearcherRef != null) {
				Searcher searcher = mSearcherRef.get();
				mSearcherRef.decreaseRef(); // dec refcount for this._searcher
				mSearcherRef = null; // isClosed() does check this
				
				if (searcher != null) 
					getSearchCore().removeInfoMBean(searcher);
			}
		}
	}
	
	public void close() { 
		try {
	        mSearcherExecutor.shutdown();
	        if (!mSearcherExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
	        	if (LOG.isErrorEnabled())
	        		LOG.error("Timeout waiting for searchExecutor to terminate");
	        }
		} catch (InterruptedException e) {
	        mSearcherExecutor.shutdownNow();
	        try {
	        	if (!mSearcherExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
	        		if (LOG.isErrorEnabled())
	        			LOG.error("Timeout waiting for searchExecutor to terminate");
	        	}
	        } catch (InterruptedException e2) {
	        	if (LOG.isErrorEnabled())
	        		LOG.error(e2.toString(), e2);
	        }
		} catch (Throwable e) {
			if (LOG.isErrorEnabled())
	    		LOG.error(e.toString(), e);
		}
		
		try {
	        // Since we waited for the searcherExecutor to shut down,
	        // there should be no more searchers warming in the background
	        // that we need to take care of.
	        //
	        // For the case that a searcher was registered *before* warming
	        // then the searchExecutor will throw an exception when getSearcher()
	        // tries to use it, and the exception handling code should close it.
	        closeSearcher();
		} catch (Throwable e) {
			if (LOG.isErrorEnabled())
	        	LOG.error(e.toString(), e);
		}
	}
	
}
