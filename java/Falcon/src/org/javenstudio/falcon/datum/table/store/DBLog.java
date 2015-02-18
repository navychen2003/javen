package org.javenstudio.falcon.datum.table.store;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.fs.FileStatus;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;
import org.javenstudio.raptor.fs.PathFilter;
import org.javenstudio.raptor.fs.Syncable;
import org.javenstudio.raptor.io.Writable;

import com.google.common.util.concurrent.NamingThreadFactory;

/**
 * DBLog stores all the edits to the DBStore.  Its the bigdb write-ahead-log
 * implementation.
 *
 * It performs logfile-rolling, so external callers are not aware that the
 * underlying file is being rolled.
 *
 * <p>
 * There is one DBLog per RegionServer.  All edits for all Regions carried by
 * a particular RegionServer are entered first in the DBLog.
 *
 * <p>
 * Each DBRegion is identified by a unique long <code>int</code>. DBRegions do
 * not need to declare themselves before using the DBLog; they simply include
 * their DBRegion-id in the <code>append</code> or
 * <code>completeCacheFlush</code> calls.
 *
 * <p>
 * An DBLog consists of multiple on-disk files, which have a chronological order.
 * As data is flushed to other (better) on-disk structures, the log becomes
 * obsolete. We can destroy all the log messages for a given DBRegion-id up to
 * the most-recent CACHEFLUSH message from that DBRegion.
 *
 * <p>
 * It's only practical to delete entire files. Thus, we delete an entire on-disk
 * file F when all of the messages in F have a log-sequence-id that's older
 * (smaller) than the most-recent CACHEFLUSH message for every DBRegion that has
 * a message in F.
 *
 * <p>
 * Synchronized methods can never execute in parallel. However, between the
 * start of a cache flush and the completion point, appends are allowed but log
 * rolling is not. To prevent log rolling taking place during this period, a
 * separate reentrant lock is used.
 *
 * <p>To read an DBLog, call {@link #getReader(org.javenstudio.raptor.fs.FileSystem,
 * org.javenstudio.raptor.fs.Path, org.javenstudio.raptor.conf.Configuration)}.
 *
 */
@SuppressWarnings("deprecation")
public class DBLog implements Syncable {
  private static final Logger LOG = Logger.getLogger(DBLog.class);
  
  static final byte[] METAFAMILY = Bytes.toBytes("METAFAMILY");
  static final byte[] METAROW = Bytes.toBytes("METAROW");
  
  static final Object[] NO_ARGS = new Object[]{};

  /**
   * Name of directory that holds recovered edits written by the wal log
   * splitting code, one per region
   */
  private static final String RECOVERED_EDITS_DIR = "recovered.edits";
  private static final Pattern EDITFILES_NAME_PATTERN =
    Pattern.compile("-?[0-9]+");
  
  private final FileSystem mFs;
  private final Path mDir;
  private final Configuration mConf;
  private final LogRollListener mListener;
  private final long mOptionalFlushInterval;
  private final long mBlocksize;
  private final int mFlushlogentries;
  private final String mPrefix;
  private final AtomicInteger mUnflushedEntries = new AtomicInteger(0);
  private final Path mOldLogDir;
  private final List<LogActionsListener> mActionListeners =
      Collections.synchronizedList(new ArrayList<LogActionsListener>());

  private static Class<? extends Writer> mLogWriterClass;
  private static Class<? extends Reader> mLogReaderClass;

  private OutputStream mDfsOut;     	 // OutputStream associated with the current SequenceFile.writer
  private int mInitialReplication;    	 // initial replication factor of SequenceFile.writer
  private Method mGetNumCurrentReplicas; // refers to DFSOutputStream.getNumCurrentReplicas
  
  // used to indirectly tell syncFs to force the sync
  private boolean mForceSync = false;

  public interface Reader {
    void init(FileSystem fs, Path path, Configuration c) throws IOException;
    void close() throws IOException;
    Entry next() throws IOException;
    Entry next(Entry reuse) throws IOException;
    void seek(long pos) throws IOException;
    long getPosition() throws IOException;
  }

  public interface Writer {
    void init(FileSystem fs, Path path, Configuration c) throws IOException;
    void close() throws IOException;
    void sync() throws IOException;
    void append(Entry entry) throws IOException;
    long getLength() throws IOException;
  }

  /**
   * Current log file.
   */
  private Writer mWriter;

  /**
   * Map of all log files but the current one.
   */
  private final SortedMap<Long, Path> mOutputfiles =
    Collections.synchronizedSortedMap(new TreeMap<Long, Path>());

  /**
   * Map of regions to first sequence/edit id in their memstore.
   */
  private final ConcurrentSkipListMap<byte[], Long> mLastSeqWritten =
    new ConcurrentSkipListMap<byte[], Long>(Bytes.BYTES_COMPARATOR);

  private volatile boolean mClosed = false;

  private final AtomicLong mLogSeqNum = new AtomicLong(0);

  // The timestamp (in ms) when the log file was created.
  private volatile long mFilenum = -1;

  //number of transactions in the current Hlog.
  private final AtomicInteger mNumEntries = new AtomicInteger(0);

  // If > than this size, roll the log. This is typically 0.95 times the size
  // of the default Hdfs block size.
  private final long mLogrollsize;

  // This lock prevents starting a log roll during a cache flush.
  // synchronized is insufficient because a cache flush spans two method calls.
  private final Lock mCacheFlushLock = new ReentrantLock();

  // We synchronize on updateLock to prevent updates and to prevent a log roll
  // during an update
  private final Object mUpdateLock = new Object();

  private final boolean mEnabled;

  /**
   * If more than this many logs, force flush of oldest region to oldest edit
   * goes to disk.  If too many and we crash, then will take forever replaying.
   * Keep the number of logs tidy.
   */
  private final int mMaxLogs;

  /**
   * Thread that handles group commit
   */
  private final LogSyncer mLogSyncerThread;

  private final List<LogEntryVisitor> mLogEntryVisitors =
      new CopyOnWriteArrayList<LogEntryVisitor>();

  /**
   * Pattern used to validate a DBLog file name
   */
  private static final Pattern sPattern = Pattern.compile(".*\\.\\d*");

  static byte[] COMPLETE_CACHE_FLUSH;
  static {
    try {
      COMPLETE_CACHE_FLUSH =
        "BIGDB::CACHEFLUSH".getBytes(DBConstants.UTF8_ENCODING);
    } catch (UnsupportedEncodingException e) {
      assert(false);
    }
  }

  // For measuring latency of writes
  private static volatile long mWriteOps;
  private static volatile long mWriteTime;
  // For measuring latency of syncs
  private static volatile long mSyncOps;
  private static volatile long mSyncTime;

  public static long getWriteOps() {
    long ret = mWriteOps;
    mWriteOps = 0;
    return ret;
  }

  public static long getWriteTime() {
    long ret = mWriteTime;
    mWriteTime = 0;
    return ret;
  }

  public static long getSyncOps() {
    long ret = mSyncOps;
    mSyncOps = 0;
    return ret;
  }

  public static long getSyncTime() {
    long ret = mSyncTime;
    mSyncTime = 0;
    return ret;
  }

  /**
   * DBLog creating with a null actions listener.
   *
   * @param fs filesystem handle
   * @param dir path to where dblogs are stored
   * @param oldLogDir path to where dblogs are archived
   * @param conf configuration to use
   * @param listener listerner used to request log rolls
   * @throws IOException
   */
  public DBLog(final FileSystem fs, final Path dir, final Path oldLogDir,
               final Configuration conf, final LogRollListener listener)
               throws IOException {
    this(fs, dir, oldLogDir, conf, listener, null, null);
  }

  /**
   * Create an edit log at the given <code>dir</code> location.
   *
   * You should never have to load an existing log. If there is a log at
   * startup, it should have already been processed and deleted by the time the
   * DBLog object is started up.
   *
   * @param fs filesystem handle
   * @param dir path to where dblogs are stored
   * @param oldLogDir path to where dblogs are archived
   * @param conf configuration to use
   * @param listener listerner used to request log rolls
   * @param actionListener optional listener for dblog actions like archiving
   * @param prefix should always be hostname and port in distributed env and
   *        it will be URL encoded before being used.
   *        If prefix is null, "dblog" will be used
   * @throws IOException
   */
  public DBLog(final FileSystem fs, final Path dir, final Path oldLogDir,
               final Configuration conf, final LogRollListener listener,
               final LogActionsListener actionListener, final String prefix)
               throws IOException {
    super();
    this.mFs = fs;
    this.mDir = dir;
    this.mConf = conf;
    this.mListener = listener;
    this.mFlushlogentries =
      conf.getInt("bigdb.regionserver.flushlogentries", 1);
    this.mBlocksize = conf.getLong("bigdb.regionserver.dblog.blocksize",
      this.mFs.getDefaultBlockSize());
    // Roll at 95% of block size.
    float multi = conf.getFloat("bigdb.regionserver.logroll.multiplier", 0.95f);
    this.mLogrollsize = (long)(this.mBlocksize * multi);
    this.mOptionalFlushInterval =
      conf.getLong("bigdb.regionserver.optionallogflushinterval", 1 * 1000);
    if (fs.exists(dir)) {
      //throw new IOException("Target DBLog directory already exists: " + dir);
      if (LOG.isWarnEnabled())
        LOG.warn("Target DBLog directory already exists: " + dir);
    } else {
      fs.mkdirs(dir);
    }
    this.mOldLogDir = oldLogDir;
    if (oldLogDir != null && !fs.exists(oldLogDir)) {
      fs.mkdirs(this.mOldLogDir);
    }
    this.mMaxLogs = conf.getInt("bigdb.regionserver.maxlogs", 32);
    this.mEnabled = conf.getBoolean("bigdb.regionserver.dblog.enabled", true);
    if (LOG.isInfoEnabled()) {
      LOG.info("DBLog configuration: blocksize=" + this.mBlocksize +
        ", rollsize=" + this.mLogrollsize +
        ", enabled=" + this.mEnabled +
        ", flushlogentries=" + this.mFlushlogentries +
        ", optionallogflushinternal=" + this.mOptionalFlushInterval + "ms");
    }
    if (actionListener != null) {
      addLogActionsListerner(actionListener);
    }
    // If prefix is null||empty then just name it dblog
    this.mPrefix = prefix == null || prefix.isEmpty() ?
        "dblog" : URLEncoder.encode(prefix, "UTF8");
    // rollWriter sets this.mDfsOut if it can.
    rollWriter();

    // handle the reflection necessary to call getNumCurrentReplicas()
    this.mGetNumCurrentReplicas = null;
    if (this.mDfsOut != null) {
      try {
        this.mGetNumCurrentReplicas = this.mDfsOut.getClass().
          getMethod("getNumCurrentReplicas", new Class<?> []{});
        this.mGetNumCurrentReplicas.setAccessible(true);
      } catch (NoSuchMethodException e) {
        // Thrown if getNumCurrentReplicas() function isn't available
      } catch (SecurityException e) {
        // Thrown if we can't get access to getNumCurrentReplicas()
        this.mGetNumCurrentReplicas = null; // could happen on setAccessible()
      }
    }
    if (this.mGetNumCurrentReplicas != null) {
      if (LOG.isInfoEnabled())
        LOG.info("Using getNumCurrentReplicas--HDFS-826");
    } else {
      if (LOG.isInfoEnabled())
        LOG.info("getNumCurrentReplicas--HDFS-826 not available" );
    }

    mLogSyncerThread = new LogSyncer(this.mOptionalFlushInterval);
    //Threads.setDaemonThreadRunning(mLogSyncerThread,
    //    Thread.currentThread().getName() + ".logSyncer");
  }

  /**
   * @return Current state of the monotonically increasing file id.
   */
  public long getFilenum() {
    return this.mFilenum;
  }

  /**
   * Called by DBRegionServer when it opens a new region to ensure that log
   * sequence numbers are always greater than the latest sequence number of the
   * region being brought on-line.
   *
   * @param newvalue We'll set log edit/sequence number to this value if it
   * is greater than the current value.
   */
  public void setSequenceNumber(final long newvalue) {
    for (long id = this.mLogSeqNum.get(); id < newvalue &&
        !this.mLogSeqNum.compareAndSet(id, newvalue); id = this.mLogSeqNum.get()) {
      // This could spin on occasion but better the occasional spin than locking
      // every increment of sequence number.
      if (LOG.isDebugEnabled())
        LOG.debug("Changed sequenceid from " + mLogSeqNum + " to " + newvalue);
    }
  }

  /**
   * @return log sequence number
   */
  public long getSequenceNumber() {
    return mLogSeqNum.get();
  }

  // usage: see TestLogRolling.java
  protected OutputStream getOutputStream() {
    return this.mDfsOut;
  }

  /**
   * Roll the log writer. That is, start writing log messages to a new file.
   *
   * Because a log cannot be rolled during a cache flush, and a cache flush
   * spans two method calls, a special lock needs to be obtained so that a cache
   * flush cannot start when the log is being rolled and the log cannot be
   * rolled during a cache flush.
   *
   * <p>Note that this method cannot be synchronized because it is possible that
   * startCacheFlush runs, obtaining the cacheFlushLock, then this method could
   * start which would obtain the lock on this but block on obtaining the
   * cacheFlushLock and then completeCacheFlush could be called which would wait
   * for the lock on this and consequently never release the cacheFlushLock
   *
   * @return If lots of logs, flush the returned regions so next time through
   * we can clean logs. Returns null if nothing to flush.
   * @throws FailedLogCloseException
   * @throws IOException
   */
  public byte[][] rollWriter() throws FailedLogCloseException, IOException {
    // Return if nothing to flush.
    if (this.mWriter != null && this.mNumEntries.get() <= 0) 
      return null;
    
    byte[][] regionsToFlush = null;
    this.mCacheFlushLock.lock();
    try {
      if (mClosed) 
        return regionsToFlush;
      
      // Do all the preparation outside of the updateLock to block
      // as less as possible the incoming writes
      long currentFilenum = this.mFilenum;
      this.mFilenum = System.currentTimeMillis();
      
      Path newPath = computeFilename();
      DBLog.Writer nextWriter = createWriter(mFs, newPath, mConf); 
      
      int nextInitialReplication = 0;
      if (mFs.exists(newPath))
        nextInitialReplication = mFs.getFileStatus(newPath).getReplication();
      
      // Can we get at the dfsclient outputstream?  If an instance of
      // SFLW, it'll have done the necessary reflection to get at the
      // protected field name.
      OutputStream nextHdfsOut = null;
      if (nextWriter instanceof SequenceFileLogWriter) {
        nextHdfsOut =
          ((SequenceFileLogWriter)nextWriter).getDFSCOutputStream();
      }
      
      synchronized (mUpdateLock) {
        // Clean up current writer.
        Path oldFile = cleanupCurrentWriter(currentFilenum);
        this.mWriter = nextWriter;
        this.mInitialReplication = nextInitialReplication;
        this.mDfsOut = nextHdfsOut;

        if (LOG.isInfoEnabled()) {
          LOG.info((oldFile != null?
            "Roll " + getPath(oldFile) + ", entries=" +
            this.mNumEntries.get() +
            ", filesize=" +
            this.mFs.getFileStatus(oldFile).getLen() + ". ": "") +
            "New dblog " + getPath(newPath));
        }
        
        this.mNumEntries.set(0);
      }
      
      // Tell our listeners that a new log was created
      if (!this.mActionListeners.isEmpty()) {
        for (LogActionsListener list : this.mActionListeners) {
          list.logRolled(newPath);
        }
      }
      
      // Can we delete any of the old log files?
      if (this.mOutputfiles.size() > 0) {
        if (this.mLastSeqWritten.size() <= 0) {
          if (LOG.isDebugEnabled())
            LOG.debug("Last sequenceid written is empty. Deleting all old dblogs");
          
          // If so, then no new writes have come in since all regions were
          // flushed (and removed from the lastSeqWritten map). Means can
          // remove all but currently open log file.
          for (Map.Entry<Long, Path> e : this.mOutputfiles.entrySet()) {
            archiveLogFile(e.getValue(), e.getKey());
          }
          
          this.mOutputfiles.clear();
        } else {
          regionsToFlush = cleanOldLogs();
        }
      }
    } finally {
      this.mCacheFlushLock.unlock();
    }
    
    return regionsToFlush;
  }

  public static String getPath(Path p) {
    return p != null ? p.toUri().getPath() : null;
  }
  
  /**
   * Get a reader for the WAL.
   * @param fs
   * @param path
   * @param conf
   * @return A WAL reader.  Close when done with it.
   * @throws IOException
   */
  public static Reader getReader(final FileSystem fs,
    final Path path, Configuration conf) throws IOException {
    try {
      if (mLogReaderClass == null) {
        mLogReaderClass = conf.getClass("bigdb.regionserver.dblog.reader.impl",
                SequenceFileLogReader.class, Reader.class);
      }

      DBLog.Reader reader = mLogReaderClass.newInstance();
      reader.init(fs, path, conf);
      return reader;
    } catch (IOException e) {
      throw e;
    }
    catch (Exception e) {
      throw new IOException("Cannot get log reader", e);
    }
  }

  /**
   * Get a writer for the WAL.
   * @param path
   * @param conf
   * @return A WAL writer.  Close when done with it.
   * @throws IOException
   */
  public static Writer createWriter(final FileSystem fs,
      final Path path, Configuration conf) throws IOException {
    try {
      if (mLogWriterClass == null) {
        mLogWriterClass = conf.getClass("bigdb.regionserver.dblog.writer.impl",
                SequenceFileLogWriter.class, Writer.class);
      }
      DBLog.Writer writer = (DBLog.Writer) mLogWriterClass.newInstance();
      writer.init(fs, path, conf);
      return writer;
    } catch (Exception e) {
      IOException ie = new IOException("cannot get log writer");
      ie.initCause(e);
      throw ie;
    }
  }

  /**
   * Clean up old commit logs.
   * @return If lots of logs, flush the returned region so next time through
   * we can clean logs. Returns null if nothing to flush.
   * @throws IOException
   */
  private byte[][] cleanOldLogs() throws IOException {
    Long oldestOutstandingSeqNum = getOldestOutstandingSeqNum();
    // Get the set of all log files whose final ID is older than or
    // equal to the oldest pending region operation
    TreeSet<Long> sequenceNumbers =
      new TreeSet<Long>(this.mOutputfiles.headMap(
        (Long.valueOf(oldestOutstandingSeqNum.longValue() + 1L))).keySet());
    // Now remove old log files (if any)
    int logsToRemove = sequenceNumbers.size();
    if (logsToRemove > 0) {
      if (LOG.isDebugEnabled()) {
        // Find associated region; helps debugging.
        byte[] oldestRegion = getOldestRegion(oldestOutstandingSeqNum);
        LOG.debug("Found " + logsToRemove + " dblogs to remove " +
          " out of total " + this.mOutputfiles.size() + "; " +
          "oldest outstanding sequenceid is " + oldestOutstandingSeqNum +
          " from region " + Bytes.toString(oldestRegion));
      }
      for (Long seq : sequenceNumbers) {
        archiveLogFile(this.mOutputfiles.remove(seq), seq);
      }
    }

    // If too many log files, figure which regions we need to flush.
    byte[][] regions = null;
    int logCount = this.mOutputfiles.size() - logsToRemove;
    if (logCount > this.mMaxLogs && this.mOutputfiles != null &&
        this.mOutputfiles.size() > 0) {
      regions = findMemstoresWithEditsOlderThan(this.mOutputfiles.firstKey(),
        this.mLastSeqWritten);
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < regions.length; i++) {
        if (i > 0) sb.append(", ");
        sb.append(Bytes.toStringBinary(regions[i]));
      }
      if (LOG.isInfoEnabled()) {
        LOG.info("Too many dblogs: logs=" + logCount + ", maxlogs=" +
          this.mMaxLogs + "; forcing flush of " + regions.length + " regions(s): " +
          sb.toString());
      }
    }
    return regions;
  }

  /**
   * Return regions (memstores) that have edits that are less than the passed
   * <code>oldestWALseqid</code>.
   * @param oldestWALseqid
   * @param regionsToSeqids
   * @return All regions whose seqid is < than <code>oldestWALseqid</code> (Not
   * necessarily in order).  Null if no regions found.
   */
  static byte[][] findMemstoresWithEditsOlderThan(final long oldestWALseqid,
      final Map<byte[], Long> regionsToSeqids) {
    //  This method is static so it can be unit tested the easier.
    List<byte[]> regions = null;
    for (Map.Entry<byte[], Long> e: regionsToSeqids.entrySet()) {
      if (e.getValue().longValue() < oldestWALseqid) {
        if (regions == null) regions = new ArrayList<byte[]>();
        regions.add(e.getKey());
      }
    }
    return regions == null ? null : 
      regions.toArray(new byte[][] {DBConstants.EMPTY_BYTE_ARRAY});
  }

  /**
   * @return Logs older than this id are safe to remove.
   */
  private Long getOldestOutstandingSeqNum() {
    return Collections.min(this.mLastSeqWritten.values());
  }

  private byte[] getOldestRegion(final Long oldestOutstandingSeqNum) {
    byte[] oldestRegion = null;
    for (Map.Entry<byte[], Long> e: this.mLastSeqWritten.entrySet()) {
      if (e.getValue().longValue() == oldestOutstandingSeqNum.longValue()) {
        oldestRegion = e.getKey();
        break;
      }
    }
    return oldestRegion;
  }

  /**
   * Cleans up current writer closing and adding to outputfiles.
   * Presumes we're operating inside an updateLock scope.
   * @return Path to current writer or null if none.
   * @throws IOException
   */
  private Path cleanupCurrentWriter(final long currentfilenum)
	  throws IOException {
    Path oldFile = null;
    if (this.mWriter != null) {
      // Close the current writer, get a new one.
      try {
        this.mWriter.close();
      } catch (IOException e) {
        // Failed close of log file.  Means we're losing edits.  For now,
        // shut ourselves down to minimize loss.  Alternative is to try and
        // keep going.  See BIGDB-930.
        FailedLogCloseException flce =
          new FailedLogCloseException("#" + currentfilenum);
        flce.initCause(e);
        throw e;
      }
      if (currentfilenum >= 0) {
        oldFile = computeFilename(currentfilenum);
        this.mOutputfiles.put(Long.valueOf(this.mLogSeqNum.get() - 1), oldFile);
      }
    }
    return oldFile;
  }

  private void archiveLogFile(final Path p, final Long seqno) throws IOException {
    Path newPath = getDBLogArchivePath(this.mOldLogDir, p);
    if (LOG.isInfoEnabled()) {
      LOG.info("moving old dblog file " + getPath(p) +
        " whose highest sequenceid is " + seqno + " to " +
        getPath(newPath));
    }
    this.mFs.rename(p, newPath);
  }

  /**
   * This is a convenience method that computes a new filename with a given
   * using the current DBLog file-number
   * @return Path
   */
  protected Path computeFilename() {
    return computeFilename(this.mFilenum);
  }

  /**
   * This is a convenience method that computes a new filename with a given
   * file-number.
   * @param file-number to use
   * @return Path
   */
  protected Path computeFilename(long filenum) {
    if (filenum < 0) {
      throw new RuntimeException("dblog file number can't be < 0");
    }
    return new Path(mDir, mPrefix + "." + filenum);
  }

  /**
   * Shut down the log and delete the log directory
   *
   * @throws IOException
   */
  public synchronized void closeAndDelete() throws IOException {
    close();
    
    FileStatus[] files = mFs.listStatus(this.mDir);
    for (FileStatus file : files) {
      mFs.rename(file.getPath(),
          getDBLogArchivePath(this.mOldLogDir, file.getPath()));
    }
    
    if (LOG.isDebugEnabled()) {
      LOG.debug("Moved " + (files != null ? files.length : 0) + " log files to " +
        getPath(this.mOldLogDir));
    }
    
    mFs.delete(mDir, true);
  }

  /**
   * Shut down the log.
   *
   * @throws IOException
   */
  public synchronized void close() throws IOException {
	if (LOG.isDebugEnabled())
	  LOG.debug("close");
	
    try {
      if (mLogSyncerThread.isAlive() || mLogSyncerThread.isRunning()) {
        mLogSyncerThread.interrupt();
        // Make sure we synced everything
        mLogSyncerThread.join(this.mOptionalFlushInterval*2);
      }
    } catch (InterruptedException e) {
      if (LOG.isErrorEnabled())
        LOG.error("Exception while waiting for syncer thread to die", e);
    }

    mCacheFlushLock.lock();
    try {
      synchronized (mUpdateLock) {
        this.mClosed = true;
        if (LOG.isDebugEnabled()) {
          LOG.debug("closing dblog writer in " + this.mDir.toString());
        }
        this.mWriter.close();
      }
    } finally {
      mCacheFlushLock.unlock();
    }
  }

  /** 
   * Append an entry to the log.
   *
   * @param regionInfo
   * @param logEdit
   * @param now Time of this edit write.
   * @throws IOException
   */
  public void append(DBRegionInfo regionInfo, WALEdit logEdit,
      final long now, final boolean isMetaRegion) throws IOException {
    byte[] regionName = regionInfo.getRegionName();
    byte[] tableName = regionInfo.getTableDesc().getName();
    this.append(regionInfo, makeKey(regionName, tableName, -1, now), logEdit);
  }

  /**
   * @param now
   * @param regionName
   * @param tableName
   * @return New log key.
   */
  protected DBLogKey makeKey(byte[] regionName, byte[] tableName, long seqnum, long now) {
    return new DBLogKey(regionName, tableName, seqnum, now);
  }

  /** 
   * Append an entry to the log.
   *
   * @param regionInfo
   * @param logEdit
   * @param logKey
   * @throws IOException
   */
  public void append(DBRegionInfo regionInfo, DBLogKey logKey, 
	  WALEdit logEdit) throws IOException {
    if (this.mClosed) 
      throw new IOException("Cannot append; log is closed");
    
    byte[] regionName = regionInfo.getRegionName();
    synchronized (mUpdateLock) {
      long seqNum = obtainSeqNum();
      logKey.setLogSeqNum(seqNum);
      // The 'lastSeqWritten' map holds the sequence number of the oldest
      // write for each region (i.e. the first edit added to the particular
      // memstore). When the cache is flushed, the entry for the
      // region being flushed is removed if the sequence number of the flush
      // is greater than or equal to the value in lastSeqWritten.
      this.mLastSeqWritten.putIfAbsent(regionName, Long.valueOf(seqNum));
      doWrite(regionInfo, logKey, logEdit);
      this.mUnflushedEntries.incrementAndGet();
      this.mNumEntries.incrementAndGet();
    }

    // sync txn to file system
    this.sync(regionInfo.isMetaRegion());
  }

  /**
   * Append a set of edits to the log. Log edits are keyed by regionName,
   * rowname, and log-sequence-id.
   *
   * Later, if we sort by these keys, we obtain all the relevant edits for a
   * given key-range of the DBRegion (TODO). Any edits that do not have a
   * matching COMPLETE_CACHEFLUSH message can be discarded.
   *
   * <p>
   * Logs cannot be restarted once closed, or once the DBLog process dies. Each
   * time the DBLog starts, it must create a new log. This means that other
   * systems should process the log appropriately upon each startup (and prior
   * to initializing DBLog).
   *
   * synchronized prevents appends during the completion of a cache flush or for
   * the duration of a log roll.
   *
   * @param info
   * @param tableName
   * @param edits
   * @param now
   * @throws IOException
   */
  public void append(DBRegionInfo info, byte[] tableName, WALEdit edits,
      final long now) throws IOException {
    if (edits.isEmpty()) return;
    
    byte[] regionName = info.getRegionName();
    if (this.mClosed) 
      throw new IOException("Cannot append; log is closed");
    
    synchronized (this.mUpdateLock) {
      long seqNum = obtainSeqNum();
      // The 'lastSeqWritten' map holds the sequence number of the oldest
      // write for each region (i.e. the first edit added to the particular
      // memstore). . When the cache is flushed, the entry for the
      // region being flushed is removed if the sequence number of the flush
      // is greater than or equal to the value in lastSeqWritten.
      this.mLastSeqWritten.putIfAbsent(regionName, seqNum);
      DBLogKey logKey = makeKey(regionName, tableName, seqNum, now);
      doWrite(info, logKey, edits);
      this.mNumEntries.incrementAndGet();

      // Only count 1 row as an unflushed entry.
      this.mUnflushedEntries.incrementAndGet();
    }
    // sync txn to file system
    this.sync(info.isMetaRegion());
  }

  /**
   * This thread is responsible to call syncFs and buffer up the writers while
   * it happens.
   */
  class LogSyncer extends Thread {

    // Using fairness to make sure locks are given in order
    private final ReentrantLock mLock = new ReentrantLock(true);

    // Condition used to wait until we have something to sync
    private final Condition mQueueEmpty = mLock.newCondition();

    // Condition used to signal that the sync is done
    private final Condition mSyncDone = mLock.newCondition();

    private final long mOptionalFlushInterval;

    private volatile boolean mSyncerRunning = false;
    private volatile boolean mSyncerShuttingDown = false;

    LogSyncer(long optionalFlushInterval) {
      this.mOptionalFlushInterval = optionalFlushInterval;
    }

    public boolean isRunning() { 
    	return mSyncerRunning;
    }
    
    @Override
    public void run() {
      try {
        mLock.lock();
        mSyncerRunning = true;
        
        // awaiting with a timeout doesn't always
        // throw exceptions on interrupt
        while (!this.isInterrupted()) {
        	
          // Wait until something has to be dbflushed or do it if we waited
          // enough time (useful if something appends but does not dbflush).
          // 0 or less means that it timed out and maybe waited a bit more.
          if (!(mQueueEmpty.awaitNanos(this.mOptionalFlushInterval*1000000) <= 0)) {
            mForceSync = true;
          }
          
          syncQueue();
        }
      } catch (InterruptedException e) {
    	if (LOG.isDebugEnabled())
          LOG.debug(getName() + "interrupted while waiting for sync requests");
    	
      } finally {
    	mSyncerRunning = false;
        mSyncerShuttingDown = true;
        mSyncDone.signalAll();
        mLock.unlock();
        
        if (LOG.isInfoEnabled())
          LOG.info(getName() + " exiting");
      }
    }

    private void syncQueue() throws InterruptedException { 
      try {

        // We got the signal, let's dbflush. We currently own the lock so new
        // writes are waiting to acquire it in addToSyncQueue while the ones
        // we dbflush are waiting on await()
        dbflush();

        // Release all the clients waiting on the dbflush. Notice that we still
        // own the lock until we get back to await at which point all the
        // other threads waiting will first acquire and release locks
        mSyncDone.signalAll();
        
      } catch (IOException e) {
      	if (LOG.isErrorEnabled())
            LOG.error("Error while syncing, requesting close of dblog ", e);
      	
        requestLogRoll();
      }
    }
    
    /**
     * This method first signals the thread that there's a sync needed
     * and then waits for it to happen before returning.
     */
    public void addToSyncQueue(boolean force) {
      // Don't bother if somehow our append was already dbflushed
      if (mUnflushedEntries.get() == 0) 
        return;
      
      mLock.lock();
      try {
        if (mSyncerShuttingDown) {
          if (LOG.isWarnEnabled())
            LOG.warn(getName() + " was shut down while waiting for sync");
          return;
        }
        if (force) {
          mForceSync = true;
        }
        
        if (isRunning()) {
	      // Wake the thread
	      mQueueEmpty.signal();
	
	      // Wait for it to dbflush
	      mSyncDone.await();
	      
        } else { 
          syncQueue();
        }
      } catch (InterruptedException e) {
    	if (LOG.isDebugEnabled())
          LOG.debug(getName() + " was interrupted while waiting for sync", e);
      } finally {
        mLock.unlock();
      }
    }
  }

  public void sync(){
    sync(false);
  }

  /**
   * This method calls the LogSyncer in order to group commit the sync
   * with other threads.
   * @param force For catalog regions, force the sync to happen
   */
  public void sync(boolean force) {
	if (LOG.isDebugEnabled()) LOG.debug("sync: force=" + force);
    mLogSyncerThread.addToSyncQueue(force);
  }

  public void dbflush() throws IOException {
	if (LOG.isDebugEnabled()) LOG.debug("dbflush");
    synchronized (this.mUpdateLock) {
      if (this.mClosed) return;
      
      boolean logRollRequested = false;
      if (this.mForceSync ||
          this.mUnflushedEntries.get() >= this.mFlushlogentries) {
        try {
          long now = System.currentTimeMillis();
          this.mWriter.sync();
          mSyncTime += System.currentTimeMillis() - now;
          mSyncOps++;
          this.mForceSync = false;
          this.mUnflushedEntries.set(0);

          // if the number of replicas in HDFS has fallen below the initial
          // value, then roll logs.
          try {
            int numCurrentReplicas = getLogReplication();
            if (numCurrentReplicas != 0 &&
                numCurrentReplicas < this.mInitialReplication) {
              if (LOG.isWarnEnabled()) {
                LOG.warn("HDFS pipeline error detected. " +
                  "Found " + numCurrentReplicas + " replicas but expecting " +
                  this.mInitialReplication + " replicas. " +
                  " Requesting close of dblog.");
              }
              requestLogRoll();
              logRollRequested = true;
            }
          } catch (Exception e) {
        	if (LOG.isWarnEnabled()) {
              LOG.warn("Unable to invoke DFSOutputStream.getNumCurrentReplicas" + e +
                       " still proceeding ahead...");
        	}
          }
        } catch (IOException e) {
          if (LOG.isFatalEnabled())
            LOG.fatal("Could not append. Requesting close of dblog", e);
          requestLogRoll();
          throw e;
        }
      }

      if (!logRollRequested && (this.mWriter.getLength() > this.mLogrollsize)) {
        requestLogRoll();
      }
    }
  }

  /**
   * This method gets the datanode replication count for the current DBLog.
   *
   * If the pipeline isn't started yet or is empty, you will get the default
   * replication factor.  Therefore, if this function returns 0, it means you
   * are not properly running with the HDFS-826 patch.
   * @throws InvocationTargetException
   * @throws IllegalAccessException
   * @throws IllegalArgumentException
   *
   * @throws Exception
   */
  protected int getLogReplication() throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
    if (this.mGetNumCurrentReplicas != null && this.mDfsOut != null) {
      Object repl = this.mGetNumCurrentReplicas.invoke(this.mDfsOut, NO_ARGS);
      if (repl instanceof Integer) {
        return ((Integer)repl).intValue();
      }
    }
    return 0;
  }

  protected boolean canGetCurReplicas() {
    return this.mGetNumCurrentReplicas != null;
  }

  public void dbsync() throws IOException {
    // Not yet implemented up in dfs so just call dbflush.
    dbflush();
  }

  private void requestLogRoll() {
    if (this.mListener != null) {
      this.mListener.logRollRequested();
    }
  }

  protected void doWrite(DBRegionInfo info, DBLogKey logKey, 
	  WALEdit logEdit) throws IOException {
    if (!this.mEnabled) {
      return;
    }
    if (!this.mLogEntryVisitors.isEmpty()) {
      for (LogEntryVisitor visitor : this.mLogEntryVisitors) {
        visitor.visitLogEntryBeforeWrite(info, logKey, logEdit);
      }
    }
    try {
      long now = System.currentTimeMillis();
      this.mWriter.append(new DBLog.Entry(logKey, logEdit));
      long took = System.currentTimeMillis() - now;
      mWriteTime += took;
      mWriteOps++;
      if (took > 1000) {
    	if (LOG.isWarnEnabled()) {
          LOG.warn(Thread.currentThread().getName() + " took " + took +
            "ms appending an edit to dblog; editcount=" + this.mNumEntries.get());
    	}
      }
    } catch (IOException e) {
      if (LOG.isFatalEnabled()) 
        LOG.fatal("Could not append. Requesting close of dblog", e);
      requestLogRoll();
      throw e;
    }
  }

  /** @return How many items have been added to the log */
  protected int getNumEntries() {
    return mNumEntries.get();
  }

  /**
   * Obtain a log sequence number.
   */
  private long obtainSeqNum() {
    return this.mLogSeqNum.incrementAndGet();
  }

  /** @return the number of log files in use */
  protected int getNumLogFiles() {
    return mOutputfiles.size();
  }

  /**
   * By acquiring a log sequence ID, we can allow log messages to continue while
   * we flush the cache.
   *
   * Acquire a lock so that we do not roll the log between the start and
   * completion of a cache-flush. Otherwise the log-seq-id for the flush will
   * not appear in the correct logfile.
   *
   * @return sequence ID to pass {@link #completeCacheFlush(byte[], byte[], long, boolean)}
   * (byte[], byte[], long)}
   * @see #completeCacheFlush(byte[], byte[], long, boolean)
   * @see #abortCacheFlush()
   */
  public long startCacheFlush() {
    this.mCacheFlushLock.lock();
    return obtainSeqNum();
  }

  /**
   * Complete the cache flush
   *
   * Protected by cacheFlushLock
   *
   * @param regionName
   * @param tableName
   * @param logSeqId
   * @throws IOException
   */
  public void completeCacheFlush(final byte[] regionName, final byte[] tableName,
      final long logSeqId, final boolean isMetaRegion) throws IOException {
    try {
      if (this.mClosed) 
        return;
      
      synchronized (mUpdateLock) {
        long now = System.currentTimeMillis();
        WALEdit edit = completeCacheFlushLogEdit();
        DBLogKey key = makeKey(regionName, tableName, logSeqId,
            System.currentTimeMillis());
        this.mWriter.append(new Entry(key, edit));
        mWriteTime += System.currentTimeMillis() - now;
        mWriteOps++;
        this.mNumEntries.incrementAndGet();
        Long seq = this.mLastSeqWritten.get(regionName);
        if (seq != null && logSeqId >= seq.longValue()) {
          this.mLastSeqWritten.remove(regionName);
        }
      }
      // sync txn to file system
      this.sync(isMetaRegion);
    } finally {
      this.mCacheFlushLock.unlock();
    }
  }

  private WALEdit completeCacheFlushLogEdit() {
    KeyValue kv = new KeyValue(METAROW, METAFAMILY, null,
      System.currentTimeMillis(), COMPLETE_CACHE_FLUSH);
    WALEdit e = new WALEdit();
    e.add(kv);
    return e;
  }

  /**
   * Abort a cache flush.
   * Call if the flush fails. Note that the only recovery for an aborted flush
   * currently is a restart of the regionserver so the snapshot content dropped
   * by the failure gets restored to the memstore.
   */
  public void abortCacheFlush() {
    this.mCacheFlushLock.unlock();
  }

  /**
   * @param family
   * @return true if the column is a meta column
   */
  public static boolean isMetaFamily(byte[] family) {
    return Bytes.equals(METAFAMILY, family);
  }

  /**
   * Split up a bunch of regionserver commit log files that are no longer
   * being written to, into new files, one per region for region to replay on
   * startup. Delete the old log files when finished.
   *
   * @param rootDir qualified root directory of the HBase instance
   * @param srcDir Directory of log files to split: e.g.
   *                <code>${ROOTDIR}/log_HOST_PORT</code>
   * @param oldLogDir directory where processed (split) logs will be archived to
   * @param fs FileSystem
   * @param conf Configuration
   * @throws IOException will throw if corrupted dblogs aren't tolerated
   * @return the list of splits
   */
  public static List<Path> splitLog(final Path rootDir, final Path srcDir,
    Path oldLogDir, final FileSystem fs, final Configuration conf) throws IOException {
    long millis = System.currentTimeMillis();
    List<Path> splits = null;
    if (!fs.exists(srcDir)) {
      // Nothing to do
      return splits;
    }
    FileStatus[] logfiles = fs.listStatus(srcDir);
    if (logfiles == null || logfiles.length == 0) {
      // Nothing to do
      return splits;
    }
    if (LOG.isInfoEnabled()) {
      LOG.info("Splitting " + logfiles.length + " dblog(s) in " +
        srcDir.toString());
    }
    splits = splitLog(rootDir, srcDir, oldLogDir, logfiles, fs, conf);
    try {
      FileStatus[] files = fs.listStatus(srcDir);
      for (FileStatus file : files) {
        Path newPath = getDBLogArchivePath(oldLogDir, file.getPath());
        if (LOG.isInfoEnabled()) {
          LOG.info("Moving " +  getPath(file.getPath()) + " to " +
            getPath(newPath));
        }
        fs.rename(file.getPath(), newPath);
      }
      if (LOG.isDebugEnabled()) {
        LOG.debug("Moved " + files.length + " log files to " +
          getPath(oldLogDir));
      }
      fs.delete(srcDir, true);
      clearOldLogs(fs, oldLogDir, conf);
    } catch (IOException e) {
      //e = RemoteExceptionHandler.checkIOException(e);
      IOException io = new IOException("Cannot delete: " + srcDir);
      io.initCause(e);
      throw io;
    }
    long endMillis = System.currentTimeMillis();
    if (LOG.isInfoEnabled()) {
      LOG.info("dblog file splitting completed in " + (endMillis - millis) +
        " millis for " + srcDir.toString());
    }
    return splits;
  }

  private static final long OLDLOG_EXPIRED_MS = 60l * 24 * 60 * 60 * 1000;
  private static final int OLDLOG_MAX_COUNT = 30;
  
  private static void clearOldLogs(final FileSystem fs, final Path oldLogDir, 
		  final Configuration conf) throws IOException {
	  long expiredTime = conf.getLong("bgdb.oldlog.expired_ms", OLDLOG_EXPIRED_MS);
	  int maxCount = conf.getInt("bigdb.oldlog.max_count", OLDLOG_MAX_COUNT);
	  if (expiredTime <= 0 || maxCount <= 0) return;
	  
	  FileStatus[] files = fs.listStatus(oldLogDir);
	  if (files == null || files.length < maxCount) return;
	  
	  long current = System.currentTimeMillis();
      for (FileStatus file : files) {
    	  if (file != null && file.isDir() == false && 
    		 (current - file.getModificationTime()) > expiredTime) {
    		  fs.delete(file.getPath(), false);
    		  
    		  if (LOG.isInfoEnabled()) 
    			LOG.info("Deleted expired dblog: " + file.getPath());
    	  }
      }
  }
  
  // Private immutable datastructure to hold Writer and its Path.
  private final static class WriterAndPath {
    private final Path mPath;
    private final Writer mWriter;
    WriterAndPath(final Path p, final Writer w) {
      this.mPath = p;
      this.mWriter = w;
    }
  }

  @SuppressWarnings("unchecked")
  public static Class<? extends DBLogKey> getKeyClass(Configuration conf) {
     return (Class<? extends DBLogKey>)
       conf.getClass("bigdb.regionserver.dblog.keyclass", DBLogKey.class);
  }

  public static DBLogKey newKey(Configuration conf) throws IOException {
    Class<? extends DBLogKey> keyClass = getKeyClass(conf);
    try {
      return keyClass.newInstance();
    } catch (InstantiationException e) {
      throw new IOException("cannot create dblog key");
    } catch (IllegalAccessException e) {
      throw new IOException("cannot create dblog key");
    }
  }

  /**
   * Sorts the DBLog edits in the given list of logfiles (that are a mix of edits on multiple regions)
   * by region and then splits them per region directories, in batches of (bigdb.dblog.split.batch.size)
   *
   * A batch consists of a set of log files that will be sorted in a single map of edits indexed by region
   * the resulting map will be concurrently written by multiple threads to their corresponding regions
   *
   * Each batch consists of more more log files that are
   *  - recovered (files is opened for append then closed to ensure no process is writing into it)
   *  - parsed (each edit in the log is appended to a list of edits indexed by region
   *    see {@link #parseDBLog} for more details)
   *  - marked as either processed or corrupt depending on parsing outcome
   *  - the resulting edits indexed by region are concurrently written to their corresponding region
   *    region directories
   *  - original files are then archived to a different directory
   *
   * @param rootDir  bigdb directory
   * @param srcDir   logs directory
   * @param oldLogDir directory where processed logs are archived to
   * @param logfiles the list of log files to split
   * @param fs
   * @param conf
   * @return
   * @throws IOException
   */
  private static List<Path> splitLog(final Path rootDir, final Path srcDir,
      Path oldLogDir, final FileStatus[] logfiles, final FileSystem fs,
      final Configuration conf) throws IOException {
    List<Path> processedLogs = new ArrayList<Path>();
    List<Path> corruptedLogs = new ArrayList<Path>();
    final Map<byte[], WriterAndPath> logWriters =
      Collections.synchronizedMap(
        new TreeMap<byte[], WriterAndPath>(Bytes.BYTES_COMPARATOR));
    List<Path> splits = null;

    // Number of logs in a read batch
    // More means faster but bigger mem consumption
    //TODO make a note on the conf rename and update bigdb-site.xml if needed
    int logFilesPerStep = conf.getInt("bigdb.dblog.split.batch.size", 3);
     boolean skipErrors = conf.getBoolean("bigdb.dblog.split.skip.errors", false);

    try {
      int i = -1;
      while (i < logfiles.length) {
        final Map<byte[], LinkedList<Entry>> editsByRegion =
          new TreeMap<byte[], LinkedList<Entry>>(Bytes.BYTES_COMPARATOR);
        for (int j = 0; j < logFilesPerStep; j++) {
          i++;
          if (i == logfiles.length) {
            break;
          }
          FileStatus log = logfiles[i];
          Path logPath = log.getPath();
          long logLength = log.getLen();
          if (LOG.isDebugEnabled()) {
            LOG.debug("Splitting dblog " + (i + 1) + " of " + logfiles.length +
              ": " + logPath + ", length=" + logLength );
          }
          try {
            FSUtils.recoverFileLease(fs, logPath, conf);
            parseDBLog(log, editsByRegion, fs, conf);
            processedLogs.add(logPath);
          } catch (EOFException eof) {
            // truncated files are expected if a RS crashes (see BIGDB-2643)
        	if (LOG.isInfoEnabled())
              LOG.info("EOF from dblog " + logPath + ".  continuing");
            processedLogs.add(logPath);
          } catch (IOException e) {
             if (skipErrors) {
               if (LOG.isWarnEnabled()) {
                 LOG.warn("Got while parsing dblog " + logPath +
                   ". Marking as corrupted", e);
               }
               corruptedLogs.add(logPath);
             } else {
               throw e;
             }
          }
        }
        writeEditsBatchToRegions(editsByRegion, logWriters, rootDir, fs, conf);
      }
      if (fs.listStatus(srcDir).length > processedLogs.size() + corruptedLogs.size()) {
        throw new IOException("Discovered orphan dblog after split. Maybe " +
          "DBRegionServer was not dead when we started");
      }
      archiveLogs(corruptedLogs, processedLogs, oldLogDir, fs, conf);
    } finally {
      splits = new ArrayList<Path>(logWriters.size());
      for (WriterAndPath wap : logWriters.values()) {
        wap.mWriter.close();
        splits.add(wap.mPath);
        if (LOG.isDebugEnabled())
          LOG.debug("Closed " + wap.mPath);
      }
    }
    return splits;
  }


  /**
   * Utility class that lets us keep track of the edit with it's key
   * Only used when splitting logs
   */
  public static class Entry implements Writable {
    private WALEdit mEdit;
    private DBLogKey mKey;

    public Entry() {
      mEdit = new WALEdit();
      mKey = new DBLogKey();
    }

    /**
     * Constructor for both params
     * @param edit log's edit
     * @param key log's key
     */
    public Entry(DBLogKey key, WALEdit edit) {
      super();
      this.mKey = key;
      this.mEdit = edit;
    }
    
    /**
     * Gets the edit
     * @return edit
     */
    public WALEdit getEdit() {
      return mEdit;
    }
    
    /**
     * Gets the key
     * @return key
     */
    public DBLogKey getKey() {
      return mKey;
    }

    @Override
    public String toString() {
      return this.mKey + "=" + this.mEdit;
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
      this.mKey.write(dataOutput);
      this.mEdit.write(dataOutput);
    }

    @Override
    public void readFields(DataInput dataInput) throws IOException {
      this.mKey.readFields(dataInput);
      this.mEdit.readFields(dataInput);
    }
  }

  /**
   * Construct the DBLog directory name
   *
   * @param info DBServerInfo for server
   * @return the DBLog directory name
   */
  //public static String getDBLogDirectoryName(DBServerInfo info) {
  //  return getDBLogDirectoryName(info.getServerName());
  //}

  /**
   * Construct the DBLog directory name
   *
   * @param serverAddress
   * @param startCode
   * @return the DBLog directory name
   */
  public static String getDBLogDirectoryName(String serverAddress,
      long startCode) {
    if (serverAddress == null || serverAddress.length() == 0) {
      return null;
    }
    //return getDBLogDirectoryName(
    //    DBServerInfo.getServerName(serverAddress, startCode));
    return serverAddress + "_" + startCode;
  }

  /**
   * Construct the DBLog directory name
   *
   * @param serverName
   * @return the DBLog directory name
   */
  public static String getDBLogDirectoryName(String serverName) {
    StringBuilder dirName = new StringBuilder(DBConstants.DBREGION_LOGDIR_NAME);
    dirName.append("/");
    dirName.append(serverName);
    return dirName.toString();
  }

  public static boolean validateDBLogFilename(String filename) {
    return sPattern.matcher(filename).matches();
  }

  private static Path getDBLogArchivePath(Path oldLogDir, Path p) {
    return new Path(oldLogDir, p.getName());
  }

  /**
   * Takes splitLogsMap and concurrently writes them to region directories using a thread pool
   *
   * @param splitLogsMap map that contains the log splitting result indexed by region
   * @param logWriters map that contains a writer per region
   * @param rootDir bigdb root dir
   * @param fs
   * @param conf
   * @throws IOException
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  private static void writeEditsBatchToRegions(
      final Map<byte[], LinkedList<Entry>> splitLogsMap,
      final Map<byte[], WriterAndPath> logWriters,
      final Path rootDir, final FileSystem fs, final Configuration conf)
      throws IOException {
    // Number of threads to use when log splitting to rewrite the logs.
    // More means faster but bigger mem consumption.
    int logWriterThreads =
      conf.getInt("bigdb.regionserver.dblog.splitlog.writer.threads", 3);
    boolean skipErrors = conf.getBoolean("bigdb.skip.errors", false);
    HashMap<byte[], Future> writeFutureResult = new HashMap<byte[], Future>();
    NamingThreadFactory f  = new NamingThreadFactory(
            "SplitWriter-%1$d", Executors.defaultThreadFactory());
    ThreadPoolExecutor threadPool = (ThreadPoolExecutor)Executors.newFixedThreadPool(logWriterThreads, f);
    for (final byte[] region : splitLogsMap.keySet()) {
      Callable splitter = createNewSplitter(rootDir, logWriters, splitLogsMap, region, fs, conf);
      writeFutureResult.put(region, threadPool.submit(splitter));
    }

    threadPool.shutdown();
    // Wait for all threads to terminate
    try {
      for (int j = 0; !threadPool.awaitTermination(5, TimeUnit.SECONDS); j++) {
        String message = "Waiting for dblog writers to terminate, elapsed " + j * 5 + " seconds";
        if (j < 30) {
          if (LOG.isDebugEnabled()) LOG.debug(message);
        } else {
          if (LOG.isInfoEnabled()) LOG.info(message);
        }
      }
    } catch(InterruptedException ex) {
      if (LOG.isWarnEnabled())
        LOG.warn("Hlog writers were interrupted, possible data loss!");
      if (!skipErrors) {
        throw new IOException("Could not finish writing log entries",  ex);
        //TODO  maybe we should fail here regardless if skipErrors is active or not
      }
    }

    for (Map.Entry<byte[], Future> entry : writeFutureResult.entrySet()) {
      try {
        entry.getValue().get();
      } catch (ExecutionException e) {
        throw (new IOException(e.getCause()));
      } catch (InterruptedException e1) {
    	if (LOG.isWarnEnabled()) {
          LOG.warn("Writer for region " +  Bytes.toString(entry.getKey()) +
                " was interrupted, however the write process should have " +
                "finished. Throwing up ", e1);
    	}
        throw (new IOException(e1.getCause()));
      }
    }
  }

  /**
   * Parse a single dblog and put the edits in @splitLogsMap
   *
   * @param logfile to split
   * @param splitLogsMap output parameter: a map with region names as keys and a
   * list of edits as values
   * @param fs the filesystem
   * @param conf the configuration
   * @throws IOException if dblog is corrupted, or can't be open
   */
  private static void parseDBLog(final FileStatus logfile,
      final Map<byte[], LinkedList<Entry>> splitLogsMap, final FileSystem fs,
      final Configuration conf) throws IOException {
    // Check for possibly empty file. With appends, currently Hadoop reports a
    // zero length even if the file has been sync'd. Revisit if HDFS-376 or
    // HDFS-878 is committed.
    long length = logfile.getLen();
    if (length <= 0) {
      if (LOG.isWarnEnabled())
        LOG.warn("File " + logfile.getPath() + " might be still open, length is 0");
    }
    Path path = logfile.getPath();
    Reader in;
    int editsCount = 0;
    try {
      in = DBLog.getReader(fs, path, conf);
    } catch (EOFException e) {
      if (length <= 0) {
        //TODO should we ignore an empty, not-last log file if skip.errors is false?
        //Either way, the caller should decide what to do. E.g. ignore if this is the last
        //log in sequence.
        //TODO is this scenario still possible if the log has been recovered (i.e. closed)
    	if (LOG.isWarnEnabled())
          LOG.warn("Could not open " + path + " for reading. File is empty" + e);
        return;
      } else {
        throw e;
      }
    }
    try {
      Entry entry;
      while ((entry = in.next()) != null) {
        byte[] region = entry.getKey().getRegionName();
        LinkedList<Entry> queue = splitLogsMap.get(region);
        if (queue == null) {
          queue = new LinkedList<Entry>();
          splitLogsMap.put(region, queue);
        }
        queue.addLast(entry);
        editsCount++;
      }
    } finally {
      if (LOG.isDebugEnabled())
        LOG.debug("Pushed=" + editsCount + " entries from " + path);
      try {
        if (in != null) {
          in.close();
        }
      } catch (IOException e) {
    	if (LOG.isWarnEnabled())
          LOG.warn("Close log reader in finally threw exception -- continuing", e);
      }
    }
  }

  private static Callable<Void> createNewSplitter(final Path rootDir,
      final Map<byte[], WriterAndPath> logWriters,
      final Map<byte[], LinkedList<Entry>> logEntries,
      final byte[] region, final FileSystem fs, final Configuration conf) {
    return new Callable<Void>() {
      public String getName() {
        return "Split writer thread for region " + Bytes.toStringBinary(region);
      }

      @Override
      public Void call() throws IOException {
        LinkedList<Entry> entries = logEntries.get(region);
        if (LOG.isDebugEnabled())
          LOG.debug(this.getName()+" got " + entries.size() + " to process");
        long threadTime = System.currentTimeMillis();
        try {
          int editsCount = 0;
          WriterAndPath wap = logWriters.get(region);
          for (Entry logEntry: entries) {
            if (wap == null) {
              Path regionedits = getRegionSplitEditsPath(fs, logEntry, rootDir);
              if (fs.exists(regionedits)) {
            	if (LOG.isWarnEnabled()) {
                  LOG.warn("Found existing old edits file. It could be the " +
                    "result of a previous failed split attempt. Deleting " +
                    regionedits + ", length=" + fs.getFileStatus(regionedits).getLen());
            	}
                if (!fs.delete(regionedits, false)) {
                  if (LOG.isWarnEnabled())
                    LOG.warn("Failed delete of old " + regionedits);
                }
              }
              Writer w = createWriter(fs, regionedits, conf);
              wap = new WriterAndPath(regionedits, w);
              logWriters.put(region, wap);
              if (LOG.isDebugEnabled()) {
                LOG.debug("Creating writer path=" + regionedits +
                  " region=" + Bytes.toStringBinary(region));
              }
            }
            wap.mWriter.append(logEntry);
            editsCount++;
          }
          if (LOG.isDebugEnabled()) {
            LOG.debug(this.getName() + " Applied " + editsCount +
              " total edits to " + Bytes.toStringBinary(region) +
              " in " + (System.currentTimeMillis() - threadTime) + "ms");
          }
        } catch (IOException e) {
          //e = RemoteExceptionHandler.checkIOException(e);
          if (LOG.isFatalEnabled())
            LOG.fatal(this.getName() + " Got while writing log entry to log", e);
          throw e;
        }
        return null;
      }
    };
  }

  /**
   * Moves processed logs to a oldLogDir after successful processing
   * Moves corrupted logs (any log that couldn't be successfully parsed
   * to corruptDir (.corrupt) for later investigation
   *
   * @param corruptedLogs
   * @param processedLogs
   * @param oldLogDir
   * @param fs
   * @param conf
   * @throws IOException
   */
  private static void archiveLogs(final List<Path> corruptedLogs,
      final List<Path> processedLogs, final Path oldLogDir,
      final FileSystem fs, final Configuration conf)
      throws IOException{
    final Path corruptDir = new Path(conf.get(DBConstants.BIGDB_DIR),
      conf.get("bigdb.regionserver.dblog.splitlog.corrupt.dir", ".corrupt"));

    fs.mkdirs(corruptDir);
    fs.mkdirs(oldLogDir);

    for (Path corrupted: corruptedLogs) {
      Path p = new Path(corruptDir, corrupted.getName());
      if (LOG.isInfoEnabled())
        LOG.info("Moving corrupted log " + corrupted + " to " + p);
      fs.rename(corrupted, p);
    }

    for (Path p: processedLogs) {
      Path newPath = getDBLogArchivePath(oldLogDir, p);
      fs.rename(p, newPath);
      if (LOG.isInfoEnabled())
        LOG.info("Archived processed log " + p + " to " + newPath);
    }
  }

  /**
   * Path to a file under RECOVERED_EDITS_DIR directory of the region found in
   * <code>logEntry</code> named for the sequenceid in the passed
   * <code>logEntry</code>: e.g. /bigdb/some_table/2323432434/recovered.edits/2332.
   * This method also ensures existence of RECOVERED_EDITS_DIR under the region
   * creating it if necessary.
   * @param fs
   * @param logEntry
   * @param rootDir HBase root dir.
   * @return Path to file into which to dump split log edits.
   * @throws IOException
   */
  private static Path getRegionSplitEditsPath(final FileSystem fs,
      final Entry logEntry, final Path rootDir) throws IOException {
    Path tableDir = DBTableDescriptor.getTableDir(rootDir,
      logEntry.getKey().getTablename());
    Path regiondir = DBRegion.getRegionDir(tableDir,
      DBRegionInfo.encodeRegionName(logEntry.getKey().getRegionName()));
    Path dir = getRegionDirRecoveredEditsDir(regiondir);
    if (!fs.exists(dir)) {
      if (!fs.mkdirs(dir)) { 
    	  if (LOG.isWarnEnabled()) 
    		  LOG.warn("mkdir failed on " + dir);
      }
    }
    return new Path(dir,
      formatRecoveredEditsFileName(logEntry.getKey().getLogSeqNum()));
   }

  static String formatRecoveredEditsFileName(final long seqid) {
    return String.format("%019d", seqid);
  }

  /**
   * Returns sorted set of edit files made by wal-log splitter.
   * @param fs
   * @param regiondir
   * @return Files in passed <code>regiondir</code> as a sorted set.
   * @throws IOException
   */
  public static NavigableSet<Path> getSplitEditFilesSorted(final FileSystem fs,
      final Path regiondir) throws IOException {
    Path editsdir = getRegionDirRecoveredEditsDir(regiondir);
    FileStatus [] files = fs.listStatus(editsdir, new PathFilter () {
      @Override
      public boolean accept(Path p) {
        boolean result = false;
        try {
          // Return files and only files that match the editfile names pattern.
          // There can be other files in this directory other than edit files.
          // In particular, on error, we'll move aside the bad edit file giving
          // it a timestamp suffix.  See moveAsideBadEditsFile.
          Matcher m = EDITFILES_NAME_PATTERN.matcher(p.getName());
          result = fs.isFile(p) && m.matches();
        } catch (IOException e) {
          if (LOG.isWarnEnabled())
            LOG.warn("Failed isFile check on " + p);
        }
        return result;
      }
    });
    NavigableSet<Path> filesSorted = new TreeSet<Path>();
    if (files == null) return filesSorted;
    for (FileStatus status: files) {
      filesSorted.add(status.getPath());
    }
    return filesSorted;
  }

  /**
   * Move aside a bad edits file.
   * @param fs
   * @param edits Edits file to move aside.
   * @return The name of the moved aside file.
   * @throws IOException
   */
  public static Path moveAsideBadEditsFile(final FileSystem fs,
      final Path edits) throws IOException {
    Path moveAsideName = new Path(edits.getParent(), edits.getName() + "." +
      System.currentTimeMillis());
    if (!fs.rename(edits, moveAsideName)) {
      if (LOG.isWarnEnabled())
        LOG.warn("Rename failed from " + edits + " to " + moveAsideName);
    }
    return moveAsideName;
  }

  /**
   * @param regiondir This regions directory in the filesystem.
   * @return The directory that holds recovered edits files for the region
   * <code>regiondir</code>
   */
  public static Path getRegionDirRecoveredEditsDir(final Path regiondir) {
    return new Path(regiondir, RECOVERED_EDITS_DIR);
  }

  /**
   *
   * @param visitor
   */
  public void addLogEntryVisitor(LogEntryVisitor visitor) {
    this.mLogEntryVisitors.add(visitor);
  }

  /**
   * 
   * @param visitor
   */
  public void removeLogEntryVisitor(LogEntryVisitor visitor) {
    this.mLogEntryVisitors.remove(visitor);
  }

  public void addLogActionsListerner(LogActionsListener list) {
    if (LOG.isInfoEnabled()) LOG.info("Adding a listener");
    this.mActionListeners.add(list);
  }

  public boolean removeLogActionsListener(LogActionsListener list) {
    return this.mActionListeners.remove(list);
  }

  public static final long FIXED_OVERHEAD = ClassSize.align(
    ClassSize.OBJECT + (5 * ClassSize.REFERENCE) +
    ClassSize.ATOMIC_INTEGER + Bytes.SIZEOF_INT + (3 * Bytes.SIZEOF_LONG));

  public static void dump(final Configuration conf, final Path p)
      throws IOException {
    FileSystem fs = FileSystem.getLocal(conf); //FSUtils.getFs(conf);
    if (!fs.exists(p)) {
      throw new FileNotFoundException(p.toString());
    }
    if (!fs.isFile(p)) {
      throw new IOException(p + " is not a file");
    }
    Reader log = getReader(fs, p, conf);
    try {
      int count = 0;
      DBLog.Entry entry;
      while ((entry = log.next()) != null) {
        System.out.println("#" + count + ", pos=" + log.getPosition() + " " +
          entry.toString());
        count++;
      }
    } finally {
      log.close();
    }
  }

  public static void split(final Configuration conf, final Path p)
      throws IOException {
    FileSystem fs = FileSystem.getLocal(conf); //FSUtils.getFs(conf);
    if (!fs.exists(p)) {
      throw new FileNotFoundException(p.toString());
    }
    final Path baseDir = new Path(conf.get(DBConstants.BIGDB_DIR));
    final Path oldLogDir = new Path(baseDir, DBConstants.DBREGION_OLDLOGDIR_NAME);
    if (!fs.getFileStatus(p).isDir()) {
      throw new IOException(p + " is not a directory");
    }
    splitLog(baseDir, p, oldLogDir, fs, conf);
  }

}
