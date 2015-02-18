package org.javenstudio.falcon.datum.table.store;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.fs.FSDataOutputStream;
import org.javenstudio.raptor.fs.FileStatus;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;
import org.javenstudio.raptor.io.Writable;
import org.javenstudio.raptor.util.Progressable;
import org.javenstudio.raptor.util.StringUtils;

import com.google.common.collect.Lists;

/**
 * DBRegion stores data for a certain region of a table.  It stores all columns
 * for each row. A given table consists of one or more DBRegions.
 *
 * <p>We maintain multiple DBStores for a single DBRegion.
 *
 * <p>An Store is a set of rows with some column data; together,
 * they make up all the data for the rows.
 *
 * <p>Each DBRegion has a 'startKey' and 'endKey'.
 * <p>The first is inclusive, the second is exclusive (except for
 * the final region)  The endKey of region 0 is the same as
 * startKey for region 1 (if it exists).  The startKey for the
 * first region is null. The endKey for the final region is null.
 *
 * <p>Locking at the DBRegion level serves only one purpose: preventing the
 * region from being closed (and consequently split) while other operations
 * are ongoing. Each row level operation obtains both a row lock and a region
 * read lock for the duration of the operation. While a scanner is being
 * constructed, getScanner holds a read lock. If the scanner is successfully
 * constructed, it holds a read lock until it is closed. A close takes out a
 * write lock and consequently will block for ongoing operations and will block
 * new operations from starting while the close is in progress.
 *
 * <p>An DBRegion is defined by its table and its key extent.
 *
 * <p>It consists of at least one Store.  The number of Stores should be
 * configurable, so that data which is accessed together is stored in the same
 * Store.  Right now, we approximate that by building a single Store for
 * each column family.  (This config info will be communicated via the
 * tabledesc.)
 *
 * <p>The DBTableDescriptor contains metainfo about the DBRegion's table.
 * regionName is a unique identifier for this DBRegion. (startKey, endKey]
 * defines the keyspace for this DBRegion.
 */
public class DBRegion implements HeapSize { 
	private static final Logger LOG = Logger.getLogger(DBRegion.class);
	
	/**
	 * Status codes used for return values of bulk operations.
	 */
	public enum OperationStatusCode {
	    NOT_RUN,
	    SUCCESS,
	    BAD_FAMILY,
	    FAILURE;
	}
	
	/** modifyTable op for replacing the table descriptor */
	public static enum Modify {
	    CLOSE_REGION,
	    TABLE_COMPACT,
	    TABLE_FLUSH,
	    TABLE_MAJOR_COMPACT,
	    TABLE_SET_HTD,
	    TABLE_SPLIT
	}
	
	/**
	 * Name of the region info file that resides just under the region directory.
	 */
	static final String REGIONINFO_FILE = ".regioninfo";
	
	static final String MERGEDIR = "merges";
	
	private static Random sRand = new Random();
	
	private final AtomicBoolean mClosed = new AtomicBoolean(false);
	
	/**
	 * Closing can take some time; use the closing flag if there is stuff we don't
	 * want to do while in closing state; e.g. like offer this region up to the
	 * master as a region to close if the carrying regionserver is overloaded.
	 * Once set, it is never cleared.
	 */
	private final AtomicBoolean mClosing = new AtomicBoolean(false);

	//////////////////////////////////////////////////////////////////////////////
	// Members
	//////////////////////////////////////////////////////////////////////////////

	private final Set<byte[]> mLockedRows =
			new TreeSet<byte[]>(Bytes.BYTES_COMPARATOR);
	
	private final Map<Integer, byte[]> mLockIds =
			new HashMap<Integer, byte[]>();
	
	private int mLockIdGenerator = 1;
  
	protected final Map<byte[], Store> mStores =
			new ConcurrentSkipListMap<byte[], Store>(Bytes.BYTES_RAWCOMPARATOR);

	//These variable are just used for getting data out of the region, to test on
	//client side
	// private int numStores = 0;
	// private int [] storeSize = null;
	// private byte[] name = null;

	private final AtomicLong mMemstoreSize = new AtomicLong(0);

	/**
	 * The directory for the table this region is part of.
	 * This directory contains the directory for this region.
	 */
	private final Path mTableDir;
  
	private final DBLog mLog;
	private final FileSystem mFs;
	private final Configuration mConf;
	protected final DBRegionInfo mRegionInfo;
	private final Path mRegiondir;
	
	private KeyValue.KVComparator mComparator;

	/**
	 * Set this when scheduling compaction if want the next compaction to be a
	 * major compaction.  Cleared each time through compaction code.
	 */
	private volatile boolean mForceMajorCompaction = false;

	/**
	 * Data structure of write state flags used coordinating flushes,
	 * compactions and closes.
	 */
	static class WriteState {
		// Set while a memstore flush is happening.
		private volatile boolean mFlushing = false;
		// Set when a flush has been requested.
		private volatile boolean mFlushRequested = false;
		// Set while a compaction is running.
		private volatile boolean mCompacting = false;
		// Gets set in close. If set, cannot compact or flush again.
		private volatile boolean mWritesEnabled = true;
		// Set if region is read-only
		private volatile boolean mReadOnly = false;

		/**
		 * Set flags that make this region read-only.
		 * @param onOff flip value for region r/o setting
		 */
		public synchronized void setReadOnly(final boolean onOff) {
			this.mWritesEnabled = !onOff;
			this.mReadOnly = onOff;
		}

		public boolean isReadOnly() {
			return this.mReadOnly;
		}

		public boolean isFlushRequested() {
			return this.mFlushRequested;
		}
	}

	private final WriteState mWritestate = new WriteState();

	private final long mMemstoreFlushSize;
	private volatile long mLastFlushTime;
	private final FlushRequester mFlushListener;
	private final long mBlockingMemStoreSize;
	private final long mThreadWakeFrequency;
	
	// Used to guard closes
	protected final ReentrantReadWriteLock mLock =
			new ReentrantReadWriteLock();

	// Stop updates lock
	private final ReentrantReadWriteLock mUpdatesLock =
			new ReentrantReadWriteLock();
	
	private boolean mSplitRequest;

	private final ReadWriteConsistencyControl mRwcc =
			new ReadWriteConsistencyControl();

	/**
	 * DBRegion constructor.  his constructor should only be used for testing and
	 * extensions.  Instances of DBRegion should be instantiated with the
	 * {@link DBRegion#newDBRegion(Path, DBLog, FileSystem, Configuration, DBRegionInfo, 
	 * FlushRequester)} method.
	 *
	 * @param tableDir qualified path of directory where region should be located,
	 * usually the table directory.
	 * @param log The DBLog is the outbound log for any updates to the DBRegion
	 * (There's a single DBLog for all the DBRegions on a single DBRegionServer.)
	 * The log file is a logfile from the previous execution that's
	 * custom-computed for this DBRegion. The DBRegionServer computes and sorts the
	 * appropriate log info for this DBRegion. If there is a previous log file
	 * (implying that the DBRegion has been written-to before), then read it from
	 * the supplied path.
	 * @param fs is the filesystem.
	 * @param conf is global configuration settings.
	 * @param regionInfo - DBRegionInfo that describes the region
	 * is new), then read them from the supplied path.
	 * @param flushListener an object that implements CacheFlushListener or null
	 * making progress to master -- otherwise master might think region deploy
	 * failed.  Can be null.
	 *
	 * @see DBRegion#newDBRegion(Path, DBLog, FileSystem, Configuration, DBRegionInfo, 
	 * FlushRequester)
	 */
	public DBRegion(Path tableDir, DBLog log, FileSystem fs, Configuration conf,
			DBRegionInfo regionInfo, FlushRequester flushListener) {
		this.mTableDir = tableDir;
		this.mComparator = regionInfo.getComparator();
		this.mLog = log;
		this.mFs = fs;
		this.mConf = conf;
		this.mRegionInfo = regionInfo;
		this.mFlushListener = flushListener;
		this.mThreadWakeFrequency = conf.getLong(DBConstants.THREAD_WAKE_FREQUENCY, 10 * 1000);
		this.mRegiondir = getRegionDir(this.mTableDir, this.mRegionInfo.getEncodedName());
		
		long flushSize = regionInfo.getTableDesc().getMemStoreFlushSize();
		if (flushSize == DBTableDescriptor.DEFAULT_MEMSTORE_FLUSH_SIZE) {
			flushSize = conf.getLong("bigdb.dbregion.memstore.flush.size",
					DBTableDescriptor.DEFAULT_MEMSTORE_FLUSH_SIZE);
		}
		this.mMemstoreFlushSize = flushSize;
		this.mBlockingMemStoreSize = this.mMemstoreFlushSize *
				conf.getLong("bigdb.dbregion.memstore.block.multiplier", 2);
		
		if (LOG.isDebugEnabled()) {
			// Write out region name as string and its encoded name.
			LOG.debug("Creating region: " + this + " regionDir=" + this.mRegiondir 
					+ " flushSize=" + flushSize);
		}
	}

	/**
	 * Initialize this region.
	 * @return What the next sequence (edit) id should be.
	 * @throws IOException e
	 */
	public long initialize() throws IOException {
		return initialize(null);
	}

	/**
	 * Initialize this region.
	 *
	 * @param reporter Tickle every so often if initialize is taking a while.
	 * @return What the next sequence (edit) id should be.
	 * @throws IOException e
	 */
	public long initialize(final Progressable reporter)
			throws IOException {
		if (LOG.isDebugEnabled())
			LOG.debug("initialize: reporter=" + reporter);
		
		// A region can be reopened if failed a split; reset flags
		this.mClosing.set(false);
		this.mClosed.set(false);

		// Write HRI to a file in case we need to recover .META.
		checkRegioninfoOnFilesystem();

		// Remove temporary data left over from old regions
		cleanupTmpDir();

		// Load in all the DBStores.  Get maximum seqid.
		long maxSeqId = -1;
		
		for (DBColumnDescriptor c : this.mRegionInfo.getTableDesc().getFamilies()) {
			Store store = instantiateDBStore(this.mTableDir, c);
			this.mStores.put(c.getName(), store);
			
			long storeSeqId = store.getMaxSequenceId();
			if (storeSeqId > maxSeqId) 
				maxSeqId = storeSeqId;
		}
		
		// Recover any edits if available.
		maxSeqId = replayRecoveredEditsIfAny(this.mRegiondir, maxSeqId, reporter);

		// Get rid of any splits or merges that were lost in-progress.  Clean out
		// these directories here on open.  We may be opening a region that was
		// being split but we crashed in the middle of it all.
		SplitTransaction.cleanupAnySplitDetritus(this);
		deleteDirectory(this.mFs, new Path(mRegiondir, MERGEDIR));

		// See if region is meant to run read-only.
		if (this.mRegionInfo.getTableDesc().isReadOnly()) 
			this.mWritestate.setReadOnly(true);

		this.mWritestate.mCompacting = false;
		this.mLastFlushTime = System.currentTimeMillis(); 
		
		// Use maximum of log sequenceid or that which was found in stores
		// (particularly if no recovered edits, seqid will be -1).
		long nextSeqid = maxSeqId + 1;
		if (LOG.isInfoEnabled())
			LOG.info("Onlined " + this.toString() + "; next sequenceid=" + nextSeqid);
		
		return nextSeqid;
	}

	/**
	 * Delete if exists.
	 * @param fs filesystem object
	 * @param dir directory to delete
	 * @return True if deleted <code>dir</code>
	 * @throws IOException e
	 */
	public static boolean deleteDirectory(final FileSystem fs, 
			final Path dir) throws IOException {
		return fs.exists(dir) && fs.delete(dir, true);
	}
  
	/**
	 * Move any passed DBStore files into place (if any).  Used to pick up split
	 * files and any merges from splits and merges dirs.
	 * @param initialFiles
	 * @throws IOException
	 */
	static void moveInitialFilesIntoPlace(final FileSystem fs,
			final Path initialFiles, final Path regiondir) throws IOException {
		if (initialFiles != null && fs.exists(initialFiles)) 
			fs.rename(initialFiles, regiondir);
	}

	/**
	 * @return True if this region has references.
	 */
	public boolean hasReferences() {
		for (Store store : this.mStores.values()) {
			for (StoreFile sf : store.getStorefiles()) {
				// Found a reference, return.
				if (sf.isReference()) return true;
			}
		}
		return false;
	}

	/**
	 * Write out an info file under the region directory.  Useful recovering
	 * mangled regions.
	 * @throws IOException
	 */
	private void checkRegioninfoOnFilesystem() throws IOException {
		// Name of this file has two leading and trailing underscores so it doesn't
		// clash w/ a store/family name.  There is possibility, but assumption is
		// that its slim (don't want to use control character in filename because
		//
		Path regioninfo = new Path(this.mRegiondir, REGIONINFO_FILE);
		if (this.mFs.exists(regioninfo) && this.mFs.getFileStatus(regioninfo).getLen() > 0) 
			return;
    
		FSDataOutputStream out = this.mFs.create(regioninfo, true);
		try {
			this.mRegionInfo.write(out);
			out.write('\n');
			out.write('\n');
			out.write(Bytes.toBytes(this.mRegionInfo.toString()));
		} finally {
			out.close();
		}
	}

	/** @return a DBRegionInfo object for this region */
	public DBRegionInfo getRegionInfo() {
		return this.mRegionInfo;
	}

	/** @return true if region is closed */
	public boolean isClosed() {
		return this.mClosed.get();
	}

	/** @return True if closing process has started. */
	public boolean isClosing() {
		return this.mClosing.get();
	}

	public ReadWriteConsistencyControl getRWCC() {
		return mRwcc;
	}

	/**
	 * Close down this DBRegion.  Flush the cache, shut down each DBStore, don't
	 * service any more calls.
	 *
	 * <p>This method could take some time to execute, so don't call it from a
	 * time-sensitive thread.
	 *
	 * @return Vector of all the storage files that the DBRegion's component
	 * DBStores make use of.  It's a list of all DBStoreFile objects. Returns empty
	 * vector if already closed and null if judged that it should not close.
	 *
	 * @throws IOException e
	 */
	public List<StoreFile> close() throws IOException {
		return close(false);
	}

	/**
	 * Close down this DBRegion.  Flush the cache unless abort parameter is true,
	 * Shut down each DBStore, don't service any more calls.
	 *
	 * This method could take some time to execute, so don't call it from a
	 * time-sensitive thread.
	 *
	 * @param abort true if server is aborting (only during testing)
	 * @return Vector of all the storage files that the DBRegion's component
	 * DBStores make use of.  It's a list of DBStoreFile objects.  Can be null if
	 * we are not to close at this time or we are already closed.
	 *
	 * @throws IOException e
	 */
	public List<StoreFile> close(final boolean abort)
			throws IOException {
		if (LOG.isDebugEnabled())
			LOG.debug("close: abort=" + abort);
		
		if (isClosed()) {
			if (LOG.isWarnEnabled())
				LOG.warn("Region " + this + " already closed");
			return null;
		}
		
		boolean wasFlushing = false;
		synchronized (mWritestate) {
			// Disable compacting and flushing by background threads for this
			// region.
			mWritestate.mWritesEnabled = false;
			wasFlushing = mWritestate.mFlushing;
			
			if (LOG.isDebugEnabled())
				LOG.debug("Closing " + this + ": disabling compactions & flushes");
			
			while (mWritestate.mCompacting || mWritestate.mFlushing) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("waiting for" +
							(mWritestate.mCompacting ? " compaction" : "") +
							(mWritestate.mFlushing ?
									(mWritestate.mCompacting ? "," : "") + " cache flush" :
									"") + " to complete for region " + this);
				}
				
				try {
					mWritestate.wait();
				} catch (InterruptedException iex) {
					// continue
				}
			}
		}
		
		// If we were not just flushing, is it worth doing a preflush...one
		// that will clear out of the bulk of the memstore before we put up
		// the close flag?
		if (!abort && !wasFlushing && worthPreFlushing()) {
			if (LOG.isInfoEnabled())
				LOG.info("Running close preflush of " + this.getRegionNameAsString());
			
			if (internalFlushcache()) {
				//if (this.mFlushListener == null)
				//	compactStores(true);
			}
		}
		
		this.mClosing.set(true);
		mLock.writeLock().lock();
		
		try {
			if (this.isClosed()) {
				// SplitTransaction handles the null
				return null;
			}
			
			if (LOG.isDebugEnabled())
				LOG.debug("Updates disabled for region " + this);
			
			// Don't flush the cache if we are aborting
			if (!abort) {
				if (internalFlushcache()) { 
					//if (this.mFlushListener == null)
					//	compactStores(true);
				}
			}

			List<StoreFile> result = new ArrayList<StoreFile>();
			for (Store store : mStores.values()) {
				result.addAll(store.close());
			}
			
			this.mClosed.set(true);
			
			if (LOG.isInfoEnabled())
				LOG.info("Closed " + this);
			
			return result;
		} finally {
			mLock.writeLock().unlock();
		}
	}

	/**
	 * @return True if its worth doing a flush before we put up the close flag.
	 */
	private boolean worthPreFlushing() {
		return this.mMemstoreSize.get() >
			this.mConf.getLong("bigdb.dbregion.preclose.flush.size", 1024 * 1024 * 5);
	}

	//////////////////////////////////////////////////////////////////////////////
	// DBRegion accessors
	//////////////////////////////////////////////////////////////////////////////

	/** @return start key for region */
	public byte[] getStartKey() {
		return this.mRegionInfo.getStartKey();
	}

	/** @return end key for region */
	public byte[] getEndKey() {
		return this.mRegionInfo.getEndKey();
	}

	/** @return region id */
	public long getRegionId() {
		return this.mRegionInfo.getRegionId();
	}

	/** @return region name */
	public byte[] getRegionName() {
		return this.mRegionInfo.getRegionName();
	}

	/** @return region name as string for logging */
	public String getRegionNameAsString() {
		return this.mRegionInfo.getRegionNameAsString();
	}

	/** @return DBTableDescriptor for this region */
	public DBTableDescriptor getTableDesc() {
		return this.mRegionInfo.getTableDesc();
	}

	/** @return DBLog in use for this region */
	public DBLog getLog() {
		return this.mLog;
	}

	/** @return Configuration object */
	public Configuration getConf() {
		return this.mConf;
	}

	/** @return region directory Path */
	public Path getRegionDir() {
		return this.mRegiondir;
	}

	/**
	 * Computes the Path of the DBRegion
	 *
	 * @param tabledir qualified path for table
	 * @param name ENCODED region name
	 * @return Path of DBRegion directory
	 */
	public static Path getRegionDir(final Path tabledir, final String name) {
		return new Path(tabledir, name);
	}

	/** @return FileSystem being used by this region */
	public FileSystem getFilesystem() {
		return this.mFs;
	}

	/** @return the last time the region was flushed */
	public long getLastFlushTime() {
		return this.mLastFlushTime;
	}

	//////////////////////////////////////////////////////////////////////////////
	// DBRegion maintenance.
	//
	// These methods are meant to be called periodically by the DBRegionServer for
	// upkeep.
	//////////////////////////////////////////////////////////////////////////////

	/** @return returns size of largest DBStore. */
	public long getLargestDBStoreSize() {
		long size = 0;
		for (Store h: mStores.values()) {
			long storeSize = h.getSize();
			if (storeSize > size) 
				size = storeSize;
		}
		return size;
	}

	/**
	 * Do preparation for pending compaction.
	 * @throws IOException
	 */
	private void doRegionCompactionPrep() throws IOException {
	}

	/**
	 * Removes the temporary directory for this Store.
	 */
	private void cleanupTmpDir() throws IOException {
		DBRegion.deleteDirectory(this.mFs, getTmpDir());
	}
  
	/**
	 * Get the temporary diretory for this region. This directory
	 * will have its contents removed when the region is reopened.
	 */
	protected Path getTmpDir() {
		return new Path(getRegionDir(), ".tmp");
	}

	protected void setForceMajorCompaction(final boolean b) {
		this.mForceMajorCompaction = b;
	}

	protected boolean getForceMajorCompaction() {
		return this.mForceMajorCompaction;
	}

	/**
	 * Called by compaction thread and after region is opened to compact the
	 * DBStores if necessary.
	 *
	 * <p>This operation could block for a long time, so don't call it from a
	 * time-sensitive thread.
	 *
	 * Note that no locking is necessary at this level because compaction only
	 * conflicts with a region split, and that cannot happen because the region
	 * server does them sequentially and not in parallel.
	 *
	 * @return mid key if split is needed
	 * @throws IOException e
	 */
	public byte[] compactStores() throws IOException {
		boolean majorCompaction = this.mForceMajorCompaction;
		this.mForceMajorCompaction = false;
		return compactStores(majorCompaction);
	}

	/**
	 * Called by compaction thread and after region is opened to compact the
	 * DBStores if necessary.
	 *
	 * <p>This operation could block for a long time, so don't call it from a
	 * time-sensitive thread.
	 *
	 * Note that no locking is necessary at this level because compaction only
	 * conflicts with a region split, and that cannot happen because the region
	 * server does them sequentially and not in parallel.
	 *
	 * @param majorCompaction True to force a major compaction regardless of thresholds
	 * @return split row if split is needed
	 * @throws IOException e
	 */
	public byte[] compactStores(final boolean majorCompaction)
			throws IOException {
		if (LOG.isDebugEnabled())
			LOG.debug("compactStores: majorCompaction=" + majorCompaction);
		
		if (this.mClosing.get()) {
			if (LOG.isDebugEnabled())
				LOG.debug("Skipping compaction on " + this + " because closing");
			return null;
		}
		
		mLock.readLock().lock();
		try {
			if (this.mClosed.get()) {
				if (LOG.isDebugEnabled())
					LOG.debug("Skipping compaction on " + this + " because closed");
				return null;
			}
			
			byte[] splitRow = null;
			if (this.mClosed.get()) 
				return splitRow;
      
			try {
				synchronized (mWritestate) {
					if (!mWritestate.mCompacting && mWritestate.mWritesEnabled) {
						mWritestate.mCompacting = true;
					} else {
						if (LOG.isInfoEnabled()) {
							LOG.info("NOT compacting region " + this +
									": compacting=" + mWritestate.mCompacting + 
									", writesEnabled=" + mWritestate.mWritesEnabled);
						}
						return splitRow;
					}
				}
				
				if (LOG.isInfoEnabled()) {
					LOG.info("Starting" + (majorCompaction? " major " : " ") +
							"compaction on region " + this);
				}
				
				long startTime = System.currentTimeMillis();
				doRegionCompactionPrep();
				
				long maxSize = -1;
				for (Store store: mStores.values()) {
					final Store.StoreSize ss = store.compact(majorCompaction);
					if (ss != null && ss.getSize() > maxSize) {
						maxSize = ss.getSize();
						splitRow = ss.getSplitRow();
					}
				}
				
				String timeTaken = StringUtils.formatTimeDiff(System.currentTimeMillis(), startTime);
				if (LOG.isInfoEnabled()) 
					LOG.info("compaction completed on region " + this + " in " + timeTaken);
				
			} finally {
				synchronized (mWritestate) {
					mWritestate.mCompacting = false;
					mWritestate.notifyAll();
				}
			}
			return splitRow;
		} finally {
			mLock.readLock().unlock();
		}
	}

	/**
	 * Flush the cache.
	 *
	 * When this method is called the cache will be flushed unless:
	 * <ol>
	 *   <li>the cache is empty</li>
	 *   <li>the region is closed.</li>
	 *   <li>a flush is already in progress</li>
	 *   <li>writes are disabled</li>
	 * </ol>
	 *
	 * <p>This method may block for some time, so it should not be called from a
	 * time-sensitive thread.
	 *
	 * @return true if cache was flushed
	 *
	 * @throws IOException general io exceptions
	 * @throws DroppedSnapshotException Thrown when replay of dblog is required
	 * because a Snapshot was not properly persisted.
	 */
	public boolean flushcache() throws IOException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("flushcache: " + this + " memstoreSize=" 
					+ mMemstoreSize.get());
		}
		
		// fail-fast instead of waiting on the lock
		if (this.mClosing.get()) {
			if (LOG.isDebugEnabled())
				LOG.debug("Skipping flush on " + this + " because closing");
			return false;
		}
		
		mLock.readLock().lock();
		try {
			if (this.mClosed.get()) {
				if (LOG.isDebugEnabled())
					LOG.debug("Skipping flush on " + this + " because closed");
				return false;
			}
			
			try {
				synchronized (mWritestate) {
					if (!mWritestate.mFlushing && mWritestate.mWritesEnabled) {
						this.mWritestate.mFlushing = true;
						
					} else {
						if (LOG.isDebugEnabled()) {
							LOG.debug("NOT flushing memstore for region " + this +
									", flushing=" + mWritestate.mFlushing + 
									", writesEnabled=" + mWritestate.mWritesEnabled);
						}
						return false;
					}
				}
				
				return internalFlushcache();
			} finally {
				synchronized (mWritestate) {
					this.mWritestate.mFlushing = false;
					this.mWritestate.mFlushRequested = false;
					this.mWritestate.notifyAll();
				}
			}
		} finally {
			mLock.readLock().unlock();
		}
	}

	/**
	 * Flush the memstore.
	 * 
	 * Flushing the memstore is a little tricky. We have a lot of updates in the
	 * memstore, all of which have also been written to the log. We need to
	 * write those updates in the memstore out to disk, while being able to
	 * process reads/writes as much as possible during the flush operation. Also,
	 * the log has to state clearly the point in time at which the memstore was
	 * flushed. (That way, during recovery, we know when we can rely on the
	 * on-disk flushed structures and when we have to recover the memstore from
	 * the log.)
	 *
	 * <p>So, we have a three-step process:
	 *
	 * <ul><li>A. Flush the memstore to the on-disk stores, noting the current
	 * sequence ID for the log.<li>
	 *
	 * <li>B. Write a FLUSHCACHE-COMPLETE message to the log, using the sequence
	 * ID that was current at the time of memstore-flush.</li>
	 *
	 * <li>C. Get rid of the memstore structures that are now redundant, as
	 * they've been flushed to the on-disk DBStores.</li>
	 * </ul>
	 * <p>This method is protected, but can be accessed via several public
	 * routes.
	 *
	 * <p> This method may block for some time.
	 *
	 * @return true if the region needs compacting
	 *
	 * @throws IOException general io exceptions
	 * @throws DroppedSnapshotException Thrown when replay of dblog is required
	 * because a Snapshot was not properly persisted.
	 */
	protected boolean internalFlushcache() throws IOException {
		return internalFlushcache(this.mLog, -1);
	}

	/**
	 * @param wal Null if we're NOT to go via dblog/wal.
	 * @param myseqid The seqid to use if <code>wal</code> is null writing out
	 * flush file.
	 * @return true if the region needs compacting
	 * @throws IOException
	 * @see {@link #internalFlushcache()}
	 */
	protected boolean internalFlushcache(final DBLog wal, final long myseqid)
			throws IOException {
		if (LOG.isDebugEnabled())
			LOG.debug("internalFlushcache: wal=" + wal + " seqid=" + myseqid);
		
		final long startTime = System.currentTimeMillis();
		
		// Clear flush flag.
		// Record latest flush time
		this.mLastFlushTime = startTime;
		
		// If nothing to flush, return and avoid logging start/stop flush.
		if (this.mMemstoreSize.get() <= 0) 
			return false;
    
		if (LOG.isDebugEnabled()) {
			LOG.debug("Started memstore flush for region " + this +
					". Current region memstore size " + StringUtils.humanReadableInt(this.mMemstoreSize.get()) +
					((wal != null)? "": "; wal is null, using passed sequenceid=" + myseqid));
		}

		// Stop updates while we snapshot the memstore of all stores. We only have
		// to do this for a moment.  Its quick.  The subsequent sequence id that
		// goes into the DBLog after we've flushed all these snapshots also goes
		// into the info file that sits beside the flushed files.
		// We also set the memstore size to zero here before we allow updates
		// again so its value will represent the size of the updates received
		// during the flush
		long sequenceId = -1L;
		long completeSequenceId = -1L;

		// We have to take a write lock during snapshot, or else a write could
		// end up in both snapshot and memstore (makes it difficult to do atomic
		// rows then)
		this.mUpdatesLock.writeLock().lock();
		
		final long currentMemStoreSize = this.mMemstoreSize.get();
		List<StoreFlusher> storeFlushers = new ArrayList<StoreFlusher>(mStores.size());
		
		try {
			sequenceId = (wal == null)? myseqid: wal.startCacheFlush();
			completeSequenceId = this.getCompleteCacheFlushSequenceId(sequenceId);

			for (Store s : mStores.values()) {
				storeFlushers.add(s.getStoreFlusher(completeSequenceId));
			}

			// prepare flush (take a snapshot)
			for (StoreFlusher flusher : storeFlushers) {
				flusher.prepare();
			}
		} finally {
			this.mUpdatesLock.writeLock().unlock();
		}

		if (LOG.isDebugEnabled())
			LOG.debug("Finished snapshotting, commencing flushing stores");

		// Any failure from here on out will be catastrophic requiring server
		// restart so dblog content can be replayed and put back into the memstore.
		// Otherwise, the snapshot content while backed up in the dblog, it will not
		// be part of the current running servers state.
		boolean compactionRequested = false;
		
		try {
			// A.  Flush memstore to all the DBStores.
			// Keep running vector of all store files that includes both old and the
			// just-made new flush store file.
			for (StoreFlusher flusher : storeFlushers) {
				flusher.flushCache();
			}
      
			// Switch snapshot (in memstore) -> new dbfile (thus causing
			// all the store scanners to reset/reseek).
			for (StoreFlusher flusher : storeFlushers) {
				boolean needsCompaction = flusher.commit();
				if (needsCompaction) 
					compactionRequested = true;
			}
			storeFlushers.clear();

			// Set down the memstore size by amount of flush.
			this.mMemstoreSize.addAndGet(-currentMemStoreSize);
		} catch (Throwable t) {
			// An exception here means that the snapshot was not persisted.
			// The dblog needs to be replayed so its content is restored to memstore.
			// Currently, only a server restart will do this.
			// We used to only catch IOEs but its possible that we'd get other
			// exceptions -- e.g. HBASE-659 was about an NPE -- so now we catch
			// all and sundry.
			if (wal != null) wal.abortCacheFlush();
			
			DroppedSnapshotException dse = new DroppedSnapshotException("region: " +
					Bytes.toStringBinary(getRegionName()));
			dse.initCause(t);
			throw dse;
		}

		// If we get to here, the DBStores have been written. If we get an
		// error in completeCacheFlush it will release the lock it is holding

		// B.  Write a FLUSHCACHE-COMPLETE message to the log.
		//     This tells future readers that the DBStores were emitted correctly,
		//     and that all updates to the log for this regionName that have lower
		//     log-sequence-ids can be safely ignored.
		if (wal != null) {
			wal.completeCacheFlush(getRegionName(),
					mRegionInfo.getTableDesc().getName(), completeSequenceId,
					this.getRegionInfo().isMetaRegion());
		}

		// C. Finally notify anyone waiting on memstore to clear:
		// e.g. checkResources().
		synchronized (this) {
			notifyAll(); // FindBugs NN_NAKED_NOTIFY
		}

		if (LOG.isDebugEnabled()) {
			long now = System.currentTimeMillis(); 
			LOG.debug("Finished memstore flush of ~" +
					StringUtils.humanReadableInt(currentMemStoreSize) + " for region " +
					this + " in " + (now - startTime) + "ms, sequenceid=" + sequenceId +
					", compaction requested=" + compactionRequested +
					((wal == null)? "; wal=null": ""));
		}
		
		return compactionRequested;
	}

	/**
	 * Get the sequence number to be associated with this cache flush. Used by
	 * TransactionalRegion to not complete pending transactions.
	 *
	 *
	 * @param currentSequenceId
	 * @return sequence id to complete the cache flush with
	 */
	protected long getCompleteCacheFlushSequenceId(long currentSequenceId) {
		return currentSequenceId;
	}

	//////////////////////////////////////////////////////////////////////////////
	// get() methods for client use.
	//////////////////////////////////////////////////////////////////////////////
	/**
	 * Return all the data for the row that matches <i>row</i> exactly,
	 * or the one that immediately preceeds it, at or immediately before
	 * <i>ts</i>.
	 *
	 * @param row row key
	 * @return map of values
	 * @throws IOException
	 */
	protected Result getClosestRowBefore(final byte[] row)
			throws IOException{
		return getClosestRowBefore(row, DBConstants.CATALOG_FAMILY);
	}

	/**
	 * Return all the data for the row that matches <i>row</i> exactly,
	 * or the one that immediately preceeds it, at or immediately before
	 * <i>ts</i>.
	 *
	 * @param row row key
	 * @param family column family to find on
	 * @return map of values
	 * @throws IOException read exceptions
	 */
	public Result getClosestRowBefore(final byte[] row, final byte[] family)
			throws IOException {
		// look across all the DBStores for this region and determine what the
		// closest key is across all column families, since the data may be sparse
		KeyValue key = null;
		
		checkRow(row);
		startRegionOperation();
		
		try {
			Store store = getStore(family);
			KeyValue kv = new KeyValue(row, DBConstants.LATEST_TIMESTAMP);
			// get the closest key. (DBStore.getRowKeyAtOrBefore can return null)
			key = store.getRowKeyAtOrBefore(kv);
			if (key == null) 
				return null;
			
			Get get = new Get(key.getRow());
			get.addFamily(family);
			return get(get, null);
		} finally {
			closeRegionOperation();
		}
	}

	/**
	 * Return an iterator that scans over the DBRegion, returning the indicated
	 * columns and rows specified by the {@link Scan}.
	 * <p>
	 * This Iterator must be closed by the caller.
	 *
	 * @param scan configured {@link Scan}
	 * @return InternalScanner
	 * @throws IOException read exceptions
	 */
	public InternalScanner getScanner(Scan scan) throws IOException {
		return getScanner(scan, null);
	}

	protected InternalScanner getScanner(Scan scan, List<KeyValueScanner> additionalScanners) 
			throws IOException {
		if (LOG.isDebugEnabled()) 
			LOG.debug("getScanner: scan=" + scan);
		
		startRegionOperation();
		try {
			// Verify families are all valid
			if (scan.hasFamilies()) {
				for (byte[] family : scan.getFamilyMap().keySet()) {
					checkFamily(family);
				}
			} else { // Adding all families to scanner
				for (byte[] family: mRegionInfo.getTableDesc().getFamiliesKeys()){
					scan.addFamily(family);
				}
			}
			return instantiateInternalScanner(scan, additionalScanners);
		} finally {
			closeRegionOperation();
		}
	}

	protected InternalScanner instantiateInternalScanner(Scan scan, 
			List<KeyValueScanner> additionalScanners) throws IOException {
		if (LOG.isDebugEnabled())
			LOG.debug("instantiateInternalScanner: scan=" + scan + " additionalScanners=" + additionalScanners);
		return new RegionScanner(scan, additionalScanners);
	}

	/**
	 * @param delete The passed delete is modified by this method. WARNING!
	 */
	private void prepareDelete(Delete delete) throws IOException {
		// Check to see if this is a deleteRow insert
		if (delete.getFamilyMap().isEmpty()){
			for (byte[] family : mRegionInfo.getTableDesc().getFamiliesKeys()){
				// Don't eat the timestamp
				delete.deleteFamily(family, delete.getTimeStamp());
			}
		} else {
			for (byte[] family : delete.getFamilyMap().keySet()) {
				if (family == null) 
					throw new NoSuchColumnFamilyException("Empty family is invalid");
				
				checkFamily(family);
			}
		}
	}

	//////////////////////////////////////////////////////////////////////////////
	// set() methods for client use.
	//////////////////////////////////////////////////////////////////////////////
	/**
	 * @param delete delete object
	 * @param lockid existing lock id, or null for grab a lock
	 * @param writeToWAL append to the write ahead lock or not
	 * @throws IOException read exceptions
	 */
	public void delete(Delete delete, Integer lockid, boolean writeToWAL)
			throws IOException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("put: delete=" + delete + " lockid=" + lockid 
					+ " writeToWAL=" + writeToWAL);
		}
		
		checkReadOnly();
		checkResources();
		
		Integer lid = null;
		startRegionOperation();
		try {
			byte[] row = delete.getRow();
			// If we did not pass an existing row lock, obtain a new one
			lid = getLock(lockid, row, true);

			// All edits for the given row (across all column families) must happen atomically.
			prepareDelete(delete);
			delete(delete.getFamilyMap(), writeToWAL);

		} finally {
			if (lockid == null) releaseRowLock(lid);
			closeRegionOperation();
		}
	}

	/**
	 * @param familyMap map of family to edits for the given family.
	 * @param writeToWAL
	 * @throws IOException
	 */
	public void delete(Map<byte[], List<KeyValue>> familyMap, 
			boolean writeToWAL) throws IOException {
		long now = System.currentTimeMillis(); 
		byte[] byteNow = Bytes.toBytes(now);
		boolean flush = false;

		mUpdatesLock.readLock().lock();

		try {
			for (Map.Entry<byte[], List<KeyValue>> e : familyMap.entrySet()) {
				byte[] family = e.getKey();
				List<KeyValue> kvs = e.getValue();
				
				Map<byte[], Integer> kvCount = new TreeMap<byte[], Integer>(Bytes.BYTES_COMPARATOR);

				for (KeyValue kv: kvs) {
					//  Check if time is LATEST, change to time of most recent addition if so
					//  This is expensive.
					if (kv.isLatestTimestamp() && kv.isDeleteType()) {
						byte[] qual = kv.getQualifier();
						if (qual == null) qual = DBConstants.EMPTY_BYTE_ARRAY;

						Integer count = kvCount.get(qual);
						if (count == null) {
							kvCount.put(qual, 1);
						} else {
							kvCount.put(qual, count + 1);
						}
						count = kvCount.get(qual);

						Get get = new Get(kv.getRow());
						get.setMaxVersions(count);
						get.addColumn(family, qual);

						List<KeyValue> result = get(get);

						if (result.size() < count) {
							// Nothing to delete
							kv.updateLatestStamp(byteNow);
							continue;
						}
						if (result.size() > count) {
							throw new RuntimeException("Unexpected size: " + result.size());
						}
						
						KeyValue getkv = result.get(count - 1);
						Bytes.putBytes(kv.getBuffer(), kv.getTimestampOffset(),
								getkv.getBuffer(), getkv.getTimestampOffset(), Bytes.SIZEOF_LONG);
					} else {
						kv.updateLatestStamp(byteNow);
					}
				}
			}

			if (writeToWAL) {
				// write/sync to WAL should happen before we touch memstore.
				//
				// If order is reversed, i.e. we write to memstore first, and
				// for some reason fail to write/sync to commit log, the memstore
				// will contain uncommitted transactions.
				//
				// bunch up all edits across all column families into a
				// single WALEdit.
				WALEdit walEdit = new WALEdit();
				addFamilyMapToWALEdit(familyMap, walEdit);
				this.mLog.append(mRegionInfo, mRegionInfo.getTableDesc().getName(), walEdit, now);
			}

			// Now make changes to the memstore.
			long addedSize = applyFamilyMapToMemstore(familyMap);
			flush = isFlushSize(mMemstoreSize.addAndGet(addedSize));
		} finally {
			this.mUpdatesLock.readLock().unlock();
		}

		if (flush) {
			// Request a cache flush.  Do it outside update lock.
			requestFlush(true);
		}
	}

	/**
	 * @param put
	 * @throws IOException
	 */
	public void put(Put put) throws IOException {
		this.put(put, null, put.getWriteToWAL());
	}

	/**
	 * @param put
	 * @param writeToWAL
	 * @throws IOException
	 */
	public void put(Put put, boolean writeToWAL) throws IOException {
		this.put(put, null, writeToWAL);
	}

	/**
	 * @param put
	 * @param lockid
	 * @throws IOException
	 */
	public void put(Put put, Integer lockid) throws IOException {
		this.put(put, lockid, put.getWriteToWAL());
	}

	/**
	 * @param put
	 * @param lockid
	 * @param writeToWAL
	 * @throws IOException
	 */
	public void put(Put put, Integer lockid, boolean writeToWAL)
			throws IOException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("put: put=" + put + " lockid=" + lockid 
					+ " writeToWAL=" + writeToWAL);
		}
		
		checkReadOnly();

		// Do a rough check that we have resources to accept a write.  The check is
		// 'rough' in that between the resource check and the call to obtain a
		// read lock, resources may run out.  For now, the thought is that this
		// will be extremely rare; we'll deal with it when it happens.
		checkResources();
		startRegionOperation();
		
		try {
			// We obtain a per-row lock, so other clients will block while one client
			// performs an update. The read lock is released by the client calling
			// #commit or #abort or if the DBRegionServer lease on the lock expires.
			// See DBRegionServer#RegionListener for how the expire on DBRegionServer
			// invokes a DBRegion#abort.
			byte[] row = put.getRow();
			
			// If we did not pass an existing row lock, obtain a new one
			Integer lid = getLock(lockid, row, true);

			try {
				// All edits for the given row (across all column families) must happen atomically.
				put(put.getFamilyMap(), writeToWAL);
			} finally {
				if (lockid == null) releaseRowLock(lid);
			}
		} finally {
			closeRegionOperation();
		}
	}

	/**
	 * Struct-like class that tracks the progress of a batch operation,
	 * accumulating status codes and tracking the index at which processing
	 * is proceeding.
	 */
	private static class BatchOperationInProgress<T> {
		private T[] mOperations;
		private OperationStatusCode[] mRetCodes;
		private int mNextIndexToProcess = 0;

		public BatchOperationInProgress(T[] operations) {
			this.mOperations = operations;
			this.mRetCodes = new OperationStatusCode[operations.length];
			Arrays.fill(this.mRetCodes, OperationStatusCode.NOT_RUN);
		}
    
		public boolean isDone() {
			return mNextIndexToProcess == mOperations.length;
		}
	}
  
	/**
	 * Perform a batch put with no pre-specified locks
	 * @see DBRegion#put(Pair[])
	 */
	public OperationStatusCode[] put(Put[] puts) throws IOException {
		@SuppressWarnings("unchecked")
		Pair<Put, Integer> putsAndLocks[] = new Pair[puts.length];

		for (int i = 0; i < puts.length; i++) {
			putsAndLocks[i] = new Pair<Put, Integer>(puts[i], null);
		}
		
		return put(putsAndLocks);
	}
  
	/**
	 * Perform a batch of puts.
	 * @param putsAndLocks the list of puts paired with their requested lock IDs.
	 * @throws IOException
	 */
	public OperationStatusCode[] put(Pair<Put, Integer>[] putsAndLocks) 
			throws IOException {
		BatchOperationInProgress<Pair<Put, Integer>> batchOp =
				new BatchOperationInProgress<Pair<Put,Integer>>(putsAndLocks);
    
		while (!batchOp.isDone()) {
			checkReadOnly();
			checkResources();

			long newSize;
			startRegionOperation();
			try {
				long addedSize = doMiniBatchPut(batchOp);
				newSize = mMemstoreSize.addAndGet(addedSize);
			} finally {
				closeRegionOperation();
			}
			
			if (isFlushSize(newSize)) 
				requestFlush(true);
		}
		
		return batchOp.mRetCodes;
	}

	private long doMiniBatchPut(BatchOperationInProgress<Pair<Put, Integer>> batchOp)
			throws IOException {
		long now = System.currentTimeMillis(); 
		byte[] byteNow = Bytes.toBytes(now);

		/** Keep track of the locks we hold so we can release them in finally clause */
		List<Integer> acquiredLocks = Lists.newArrayListWithCapacity(batchOp.mOperations.length);
		
		// We try to set up a batch in the range [firstIndex,lastIndexExclusive)
		int firstIndex = batchOp.mNextIndexToProcess;
		int lastIndexExclusive = firstIndex;
		boolean success = false;
		
		try {
			// ------------------------------------
			// STEP 1. Try to acquire as many locks as we can, and ensure
			// we acquire at least one.
			// ----------------------------------
			int numReadyToWrite = 0;
			while (lastIndexExclusive < batchOp.mOperations.length) {
				Pair<Put, Integer> nextPair = batchOp.mOperations[lastIndexExclusive];
				Put put = nextPair.getFirst();
				Integer providedLockId = nextPair.getSecond();

				// Check the families in the put. If bad, skip this one.
				try {
					checkFamilies(put.getFamilyMap().keySet());
				} catch (NoSuchColumnFamilyException nscf) {
					if (LOG.isWarnEnabled())
						LOG.warn("No such column family in batch put", nscf);
					
					batchOp.mRetCodes[lastIndexExclusive] = OperationStatusCode.BAD_FAMILY;
					lastIndexExclusive++;
					continue;
				}

				// If we haven't got any rows in our batch, we should block to
				// get the next one.
				boolean shouldBlock = numReadyToWrite == 0;
				Integer acquiredLockId = getLock(providedLockId, put.getRow(), shouldBlock);
				
				if (acquiredLockId == null) {
					// We failed to grab another lock
					assert !shouldBlock : "Should never fail to get lock when blocking";
					break; // stop acquiring more rows for this batch
				}
				if (providedLockId == null) {
					acquiredLocks.add(acquiredLockId);
				}
				
				lastIndexExclusive++;
				numReadyToWrite++;
			}
			
			// We've now grabbed as many puts off the list as we can
			assert numReadyToWrite > 0;

			// ------------------------------------
			// STEP 2. Update any LATEST_TIMESTAMP timestamps
			// ----------------------------------
			for (int i = firstIndex; i < lastIndexExclusive; i++) {
				updateKVTimestamps(
						batchOp.mOperations[i].getFirst().getFamilyMap().values(),
						byteNow);
			}
      
			// ------------------------------------
			// STEP 3. Write to WAL
			// ----------------------------------
			WALEdit walEdit = new WALEdit();
			for (int i = firstIndex; i < lastIndexExclusive; i++) {
				// Skip puts that were determined to be invalid during preprocessing
				if (batchOp.mRetCodes[i] != OperationStatusCode.NOT_RUN) continue;
        
				Put p = batchOp.mOperations[i].getFirst();
				if (!p.getWriteToWAL()) continue;
				addFamilyMapToWALEdit(p.getFamilyMap(), walEdit);
			}
      
			// Append the edit to WAL
			this.mLog.append(mRegionInfo, mRegionInfo.getTableDesc().getName(), walEdit, now);

			// ------------------------------------
			// STEP 4. Write back to memstore
			// ----------------------------------
			long addedSize = 0;
			for (int i = firstIndex; i < lastIndexExclusive; i++) {
				if (batchOp.mRetCodes[i] != OperationStatusCode.NOT_RUN) continue;

				Put p = batchOp.mOperations[i].getFirst();
				addedSize += applyFamilyMapToMemstore(p.getFamilyMap());
				batchOp.mRetCodes[i] = OperationStatusCode.SUCCESS;
			}
			
			success = true;
			return addedSize;
			
		} finally {
			for (Integer toRelease : acquiredLocks) {
				releaseRowLock(toRelease);
			}
			if (!success) {
				for (int i = firstIndex; i < lastIndexExclusive; i++) {
					if (batchOp.mRetCodes[i] == OperationStatusCode.NOT_RUN) {
						batchOp.mRetCodes[i] = OperationStatusCode.FAILURE;
					}
				}
			}
			batchOp.mNextIndexToProcess = lastIndexExclusive;
		}
	}

	//TODO, Think that gets/puts and deletes should be refactored a bit so that
	//the getting of the lock happens before, so that you would just pass it into
	//the methods. So in the case of checkAndMutate you could just do lockRow,
	//get, put, unlockRow or something
	/**
	 * @param row
	 * @param family
	 * @param qualifier
	 * @param expectedValue
	 * @param lockId
	 * @param writeToWAL
	 * @throws IOException
	 * @return true if the new put was execute, false otherwise
	 */
	public boolean checkAndMutate(byte[] row, byte[] family, byte[] qualifier,
			byte[] expectedValue, Writable w, Integer lockId, boolean writeToWAL)
					throws IOException{
		checkReadOnly();
		
		//TODO, add check for value length or maybe even better move this to the
		//client if this becomes a global setting
		checkResources();
		
		boolean isPut = w instanceof Put;
		if (!isPut && !(w instanceof Delete))
			throw new IOException("Action must be Put or Delete");

		startRegionOperation();
		try {
			RowLock lock = isPut ? ((Put)w).getRowLock() : ((Delete)w).getRowLock();
			Get get = new Get(row, lock);
			checkFamily(family);
			get.addColumn(family, qualifier);

			// Lock row
			Integer lid = getLock(lockId, get.getRow(), true);
			List<KeyValue> result = new ArrayList<KeyValue>();
			
			try {
				result = get(get);

				boolean matches = false;
				if (result.size() == 0 && (expectedValue == null || expectedValue.length == 0)) {
					matches = true;
				} else if (result.size() == 1) {
					//Compare the expected value with the actual value
					byte[] actualValue = result.get(0).getValue();
					matches = Bytes.equals(expectedValue, actualValue);
				}
				
				//If matches put the new put or delete the new delete
				if (matches) {
					// All edits for the given row (across all column families) must happen atomically.
					if (isPut) {
						put(((Put)w).getFamilyMap(), writeToWAL);
					} else {
						Delete d = (Delete)w;
						prepareDelete(d);
						delete(d.getFamilyMap(), writeToWAL);
					}
					return true;
				}
				return false;
			} finally {
				if (lockId == null) releaseRowLock(lid);
			}
		} finally {
			closeRegionOperation();
		}
	}

	/**
	 * Replaces any KV timestamps set to {@link DBConstants#LATEST_TIMESTAMP}
	 * with the provided current timestamp.
	 */
	private void updateKVTimestamps(final Iterable<List<KeyValue>> keyLists, final byte[] now) {
		for (List<KeyValue> keys: keyLists) {
			if (keys == null) continue;
			for (KeyValue key : keys) {
				key.updateLatestStamp(now);
			}
		}
	}

//  /*
//   * Utility method to verify values length.
//   * @param batchUpdate The update to verify
//   * @throws IOException Thrown if a value is too long
//   */
//  private void validateValuesLength(Put put)
//  throws IOException {
//    Map<byte[], List<KeyValue>> families = put.getFamilyMap();
//    for(Map.Entry<byte[], List<KeyValue>> entry : families.entrySet()) {
//      DBColumnDescriptor hcd =
//        this.regionInfo.getTableDesc().getFamily(entry.getKey());
//      int maxLen = hcd.getMaxValueLength();
//      for(KeyValue kv : entry.getValue()) {
//        if(kv.getValueLength() > maxLen) {
//          throw new ValueOverMaxLengthException("Value in column "
//            + Bytes.toString(kv.getColumn()) + " is too long. "
//            + kv.getValueLength() + " > " + maxLen);
//        }
//      }
//    }
//  }

	/**
	 * Check if resources to support an update.
	 *
	 * Here we synchronize on DBRegion, a broad scoped lock.  Its appropriate
	 * given we're figuring in here whether this region is able to take on
	 * writes.  This is only method with a synchronize (at time of writing),
	 * this and the synchronize on 'this' inside in internalFlushCache to send
	 * the notify.
	 */
	private void checkResources() throws IOException {
		// If catalog region, do not impose resource constraints or block updates.
		if (this.getRegionInfo().isMetaRegion()) return;

		boolean blocked = false;
		while (this.mMemstoreSize.get() > this.mBlockingMemStoreSize) {
			if (requestFlush(true) == false)
				break;
			
			if (!blocked) {
				if (LOG.isInfoEnabled()) {
					LOG.info("Blocking updates for '" + Thread.currentThread().getName() +
							"' on region " + Bytes.toStringBinary(getRegionName()) +
							": memstore size " + StringUtils.humanReadableInt(this.mMemstoreSize.get()) +
							" is >= than blocking " + StringUtils.humanReadableInt(this.mBlockingMemStoreSize) + " size");
				}
			}
			blocked = true;
			
			synchronized(this) {
				try {
					wait(mThreadWakeFrequency);
				} catch (InterruptedException e) {
					// continue;
				}
			}
		}
		if (blocked) {
			if (LOG.isInfoEnabled()) {
				LOG.info("Unblocking updates for region " + this + " '"
						+ Thread.currentThread().getName() + "'");
			}
		}
	}

	/**
	 * @throws IOException Throws exception if region is in read-only mode.
	 */
	protected void checkReadOnly() throws IOException {
		if (this.mWritestate.isReadOnly()) {
			throw new IOException("region is read only");
		}
	}

	/**
	 * Add updates first to the dblog and then add values to memstore.
	 * Warning: Assumption is caller has lock on passed in row.
	 * @param family
	 * @param edits Cell updates by column
	 * @praram now
	 * @throws IOException
	 */
	private void put(final byte[] family, final List<KeyValue> edits)
			throws IOException {
		Map<byte[], List<KeyValue>> familyMap = new HashMap<byte[], List<KeyValue>>();
		familyMap.put(family, edits);
		this.put(familyMap, true);
	}

	/**
	 * Add updates first to the dblog (if writeToWal) and then add values to memstore.
	 * Warning: Assumption is caller has lock on passed in row.
	 * @param familyMap map of family to edits for the given family.
	 * @param writeToWAL if true, then we should write to the log
	 * @throws IOException
	 */
	private void put(final Map<byte[], List<KeyValue>> familyMap,
			boolean writeToWAL) throws IOException {
		long now = System.currentTimeMillis();
		byte[] byteNow = Bytes.toBytes(now);
		boolean flush = false;
    
		this.mUpdatesLock.readLock().lock();
		try {
			checkFamilies(familyMap.keySet());
			updateKVTimestamps(familyMap.values(), byteNow);
			
			// write/sync to WAL should happen before we touch memstore.
			//
			// If order is reversed, i.e. we write to memstore first, and
			// for some reason fail to write/sync to commit log, the memstore
			// will contain uncommitted transactions.
			if (writeToWAL) {
				WALEdit walEdit = new WALEdit();
				addFamilyMapToWALEdit(familyMap, walEdit);
				this.mLog.append(mRegionInfo, mRegionInfo.getTableDesc().getName(), walEdit, now);
			}

			long addedSize = applyFamilyMapToMemstore(familyMap);
			flush = isFlushSize(mMemstoreSize.addAndGet(addedSize));
		} finally {
			this.mUpdatesLock.readLock().unlock();
		}
		
		if (flush) {
			// Request a cache flush.  Do it outside update lock.
			requestFlush(true);
		}
	}

	/**
	 * Atomically apply the given map of family->edits to the memstore.
	 * This handles the consistency control on its own, but the caller
	 * should already have locked updatesLock.readLock(). This also does
	 * <b>not</b> check the families for validity.
	 *
	 * @return the additional memory usage of the memstore caused by the
	 * new entries.
	 */
	private long applyFamilyMapToMemstore(Map<byte[], List<KeyValue>> familyMap) {
		ReadWriteConsistencyControl.WriteEntry w = null;
		long size = 0;
		
		try {
			w = mRwcc.beginMemstoreInsert();

			for (Map.Entry<byte[], List<KeyValue>> e : familyMap.entrySet()) {
				byte[] family = e.getKey();
				List<KeyValue> edits = e.getValue();
  
				Store store = getStore(family);
				for (KeyValue kv : edits) {
					kv.setMemstoreTS(w.getWriteNumber());
					size += store.add(kv);
				}
			}
		} finally {
			mRwcc.completeMemstoreInsert(w);
		}
		
		return size;
	}

	/**
	 * Check the collection of families for validity.
	 * @throws NoSuchColumnFamilyException if a family does not exist.
	 */
	private void checkFamilies(Collection<byte[]> families)
			throws NoSuchColumnFamilyException {
		for (byte[] family : families) {
			checkFamily(family);
		}
	}

	/**
	 * Append the given map of family->edits to a WALEdit data structure.
	 * This does not write to the DBLog itself.
	 * @param familyMap map of family->edits
	 * @param walEdit the destination entry to append into
	 */
	private void addFamilyMapToWALEdit(Map<byte[], List<KeyValue>> familyMap,
			WALEdit walEdit) {
		for (List<KeyValue> edits : familyMap.values()) {
			for (KeyValue kv : edits) {
				walEdit.add(kv);
			}
		}
	}

	private boolean requestFlush(boolean flushIfNotRequested) throws IOException {
		if (this.mFlushListener == null) {
			if (flushIfNotRequested) {
				if (flushcache()) compactStores();
			}
			return false;
		}
    
		synchronized (mWritestate) {
			if (this.mWritestate.isFlushRequested()) 
				return true;
			
			mWritestate.mFlushRequested = true;
		}
		
		// Make request outside of synchronize block; HBASE-818.
		this.mFlushListener.request(this);
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("Flush requested on " + this);
		
		return true;
	}

	/**
	 * @param size
	 * @return True if size is over the flush threshold
	 */
	private boolean isFlushSize(final long size) {
		return size > this.mMemstoreFlushSize;
	}

	/**
	 * Read the edits log put under this region by wal log splitting process.  Put
	 * the recovered edits back up into this region.
	 *
	 * <p>We can ignore any log message that has a sequence ID that's equal to or
	 * lower than minSeqId.  (Because we know such log messages are already
	 * reflected in the DBFiles.)
	 * 
	 * <p>While this is running we are putting pressure on memory yet we are
	 * outside of our usual accounting because we are not yet an onlined region
	 * (this stuff is being run as part of Region initialization).  This means
	 * that if we're up against global memory limits, we'll not be flagged to flush
	 * because we are not online. We can't be flushed by usual mechanisms anyways;
	 * we're not yet online so our relative sequenceids are not yet aligned with
	 * DBLog sequenceids -- not till we come up online, post processing of split
	 * edits.
	 * 
	 * <p>But to help relieve memory pressure, at least manage our own heap size
	 * flushing if are in excess of per-region limits.  Flushing, though, we have
	 * to be careful and avoid using the regionserver/dblog sequenceid.  Its running
	 * on a different line to whats going on in here in this region context so if we
	 * crashed replaying these edits, but in the midst had a flush that used the
	 * regionserver log with a sequenceid in excess of whats going on in here
	 * in this region and with its split editlogs, then we could miss edits the
	 * next time we go to recover. So, we have to flush inline, using seqids that
	 * make sense in a this single region context only -- until we online.
	 * 
	 * @param regiondir
	 * @param minSeqId Any edit found in split editlogs needs to be in excess of
	 * this minSeqId to be applied, else its skipped.
	 * @param reporter
	 * @return the sequence id of the last edit added to this region out of the
	 * recovered edits log or <code>minSeqId</code> if nothing added from editlogs.
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	protected long replayRecoveredEditsIfAny(final Path regiondir,
			final long minSeqId, final Progressable reporter)
			throws UnsupportedEncodingException, IOException {
		long seqid = minSeqId;
		NavigableSet<Path> files = DBLog.getSplitEditFilesSorted(this.mFs, regiondir);
		if (files == null || files.isEmpty()) return seqid;
		
		for (Path edits : files) {
			if (LOG.isDebugEnabled())
				LOG.debug("replayRecoveredEditsIfAny: edits=" + edits);
			
			if (edits == null || !this.mFs.exists(edits)) {
				if (LOG.isWarnEnabled())
					LOG.warn("Null or non-existent edits file: " + edits);
				continue;
			}
			if (isZeroLengthThenDelete(this.mFs, edits)) 
				continue;
			
			try {
				seqid = replayRecoveredEdits(edits, seqid, reporter);
			} catch (IOException e) {
				boolean skipErrors = mConf.getBoolean("bigdb.skip.errors", false);
				if (skipErrors) {
					Path p = DBLog.moveAsideBadEditsFile(mFs, edits);
					
					if (LOG.isErrorEnabled()) {
						LOG.error("bigdb.skip.errors=true so continuing. Renamed " + edits +
								" as " + p, e);
					}
				} else {
					throw e;
				}
			}
		}
		
		if (seqid > minSeqId) {
			// Then we added some edits to memory. Flush and cleanup split edit files.
			internalFlushcache(null, seqid);
			
			for (Path file: files) {
				if (!this.mFs.delete(file, false)) {
					if (LOG.isErrorEnabled())
						LOG.error("Failed delete of " + file);
				} else {
					if (LOG.isDebugEnabled())
						LOG.debug("Deleted recovered.edits file=" + file);
				}
			}
		}
		
		return seqid;
	}

	/**
	 * @param edits File of recovered edits.
	 * @param minSeqId Minimum sequenceid found in a store file.  Edits in log
	 * must be larger than this to be replayed.
	 * @param reporter
	 * @return the sequence id of the last edit added to this region out of the
	 * recovered edits log or <code>minSeqId</code> if nothing added from editlogs.
	 * @throws IOException
	 */
	private long replayRecoveredEdits(final Path edits,
			final long minSeqId, final Progressable reporter) throws IOException {
		if (LOG.isInfoEnabled())
			LOG.info("Replaying edits from " + edits + "; minSequenceid=" + minSeqId);
		
		DBLog.Reader reader = DBLog.getReader(this.mFs, edits, mConf);
		try {
			return replayRecoveredEdits(reader, minSeqId, reporter);
		} finally {
			reader.close();
		}
	}

	/**
	 * @param reader Reader against file of recovered edits.
	 * @param minSeqId Any edit found in split editlogs needs to be in excess of
	 * this minSeqId to be applied, else its skipped.
	 * @param reporter
	 * @return the sequence id of the last edit added to this region out of the
	 * recovered edits log or <code>minSeqId</code> if nothing added from editlogs.
	 * @throws IOException
	 */
	private long replayRecoveredEdits(final DBLog.Reader reader,
			final long minSeqId, final Progressable reporter) throws IOException {
		long currentEditSeqId = minSeqId;
		long firstSeqIdInLog = -1;
		long skippedEdits = 0;
		long editsCount = 0;
		
		DBLog.Entry entry;
		Store store = null;
		// How many edits to apply before we send a progress report.
		int interval = this.mConf.getInt("bigdb.dbstore.report.interval.edits", 2000);
		
		while ((entry = reader.next()) != null) {
			DBLogKey key = entry.getKey();
			WALEdit val = entry.getEdit();
			
			if (firstSeqIdInLog == -1) 
				firstSeqIdInLog = key.getLogSeqNum();
			
			// Now, figure if we should skip this edit.
			if (key.getLogSeqNum() <= currentEditSeqId) {
				skippedEdits++;
				continue;
			}
			
			currentEditSeqId = key.getLogSeqNum();
			boolean flush = false;
			for (KeyValue kv: val.getKeyValues()) {
				// Check this edit is for me. Also, guard against writing the special
				// METACOLUMN info such as HBASE::CACHEFLUSH entries
				if (kv.matchingFamily(DBLog.METAFAMILY) || !Bytes.equals(key.getRegionName(), this.mRegionInfo.getRegionName())) {
					skippedEdits++;
					continue;
				}
				
				// Figure which store the edit is meant for.
				if (store == null || !kv.matchingFamily(store.getFamily().getName())) 
					store = this.mStores.get(kv.getFamily());
				
				if (store == null) {
					// This should never happen.  Perhaps schema was changed between
					// crash and redeploy?
					if (LOG.isWarnEnabled())
						LOG.warn("No family for " + kv);
					
					skippedEdits++;
					continue;
				}
				
				// Once we are over the limit, restoreEdit will keep returning true to
				// flush -- but don't flush until we've played all the kvs that make up
				// the WALEdit.
				flush = restoreEdit(store, kv);
				editsCount++;
			}
			
			if (flush) internalFlushcache(null, currentEditSeqId);

			// Every 'interval' edits, tell the reporter we're making progress.
			// Have seen 60k edits taking 3minutes to complete.
			if (reporter != null && (editsCount % interval) == 0) 
				reporter.progress();
		}
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("Applied " + editsCount + ", skipped " + skippedEdits +
					", firstSequenceidInLog=" + firstSeqIdInLog +
					", maxSequenceidInLog=" + currentEditSeqId);
		}
		
		return currentEditSeqId;
	}

	/**
	 * Used by tests
	 * @param s Store to add edit too.
	 * @param kv KeyValue to add.
	 * @return True if we should flush.
	 */
	protected boolean restoreEdit(final Store s, final KeyValue kv) {
		return isFlushSize(this.mMemstoreSize.addAndGet(s.add(kv)));
	}

	/**
	 * @param fs
	 * @param p File to check.
	 * @return True if file was zero-length (and if so, we'll delete it in here).
	 * @throws IOException
	 */
	private static boolean isZeroLengthThenDelete(final FileSystem fs, final Path p)
			throws IOException {
		FileStatus stat = fs.getFileStatus(p);
		if (stat.getLen() > 0) return false;
		
		if (LOG.isWarnEnabled())
			LOG.warn("File " + p + " is zero-length, deleting.");
		
		fs.delete(p, false);
		return true;
	}

	protected Store instantiateDBStore(Path tableDir, DBColumnDescriptor c)
			throws IOException {
		if (LOG.isDebugEnabled())
			LOG.debug("instantiateDBStore: tableDir=" + tableDir + " columnDescriptor=" + c);
		return new Store(tableDir, this, c, this.mFs, this.mConf);
	}

	/**
	 * Return DBStore instance.
	 * Use with caution.  Exposed for use of fixup utilities.
	 * @param column Name of column family hosted by this region.
	 * @return Store that goes with the family on passed <code>column</code>.
	 * TODO: Make this lookup faster.
	 */
	public Store getStore(final byte[] column) {
		return this.mStores.get(column);
	}

	//////////////////////////////////////////////////////////////////////////////
	// Support code
	//////////////////////////////////////////////////////////////////////////////

	/** Make sure this is a valid row for the DBRegion */
	private void checkRow(final byte[] row) throws IOException {
		if(!rowIsInRange(mRegionInfo, row)) {
			throw new WrongRegionException("Requested row out of range for " +
					"DBRegion " + this + ", startKey='" +
					Bytes.toStringBinary(mRegionInfo.getStartKey()) + "', getEndKey()='" +
					Bytes.toStringBinary(mRegionInfo.getEndKey()) + "', row='" +
					Bytes.toStringBinary(row) + "'");
		}
	}

	/**
	 * Obtain a lock on the given row.  Blocks until success.
	 *
	 * I know it's strange to have two mappings:
	 * <pre>
	 *   ROWS  ==> LOCKS
	 * </pre>
	 * as well as
	 * <pre>
	 *   LOCKS ==> ROWS
	 * </pre>
	 *
	 * But it acts as a guard on the client; a miswritten client just can't
	 * submit the name of a row and start writing to it; it must know the correct
	 * lockid, which matches the lock list in memory.
	 *
	 * <p>It would be more memory-efficient to assume a correctly-written client,
	 * which maybe we'll do in the future.
	 *
	 * @param row Name of row to lock.
	 * @throws IOException
	 * @return The id of the held lock.
	 */
	public Integer obtainRowLock(final byte[] row) throws IOException {
		startRegionOperation();
		try {
			return internalObtainRowLock(row, true);
		} finally {
			closeRegionOperation();
		}
	}

	/**
	 * Tries to obtain a row lock on the given row, but does not block if the
	 * row lock is not available. If the lock is not available, returns false.
	 * Otherwise behaves the same as the above method.
	 * @see DBRegion#obtainRowLock(byte[])
	 */
	public Integer tryObtainRowLock(final byte[] row) throws IOException {
		startRegionOperation();
		try {
			return internalObtainRowLock(row, false);
		} finally {
			closeRegionOperation();
		}
	}
  
	/**
	 * Obtains or tries to obtain the given row lock.
	 * @param waitForLock if true, will block until the lock is available.
	 *        Otherwise, just tries to obtain the lock and returns
	 *        null if unavailable.
	 */
	private Integer internalObtainRowLock(final byte[] row, boolean waitForLock)
			throws IOException {
		checkRow(row);
		startRegionOperation();
		
		try {
			synchronized (mLockedRows) {
				while (mLockedRows.contains(row)) {
					if (!waitForLock) 
						return null;
					try {
						mLockedRows.wait();
					} catch (InterruptedException ie) {
						// Empty
					}
				}
				
				// generate a new lockid. Attempt to insert the new [lockid, row].
				// if this lockid already exists in the map then revert and retry
				// We could have first done a lockIds.get, and if it does not exist only
				// then do a lockIds.put, but the hope is that the lockIds.put will
				// mostly return null the first time itself because there won't be
				// too many lockId collisions.
				byte[] prev = null;
				Integer lockId = null;
				
				do {
					lockId = new Integer(mLockIdGenerator++);
					prev = mLockIds.put(lockId, row);
					if (prev != null) {
						mLockIds.put(lockId, prev);    // revert old value
						mLockIdGenerator = sRand.nextInt(); // generate new start point
					}
				} while (prev != null);

				mLockedRows.add(row);
				mLockedRows.notifyAll();
				return lockId;
			}
		} finally {
			closeRegionOperation();
		}
	}
  
	/**
	 * Used by unit tests.
	 * @param lockid
	 * @return Row that goes with <code>lockid</code>
	 */
	protected byte[] getRowFromLock(final Integer lockid) {
		synchronized (mLockedRows) {
			return mLockIds.get(lockid);
		}
	}

	/**
	 * Release the row lock!
	 * @param lockid  The lock ID to release.
	 */
	protected void releaseRowLock(final Integer lockid) {
		synchronized (mLockedRows) {
			byte[] row = mLockIds.remove(lockid);
			mLockedRows.remove(row);
			mLockedRows.notifyAll();
		}
	}

	/**
	 * See if row is currently locked.
	 * @param lockid
	 * @return boolean
	 */
	protected boolean isRowLocked(final Integer lockid) {
		synchronized (mLockedRows) {
			if (mLockIds.get(lockid) != null) 
				return true;
			else
				return false;
		}
	}

	/**
	 * Returns existing row lock if found, otherwise
	 * obtains a new row lock and returns it.
	 * @param lockid requested by the user, or null if the user didn't already hold lock
	 * @param row the row to lock
	 * @param waitForLock if true, will block until the lock is available, otherwise will
	 * simply return null if it could not acquire the lock.
	 * @return lockid or null if waitForLock is false and the lock was unavailable.
	 */
	private Integer getLock(Integer lockid, byte[] row, boolean waitForLock)
			throws IOException {
		Integer lid = null;
		if (lockid == null) {
			lid = internalObtainRowLock(row, waitForLock);
		} else {
			if (!isRowLocked(lockid)) {
				throw new IOException("Invalid row lock");
			}
			lid = lockid;
		}
		return lid;
	}

	public void bulkLoadDBFile(String dbfilePath, byte[] familyName)
			throws IOException {
		startRegionOperation();
		try {
			Store store = getStore(familyName);
			if (store == null) {
				throw new DoNotRetryIOException("No such column family " + 
						Bytes.toStringBinary(familyName));
			}
			store.bulkLoadDBFile(dbfilePath);
		} finally {
			closeRegionOperation();
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null || !(o instanceof DBRegion)) 
			return false;
    
		return this.hashCode() == ((DBRegion)o).hashCode();
	}

	@Override
	public int hashCode() {
		return Bytes.hashCode(this.mRegionInfo.getRegionName());
	}

	@Override
	public String toString() {
		return this.mRegionInfo.getRegionNameAsString();
	}

	/** @return Path of region base directory */
	public Path getTableDir() {
		return this.mTableDir;
	}

	/**
	 * RegionScanner is an iterator through a bunch of rows in an DBRegion.
	 * <p>
	 * It is used to combine scanners from multiple Stores (aka column families).
	 */
	class RegionScanner implements InternalScanner {
		// Package local for testability
		private List<KeyValue> mResults = new ArrayList<KeyValue>();
		private KeyValueHeap mStoreHeap = null;
		private final byte[] mStopRow;
		private Filter mFilter;
		private int mBatch;
		private int mIsScan;
		private boolean mFilterClosed = false;
		private long mReadPt;

		RegionScanner(Scan scan, List<KeyValueScanner> additionalScanners) 
				throws IOException {
			//DebugPrint.println("DBRegionScanner.<init>");
			this.mFilter = scan.getFilter();
			this.mBatch = scan.getBatch();
			if (Bytes.equals(scan.getStopRow(), DBConstants.EMPTY_END_ROW)) {
				this.mStopRow = null;
			} else {
				this.mStopRow = scan.getStopRow();
			}
			
			// If we are doing a get, we want to be [startRow,endRow] normally
			// it is [startRow,endRow) and if startRow=endRow we get nothing.
			this.mIsScan = scan.isGetScan() ? -1 : 0;

			this.mReadPt = ReadWriteConsistencyControl.resetThreadReadPoint(mRwcc);

			List<KeyValueScanner> scanners = new ArrayList<KeyValueScanner>();
			if (additionalScanners != null) 
				scanners.addAll(additionalScanners);

			for (Map.Entry<byte[], NavigableSet<byte[]>> entry : scan.getFamilyMap().entrySet()) {
				Store store = mStores.get(entry.getKey());
				scanners.add(store.getScanner(scan, entry.getValue()));
			}
			
			this.mStoreHeap = new KeyValueHeap(scanners, mComparator);
		}

		RegionScanner(Scan scan) throws IOException {
			this(scan, null);
		}

		/**
		 * Reset both the filter and the old filter.
		 */
		protected void resetFilters() {
			if (mFilter != null) mFilter.reset();
		}

		public synchronized boolean next(List<KeyValue> outResults, int limit)
				throws IOException {
			if (this.mFilterClosed) {
				throw new UnknownScannerException("Scanner was closed (timed out?) " +
						"after we renewed it. Could be caused by a very slow scanner " +
						"or a lengthy garbage collection");
			}
			
			startRegionOperation();
			try {
				// This could be a new thread from the last time we called next().
				ReadWriteConsistencyControl.setThreadReadPoint(this.mReadPt);

				mResults.clear();
				boolean returnResult = nextInternal(limit);

				outResults.addAll(mResults);
				resetFilters();
				if (isFilterDone()) 
					return false;
				
				return returnResult;
			} finally {
				closeRegionOperation();
			}
		}

		public synchronized boolean next(List<KeyValue> outResults)
				throws IOException {
			// apply the batching limit by default
			return next(outResults, mBatch);
		}

		/**
		 * @return True if a filter rules the scanner is over, done.
		 */
		protected synchronized boolean isFilterDone() {
			return this.mFilter != null && this.mFilter.filterAllRemaining();
		}

		private boolean nextInternal(int limit) throws IOException {
			while (true) {
				byte[] currentRow = peekRow();
				if (isStopRow(currentRow)) {
					if (mFilter != null && mFilter.hasFilterRow()) 
						mFilter.filterRow(mResults);
					
					if (mFilter != null && mFilter.filterRow()) 
						mResults.clear();

					return false;
				} else if (filterRowKey(currentRow)) {
					nextRow(currentRow);
					
				} else {
					byte[] nextRow;
					do {
						this.mStoreHeap.next(mResults, limit - mResults.size());
						if (limit > 0 && mResults.size() == limit) {
							if (this.mFilter != null && this.mFilter.hasFilterRow()) 
								throw new IncompatibleFilterException("Filter with filterRow(List<KeyValue>) incompatible with scan with limit!");
							return true; // we are expecting more yes, but also limited to how many we can return.
						}
					} while (Bytes.equals(currentRow, nextRow = peekRow()));

					final boolean stopRow = isStopRow(nextRow);
					// now that we have an entire row, lets process with a filters:

					// first filter with the filterRow(List)
					if (mFilter != null && mFilter.hasFilterRow()) {
						mFilter.filterRow(mResults);
					}

					if (mResults.isEmpty() || filterRow()) {
						// this seems like a redundant step - we already consumed the row
						// there're no left overs.
						// the reasons for calling this method are:
						// 1. reset the filters.
						// 2. provide a hook to fast forward the row (used by subclasses)
						nextRow(currentRow);

						// This row was totally filtered out, if this is NOT the last row,
						// we should continue on.

						if (!stopRow) continue;
					}
					return !stopRow;
				}
			}
		}

		private boolean filterRow() {
			return mFilter != null && mFilter.filterRow();
		}
    
		private boolean filterRowKey(byte[] row) {
			return mFilter != null && mFilter.filterRowKey(row, 0, row.length);
		}

		protected void nextRow(byte[] currentRow) throws IOException {
			while (Bytes.equals(currentRow, peekRow())) {
				this.mStoreHeap.next(MOCKED_LIST);
			}
			mResults.clear();
			resetFilters();
		}

		private byte[] peekRow() {
			KeyValue kv = this.mStoreHeap.peek();
			return kv == null ? null : kv.getRow();
		}

		private boolean isStopRow(byte[] currentRow) {
			return currentRow == null ||
					(mStopRow != null && mComparator.compareRows(mStopRow, 0, mStopRow.length,
							currentRow, 0, currentRow.length) <= mIsScan);
		}

		public synchronized void close() {
			if (mStoreHeap != null) {
				mStoreHeap.close();
				mStoreHeap = null;
			}
			this.mFilterClosed = true;
		}
	}

	// Utility methods
	/**
	 * A utility method to create new instances of DBRegion based on the
	 * {@link DBConstants#REGION_IMPL} configuration property.
	 * @param tableDir qualified path of directory where region should be located,
	 * usually the table directory.
	 * @param log The DBLog is the outbound log for any updates to the DBRegion
	 * (There's a single DBLog for all the DBRegions on a single DBRegionServer.)
	 * The log file is a logfile from the previous execution that's
	 * custom-computed for this DBRegion. The DBRegionServer computes and sorts the
	 * appropriate log info for this DBRegion. If there is a previous log file
	 * (implying that the DBRegion has been written-to before), then read it from
	 * the supplied path.
	 * @param fs is the filesystem.
	 * @param conf is global configuration settings.
	 * @param regionInfo - DBRegionInfo that describes the region
	 * is new), then read them from the supplied path.
	 * @param flushListener an object that implements CacheFlushListener or null
	 * making progress to master -- otherwise master might think region deploy
	 * failed.  Can be null.
	 * @return the new instance
	 */
	public static DBRegion newDBRegion(Path tableDir, DBLog log, FileSystem fs, 
			Configuration conf, DBRegionInfo regionInfo, FlushRequester flushListener) {
		try {
			@SuppressWarnings("unchecked")
			Class<? extends DBRegion> regionClass =
			(Class<? extends DBRegion>) conf.getClass(DBConstants.REGION_IMPL, DBRegion.class);

			Constructor<? extends DBRegion> c =
					regionClass.getConstructor(Path.class, DBLog.class, FileSystem.class,
							Configuration.class, DBRegionInfo.class, FlushRequester.class);

			return c.newInstance(tableDir, log, fs, conf, regionInfo, flushListener);
		} catch (Throwable e) {
			// todo: what should I throw here?
			throw new IllegalStateException("Could not instantiate a region instance.", e);
		}
	}

	/**
	 * Convenience method creating new DBRegions. Used by createTable and by the
	 * bootstrap code in the HMaster constructor.
	 * Note, this method creates an {@link DBLog} for the created region. It
	 * needs to be closed explicitly.  Use {@link DBRegion#getLog()} to get
	 * access.
	 * @param info Info for region to create.
	 * @param rootDir Root directory for HBase instance
	 * @param conf
	 * @return new DBRegion
	 *
	 * @throws IOException
	 */
	public static DBRegion createDBRegion(final DBRegionInfo info, final Path rootDir,
			final Configuration conf) throws IOException {
		Path tableDir = DBTableDescriptor.getTableDir(rootDir, info.getTableDesc().getName());
		Path regionDir = DBRegion.getRegionDir(tableDir, info.getEncodedName());
		FileSystem fs = FileSystem.getLocal(conf); //FSUtils.getFs(conf);
		fs.mkdirs(regionDir);
		DBRegion region = DBRegion.newDBRegion(tableDir,
				new DBLog(fs, new Path(regionDir, DBConstants.DBREGION_LOGDIR_NAME),
						new Path(regionDir, DBConstants.DBREGION_OLDLOGDIR_NAME), conf, null),
						fs, conf, info, null);
		region.initialize();
		return region;
	}

	/**
	 * Convenience method to open a DBRegion outside of an DBRegionServer context.
	 * @param info Info for region to be opened.
	 * @param rootDir Root directory for HBase instance
	 * @param log DBLog for region to use. This method will call
	 * DBLog#setSequenceNumber(long) passing the result of the call to
	 * DBRegion#getMinSequenceId() to ensure the log id is properly kept
	 * up.  DBRegionStore does this every time it opens a new region.
	 * @param conf
	 * @return new DBRegion
	 *
	 * @throws IOException
	 */
	public static DBRegion openDBRegion(final DBRegionInfo info, final Path rootDir,
			final DBLog log, final Configuration conf) throws IOException {
		if (LOG.isDebugEnabled()) 
			LOG.debug("Opening region: " + info);
    
		if (info == null) 
			throw new NullPointerException("Passed region info is null");
    
		DBRegion r = DBRegion.newDBRegion(
				DBTableDescriptor.getTableDir(rootDir, info.getTableDesc().getName()),
				log, FileSystem.getLocal(conf) /*FSUtils.getFs(conf)*/, conf, info, null);
    
		long seqid = r.initialize();
		// If seqid  > current wal seqid, the wal seqid is updated.
		if (log != null) log.setSequenceNumber(seqid);
    
		return r;
	}

	/**
	 * Inserts a new region's meta information into the passed
	 * <code>meta</code> region. Used by the HMaster bootstrap code adding
	 * new table to ROOT table.
	 *
	 * @param meta META DBRegion to be updated
	 * @param r DBRegion to add to <code>meta</code>
	 *
	 * @throws IOException
	 */
	public static void addRegionToMETA(DBRegion meta, DBRegion r)
			throws IOException {
		meta.checkResources();
		// The row key is the region name
		byte[] row = r.getRegionName();
		Integer lid = meta.obtainRowLock(row);
		try {
			final List<KeyValue> edits = new ArrayList<KeyValue>(1);
			edits.add(new KeyValue(row, DBConstants.CATALOG_FAMILY,
					DBConstants.REGIONINFO_QUALIFIER,
					System.currentTimeMillis(), 
					Writables.getBytes(r.getRegionInfo())));
			meta.put(DBConstants.CATALOG_FAMILY, edits);
		} finally {
			meta.releaseRowLock(lid);
		}
	}

	/**
	 * Delete a region's meta information from the passed
	 * <code>meta</code> region.  Deletes the row.
	 * @param srvr META server to be updated
	 * @param metaRegionName Meta region name
	 * @param regionName DBRegion to remove from <code>meta</code>
	 *
	 * @throws IOException
	 */
	public static void removeRegionFromMETA(final DBRegionInterface srvr,
			final byte[] metaRegionName, final byte[] regionName) throws IOException {
		Delete delete = new Delete(regionName);
		srvr.delete(metaRegionName, delete);
	}

	/**
	 * Utility method used by HMaster marking regions offlined.
	 * @param srvr META server to be updated
	 * @param metaRegionName Meta region name
	 * @param info DBRegion to update in <code>meta</code>
	 *
	 * @throws IOException
	 */
	public static void offlineRegionInMETA(final DBRegionInterface srvr,
			final byte[] metaRegionName, final DBRegionInfo info) throws IOException {
		// Puts and Deletes used to be "atomic" here.  We can use row locks if
		// we need to keep that property, or we can expand Puts and Deletes to
		// allow them to be committed at once.
		byte[] row = info.getRegionName();
		Put put = new Put(row);
		info.setOffline(true);
		put.add(DBConstants.CATALOG_FAMILY, DBConstants.REGIONINFO_QUALIFIER,
				Writables.getBytes(info));
		srvr.put(metaRegionName, put);
		cleanRegionInMETA(srvr, metaRegionName, info);
	}

	/**
	 * Clean COL_SERVER and COL_STARTCODE for passed <code>info</code> in
	 * <code>.META.</code>
	 * @param srvr
	 * @param metaRegionName
	 * @param info
	 * @throws IOException
	 */
	public static void cleanRegionInMETA(final DBRegionInterface srvr,
			final byte[] metaRegionName, final DBRegionInfo info) throws IOException {
		Delete del = new Delete(info.getRegionName());
		del.deleteColumns(DBConstants.CATALOG_FAMILY, DBConstants.SERVER_QUALIFIER);
		del.deleteColumns(DBConstants.CATALOG_FAMILY,
				DBConstants.STARTCODE_QUALIFIER);
		srvr.delete(metaRegionName, del);
	}

	/**
	 * Deletes all the files for a DBRegion
	 *
	 * @param fs the file system object
	 * @param rootdir qualified path of HBase root directory
	 * @param info DBRegionInfo for region to be deleted
	 * @throws IOException
	 */
	public static void deleteRegion(FileSystem fs, Path rootdir, DBRegionInfo info)
			throws IOException {
		deleteRegion(fs, DBRegion.getRegionDir(rootdir, info));
	}

	private static void deleteRegion(FileSystem fs, Path regiondir)
			throws IOException {
		if (LOG.isDebugEnabled()) 
			LOG.debug("DELETING region " + regiondir.toString());
    
		if (!fs.delete(regiondir, true)) {
			if (LOG.isWarnEnabled())
				LOG.warn("Failed delete of " + regiondir);
		}
	}

	/**
	 * Computes the Path of the DBRegion
	 *
	 * @param rootdir qualified path of HBase root directory
	 * @param info DBRegionInfo for the region
	 * @return qualified path of region directory
	 */
	public static Path getRegionDir(final Path rootdir, final DBRegionInfo info) {
		return new Path(
				DBTableDescriptor.getTableDir(rootdir, info.getTableDesc().getName()),
				info.getEncodedName());
	}

	/**
	 * Determines if the specified row is within the row range specified by the
	 * specified DBRegionInfo
	 *
	 * @param info DBRegionInfo that specifies the row range
	 * @param row row to be checked
	 * @return true if the row is within the range specified by the DBRegionInfo
	 */
	public static boolean rowIsInRange(DBRegionInfo info, final byte[] row) {
		return ((info.getStartKey().length == 0) ||
				(Bytes.compareTo(info.getStartKey(), row) <= 0)) &&
				((info.getEndKey().length == 0) ||
						(Bytes.compareTo(info.getEndKey(), row) > 0));
	}

	/**
	 * Make the directories for a specific column family
	 *
	 * @param fs the file system
	 * @param tabledir base directory where region will live (usually the table dir)
	 * @param hri
	 * @param colFamily the column family
	 * @throws IOException
	 */
	public static void makeColumnFamilyDirs(FileSystem fs, Path tabledir,
			final DBRegionInfo hri, byte[] colFamily) throws IOException {
		Path dir = Store.getStoreHomedir(tabledir, hri.getEncodedName(), colFamily);
		if (!fs.mkdirs(dir)) {
			if (LOG.isWarnEnabled())
				LOG.warn("Failed to create " + dir);
		}
	}

	/**
	 * Merge two DBRegions.  The regions must be adjacent and must not overlap.
	 *
	 * @param srcA
	 * @param srcB
	 * @return new merged DBRegion
	 * @throws IOException
	 */
	public static DBRegion mergeAdjacent(final DBRegion srcA, final DBRegion srcB)
			throws IOException {
		DBRegion a = srcA;
		DBRegion b = srcB;

		// Make sure that srcA comes first; important for key-ordering during
		// write of the merged file.
		if (srcA.getStartKey() == null) {
			if (srcB.getStartKey() == null) {
				throw new IOException("Cannot merge two regions with null start key");
			}
			// A's start key is null but B's isn't. Assume A comes before B
		} else if ((srcB.getStartKey() == null) ||
				(Bytes.compareTo(srcA.getStartKey(), srcB.getStartKey()) > 0)) {
			a = srcB;
			b = srcA;
		}

		if (!(Bytes.compareTo(a.getEndKey(), b.getStartKey()) == 0)) 
			throw new IOException("Cannot merge non-adjacent regions");
		
		return merge(a, b);
	}

	/**
	 * Merge two regions whether they are adjacent or not.
	 *
	 * @param a region a
	 * @param b region b
	 * @return new merged region
	 * @throws IOException
	 */
	public static DBRegion merge(DBRegion a, DBRegion b) throws IOException {
		if (!a.getRegionInfo().getTableDesc().getNameAsString().equals(
				b.getRegionInfo().getTableDesc().getNameAsString())) {
			throw new IOException("Regions do not belong to the same table");
		}

		FileSystem fs = a.getFilesystem();

		// Make sure each region's cache is empty
		a.flushcache();
		b.flushcache();

		// Compact each region so we only have one store file per family
		a.compactStores(true);
		if (LOG.isDebugEnabled()) {
			LOG.debug("Files for region: " + a);
			listPaths(fs, a.getRegionDir());
		}
    
		b.compactStores(true);
		if (LOG.isDebugEnabled()) {
			LOG.debug("Files for region: " + b);
			listPaths(fs, b.getRegionDir());
		}

		Configuration conf = a.getConf();
		DBTableDescriptor tabledesc = a.getTableDesc();
		DBLog log = a.getLog();
		Path tableDir = a.getTableDir();
		
		// Presume both are of same region type -- i.e. both user or catalog
		// table regions.  This way can use comparator.
		final byte[] startKey =
				(a.mComparator.matchingRows(a.getStartKey(), 0, a.getStartKey().length,
						DBConstants.EMPTY_BYTE_ARRAY, 0, DBConstants.EMPTY_BYTE_ARRAY.length)
						|| b.mComparator.matchingRows(b.getStartKey(), 0,
								b.getStartKey().length, DBConstants.EMPTY_BYTE_ARRAY, 0,
								DBConstants.EMPTY_BYTE_ARRAY.length))
								? DBConstants.EMPTY_BYTE_ARRAY
										: (a.mComparator.compareRows(a.getStartKey(), 0, a.getStartKey().length,
												b.getStartKey(), 0, b.getStartKey().length) <= 0
												? a.getStartKey() : b.getStartKey());
		
		final byte[] endKey =
				(a.mComparator.matchingRows(a.getEndKey(), 0, a.getEndKey().length,
						DBConstants.EMPTY_BYTE_ARRAY, 0, DBConstants.EMPTY_BYTE_ARRAY.length)
						|| a.mComparator.matchingRows(b.getEndKey(), 0, b.getEndKey().length,
								DBConstants.EMPTY_BYTE_ARRAY, 0,
								DBConstants.EMPTY_BYTE_ARRAY.length))
								? DBConstants.EMPTY_BYTE_ARRAY
										: (a.mComparator.compareRows(a.getEndKey(), 0, a.getEndKey().length,
												b.getEndKey(), 0, b.getEndKey().length) <= 0
												? b.getEndKey() : a.getEndKey());

		DBRegionInfo newRegionInfo = new DBRegionInfo(tabledesc, startKey, endKey);
		if (LOG.isInfoEnabled())
			LOG.info("Creating new region " + newRegionInfo.toString());
		
		String encodedName = newRegionInfo.getEncodedName();
		Path newRegionDir = DBRegion.getRegionDir(a.getTableDir(), encodedName);
		if (fs.exists(newRegionDir)) {
			throw new IOException("Cannot merge; target file collision at " + newRegionDir);
		}
		fs.mkdirs(newRegionDir);

		if (LOG.isInfoEnabled()) {
			LOG.info("starting merge of regions: " + a + " and " + b +
					" into new region " + newRegionInfo.toString() +
					" with start key <" + Bytes.toString(startKey) + "> and end key <" +
					Bytes.toString(endKey) + ">");
		}

		// Move DBStoreFiles under new region directory
		Map<byte[], List<StoreFile>> byFamily =
				new TreeMap<byte[], List<StoreFile>>(Bytes.BYTES_COMPARATOR);
		
		byFamily = filesByFamily(byFamily, a.close());
		byFamily = filesByFamily(byFamily, b.close());
		
		for (Map.Entry<byte[], List<StoreFile>> es : byFamily.entrySet()) {
			byte[] colFamily = es.getKey();
			makeColumnFamilyDirs(fs, tableDir, newRegionInfo, colFamily);
			
			// Because we compacted the source regions we should have no more than two
			// DBStoreFiles per family and there will be no reference store
			List<StoreFile> srcFiles = es.getValue();
			
			if (srcFiles.size() == 2) {
				long seqA = srcFiles.get(0).getMaxSequenceId();
				long seqB = srcFiles.get(1).getMaxSequenceId();
				if (seqA == seqB) {
					// Can't have same sequenceid since on open of a store, this is what
					// distingushes the files (see the map of stores how its keyed by
					// sequenceid).
					throw new IOException("Files have same sequenceid: " + seqA);
				}
			}
			
			for (StoreFile hsf: srcFiles) {
				StoreFile.rename(fs, hsf.getPath(),
						StoreFile.getUniqueFile(fs, Store.getStoreHomedir(tableDir,
								newRegionInfo.getEncodedName(), colFamily)));
			}
		}
    
		if (LOG.isDebugEnabled()) {
			LOG.debug("Files for new region");
			listPaths(fs, newRegionDir);
		}
    
		DBRegion dstRegion = DBRegion.newDBRegion(tableDir, log, fs, conf, newRegionInfo, null);
		dstRegion.initialize();
		dstRegion.compactStores();
    
		if (LOG.isDebugEnabled()) {
			LOG.debug("Files for new region");
			listPaths(fs, dstRegion.getRegionDir());
		}
    
		deleteRegion(fs, a.getRegionDir());
		deleteRegion(fs, b.getRegionDir());

		if (LOG.isInfoEnabled())
			LOG.info("merge completed. New region is " + dstRegion);

		return dstRegion;
	}

	/**
	 * Fills a map with a vector of store files keyed by column family.
	 * @param byFamily Map to fill.
	 * @param storeFiles Store files to process.
	 * @param family
	 * @return Returns <code>byFamily</code>
	 */
	private static Map<byte[], List<StoreFile>> filesByFamily(
			Map<byte[], List<StoreFile>> byFamily, List<StoreFile> storeFiles) {
		for (StoreFile src: storeFiles) {
			byte[] family = src.getFamily();
			List<StoreFile> v = byFamily.get(family);
			if (v == null) {
				v = new ArrayList<StoreFile>();
				byFamily.put(family, v);
			}
			v.add(src);
		}
		return byFamily;
	}

	/**
	 * @return True if needs a mojor compaction.
	 * @throws IOException
	 */
	protected boolean isMajorCompaction() throws IOException {
		for (Store store : this.mStores.values()) {
			if (store.isMajorCompaction()) 
				return true;
		}
		return false;
	}

	/**
	 * List the files under the specified directory
	 *
	 * @param fs
	 * @param dir
	 * @throws IOException
	 */
	private static void listPaths(FileSystem fs, Path dir) throws IOException {
		if (LOG.isDebugEnabled()) {
			FileStatus[] stats = fs.listStatus(dir);
			if (stats == null || stats.length == 0) 
				return;
			
			for (int i = 0; i < stats.length; i++) {
				String path = stats[i].getPath().toString();
				if (stats[i].isDir()) {
					LOG.debug("d " + path);
					listPaths(fs, stats[i].getPath());
				} else {
					LOG.debug("f " + path + " size=" + stats[i].getLen());
				}
			}
		}
	}

	//
	// HBASE-880
	//
	/**
	 * @param get get object
	 * @param lockid existing lock id, or null for no previous lock
	 * @return result
	 * @throws IOException read exceptions
	 */
	public Result get(final Get get, final Integer lockid) throws IOException {
		if (LOG.isDebugEnabled()) 
			LOG.debug("get: get=" + get + " lockid=" + lockid);
		
		// Verify families are all valid
		if (get.hasFamilies()) {
			for (byte[] family: get.familySet()) {
				checkFamily(family);
			}
		} else { // Adding all families to scanner
			for (byte[] family : mRegionInfo.getTableDesc().getFamiliesKeys()) {
				get.addFamily(family);
			}
		}
		List<KeyValue> result = get(get);

		return new Result(result);
	}

	/**
	 * Do a get based on the get parameter.
	 */
	private List<KeyValue> get(final Get get) throws IOException {
		Scan scan = new Scan(get);
		List<KeyValue> results = new ArrayList<KeyValue>();

		InternalScanner scanner = null;
		try {
			scanner = getScanner(scan);
			scanner.next(results);
		} finally {
			if (scanner != null)
				scanner.close();
		}
		return results;
	}

	/**
	 *
	 * @param row
	 * @param family
	 * @param qualifier
	 * @param amount
	 * @param writeToWAL
	 * @return The new value.
	 * @throws IOException
	 */
	public long incrementColumnValue(byte[] row, byte[] family,
			byte[] qualifier, long amount, boolean writeToWAL) throws IOException {
		checkRow(row);
		
		boolean flush = false;
		// Lock row
		long result = amount;
		
		startRegionOperation();
		
		try {
			Integer lid = obtainRowLock(row);
			try {
				Store store = mStores.get(family);

				// Get the old value:
				Get get = new Get(row);
				get.addColumn(family, qualifier);

				List<KeyValue> results = get(get);

				if (!results.isEmpty()) {
					KeyValue kv = results.get(0);
					byte[] buffer = kv.getBuffer();
					int valueOffset = kv.getValueOffset();
					result += Bytes.toLong(buffer, valueOffset, Bytes.SIZEOF_LONG);
				}

				// bulid the KeyValue now:
				KeyValue newKv = new KeyValue(row, family,
						qualifier, System.currentTimeMillis(), 
						Bytes.toBytes(result));

				// now log it:
				if (writeToWAL) {
					long now = System.currentTimeMillis(); 
					WALEdit walEdit = new WALEdit();
					walEdit.add(newKv);
					this.mLog.append(mRegionInfo, mRegionInfo.getTableDesc().getName(),
							walEdit, now);
				}

				// Now request the ICV to the store, this will set the timestamp
				// appropriately depending on if there is a value in memcache or not.
				// returns the
				long size = store.updateColumnValue(row, family, qualifier, result);

				size = this.mMemstoreSize.addAndGet(size);
				flush = isFlushSize(size);
			} finally {
				releaseRowLock(lid);
			}
		} finally {
			closeRegionOperation();
		}

		if (flush) {
			// Request a cache flush.  Do it outside update lock.
			requestFlush(true);
		}

		return result;
	}

	//
	// New HBASE-880 Helpers
	//
	private void checkFamily(final byte[] family)
			throws NoSuchColumnFamilyException {
		if (!mRegionInfo.getTableDesc().hasFamily(family)) {
			throw new NoSuchColumnFamilyException("Column family " +
					Bytes.toString(family) + " does not exist in region " + this + 
					" in table " + mRegionInfo.getTableDesc());
		}
	}

	public static final long FIXED_OVERHEAD = ClassSize.align(
			(4 * Bytes.SIZEOF_LONG) + Bytes.SIZEOF_BOOLEAN +
			(18 * ClassSize.REFERENCE) + ClassSize.OBJECT + Bytes.SIZEOF_INT);

	public static final long DEEP_OVERHEAD = ClassSize.align(FIXED_OVERHEAD +
			ClassSize.OBJECT + (2 * ClassSize.ATOMIC_BOOLEAN) +
			ClassSize.ATOMIC_LONG + ClassSize.ATOMIC_INTEGER +

			// Using TreeMap for TreeSet
			ClassSize.TREEMAP +

			// Using TreeMap for HashMap
			ClassSize.TREEMAP +

			ClassSize.CONCURRENT_SKIPLISTMAP + ClassSize.CONCURRENT_SKIPLISTMAP_ENTRY +
			ClassSize.align(ClassSize.OBJECT +
					(5 * Bytes.SIZEOF_BOOLEAN)) +
					(3 * ClassSize.REENTRANT_LOCK));

	@Override
	public long heapSize() {
		long heapSize = DEEP_OVERHEAD;
		for (Store store : this.mStores.values()) {
			heapSize += store.heapSize();
		}
		return heapSize;
	}

	/**
	 * For internal use in forcing splits ahead of file size limit.
	 * @param b
	 * @return previous value
	 */
	public boolean shouldSplit(boolean b) {
		boolean old = this.mSplitRequest;
		this.mSplitRequest = b;
		return old;
	}

	/**
	 * Checks every store to see if one has too many
	 * store files
	 * @return true if any store has too many store files
	 */
	public boolean hasTooManyStoreFiles() {
		for (Store store : mStores.values()) {
			if (store.hasTooManyStoreFiles()) 
				return true;
		}
		return false;
	}

	/**
	 * This method needs to be called before any public call that reads or
	 * modifies data. It has to be called just before a try.
	 * #closeRegionOperation needs to be called in the try's finally block
	 * Acquires a read lock and checks if the region is closing or closed.
   	 * @throws NotServingRegionException when the region is closing or closed
   	 */
	private void startRegionOperation() throws NotServingRegionException {
		if (this.mClosing.get()) {
			throw new NotServingRegionException(
					mRegionInfo.getRegionNameAsString() + " is closing");
		}
		mLock.readLock().lock();
		if (this.mClosed.get()) {
			mLock.readLock().unlock();
			throw new NotServingRegionException(
					mRegionInfo.getRegionNameAsString() + " is closed");
		}
	}

	/**
	 * Closes the lock. This needs to be called in the finally block corresponding
	 * to the try block of #startRegionOperation
	 */
	private void closeRegionOperation(){
		mLock.readLock().unlock();
	}

	/**
	 * A mocked list implementaion - discards all updates.
	 */
	private static final List<KeyValue> MOCKED_LIST = 
		new AbstractList<KeyValue>() {
			@Override
			public void add(int index, KeyValue element) {
				// do nothing
			}

			@Override
			public boolean addAll(int index, Collection<? extends KeyValue> c) {
				return false; // this list is never changed as a result of an update
			}

			@Override
			public KeyValue get(int index) {
				throw new UnsupportedOperationException();
			}

			@Override
			public int size() {
				return 0;
			}
		};

	/**
	 * Process table.
	 * Do major compaction or list content.
	 * @param fs
	 * @param p
	 * @param log
	 * @param c
	 * @param majorCompact
	 * @throws IOException
	 */
	public static void processTable(final FileSystem fs, final Path p,
			final DBLog log, final Configuration c,
			final boolean majorCompact) throws IOException {
		if (LOG.isDebugEnabled())
			LOG.debug("processTable: tableDir=" + p);
		
		DBRegion region = null;
		String rootStr = Bytes.toString(DBConstants.ROOT_TABLE_NAME);
		String metaStr = Bytes.toString(DBConstants.META_TABLE_NAME);
		
		// Currently expects tables have one region only.
		if (p.getName().startsWith(rootStr)) {
			region = DBRegion.newDBRegion(p, log, fs, c, DBRegionInfo.ROOT_REGIONINFO, null);
		} else if (p.getName().startsWith(metaStr)) {
			region = DBRegion.newDBRegion(p, log, fs, c, DBRegionInfo.FIRST_META_REGIONINFO, null);
		} else {
			throw new IOException("Not a known catalog table: " + p.toString());
		}
		
		try {
			region.initialize();
			if (majorCompact) {
				if (LOG.isDebugEnabled())
					LOG.debug("processTable: do compactStores ...");
				
				region.compactStores(true);
			} else {
				if (LOG.isDebugEnabled())
					LOG.debug("processTable: do scan ...");
				
				// Default behavior
				Scan scan = new Scan();
				// scan.addFamily(DBConstants.CATALOG_FAMILY);
				InternalScanner scanner = region.getScanner(scan);
				try {
					List<KeyValue> kvs = new ArrayList<KeyValue>();
					boolean done = false;
					do {
						kvs.clear();
						done = scanner.next(kvs);
						
						if (LOG.isInfoEnabled())
							LOG.info("processTable: keyValues=" + kvs);
					} while (done);
				} finally {
					scanner.close();
				}
			}
		} finally {
			region.close();
		}
	}
	
}
