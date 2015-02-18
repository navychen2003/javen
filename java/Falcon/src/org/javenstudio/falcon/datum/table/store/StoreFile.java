package org.javenstudio.falcon.datum.table.store;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.nio.ByteBuffer;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;
import org.javenstudio.raptor.io.RawComparator;
import org.javenstudio.raptor.io.WritableUtils;
import org.javenstudio.raptor.util.StringUtils;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

/**
 * A Store data file.  Stores usually have one or more of these files.  They
 * are produced by flushing the memstore to disk.  To
 * create, call {@link #createWriter(FileSystem, Path, int)} and append data.  Be
 * sure to add any metadata before calling close on the Writer
 * (Use the appendMetadata convenience methods). On close, a StoreFile is
 * sitting in the Filesystem.  To refer to it, create a StoreFile instance
 * passing filesystem and path.  To read, call {@link #createReader()}.
 * <p>StoreFiles may also reference store files in another Store.
 *
 * The reason for this weird pattern where you use a different instance for the
 * writer and a reader is that we write once but read a lot more.
 */
public class StoreFile {
  private static final Logger LOG = Logger.getLogger(StoreFile.class);

  // Config keys.
  static final String IO_STOREFILE_BLOOM_ERROR_RATE = "io.storefile.bloom.error.rate";
  static final String IO_STOREFILE_BLOOM_MAX_FOLD = "io.storefile.bloom.max.fold";
  static final String IO_STOREFILE_BLOOM_ENABLED = "io.storefile.bloom.enabled";
  static final String DBFILE_BLOCK_CACHE_SIZE_KEY = "dbfile.block.cache.size";

  public static enum BloomType {
    /**
     * Bloomfilters disabled
     */
    NONE,
    /**
     * Bloom enabled with Table row as Key
     */
    ROW,
    /**
     * Bloom enabled with Table row & column (family+qualifier) as Key
     */
    ROWCOL
  }
  
  // Keys for fileinfo values in DBFile
  /** Max Sequence ID in FileInfo */
  public static final byte[] MAX_SEQ_ID_KEY = Bytes.toBytes("MAX_SEQ_ID_KEY");
  /** Major compaction flag in FileInfo */
  public static final byte[] MAJOR_COMPACTION_KEY = Bytes.toBytes("MAJOR_COMPACTION_KEY");
  /** Bloom filter Type in FileInfo */
  static final byte[] BLOOM_FILTER_TYPE_KEY = Bytes.toBytes("BLOOM_FILTER_TYPE");
  /** Key for Timerange information in metadata*/
  static final byte[] TIMERANGE_KEY = Bytes.toBytes("TIMERANGE");

  /** Meta data block name for bloom filter meta-info (ie: bloom params/specs) */
  static final String BLOOM_FILTER_META_KEY = "BLOOM_FILTER_META";
  /** Meta data block name for bloom filter data (ie: bloom bits) */
  static final String BLOOM_FILTER_DATA_KEY = "BLOOM_FILTER_DATA";

  // Make default block size for StoreFiles 8k while testing.  TODO: FIX!
  // Need to make it 8k for testing.
  public static final int DEFAULT_BLOCKSIZE_SMALL = 8 * 1024;

  private static BlockCache sDBFileBlockCache = null;

  private final FileSystem mFs;
  // This file's path.
  private final Path mPath;
  // If this storefile references another, this is the reference instance.
  private Reference mReference;
  // If this StoreFile references another, this is the other files path.
  private Path mReferencePath;
  // Should the block cache be used or not.
  private boolean mBlockcache;
  // Is this from an in-memory store
  private boolean mInMemory;

  // Keys for metadata stored in backing DBFile.
  // Set when we obtain a Reader.
  private long mSequenceid = -1;

  // If true, this file was product of a major compaction.  Its then set
  // whenever you get a Reader.
  private AtomicBoolean mMajorCompaction = null;

  /** Meta key set when store file is a result of a bulk load */
  public static final byte[] BULKLOAD_TASK_KEY =
    Bytes.toBytes("BULKLOAD_SOURCE_TASK");
  public static final byte[] BULKLOAD_TIME_KEY =
    Bytes.toBytes("BULKLOAD_TIMESTAMP");

  /**
   * Map of the metadata entries in the corresponding DBFile
   */
  private Map<byte[], byte[]> mMetadataMap;

  /**
   * Regex that will work for straight filenames and for reference names.
   * If reference, then the regex has more than just one group.  Group 1 is
   * this files id.  Group 2 the referenced region name, etc.
   */
  private static final Pattern REF_NAME_PARSER =
    Pattern.compile("^(\\d+)(?:\\.(.+))?$");

  // StoreFile.Reader
  private volatile Reader mReader;

  // Used making file ids.
  private final static Random sRand = new Random();
  private final Configuration mConf;
  private final BloomType mBloomType;

  /**
   * Constructor, loads a reader and it's indices, etc. May allocate a
   * substantial amount of ram depending on the underlying files (10-20MB?).
   *
   * @param fs  The current file system to use.
   * @param p  The path of the file.
   * @param blockcache  <code>true</code> if the block cache is enabled.
   * @param conf  The current configuration.
   * @param bt The bloom type to use for this store file
   * @throws IOException When opening the reader fails.
   */
  StoreFile(final FileSystem fs,
            final Path p,
            final boolean blockcache,
            final Configuration conf,
            final BloomType bt,
            final boolean inMemory)
      throws IOException {
    this.mConf = conf;
    this.mFs = fs;
    this.mPath = p;
    this.mBlockcache = blockcache;
    this.mInMemory = inMemory;
    if (isReference(p)) {
      this.mReference = Reference.read(fs, p);
      this.mReferencePath = getReferredToFile(this.mPath);
    }
    // ignore if the column family config says "no bloom filter"
    // even if there is one in the dbfile.
    if (conf.getBoolean(IO_STOREFILE_BLOOM_ENABLED, true)) {
      this.mBloomType = bt;
    } else {
      this.mBloomType = BloomType.NONE;
      if (LOG.isInfoEnabled())
        LOG.info("Ignoring bloom filter check for file (disabled in config)");
    }
  }

  /**
   * @return Path or null if this StoreFile was made with a Stream.
   */
  public Path getPath() {
    return this.mPath;
  }

  /**
   * @return The Store/ColumnFamily this file belongs to.
   */
  public byte[] getFamily() {
    return Bytes.toBytes(this.mPath.getParent().getName());
  }

  /**
   * @return True if this is a StoreFile Reference; call after {@link #open()}
   * else may get wrong answer.
   */
  public boolean isReference() {
    return this.mReference != null;
  }

  /**
   * @param p Path to check.
   * @return True if the path has format of a DBStoreFile reference.
   */
  public static boolean isReference(final Path p) {
    return !p.getName().startsWith("_") &&
      isReference(p, REF_NAME_PARSER.matcher(p.getName()));
  }

  /**
   * @param p Path to check.
   * @param m Matcher to use.
   * @return True if the path has format of a DBStoreFile reference.
   */
  public static boolean isReference(final Path p, final Matcher m) {
    if (m == null || !m.matches()) {
      if (LOG.isWarnEnabled())
        LOG.warn("Failed match of store file name " + p.toString());
      throw new RuntimeException("Failed match of store file name " +
          p.toString());
    }
    return m.groupCount() > 1 && m.group(2) != null;
  }

  /**
   * Return path to the file referred to by a Reference.  Presumes a directory
   * hierarchy of <code>${bigdb.rootdir}/tablename/regionname/familyname</code>.
   * @param p Path to a Reference file.
   * @return Calculated path to parent region file.
   * @throws IOException
   */
  static Path getReferredToFile(final Path p) {
    Matcher m = REF_NAME_PARSER.matcher(p.getName());
    if (m == null || !m.matches()) {
      if (LOG.isWarnEnabled())
        LOG.warn("Failed match of store file name " + p.toString());
      throw new RuntimeException("Failed match of store file name " +
          p.toString());
    }
    // Other region name is suffix on the passed Reference file name
    String otherRegion = m.group(2);
    // Tabledir is up two directories from where Reference was written.
    Path tableDir = p.getParent().getParent().getParent();
    String nameStrippedOfSuffix = m.group(1);
    // Build up new path with the referenced region in place of our current
    // region in the reference path.  Also strip regionname suffix from name.
    return new Path(new Path(new Path(tableDir, otherRegion),
      p.getParent().getName()), nameStrippedOfSuffix);
  }

  /**
   * @return True if this file was made by a major compaction.
   */
  public boolean isMajorCompaction() {
    if (this.mMajorCompaction == null) {
      throw new NullPointerException("This has not been set yet");
    }
    return this.mMajorCompaction.get();
  }

  /**
   * @return This files maximum edit sequence id.
   */
  public long getMaxSequenceId() {
    if (this.mSequenceid == -1) {
      throw new IllegalAccessError("Has not been initialized");
    }
    return this.mSequenceid;
  }

  /**
   * Return the highest sequence ID found across all storefiles in
   * the given list. Store files that were created by a mapreduce
   * bulk load are ignored, as they do not correspond to any edit
   * log items.
   * @return 0 if no non-bulk-load files are provided or, this is Store that
   * does not yet have any store files.
   */
  public static long getMaxSequenceIdInList(List<StoreFile> sfs) {
    long max = 0;
    for (StoreFile sf : sfs) {
      if (!sf.isBulkLoadResult()) {
        max = Math.max(max, sf.getMaxSequenceId());
      }
    }
    return max;
  }

  /**
   * @return true if this storefile was created by DBFileOutputFormat
   * for a bulk load.
   */
  public boolean isBulkLoadResult() {
    return mMetadataMap.containsKey(BULKLOAD_TIME_KEY);
  }

  /**
   * Return the timestamp at which this bulk load file was generated.
   */
  public long getBulkLoadTimestamp() {
    return Bytes.toLong(mMetadataMap.get(BULKLOAD_TIME_KEY));
  }

  /**
   * Returns the block cache or <code>null</code> in case none should be used.
   *
   * @param conf  The current configuration.
   * @return The block cache or <code>null</code>.
   */
  public static synchronized BlockCache getBlockCache(Configuration conf) {
    if (sDBFileBlockCache != null) return sDBFileBlockCache;

    float cachePercentage = conf.getFloat(DBFILE_BLOCK_CACHE_SIZE_KEY, 0.0f);
    // There should be a better way to optimize this. But oh well.
    if (cachePercentage == 0L) return null;
    if (cachePercentage > 1.0) {
      throw new IllegalArgumentException(DBFILE_BLOCK_CACHE_SIZE_KEY +
        " must be between 0.0 and 1.0, not > 1.0");
    }

    // Calculate the amount of heap to give the heap.
    MemoryUsage mu = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
    long cacheSize = (long)(mu.getMax() * cachePercentage);
    if (LOG.isInfoEnabled()) {
      LOG.info("Allocating LruBlockCache with maximum size " +
        StringUtils.humanReadableInt(cacheSize));
    }
    sDBFileBlockCache = new LruBlockCache(cacheSize, DEFAULT_BLOCKSIZE_SMALL);
    return sDBFileBlockCache;
  }

  public static synchronized void shutdownBlockCache() { 
	  if (LOG.isDebugEnabled()) LOG.debug("shutdownBlockCache");
	  
	  BlockCache blockCache = sDBFileBlockCache;
	  sDBFileBlockCache = null;
	  
	  if (blockCache != null)
		  blockCache.shutdown();
  }
  
  /**
   * @return the blockcache
   */
  public BlockCache getBlockCache() {
    return mBlockcache ? getBlockCache(mConf) : null;
  }

  /**
   * Opens reader on this store file.  Called by Constructor.
   * @return Reader for the store file.
   * @throws IOException
   * @see #closeReader()
   */
  private Reader open() throws IOException {
    if (this.mReader != null) {
      throw new IllegalAccessError("Already open");
    }

    if (isReference()) {
      this.mReader = new HalfStoreFileReader(this.mFs, this.mReferencePath,
          getBlockCache(), this.mReference);
    } else {
      this.mReader = new Reader(this.mFs, this.mPath, getBlockCache(),
          this.mInMemory);
    }

    // Load up indices and fileinfo.
    mMetadataMap = Collections.unmodifiableMap(this.mReader.loadFileInfo());
    // Read in our metadata.
    byte[] b = mMetadataMap.get(MAX_SEQ_ID_KEY);
    
    if (b != null) {
      // By convention, if halfdbfile, top half has a sequence number > bottom
      // half. Thats why we add one in below. Its done for case the two halves
      // are ever merged back together --rare.  Without it, on open of store,
      // since store files are distingushed by sequence id, the one half would
      // subsume the other.
      this.mSequenceid = Bytes.toLong(b);
      if (isReference()) {
        if (Reference.isTopFileRegion(this.mReference.getFileRegion())) {
          this.mSequenceid += 1;
        }
      }
    }

    b = mMetadataMap.get(MAJOR_COMPACTION_KEY);
    if (b != null) {
      boolean mc = Bytes.toBoolean(b);
      if (this.mMajorCompaction == null) {
        this.mMajorCompaction = new AtomicBoolean(mc);
      } else {
        this.mMajorCompaction.set(mc);
      }
    }

    if (this.mBloomType != BloomType.NONE) {
      this.mReader.loadBloomfilter();
    }

    try {
      byte[] timerangeBytes = mMetadataMap.get(TIMERANGE_KEY);
      if (timerangeBytes != null) {
        this.mReader.mTimeRangeTracker = new TimeRangeTracker();
        Writables.copyWritable(timerangeBytes, this.mReader.mTimeRangeTracker);
      }
    } catch (IllegalArgumentException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Error reading timestamp range data from meta -- " +
          "proceeding without", e);
      }
      this.mReader.mTimeRangeTracker = null;
    }
    
    return this.mReader;
  }

  /**
   * @return Reader for StoreFile. creates if necessary
   * @throws IOException
   */
  public Reader createReader() throws IOException {
    if (this.mReader == null) {
      this.mReader = open();
    }
    return this.mReader;
  }

  /**
   * @return Current reader.  Must call createReader first else returns null.
   * @throws IOException
   * @see {@link #createReader()}
   */
  public Reader getReader() {
    return this.mReader;
  }

  /**
   * @throws IOException
   */
  public synchronized void closeReader() throws IOException {
    if (this.mReader != null) {
      this.mReader.close();
      this.mReader = null;
    }
  }

  /**
   * Delete this file
   * @throws IOException
   */
  public void deleteReader() throws IOException {
    closeReader();
    this.mFs.delete(getPath(), true);
  }

  @Override
  public String toString() {
    return this.mPath.toString() +
      (isReference()? "-" + this.mReferencePath + "-" + mReference.toString(): "");
  }

  /**
   * @return a length description of this StoreFile, suitable for debug output
   */
  public String toStringDetailed() {
    StringBuilder sb = new StringBuilder();
    sb.append(this.mPath.toString());
    sb.append(", isReference=").append(isReference());
    sb.append(", isBulkLoadResult=").append(isBulkLoadResult());
    if (isBulkLoadResult()) {
      sb.append(", bulkLoadTS=").append(getBulkLoadTimestamp());
    } else {
      sb.append(", seqid=").append(getMaxSequenceId());
    }
    sb.append(", majorCompaction=").append(isMajorCompaction());
    return sb.toString();
  }

  /**
   * Utility to help with rename.
   * @param fs
   * @param src
   * @param tgt
   * @return True if succeeded.
   * @throws IOException
   */
  public static Path rename(final FileSystem fs,
                            final Path src,
                            final Path tgt)
      throws IOException {
    if (!fs.exists(src)) {
      throw new FileNotFoundException(src.toString());
    }
    if (!fs.rename(src, tgt)) {
      throw new IOException("Failed rename of " + src + " to " + tgt);
    }
    return tgt;
  }

  /**
   * Get a store file writer. Client is responsible for closing file when done.
   *
   * @param fs
   * @param dir Path to family directory.  Makes the directory if doesn't exist.
   * Creates a file with a unique name in this directory.
   * @param blocksize size per filesystem block
   * @return StoreFile.Writer
   * @throws IOException
   */
  public static Writer createWriter(final FileSystem fs,
                                    final Path dir,
                                    final int blocksize)
      throws IOException {
    return createWriter(fs, dir, blocksize, null, null, null, BloomType.NONE, 0);
  }

  /**
   * Create a store file writer. Client is responsible for closing file when done.
   * If metadata, add BEFORE closing using appendMetadata()
   * @param fs
   * @param dir Path to family directory.  Makes the directory if doesn't exist.
   * Creates a file with a unique name in this directory.
   * @param blocksize
   * @param algorithm Pass null to get default.
   * @param conf HBase system configuration. used with bloom filters
   * @param bloomType column family setting for bloom filters
   * @param c Pass null to get default.
   * @param maxKeySize peak theoretical entry size (maintains error rate)
   * @return DBFile.Writer
   * @throws IOException
   */
  public static StoreFile.Writer createWriter(final FileSystem fs,
                                              final Path dir,
                                              final int blocksize,
                                              final Compression.Algorithm algorithm,
                                              final KeyValue.KVComparator c,
                                              final Configuration conf,
                                              BloomType bloomType,
                                              int maxKeySize)
      throws IOException {
    if (!fs.exists(dir)) {
      fs.mkdirs(dir);
    }
    Path path = getUniqueFile(fs, dir);
    if (conf == null || !conf.getBoolean(IO_STOREFILE_BLOOM_ENABLED, true)) {
      bloomType = BloomType.NONE;
    }

    return new Writer(fs, path, blocksize,
        algorithm == null? DBFile.DEFAULT_COMPRESSION_ALGORITHM: algorithm,
        conf, c == null? KeyValue.COMPARATOR: c, bloomType, maxKeySize);
  }

  /**
   * @param fs
   * @param dir Directory to create file in.
   * @return random filename inside passed <code>dir</code>
   */
  public static Path getUniqueFile(final FileSystem fs, final Path dir)
      throws IOException {
    if (!fs.getFileStatus(dir).isDir()) {
      throw new IOException("Expecting " + dir.toString() +
        " to be a directory");
    }
    return fs.getFileStatus(dir).isDir()? getRandomFilename(fs, dir): dir;
  }

  /**
   *
   * @param fs
   * @param dir
   * @return Path to a file that doesn't exist at time of this invocation.
   * @throws IOException
   */
  static Path getRandomFilename(final FileSystem fs, final Path dir)
      throws IOException {
    return getRandomFilename(fs, dir, null);
  }

  /**
   *
   * @param fs
   * @param dir
   * @param suffix
   * @return Path to a file that doesn't exist at time of this invocation.
   * @throws IOException
   */
  static Path getRandomFilename(final FileSystem fs,
                                final Path dir,
                                final String suffix)
      throws IOException {
    long id = -1;
    Path p = null;
    do {
      id = Math.abs(sRand.nextLong());
      p = new Path(dir, Long.toString(id) +
        ((suffix == null || suffix.length() <= 0)? "": suffix));
    } while(fs.exists(p));
    return p;
  }

  /**
   * Write out a split reference.
   *
   * Package local so it doesnt leak out of regionserver.
   *
   * @param fs
   * @param splitDir Presumes path format is actually
   * <code>SOME_DIRECTORY/REGIONNAME/FAMILY</code>.
   * @param f File to split.
   * @param splitRow
   * @param range
   * @return Path to created reference.
   * @throws IOException
   */
  static Path split(final FileSystem fs,
                    final Path splitDir,
                    final StoreFile f,
                    final byte[] splitRow,
                    final Reference.Range range)
      throws IOException {
    // A reference to the bottom half of the hsf store file.
    Reference r = new Reference(splitRow, range);
    // Add the referred-to regions name as a dot separated suffix.
    // See REF_NAME_PARSER regex above.  The referred-to regions name is
    // up in the path of the passed in <code>f</code> -- parentdir is family,
    // then the directory above is the region name.
    String parentRegionName = f.getPath().getParent().getParent().getName();
    // Write reference with same file id only with the other region name as
    // suffix and into the new region location (under same family).
    Path p = new Path(splitDir, f.getPath().getName() + "." + parentRegionName);
    return r.write(fs, p);
  }

  /**
   * A StoreFile writer.  Use this to read/write HBase Store Files. It is package
   * local because it is an implementation detail of the HBase regionserver.
   */
  public static class Writer {
    private final BloomFilter mBloomFilter;
    private final BloomType mBloomType;
    private KeyValue.KVComparator mKvComparator;
    private KeyValue mLastKv = null;
    private byte[] mLastByteArray = null;
    private TimeRangeTracker mTimeRangeTracker = new TimeRangeTracker();
    
    /** 
     * isTimeRangeTrackerSet keeps track if the timeRange has already been set
     * When flushing a memstore, we set TimeRange and use this variable to
     * indicate that it doesn't need to be calculated again while
     * appending KeyValues.
     * It is not set in cases of compactions when it is recalculated using only
     * the appended KeyValues*/
    private boolean mIsTimeRangeTrackerSet = false;

    protected DBFile.Writer mWriter;
    
    /**
     * Creates an DBFile.Writer that also write helpful meta data.
     * @param fs file system to write to
     * @param path file name to create
     * @param blocksize HDFS block size
     * @param compress HDFS block compression
     * @param conf user configuration
     * @param comparator key comparator
     * @param bloomType bloom filter setting
     * @param maxKeys maximum amount of keys to add (for blooms)
     * @throws IOException problem writing to FS
     */
    public Writer(FileSystem fs, Path path, int blocksize,
        Compression.Algorithm compress, final Configuration conf,
        final KeyValue.KVComparator comparator, BloomType bloomType, int maxKeys)
        throws IOException {
      this.mWriter = new DBFile.Writer(fs, path, blocksize, compress, 
    		  comparator.getRawComparator());
      this.mKvComparator = comparator;

      if (bloomType != BloomType.NONE && conf != null) {
        float err = conf.getFloat(IO_STOREFILE_BLOOM_ERROR_RATE, (float)0.01);
        // Since in row+col blooms we have 2 calls to shouldSeek() instead of 1
        // and the false positives are adding up, we should keep the error rate
        // twice as low in order to maintain the number of false positives as
        // desired by the user
        if (bloomType == BloomType.ROWCOL) {
          err /= 2;
        }
        int maxFold = conf.getInt(IO_STOREFILE_BLOOM_MAX_FOLD, 7);

        this.mBloomFilter = new ByteBloomFilter(maxKeys, err,
            Hash.getHashType(conf), maxFold);
        this.mBloomFilter.allocBloom();
        this.mBloomType = bloomType;
      } else {
        this.mBloomFilter = null;
        this.mBloomType = BloomType.NONE;
      }
    }

    /**
     * Writes meta data.
     * Call before {@link #close()} since its written as meta data to this file.
     * @param maxSequenceId Maximum sequence id.
     * @param majorCompaction True if this file is product of a major compaction
     * @throws IOException problem writing to FS
     */
    public void appendMetadata(final long maxSequenceId, 
    	final boolean majorCompaction) throws IOException {
      mWriter.appendFileInfo(MAX_SEQ_ID_KEY, Bytes.toBytes(maxSequenceId));
      mWriter.appendFileInfo(MAJOR_COMPACTION_KEY,
          Bytes.toBytes(majorCompaction));
      appendTimeRangeMetadata();
    }

    /**
     * Add TimestampRange to Metadata
     */
    public void appendTimeRangeMetadata() throws IOException {
      appendFileInfo(TIMERANGE_KEY,WritableUtils.toByteArray(mTimeRangeTracker));
    }

    /**
     * Set TimeRangeTracker
     * @param trt
     */
    public void setTimeRangeTracker(final TimeRangeTracker trt) {
      this.mTimeRangeTracker = trt;
      this.mIsTimeRangeTrackerSet = true;
    }

    /**
     * If the timeRangeTracker is not set,
     * update TimeRangeTracker to include the timestamp of this key
     * @param kv
     * @throws IOException
     */
    public void includeInTimeRangeTracker(final KeyValue kv) {
      if (!mIsTimeRangeTrackerSet) {
        mTimeRangeTracker.includeTimestamp(kv);
      }
    }

    /**
     * If the timeRangeTracker is not set,
     * update TimeRangeTracker to include the timestamp of this key
     * @param key
     * @throws IOException
     */
    public void includeInTimeRangeTracker(final byte[] key) {
      if (!mIsTimeRangeTrackerSet) {
        mTimeRangeTracker.includeTimestamp(key);
      }
    }

    public void append(final KeyValue kv) throws IOException {
      if (this.mBloomFilter != null) {
        // only add to the bloom filter on a new, unique key
        boolean newKey = true;
        if (this.mLastKv != null) {
          switch(mBloomType) {
          case ROW:
            newKey = ! mKvComparator.matchingRows(kv, mLastKv);
            break;
          case ROWCOL:
            newKey = ! mKvComparator.matchingRowColumn(kv, mLastKv);
            break;
          case NONE:
            newKey = false;
          }
        }
        if (newKey) {
          /**
           * http://2.bp.blogspot.com/_Cib_A77V54U/StZMrzaKufI/AAAAAAAAADo/ZhK7bGoJdMQ/s400/KeyValue.png
           * Key = RowLen + Row + FamilyLen + Column [Family + Qualifier] + TimeStamp
           *
           * 2 Types of Filtering:
           *  1. Row = Row
           *  2. RowCol = Row + Qualifier
           */
          switch (mBloomType) {
          case ROW:
            this.mBloomFilter.add(kv.getBuffer(), kv.getRowOffset(),
                kv.getRowLength());
            break;
          case ROWCOL:
            // merge(row, qualifier)
            int ro = kv.getRowOffset();
            int rl = kv.getRowLength();
            int qo = kv.getQualifierOffset();
            int ql = kv.getQualifierLength();
            byte[] result = new byte[rl + ql];
            System.arraycopy(kv.getBuffer(), ro, result, 0,  rl);
            System.arraycopy(kv.getBuffer(), qo, result, rl, ql);
            this.mBloomFilter.add(result);
            break;
          default:
          }
          this.mLastKv = kv;
        }
      }
      mWriter.append(kv);
      includeInTimeRangeTracker(kv);
    }

    public Path getPath() {
      return this.mWriter.getPath();
    }

    public void append(final byte[] key, final byte[] value) throws IOException {
      if (this.mBloomFilter != null) {
        // only add to the bloom filter on a new row
        if (this.mLastByteArray == null || !Arrays.equals(key, mLastByteArray)) {
          this.mBloomFilter.add(key);
          this.mLastByteArray = key;
        }
      }
      mWriter.append(key, value);
      includeInTimeRangeTracker(key);
    }

    public void close() throws IOException {
      // make sure we wrote something to the bloom before adding it
      if (this.mBloomFilter != null && this.mBloomFilter.getKeyCount() > 0) {
        mBloomFilter.compactBloom();
        if (this.mBloomFilter.getMaxKeys() > 0) {
          int b = this.mBloomFilter.getByteSize();
          int k = this.mBloomFilter.getKeyCount();
          int m = this.mBloomFilter.getMaxKeys();
          if (LOG.isInfoEnabled()) {
            LOG.info("Bloom added to DBFile.  " + b + "B, " +
              k + "/" + m + " (" + NumberFormat.getPercentInstance().format(
                ((double)k) / ((double)m)) + ")");
          }
        }
        mWriter.appendMetaBlock(BLOOM_FILTER_META_KEY, mBloomFilter.getMetaWriter());
        mWriter.appendMetaBlock(BLOOM_FILTER_DATA_KEY, mBloomFilter.getDataWriter());
        mWriter.appendFileInfo(BLOOM_FILTER_TYPE_KEY, Bytes.toBytes(mBloomType.toString()));
      }
      mWriter.close();
    }

    public void appendFileInfo(byte[] key, byte[] value) throws IOException {
      mWriter.appendFileInfo(key, value);
    }
  }

  /**
   * Reader for a StoreFile.
   */
  public static class Reader {
    //static final Logger LOG = Logger.getLogger(Reader.class);

    protected BloomFilter mBloomFilter = null;
    protected BloomType mBloomFilterType;
    private final DBFile.Reader mReader;
    protected TimeRangeTracker mTimeRangeTracker = null;

    public Reader(FileSystem fs, Path path, BlockCache blockCache, boolean inMemory)
        throws IOException {
      mReader = new DBFile.Reader(fs, path, blockCache, inMemory);
      mBloomFilterType = BloomType.NONE;
    }

    public RawComparator<byte[]> getComparator() {
      return mReader.getComparator();
    }

    /**
     * Get a scanner to scan over this StoreFile.
     *
     * @param cacheBlocks should this scanner cache blocks?
     * @param pread use pread (for highly concurrent small readers)
     * @return a scanner
     */
    public StoreFileScanner getStoreFileScanner(boolean cacheBlocks, boolean pread) {
      return new StoreFileScanner(this, getScanner(cacheBlocks, pread));
    }

    /**
     * Warning: Do not write further code which depends on this call. Instead
     * use getStoreFileScanner() which uses the StoreFileScanner class/interface
     * which is the preferred way to scan a store with higher level concepts.
     *
     * @param cacheBlocks should we cache the blocks?
     * @param pread use pread (for concurrent small readers)
     * @return the underlying DBFileScanner
     */
    @Deprecated
    public DBFileScanner getScanner(boolean cacheBlocks, boolean pread) {
      return mReader.getScanner(cacheBlocks, pread);
    }

    public void close() throws IOException {
      mReader.close();
    }

    public boolean shouldSeek(Scan scan, final SortedSet<byte[]> columns) {
      return (passesTimerangeFilter(scan) && passesBloomFilter(scan,columns));
    }

    /**
     * Check if this storeFile may contain keys within the TimeRange
     * @param scan
     * @return False if it definitely does not exist in this StoreFile
     */
    private boolean passesTimerangeFilter(Scan scan) {
      if (mTimeRangeTracker == null) {
        return true;
      } else {
        return mTimeRangeTracker.includesTimeRange(scan.getTimeRange());
      }
    }

    private boolean passesBloomFilter(Scan scan, final SortedSet<byte[]> columns) {
      if (this.mBloomFilter == null || !scan.isGetScan()) 
        return true;
      
      byte[] row = scan.getStartRow();
      byte[] key;
      switch (this.mBloomFilterType) {
        case ROW:
          key = row;
          break;
        case ROWCOL:
          if (columns != null && columns.size() == 1) {
            byte[] col = columns.first();
            key = Bytes.add(row, col);
            break;
          }
          //$FALL-THROUGH$
        default:
          return true;
      }

      try {
        ByteBuffer bloom = mReader.getMetaBlock(BLOOM_FILTER_DATA_KEY, true);
        if (bloom != null) {
          if (this.mBloomFilterType == BloomType.ROWCOL) {
            // Since a Row Delete is essentially a DeleteFamily applied to all
            // columns, a file might be skipped if using row+col Bloom filter.
            // In order to ensure this file is included an additional check is
            // required looking only for a row bloom.
            return this.mBloomFilter.contains(key, bloom) ||
                this.mBloomFilter.contains(row, bloom);
          }
          else {
            return this.mBloomFilter.contains(key, bloom);
          }
        }
      } catch (IOException e) {
    	if (LOG.isErrorEnabled()) 
          LOG.error("Error reading bloom filter data -- proceeding without", e);
    	
        setBloomFilterFaulty();
      } catch (IllegalArgumentException e) {
    	if (LOG.isErrorEnabled())
          LOG.error("Bad bloom filter data -- proceeding without", e);
    	
        setBloomFilterFaulty();
      }

      return true;
    }

    public Map<byte[], byte[]> loadFileInfo() throws IOException {
      Map<byte[], byte[]> fi = mReader.loadFileInfo();

      byte[] b = fi.get(BLOOM_FILTER_TYPE_KEY);
      if (b != null) {
        mBloomFilterType = BloomType.valueOf(Bytes.toString(b));
      }

      return fi;
    }

    public void loadBloomfilter() {
      if (this.mBloomFilter != null) 
        return; // already loaded

      try {
        ByteBuffer b = mReader.getMetaBlock(BLOOM_FILTER_META_KEY, false);
        if (b != null) {
          if (mBloomFilterType == BloomType.NONE) 
            throw new IOException("valid bloom filter type not found in FileInfo");
          
          this.mBloomFilter = new ByteBloomFilter(b);
          if (LOG.isInfoEnabled()) {
            LOG.info("Loaded " + (mBloomFilterType == BloomType.ROW ? "row":"col")
              + " bloom filter metadata for " + mReader.getName());
          }
        }
      } catch (IOException e) {
    	if (LOG.isErrorEnabled())
          LOG.error("Error reading bloom filter meta -- proceeding without", e);
    	
        this.mBloomFilter = null;
      } catch (IllegalArgumentException e) {
    	if (LOG.isErrorEnabled())
          LOG.error("Bad bloom filter meta -- proceeding without", e);
    	
        this.mBloomFilter = null;
      }
    }

    public int getFilterEntries() {
      return (this.mBloomFilter != null) ? this.mBloomFilter.getKeyCount()
          : mReader.getFilterEntries();
    }

    public ByteBuffer getMetaBlock(String bloomFilterDataKey, 
    	boolean cacheBlock) throws IOException {
      return mReader.getMetaBlock(bloomFilterDataKey, cacheBlock);
    }

    public void setBloomFilterFaulty() {
      mBloomFilter = null;
    }

    public byte[] getLastKey() {
      return mReader.getLastKey();
    }

    public byte[] midkey() throws IOException {
      return mReader.midkey();
    }

    public long length() {
      return mReader.length();
    }

    public int getEntries() {
      return mReader.getEntries();
    }

    public byte[] getFirstKey() {
      return mReader.getFirstKey();
    }

    public long indexSize() {
      return mReader.indexSize();
    }

    public BloomType getBloomFilterType() {
      return this.mBloomFilterType;
    }
  }

  /**
   * Useful comparators for comparing StoreFiles.
   */
  static abstract class Comparators {
    /**
     * Comparator that compares based on the flush time of
     * the StoreFiles. All bulk loads are placed before all non-
     * bulk loads, and then all files are sorted by sequence ID.
     * If there are ties, the path name is used as a tie-breaker.
     */
    static final Comparator<StoreFile> FLUSH_TIME =
      Ordering.compound(ImmutableList.of(
          Ordering.natural().onResultOf(new GetBulkTime()),
          Ordering.natural().onResultOf(new GetSeqId()),
          Ordering.natural().onResultOf(new GetPathName())
      ));

    private static class GetBulkTime implements Function<StoreFile, Long> {
      @Override
      public Long apply(StoreFile sf) {
        if (!sf.isBulkLoadResult()) return Long.MAX_VALUE;
        return sf.getBulkLoadTimestamp();
      }
    }
    
    private static class GetSeqId implements Function<StoreFile, Long> {
      @Override
      public Long apply(StoreFile sf) {
        if (sf.isBulkLoadResult()) return -1L;
        return sf.getMaxSequenceId();
      }
    }
    
    private static class GetPathName implements Function<StoreFile, String> {
      @Override
      public String apply(StoreFile sf) {
        return sf.getPath().getName();
      }
    }
  }
  
}
