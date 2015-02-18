package org.javenstudio.falcon.datum.table.store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.fs.FileStatus;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.FileUtil;
import org.javenstudio.raptor.fs.Path;
import org.javenstudio.raptor.util.StringUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * A Store holds a column family in a Region.  Its a memstore and a set of zero
 * or more StoreFiles, which stretch backwards over time.
 *
 * <p>There's no reason to consider append-logging at this level; all logging
 * and locking is handled at the DBRegion level.  Store just provides
 * services to manage sets of StoreFiles.  One of the most important of those
 * services is compaction services where files are aggregated once they pass
 * a configurable threshold.
 *
 * <p>The only thing having to do with logs that Store needs to deal with is
 * the reconstructionLog.  This is a segment of an DBRegion's log that might
 * NOT be present upon startup.  If the param is NULL, there's nothing to do.
 * If the param is non-NULL, we need to process the log to reconstruct
 * a TreeMap that might not have been written to disk before the process
 * died.
 *
 * <p>It's assumed that after this constructor returns, the reconstructionLog
 * file will be deleted (by whoever has instantiated the Store).
 *
 * <p>Locking and transactions are handled at a higher level.  This API should
 * not be called directly but by an DBRegion manager.
 */
public class Store implements HeapSize {
  private static final Logger LOG = Logger.getLogger(Store.class);
  
  private final ReentrantReadWriteLock mLock = new ReentrantReadWriteLock();
  private final Object mFlushLock = new Object();
  protected final MemStore mMemstore;
  // This stores directory in the filesystem.
  private final Path mHomedir;
  private final DBRegion mRegion;
  private final DBColumnDescriptor mFamily;
  private final FileSystem mFs;
  private final Configuration mConf;
  // ttl in milliseconds.
  protected long mTtl;
  private long mMajorCompactionTime;
  private int mMaxFilesToCompact;
  private final long mDesiredMaxFileSize;
  private volatile long mStoreSize = 0L;
  private final String mStoreNameStr;
  private final boolean mInMemory;

  /**
   * List of store files inside this store. This is an immutable list that
   * is atomically replaced when its contents change.
   */
  private ImmutableList<StoreFile> mStorefiles = null;

  // All access must be synchronized.
  private final CopyOnWriteArraySet<ChangedReadersObserver> mChangedReaderObservers =
    new CopyOnWriteArraySet<ChangedReadersObserver>();

  private final Object mCompactLock = new Object();
  private final int mCompactionThreshold;
  private final int mBlocksize;
  private final boolean mBlockcache;
  private final Compression.Algorithm mCompression;

  // Comparing KeyValues
  protected final KeyValue.KVComparator mComparator;

  /**
   * Constructor
   * @param basedir qualified path under which the region directory lives;
   * generally the table subdirectory
   * @param region
   * @param family DBColumnDescriptor for this column
   * @param fs file system object
   * @param conf configuration object
   * failed.  Can be null.
   * @throws IOException
   */
  protected Store(Path basedir, DBRegion region, DBColumnDescriptor family,
    FileSystem fs, Configuration conf) throws IOException {
    DBRegionInfo info = region.mRegionInfo;
    this.mFs = fs;
    this.mHomedir = getStoreHomedir(basedir, info.getEncodedName(), family.getName());
    if (!this.mFs.exists(this.mHomedir)) {
      if (!this.mFs.mkdirs(this.mHomedir))
        throw new IOException("Failed create of: " + this.mHomedir.toString());
    }
    this.mRegion = region;
    this.mFamily = family;
    this.mConf = conf;
    this.mBlockcache = family.isBlockCacheEnabled();
    this.mBlocksize = family.getBlocksize();
    this.mCompression = family.getCompression();
    this.mComparator = info.getComparator();
    // getTimeToLive returns ttl in seconds.  Convert to milliseconds.
    this.mTtl = family.getTimeToLive();
    if (this.mTtl == DBConstants.FOREVER) {
      // default is unlimited ttl.
      this.mTtl = Long.MAX_VALUE;
    } else if (mTtl == -1) {
      this.mTtl = Long.MAX_VALUE;
    } else {
      // second -> ms adjust for user data
      this.mTtl *= 1000;
    }
    this.mMemstore = new MemStore(this.mComparator);
    this.mStoreNameStr = Bytes.toString(this.mFamily.getName());

    // By default, we compact if an DBStore has more than
    // MIN_COMMITS_FOR_COMPACTION map files
    this.mCompactionThreshold =
      conf.getInt("bigdb.dbstore.compactionThreshold", 3);

    // Check if this is in-memory store
    this.mInMemory = family.isInMemory();

    // By default we split region if a file > DBConstants.DEFAULT_MAX_FILE_SIZE.
    long maxFileSize = info.getTableDesc().getMaxFileSize();
    if (maxFileSize == DBConstants.DEFAULT_MAX_FILE_SIZE) {
      maxFileSize = conf.getLong("bigdb.dbregion.max.filesize",
        DBConstants.DEFAULT_MAX_FILE_SIZE);
    }
    this.mDesiredMaxFileSize = maxFileSize;

    this.mMajorCompactionTime =
      conf.getLong(DBConstants.MAJOR_COMPACTION_PERIOD, 86400000);
    if (family.getValue(DBConstants.MAJOR_COMPACTION_PERIOD) != null) {
      String strCompactionTime =
        family.getValue(DBConstants.MAJOR_COMPACTION_PERIOD);
      this.mMajorCompactionTime = (new Long(strCompactionTime)).longValue();
    }

    this.mMaxFilesToCompact = conf.getInt("bigdb.dbstore.compaction.max", 10);
    this.mStorefiles = ImmutableList.copyOf(loadStoreFiles());
  }

  public DBColumnDescriptor getFamily() {
    return this.mFamily;
  }

  /**
   * @return The maximum sequence id in all store files.
   */
  protected long getMaxSequenceId() {
    return StoreFile.getMaxSequenceIdInList(this.getStorefiles());
  }

  /**
   * @param tabledir
   * @param encodedName Encoded region name.
   * @param family
   * @return Path to family/Store home directory.
   */
  public static Path getStoreHomedir(final Path tabledir,
      final String encodedName, final byte[] family) {
    return new Path(tabledir, new Path(encodedName,
      new Path(Bytes.toString(family))));
  }

  /**
   * Return the directory in which this store stores its
   * StoreFiles
   */
  public Path getHomedir() {
    return mHomedir;
  }

  /**
   * Creates a series of StoreFile loaded from the given directory.
   * @throws IOException
   */
  private List<StoreFile> loadStoreFiles() throws IOException {
    ArrayList<StoreFile> results = new ArrayList<StoreFile>();
    FileStatus files[] = this.mFs.listStatus(this.mHomedir);
    for (int i = 0; files != null && i < files.length; i++) {
      // Skip directories.
      if (files[i].isDir()) 
        continue;
      
      Path p = files[i].getPath();
      // Check for empty file.  Should never be the case but can happen
      // after data loss in hdfs for whatever reason (upgrade, etc.): HBASE-646
      if (this.mFs.getFileStatus(p).getLen() <= 0) {
    	if (LOG.isWarnEnabled())
          LOG.warn("Skipping " + p + " because its empty. HBASE-646 DATA LOSS?");
        continue;
      }
      
      StoreFile curfile = null;
      try {
        curfile = new StoreFile(mFs, p, mBlockcache, this.mConf,
            this.mFamily.getBloomFilterType(), this.mInMemory);
        curfile.createReader();
      } catch (IOException ioe) {
    	if (LOG.isWarnEnabled()) {
          LOG.warn("Failed open of " + p + "; presumption is that file was " +
            "corrupted at flush and lost edits picked up by commit log replay. " +
            "Verify!", ioe);
    	}
        continue;
      }
      long length = curfile.getReader().length();
      this.mStoreSize += length;
      if (LOG.isDebugEnabled()) {
        LOG.debug("loaded " + curfile.toStringDetailed());
      }
      results.add(curfile);
    }
    Collections.sort(results, StoreFile.Comparators.FLUSH_TIME);
    return results;
  }

  /**
   * Adds a value to the memstore
   *
   * @param kv
   * @return memstore size delta
   */
  protected long add(final KeyValue kv) {
    mLock.readLock().lock();
    try {
      return this.mMemstore.add(kv);
    } finally {
      mLock.readLock().unlock();
    }
  }

  /**
   * Adds a value to the memstore
   *
   * @param kv
   * @return memstore size delta
   */
  protected long delete(final KeyValue kv) {
    mLock.readLock().lock();
    try {
      return this.mMemstore.delete(kv);
    } finally {
      mLock.readLock().unlock();
    }
  }

  /**
   * @return All store files.
   */
  protected List<StoreFile> getStorefiles() {
    return this.mStorefiles;
  }

  public void bulkLoadDBFile(String srcPathStr) throws IOException {
    Path srcPath = new Path(srcPathStr);

    DBFile.Reader reader  = null;
    try {
      if (LOG.isInfoEnabled()) {
        LOG.info("Validating dbfile at " + srcPath + " for inclusion in "
          + "store " + this + " region " + this.mRegion);
      }
      
      reader = new DBFile.Reader(srcPath.getFileSystem(mConf),
          srcPath, null, false);
      reader.loadFileInfo();

      byte[] firstKey = reader.getFirstRowKey();
      byte[] lk = reader.getLastKey();
      byte[] lastKey =
          (lk == null) ? null :
              KeyValue.createKeyValueFromKey(lk).getRow();

      if (LOG.isDebugEnabled()) {
        LOG.debug("DBFile bounds: first=" + Bytes.toStringBinary(firstKey) +
          " last=" + Bytes.toStringBinary(lastKey));
        LOG.debug("Region bounds: first=" +
          Bytes.toStringBinary(mRegion.getStartKey()) +
          " last=" + Bytes.toStringBinary(mRegion.getEndKey()));
      }
      
      DBRegionInfo hri = mRegion.getRegionInfo();
      if (!hri.containsRange(firstKey, lastKey)) {
        throw new WrongRegionException(
            "Bulk load file " + srcPathStr + " does not fit inside region "
            + this.mRegion);
      }
    } finally {
      if (reader != null) reader.close();
    }

    // Move the file if it's on another filesystem
    FileSystem srcFs = srcPath.getFileSystem(mConf);
    if (!srcFs.equals(mFs)) {
      if (LOG.isInfoEnabled()) {
        LOG.info("File " + srcPath + " on different filesystem than " +
          "destination store - moving to this filesystem.");
      }
      Path tmpPath = getTmpPath();
      FileUtil.copy(srcFs, srcPath, mFs, tmpPath, false, mConf);
      if (LOG.isInfoEnabled()) {
        LOG.info("Copied to temporary path on dst filesystem: " + tmpPath);
      }
      srcPath = tmpPath;
    }

    Path dstPath = StoreFile.getRandomFilename(mFs, mHomedir);
    if (LOG.isInfoEnabled()) {
      LOG.info("Renaming bulk load file " + srcPath + " to " + dstPath);
    }
    StoreFile.rename(mFs, srcPath, dstPath);

    StoreFile sf = new StoreFile(mFs, dstPath, mBlockcache,
        this.mConf, this.mFamily.getBloomFilterType(), this.mInMemory);
    sf.createReader();

    if (LOG.isInfoEnabled()) {
      LOG.info("Moved dbfile " + srcPath + " into store directory " +
        mHomedir + " - updating store file list.");
    }

    // Append the new storefile into the list
    this.mLock.writeLock().lock();
    try {
      ArrayList<StoreFile> newFiles = new ArrayList<StoreFile>(mStorefiles);
      newFiles.add(sf);
      this.mStorefiles = ImmutableList.copyOf(newFiles);
      notifyChangedReadersObservers();
    } finally {
      this.mLock.writeLock().unlock();
    }
    
    if (LOG.isInfoEnabled()) {
      LOG.info("Successfully loaded store file " + srcPath
        + " into store " + this + " (new location: " + dstPath + ")");
    }
  }

  /**
   * Get a temporary path in this region. These temporary files
   * will get cleaned up when the region is re-opened if they are
   * still around.
   */
  private Path getTmpPath() throws IOException {
    return StoreFile.getRandomFilename(mFs, mRegion.getTmpDir());
  }

  /**
   * Close all the readers
   *
   * We don't need to worry about subsequent requests because the DBRegion holds
   * a write lock that will prevent any more reads or writes.
   *
   * @throws IOException
   */
  protected ImmutableList<StoreFile> close() throws IOException {
    this.mLock.writeLock().lock();
    try {
      ImmutableList<StoreFile> result = mStorefiles;

      // Clear so metrics doesn't find them.
      mStorefiles = ImmutableList.of();

      for (StoreFile f: result) {
        f.closeReader();
      }
      
      if (LOG.isDebugEnabled())
        LOG.debug("closed " + this.mStoreNameStr);
      
      return result;
    } finally {
      this.mLock.writeLock().unlock();
    }
  }

  /**
   * Snapshot this stores memstore.  Call before running
   * {@link #flushCache(long, SortedSet<KeyValue>)} so it has some work to do.
   */
  protected void snapshot() {
    this.mMemstore.snapshot();
  }

  /**
   * Write out current snapshot.  Presumes {@link #snapshot()} has been called
   * previously.
   * @param logCacheFlushId flush sequence number
   * @param snapshot
   * @return true if a compaction is needed
   * @throws IOException
   */
  private StoreFile flushCache(final long logCacheFlushId,
      SortedSet<KeyValue> snapshot,
      TimeRangeTracker snapshotTimeRangeTracker) throws IOException {
    // If an exception happens flushing, we let it out without clearing
    // the memstore snapshot.  The old snapshot will be returned when we say
    // 'snapshot', the next time flush comes around.
    return internalFlushCache(snapshot, logCacheFlushId, snapshotTimeRangeTracker);
  }

  /**
   * @param cache
   * @param logCacheFlushId
   * @return StoreFile created.
   * @throws IOException
   */
  private StoreFile internalFlushCache(final SortedSet<KeyValue> set,
      final long logCacheFlushId, TimeRangeTracker snapshotTimeRangeTracker)
      throws IOException {
    StoreFile.Writer writer = null;
    long flushed = 0;
    // Don't flush if there are no entries.
    if (set.size() == 0) 
      return null;
    
    long oldestTimestamp = System.currentTimeMillis() - mTtl;
    // TODO:  We can fail in the below block before we complete adding this
    // flush to list of store files.  Add cleanup of anything put on filesystem
    // if we fail.
    synchronized (mFlushLock) {
      // A. Write the map out to the disk
      writer = createWriterInTmp(set.size());
      writer.setTimeRangeTracker(snapshotTimeRangeTracker);
      @SuppressWarnings("unused")
	  int entries = 0;
      try {
        for (KeyValue kv: set) {
          if (!isExpired(kv, oldestTimestamp)) {
            writer.append(kv);
            entries++;
            flushed += this.mMemstore.heapSizeChange(kv, true);
          }
        }
      } finally {
        // Write out the log sequence number that corresponds to this output
        // dbfile.  The dbfile is current up to and including logCacheFlushId.
        writer.appendMetadata(logCacheFlushId, false);
        writer.close();
      }
    }

    // Write-out finished successfully, move into the right spot
    Path dstPath = StoreFile.getUniqueFile(mFs, mHomedir);
    if (LOG.isInfoEnabled()) {
      LOG.info("Renaming flushed file at " + writer.getPath() + " to " + dstPath);
    }
    mFs.rename(writer.getPath(), dstPath);

    StoreFile sf = new StoreFile(this.mFs, dstPath, mBlockcache,
      this.mConf, this.mFamily.getBloomFilterType(), this.mInMemory);
    StoreFile.Reader r = sf.createReader();
    this.mStoreSize += r.length();
    
    if (LOG.isInfoEnabled()) {
      LOG.info("Added " + sf + ", entries=" + r.getEntries() +
        ", sequenceid=" + logCacheFlushId +
        ", memsize=" + StringUtils.humanReadableInt(flushed) +
        ", filesize=" + StringUtils.humanReadableInt(r.length()) +
        " to " + this.mRegion.mRegionInfo.getRegionNameAsString());
    }
    
    return sf;
  }

  /**
   * @return Writer for a new StoreFile in the tmp dir.
   */
  private StoreFile.Writer createWriterInTmp(int maxKeyCount)
      throws IOException {
    return StoreFile.createWriter(this.mFs, mRegion.getTmpDir(), this.mBlocksize,
        this.mCompression, this.mComparator, this.mConf,
        this.mFamily.getBloomFilterType(), maxKeyCount);
  }

  /**
   * Change storefiles adding into place the Reader produced by this new flush.
   * @param sf
   * @param set That was used to make the passed file <code>p</code>.
   * @throws IOException
   * @return Whether compaction is required.
   */
  private boolean updateStorefiles(final StoreFile sf,
                                   final SortedSet<KeyValue> set)
      throws IOException {
    this.mLock.writeLock().lock();
    try {
      ArrayList<StoreFile> newList = new ArrayList<StoreFile>(mStorefiles);
      newList.add(sf);
      mStorefiles = ImmutableList.copyOf(newList);
      this.mMemstore.clearSnapshot(set);

      // Tell listeners of the change in readers.
      notifyChangedReadersObservers();

      return this.mStorefiles.size() >= this.mCompactionThreshold;
    } finally {
      this.mLock.writeLock().unlock();
    }
  }

  /**
   * Notify all observers that set of Readers has changed.
   * @throws IOException
   */
  private void notifyChangedReadersObservers() throws IOException {
    for (ChangedReadersObserver o: this.mChangedReaderObservers) {
      o.updateReaders();
    }
  }

  /**
   * @param o Observer who wants to know about changes in set of Readers
   */
  protected void addChangedReaderObserver(ChangedReadersObserver o) {
    this.mChangedReaderObservers.add(o);
  }

  /**
   * @param o Observer no longer interested in changes in set of Readers.
   */
  protected void deleteChangedReaderObserver(ChangedReadersObserver o) {
    if (!this.mChangedReaderObservers.remove(o)) {
      if (LOG.isWarnEnabled())
        LOG.warn("Not in set" + o);
    }
  }

  //////////////////////////////////////////////////////////////////////////////
  // Compaction
  //////////////////////////////////////////////////////////////////////////////

  /**
   * Compact the StoreFiles.  This method may take some time, so the calling
   * thread must be able to block for long periods.
   *
   * <p>During this time, the Store can work as usual, getting values from
   * StoreFiles and writing new StoreFiles from the memstore.
   *
   * Existing StoreFiles are not destroyed until the new compacted StoreFile is
   * completely written-out to disk.
   *
   * <p>The compactLock prevents multiple simultaneous compactions.
   * The structureLock prevents us from interfering with other write operations.
   *
   * <p>We don't want to hold the structureLock for the whole time, as a compact()
   * can be lengthy and we want to allow cache-flushes during this period.
   *
   * @param mc True to force a major compaction regardless of thresholds
   * @return row to split around if a split is needed, null otherwise
   * @throws IOException
   */
  protected StoreSize compact(final boolean mc) throws IOException {
    boolean forceSplit = this.mRegion.shouldSplit(false);
    boolean majorcompaction = mc;
    
    synchronized (mCompactLock) {
      // filesToCompact are sorted oldest to newest.
      List<StoreFile> filesToCompact = this.mStorefiles;
      if (filesToCompact.isEmpty()) {
    	if (LOG.isDebugEnabled())
          LOG.debug(this.mStoreNameStr + ": no store files to compact");
        return null;
      }

      // Max-sequenceID is the last key of the storefiles TreeMap
      long maxId = StoreFile.getMaxSequenceIdInList(mStorefiles);

      // Check to see if we need to do a major compaction on this region.
      // If so, change doMajorCompaction to true to skip the incremental
      // compacting below. Only check if doMajorCompaction is not true.
      if (!majorcompaction) {
        majorcompaction = isMajorCompaction(filesToCompact);
      }

      boolean references = hasReferences(filesToCompact);
      if (!majorcompaction && !references &&
          (forceSplit || (filesToCompact.size() < mCompactionThreshold))) {
        return checkSplit(forceSplit);
      }

      // HBASE-745, preparing all store file sizes for incremental compacting
      // selection.
      int countOfFiles = filesToCompact.size();
      long totalSize = 0;
      long [] fileSizes = new long[countOfFiles];
      long skipped = 0;
      int point = 0;
      
      for (int i = 0; i < countOfFiles; i++) {
        StoreFile file = filesToCompact.get(i);
        Path path = file.getPath();
        if (path == null) {
          if (LOG.isWarnEnabled())
            LOG.warn("Path is null for " + file);
          return null;
        }
        StoreFile.Reader r = file.getReader();
        if (r == null) {
          if (LOG.isWarnEnabled())
            LOG.warn("StoreFile " + file + " has a null Reader");
          return null;
        }
        long len = file.getReader().length();
        fileSizes[i] = len;
        totalSize += len;
      }

      if (!majorcompaction && !references) {
        // Here we select files for incremental compaction.
        // The rule is: if the largest(oldest) one is more than twice the
        // size of the second, skip the largest, and continue to next...,
        // until we meet the compactionThreshold limit.

        // A problem with the above heuristic is that we could go through all of
        // filesToCompact and the above condition could hold for all files and
        // we'd end up with nothing to compact.  To protect against this, we'll
        // compact the tail -- up to the last 4 files -- of filesToCompact
        // regardless.
        // BANDAID for HBASE-2990, setting to 2
        int tail = Math.min(countOfFiles, 2);
        for (point = 0; point < (countOfFiles - tail); point++) {
          if (((fileSizes[point] < fileSizes[point + 1] * 2) &&
               (countOfFiles - point) <= mMaxFilesToCompact)) {
            break;
          }
          skipped += fileSizes[point];
        }
        
        filesToCompact = new ArrayList<StoreFile>(filesToCompact.subList(point,
          countOfFiles));
        
        if (filesToCompact.size() <= 1) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Skipped compaction of 1 file; compaction size of " +
              this.mStoreNameStr + ": " +
              StringUtils.humanReadableInt(totalSize) + "; Skipped " + point +
              " files, size: " + skipped);
          }
          return checkSplit(forceSplit);
        }
        
        if (LOG.isDebugEnabled()) {
          LOG.debug("Compaction size of " + this.mStoreNameStr + ": " +
            StringUtils.humanReadableInt(totalSize) + "; Skipped " + point +
            " file(s), size: " + skipped);
        }
      }

      // Ready to go.  Have list of files to compact.
      if (LOG.isInfoEnabled()) {
        LOG.info("Started compaction of " + filesToCompact.size() + " file(s) in " +
          this.mStoreNameStr + " of " + this.mRegion.getRegionInfo().getRegionNameAsString() +
          (references? ", hasReferences=true,": " ") + " into " +
          mRegion.getTmpDir() + ", sequenceid=" + maxId);
      }
      
      StoreFile.Writer writer = compact(filesToCompact, majorcompaction, maxId);
      // Move the compaction into place.
      StoreFile sf = completeCompaction(filesToCompact, writer);
      
      if (LOG.isInfoEnabled()) {
        LOG.info("Completed" + (majorcompaction? " major ": " ") +
          "compaction of " + filesToCompact.size() + " file(s) in " +
          this.mStoreNameStr + " of " + this.mRegion.getRegionInfo().getRegionNameAsString() +
          "; new storefile is " + (sf == null? "none": sf.toString()) +
          "; store size is " + StringUtils.humanReadableInt(mStoreSize));
      }
    }
    
    return checkSplit(forceSplit);
  }

  /**
   * @param files
   * @return True if any of the files in <code>files</code> are References.
   */
  private boolean hasReferences(Collection<StoreFile> files) {
    if (files != null && files.size() > 0) {
      for (StoreFile hsf: files) {
        if (hsf.isReference()) 
          return true;
      }
    }
    return false;
  }

  /**
   * Gets lowest timestamp from files in a dir
   *
   * @param fs
   * @param dir
   * @throws IOException
   */
  static long getLowestTimestamp(FileSystem fs, Path dir) throws IOException {
    FileStatus[] stats = fs.listStatus(dir);
    if (stats == null || stats.length == 0) {
      return 0l;
    }
    long lowTimestamp = Long.MAX_VALUE;
    for (int i = 0; i < stats.length; i++) {
      long timestamp = stats[i].getModificationTime();
      if (timestamp < lowTimestamp){
        lowTimestamp = timestamp;
      }
    }
    return lowTimestamp;
  }

  /**
   * @return True if we should run a major compaction.
   */
  protected boolean isMajorCompaction() throws IOException {
    return isMajorCompaction(mStorefiles);
  }

  /**
   * @param filesToCompact Files to compact. Can be null.
   * @return True if we should run a major compaction.
   */
  private boolean isMajorCompaction(final List<StoreFile> filesToCompact) 
	  throws IOException {
    boolean result = false;
    if (filesToCompact == null || filesToCompact.isEmpty() ||
        mMajorCompactionTime == 0) {
      return result;
    }
    
    long lowTimestamp = getLowestTimestamp(mFs,
      filesToCompact.get(0).getPath().getParent());
    long now = System.currentTimeMillis();
    
    if (lowTimestamp > 0l && lowTimestamp < (now - this.mMajorCompactionTime)) {
      // Major compaction time has elapsed.
      long elapsedTime = now - lowTimestamp;
      
      if (filesToCompact.size() == 1 &&
          filesToCompact.get(0).isMajorCompaction() &&
          (this.mTtl == DBConstants.FOREVER || elapsedTime < this.mTtl)) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Skipping major compaction of " + this.mStoreNameStr +
            " because one (major) compacted file only and elapsedTime " +
            elapsedTime + "ms is < ttl=" + this.mTtl);
        }
      } else {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Major compaction triggered on store " + this.mStoreNameStr +
            "; time since last major compaction " + (now - lowTimestamp) + "ms");
        }
        result = true;
      }
    }
    
    return result;
  }

  /**
   * Do a minor/major compaction.  Uses the scan infrastructure to make it easy.
   *
   * @param filesToCompact which files to compact
   * @param majorCompaction true to major compact (prune all deletes, max versions, etc)
   * @param maxId Readers maximum sequence id.
   * @return Product of compaction or null if all cells expired or deleted and
   * nothing made it through the compaction.
   * @throws IOException
   */
  private StoreFile.Writer compact(final List<StoreFile> filesToCompact,
      final boolean majorCompaction, final long maxId)
      throws IOException {
    // calculate maximum key count after compaction (for blooms)
    int maxKeyCount = 0;
    for (StoreFile file : filesToCompact) {
      StoreFile.Reader r = file.getReader();
      if (r != null) {
        // NOTE: getFilterEntries could cause under-sized blooms if the user
        //       switches bloom type (e.g. from ROW to ROWCOL)
        maxKeyCount += (r.getBloomFilterType() == mFamily.getBloomFilterType())
          ? r.getFilterEntries() : r.getEntries();
      }
    }

    // For each file, obtain a scanner:
    List<StoreFileScanner> scanners = StoreFileScanner
      .getScannersForStoreFiles(filesToCompact, false, false);

    // Make the instantiation lazy in case compaction produces no product; i.e.
    // where all source cells are expired or deleted.
    StoreFile.Writer writer = null;
    try {
      if (majorCompaction) {
        InternalScanner scanner = null;
        try {
          Scan scan = new Scan();
          scan.setMaxVersions(mFamily.getMaxVersions());
          scanner = new StoreScanner(this, scan, scanners);
          // since scanner.next() can return 'false' but still be delivering data,
          // we have to use a do/while loop.
          ArrayList<KeyValue> kvs = new ArrayList<KeyValue>();
          while (scanner.next(kvs)) {
            // output to writer:
            for (KeyValue kv : kvs) {
              if (writer == null) {
                writer = createWriterInTmp(maxKeyCount);
              }
              writer.append(kv);
            }
            kvs.clear();
          }
        } finally {
          if (scanner != null) 
            scanner.close();
        }
      } else {
        MinorCompactingStoreScanner scanner = null;
        try {
          scanner = new MinorCompactingStoreScanner(this, scanners);
          writer = createWriterInTmp(maxKeyCount);
          while (scanner.next(writer)) {
            // Nothing to do
          }
        } finally {
          if (scanner != null)
            scanner.close();
        }
      }
    } finally {
      if (writer != null) {
        writer.appendMetadata(maxId, majorCompaction);
        writer.close();
      }
    }
    return writer;
  }

  /**
   * It's assumed that the compactLock  will be acquired prior to calling this
   * method!  Otherwise, it is not thread-safe!
   *
   * <p>It works by processing a compaction that's been written to disk.
   *
   * <p>It is usually invoked at the end of a compaction, but might also be
   * invoked at DBStore startup, if the prior execution died midway through.
   *
   * <p>Moving the compacted TreeMap into place means:
   * <pre>
   * 1) Moving the new compacted StoreFile into place
   * 2) Unload all replaced StoreFile, close and collect list to delete.
   * 3) Loading the new TreeMap.
   * 4) Compute new store size
   * </pre>
   *
   * @param compactedFiles list of files that were compacted
   * @param compactedFile StoreFile that is the result of the compaction
   * @return StoreFile created. May be null.
   * @throws IOException
   */
  private StoreFile completeCompaction(final List<StoreFile> compactedFiles,
                                       final StoreFile.Writer compactedFile)
      throws IOException {
    // 1. Moving the new files into place -- if there is a new file (may not
    // be if all cells were expired or deleted).
    StoreFile result = null;
    if (compactedFile != null) {
      Path p = null;
      try {
        p = StoreFile.rename(this.mFs, compactedFile.getPath(),
          StoreFile.getRandomFilename(mFs, this.mHomedir));
      } catch (IOException e) {
    	if (LOG.isErrorEnabled())
          LOG.error("Failed move of compacted file " + compactedFile.getPath(), e);
        return null;
      }
      result = new StoreFile(this.mFs, p, mBlockcache, this.mConf,
          this.mFamily.getBloomFilterType(), this.mInMemory);
      result.createReader();
    }
    this.mLock.writeLock().lock();
    try {
      try {
        // 2. Unloading
        // 3. Loading the new TreeMap.
        // Change this.storefiles so it reflects new state but do not
        // delete old store files until we have sent out notification of
        // change in case old files are still being accessed by outstanding
        // scanners.
        ArrayList<StoreFile> newStoreFiles = new ArrayList<StoreFile>();
        for (StoreFile sf : mStorefiles) {
          if (!compactedFiles.contains(sf)) {
            newStoreFiles.add(sf);
          }
        }

        // If a StoreFile result, move it into place.  May be null.
        if (result != null) {
          newStoreFiles.add(result);
        }

	    this.mStorefiles = ImmutableList.copyOf(newStoreFiles);

        // Tell observers that list of StoreFiles has changed.
        notifyChangedReadersObservers();
        // Finally, delete old store files.
        for (StoreFile hsf: compactedFiles) {
          hsf.deleteReader();
        }
      } catch (IOException e) {
        //e = RemoteExceptionHandler.checkIOException(e);
    	if (LOG.isErrorEnabled()) {
          LOG.error("Failed replacing compacted files in " + this.mStoreNameStr +
            ". Compacted file is " + (result == null? "none": result.toString()) +
            ".  Files replaced " + compactedFiles.toString() +
            " some of which may have been already removed", e);
    	}
      }
      // 4. Compute new store size
      this.mStoreSize = 0L;
      for (StoreFile hsf : this.mStorefiles) {
        StoreFile.Reader r = hsf.getReader();
        if (r == null) {
          if (LOG.isWarnEnabled())
            LOG.warn("StoreFile " + hsf + " has a null Reader");
          continue;
        }
        this.mStoreSize += r.length();
      }
    } finally {
      this.mLock.writeLock().unlock();
    }
    return result;
  }

  // ////////////////////////////////////////////////////////////////////////////
  // Accessors.
  // (This is the only section that is directly useful!)
  //////////////////////////////////////////////////////////////////////////////
  /**
   * @return the number of files in this store
   */
  public int getNumberOfstorefiles() {
    return this.mStorefiles.size();
  }

  /**
   * @param wantedVersions How many versions were asked for.
   * @return wantedVersions or this families' {@link DBConstants#VERSIONS}.
   */
  protected int versionsToReturn(final int wantedVersions) {
    if (wantedVersions <= 0) {
      throw new IllegalArgumentException("Number of versions must be > 0");
    }
    // Make sure we do not return more than maximum versions for this store.
    int maxVersions = this.mFamily.getMaxVersions();
    return wantedVersions > maxVersions ? maxVersions: wantedVersions;
  }

  static boolean isExpired(final KeyValue key, final long oldestTimestamp) {
    return key.getTimestamp() < oldestTimestamp;
  }

  /**
   * Find the key that matches <i>row</i> exactly, or the one that immediately
   * preceeds it. WARNING: Only use this method on a table where writes occur
   * with strictly increasing timestamps. This method assumes this pattern of
   * writes in order to make it reasonably performant.  Also our search is
   * dependent on the axiom that deletes are for cells that are in the container
   * that follows whether a memstore snapshot or a storefile, not for the
   * current container: i.e. we'll see deletes before we come across cells we
   * are to delete. Presumption is that the memstore#kvset is processed before
   * memstore#snapshot and so on.
   * @param kv First possible item on targeted row; i.e. empty columns, latest
   * timestamp and maximum type.
   * @return Found keyvalue or null if none found.
   * @throws IOException
   */
  protected KeyValue getRowKeyAtOrBefore(final KeyValue kv) throws IOException {
    GetClosestRowBeforeTracker state = new GetClosestRowBeforeTracker(
      this.mComparator, kv, this.mTtl, this.mRegion.getRegionInfo().isMetaRegion());
    this.mLock.readLock().lock();
    try {
      // First go to the memstore.  Pick up deletes and candidates.
      this.mMemstore.getRowKeyAtOrBefore(state);
      // Check if match, if we got a candidate on the asked for 'kv' row.
      // Process each store file. Run through from newest to oldest.
      for (StoreFile sf : Iterables.reverse(mStorefiles)) {
        // Update the candidate keys from the current map file
        rowAtOrBeforeFromStoreFile(sf, state);
      }
      return state.getCandidate();
    } finally {
      this.mLock.readLock().unlock();
    }
  }

  /**
   * Check an individual MapFile for the row at or before a given row.
   * @param f
   * @param state
   * @throws IOException
   */
  @SuppressWarnings("deprecation")
  private void rowAtOrBeforeFromStoreFile(final StoreFile f,
      final GetClosestRowBeforeTracker state) throws IOException {
    StoreFile.Reader r = f.getReader();
    if (r == null) {
      if (LOG.isWarnEnabled())
        LOG.warn("StoreFile " + f + " has a null Reader");
      return;
    }
    
    // TODO: Cache these keys rather than make each time?
    byte[] fk = r.getFirstKey();
    KeyValue firstKV = KeyValue.createKeyValueFromKey(fk, 0, fk.length);
    byte[] lk = r.getLastKey();
    KeyValue lastKV = KeyValue.createKeyValueFromKey(lk, 0, lk.length);
    KeyValue firstOnRow = state.getTargetKey();
    
    if (this.mComparator.compareRows(lastKV, firstOnRow) < 0) {
      // If last key in file is not of the target table, no candidates in this
      // file.  Return.
      if (!state.isTargetTable(lastKV)) return;
      // If the row we're looking for is past the end of file, set search key to
      // last key. TODO: Cache last and first key rather than make each time.
      firstOnRow = new KeyValue(lastKV.getRow(), DBConstants.LATEST_TIMESTAMP);
    }
    
    // Get a scanner that caches blocks and that uses pread.
    DBFileScanner scanner = r.getScanner(true, true);
    // Seek scanner.  If can't seek it, return.
    if (!seekToScanner(scanner, firstOnRow, firstKV)) return;
    // If we found candidate on firstOnRow, just return. THIS WILL NEVER HAPPEN!
    // Unlikely that there'll be an instance of actual first row in table.
    if (walkForwardInSingleRow(scanner, firstOnRow, state)) return;
    
    // If here, need to start backing up.
    while (scanner.seekBefore(firstOnRow.getBuffer(), firstOnRow.getKeyOffset(),
       firstOnRow.getKeyLength())) {
      KeyValue kv = scanner.getKeyValue();
      if (!state.isTargetTable(kv)) break;
      if (!state.isBetterCandidate(kv)) break;
      // Make new first on row.
      firstOnRow = new KeyValue(kv.getRow(), DBConstants.LATEST_TIMESTAMP);
      // Seek scanner.  If can't seek it, break.
      if (!seekToScanner(scanner, firstOnRow, firstKV)) break;
      // If we find something, break;
      if (walkForwardInSingleRow(scanner, firstOnRow, state)) break;
    }
  }

  /**
   * Seek the file scanner to firstOnRow or first entry in file.
   * @param scanner
   * @param firstOnRow
   * @param firstKV
   * @return True if we successfully seeked scanner.
   * @throws IOException
   */
  private boolean seekToScanner(final DBFileScanner scanner,
                                final KeyValue firstOnRow,
                                final KeyValue firstKV)
      throws IOException {
    KeyValue kv = firstOnRow;
    // If firstOnRow < firstKV, set to firstKV
    if (this.mComparator.compareRows(firstKV, firstOnRow) == 0) kv = firstKV;
    int result = scanner.seekTo(kv.getBuffer(), kv.getKeyOffset(),
      kv.getKeyLength());
    return result >= 0;
  }

  /**
   * When we come in here, we are probably at the kv just before we break into
   * the row that firstOnRow is on.  Usually need to increment one time to get
   * on to the row we are interested in.
   * @param scanner
   * @param firstOnRow
   * @param state
   * @return True we found a candidate.
   * @throws IOException
   */
  private boolean walkForwardInSingleRow(final DBFileScanner scanner,
                                         final KeyValue firstOnRow,
                                         final GetClosestRowBeforeTracker state)
      throws IOException {
    boolean foundCandidate = false;
    do {
      KeyValue kv = scanner.getKeyValue();
      // If we are not in the row, skip.
      if (this.mComparator.compareRows(kv, firstOnRow) < 0) continue;
      // Did we go beyond the target row? If so break.
      if (state.isTooFar(kv, firstOnRow)) break;
      if (state.isExpired(kv)) 
        continue;
      
      // If we added something, this row is a contender. break.
      if (state.handle(kv)) {
        foundCandidate = true;
        break;
      }
    } while(scanner.next());
    return foundCandidate;
  }

  /**
   * Determines if DBStore can be split
   * @param force Whether to force a split or not.
   * @return a StoreSize if store can be split, null otherwise.
   */
  protected StoreSize checkSplit(final boolean force) {
    this.mLock.readLock().lock();
    try {
      // Iterate through all store files
      if (this.mStorefiles.isEmpty()) {
        return null;
      }
      if (!force && (mStoreSize < this.mDesiredMaxFileSize)) {
        return null;
      }

      if (this.mRegion.getRegionInfo().isMetaRegion()) {
        if (force) {
          if (LOG.isWarnEnabled())
            LOG.warn("Cannot split meta regions in BigDB 0.20");
        }
        return null;
      }

      // Not splitable if we find a reference store file present in the store.
      boolean splitable = true;
      long maxSize = 0L;
      StoreFile largestSf = null;
      
      for (StoreFile sf : mStorefiles) {
        if (splitable) {
          splitable = !sf.isReference();
          if (!splitable) {
            // RETURN IN MIDDLE OF FUNCTION!!! If not splitable, just return.
            if (LOG.isDebugEnabled()) {
              LOG.debug(sf +  " is not splittable");
            }
            return null;
          }
        }
        
        StoreFile.Reader r = sf.getReader();
        if (r == null) {
          if (LOG.isWarnEnabled())
            LOG.warn("Storefile " + sf + " Reader is null");
          continue;
        }
        
        long size = r.length();
        if (size > maxSize) {
          // This is the largest one so far
          maxSize = size;
          largestSf = sf;
        }
      }
      
      StoreFile.Reader r = largestSf.getReader();
      if (r == null) {
    	if (LOG.isWarnEnabled())
          LOG.warn("Storefile " + largestSf + " Reader is null");
        return null;
      }
      
      // Get first, last, and mid keys.  Midkey is the key that starts block
      // in middle of dbfile.  Has column and timestamp.  Need to return just
      // the row we want to split on as midkey.
      byte[] midkey = r.midkey();
      if (midkey != null) {
        KeyValue mk = KeyValue.createKeyValueFromKey(midkey, 0, midkey.length);
        byte[] fk = r.getFirstKey();
        KeyValue firstKey = KeyValue.createKeyValueFromKey(fk, 0, fk.length);
        byte[] lk = r.getLastKey();
        KeyValue lastKey = KeyValue.createKeyValueFromKey(lk, 0, lk.length);
        
        // if the midkey is the same as the first and last keys, then we cannot
        // (ever) split this region.
        if (this.mComparator.compareRows(mk, firstKey) == 0 &&
            this.mComparator.compareRows(mk, lastKey) == 0) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("cannot split because midkey is the same as first or " +
              "last row");
          }
          return null;
        }
        return new StoreSize(maxSize, mk.getRow());
      }
    } catch(IOException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("Failed getting store size for " + this.mStoreNameStr, e);
    } finally {
      this.mLock.readLock().unlock();
    }
    return null;
  }

  /** @return aggregate size of DBStore */
  public long getSize() {
    return mStoreSize;
  }

  //////////////////////////////////////////////////////////////////////////////
  // File administration
  //////////////////////////////////////////////////////////////////////////////

  /**
   * Return a scanner for both the memstore and the DBStore files
   * @throws IOException
   */
  public KeyValueScanner getScanner(Scan scan,
      final NavigableSet<byte[]> targetCols) throws IOException {
    this.mLock.readLock().lock();
    try {
      return new StoreScanner(this, scan, targetCols);
    } finally {
      this.mLock.readLock().unlock();
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{name=" + this.mStoreNameStr + "}";
  }

  /**
   * @return Count of store files
   */
  protected int getStorefilesCount() {
    return this.mStorefiles.size();
  }

  /**
   * @return The size of the store files, in bytes.
   */
  protected long getStorefilesSize() {
    long size = 0;
    for (StoreFile s: mStorefiles) {
      StoreFile.Reader r = s.getReader();
      if (r == null) {
    	if (LOG.isWarnEnabled())
          LOG.warn("StoreFile " + s + " has a null Reader");
        continue;
      }
      size += r.length();
    }
    return size;
  }

  /**
   * @return The size of the store file indexes, in bytes.
   */
  protected long getStorefilesIndexSize() {
    long size = 0;
    for (StoreFile s: mStorefiles) {
      StoreFile.Reader r = s.getReader();
      if (r == null) {
    	if (LOG.isWarnEnabled())
          LOG.warn("StoreFile " + s + " has a null Reader");
        continue;
      }
      size += r.indexSize();
    }
    return size;
  }

  /**
   * Datastructure that holds size and row to split a file around.
   * TODO: Take a KeyValue rather than row.
   */
  static class StoreSize {
    private final long mSize;
    private final byte[] mRow;

    StoreSize(long size, byte[] row) {
      this.mSize = size;
      this.mRow = row;
    }
    
    /* @return the size */
    public long getSize() {
      return mSize;
    }

    public byte[] getSplitRow() {
      return this.mRow;
    }
  }

  protected DBRegion getDBRegion() {
    return this.mRegion;
  }

  protected DBRegionInfo getDBRegionInfo() {
    return this.mRegion.mRegionInfo;
  }

  /**
   * Increments the value for the given row/family/qualifier.
   *
   * This function will always be seen as atomic by other readers
   * because it only puts a single KV to memstore. Thus no
   * read/write control necessary.
   *
   * @param row
   * @param f
   * @param qualifier
   * @param newValue the new value to set into memstore
   * @return memstore size delta
   * @throws IOException
   */
  public long updateColumnValue(byte[] row, byte[] f,
                                byte[] qualifier, long newValue)
      throws IOException {
    this.mLock.readLock().lock();
    try {
      long now = System.currentTimeMillis(); 

      return this.mMemstore.updateColumnValue(row,
          f,
          qualifier,
          newValue,
          now);
    } finally {
      this.mLock.readLock().unlock();
    }
  }

  public StoreFlusher getStoreFlusher(long cacheFlushId) {
    return new StoreFlusherImpl(cacheFlushId);
  }

  private class StoreFlusherImpl implements StoreFlusher {
    private long mCacheFlushId;
    private SortedSet<KeyValue> mSnapshot;
    private StoreFile mStoreFile;
    private TimeRangeTracker mSnapshotTimeRangeTracker;

    private StoreFlusherImpl(long cacheFlushId) {
      this.mCacheFlushId = cacheFlushId;
    }

    @Override
    public void prepare() {
      mMemstore.snapshot();
      this.mSnapshot = mMemstore.getSnapshot();
      this.mSnapshotTimeRangeTracker = mMemstore.getSnapshotTimeRangeTracker();
    }

    @Override
    public void flushCache() throws IOException {
      mStoreFile = Store.this.flushCache(mCacheFlushId, mSnapshot, mSnapshotTimeRangeTracker);
    }

    @Override
    public boolean commit() throws IOException {
      if (mStoreFile == null) 
        return false;
      
      // Add new file to store files.  Clear snapshot too while we have
      // the Store write lock.
      return Store.this.updateStorefiles(mStoreFile, mSnapshot);
    }
  }

  /**
   * See if there's too much store files in this store
   * @return true if number of store files is greater than
   *  the number defined in compactionThreshold
   */
  public boolean hasTooManyStoreFiles() {
    return this.mStorefiles.size() > this.mCompactionThreshold;
  }

  public static final long FIXED_OVERHEAD = ClassSize.align(
      ClassSize.OBJECT + (14 * ClassSize.REFERENCE) +
      (4 * Bytes.SIZEOF_LONG) + (3 * Bytes.SIZEOF_INT) + (Bytes.SIZEOF_BOOLEAN * 2));

  public static final long DEEP_OVERHEAD = ClassSize.align(FIXED_OVERHEAD +
      ClassSize.OBJECT + ClassSize.REENTRANT_LOCK +
      ClassSize.CONCURRENT_SKIPLISTMAP +
      ClassSize.CONCURRENT_SKIPLISTMAP_ENTRY + ClassSize.OBJECT);

  @Override
  public long heapSize() {
    return DEEP_OVERHEAD + this.mMemstore.heapSize();
  }
}
