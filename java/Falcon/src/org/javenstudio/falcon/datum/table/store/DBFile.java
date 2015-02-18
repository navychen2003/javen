package org.javenstudio.falcon.datum.table.store;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.javenstudio.raptor.fs.FSDataInputStream;
import org.javenstudio.raptor.fs.FSDataOutputStream;
import org.javenstudio.raptor.fs.FileStatus;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;
import org.javenstudio.raptor.fs.PathFilter;
import org.javenstudio.raptor.io.IOUtils;
import org.javenstudio.raptor.io.RawComparator;
import org.javenstudio.raptor.io.Writable;
import org.javenstudio.raptor.io.compress.Compressor;
import org.javenstudio.raptor.io.compress.Decompressor;

/**
 * File format for bigdb.
 * A file of sorted key/value pairs. Both keys and values are byte arrays.
 * <p>
 * The memory footprint of a DBFile includes the following (below is taken from the
 * <a
 * href=https://issues.apache.org/jira/browse/HADOOP-3315>TFile</a> documentation
 * but applies also to DBFile):
 * <ul>
 * <li>Some constant overhead of reading or writing a compressed block.
 * <ul>
 * <li>Each compressed block requires one compression/decompression codec for
 * I/O.
 * <li>Temporary space to buffer the key.
 * <li>Temporary space to buffer the value.
 * </ul>
 * <li>DBFile index, which is proportional to the total number of Data Blocks.
 * The total amount of memory needed to hold the index can be estimated as
 * (56+AvgKeySize)*NumBlocks.
 * </ul>
 * Suggestions on performance optimization.
 * <ul>
 * <li>Minimum block size. We recommend a setting of minimum block size between
 * 8KB to 1MB for general usage. Larger block size is preferred if files are
 * primarily for sequential access. However, it would lead to inefficient random
 * access (because there are more data to decompress). Smaller blocks are good
 * for random access, but require more memory to hold the block index, and may
 * be slower to create (because we must flush the compressor stream at the
 * conclusion of each data block, which leads to an FS I/O flush). Further, due
 * to the internal caching in Compression codec, the smallest possible block
 * size would be around 20KB-30KB.
 * <li>The current implementation does not offer true multi-threading for
 * reading. The implementation uses FSDataInputStream seek()+read(), which is
 * shown to be much faster than positioned-read call in single thread mode.
 * However, it also means that if multiple threads attempt to access the same
 * DBFile (using multiple scanners) simultaneously, the actual I/O is carried out
 * sequentially even if they access different DFS blocks (Reexamine! pread seems
 * to be 10% faster than seek+read in my testing -- stack).
 * <li>Compression codec. Use "none" if the data is not very compressable (by
 * compressable, I mean a compression ratio at least 2:1). Generally, use "lzo"
 * as the starting point for experimenting. "gz" overs slightly better
 * compression ratio over "lzo" but requires 4x CPU to compress and 2x CPU to
 * decompress, comparing to "lzo".
 * </ul>
 *
 * For more on the background behind DBFile, see <a
 * href=https://issues.apache.org/jira/browse/BIGDB-61>BIGDB-61</a>.
 * <p>
 * File is made of data blocks followed by meta data blocks (if any), a fileinfo
 * block, data block index, meta data block index, and a fixed size trailer
 * which records the offsets at which file changes content type.
 * <pre>&lt;data blocks>&lt;meta blocks>&lt;fileinfo>&lt;data index>&lt;meta index>&lt;trailer></pre>
 * Each block has a bit of magic at its start.  Block are comprised of
 * key/values.  In data blocks, they are both byte arrays.  Metadata blocks are
 * a String key and a byte array value.  An empty file looks like this:
 * <pre>&lt;fileinfo>&lt;trailer></pre>.  That is, there are not data nor meta
 * blocks present.
 * <p>
 * TODO: Do scanners need to be able to take a start and end row?
 * TODO: Should BlockIndex know the name of its file?  Should it have a Path
 * that points at its file say for the case where an index lives apart from
 * an DBFile instance?
 */
public class DBFile {
  //private static final Logger LOG = Logger.getLogger(DBFile.class);

  /* These values are more or less arbitrary, and they are used as a
   * form of check to make sure the file isn't completely corrupt.
   */
  final static byte[] DATABLOCKMAGIC =
    {'D', 'A', 'T', 'A', 'B', 'L', 'K', 42 };
  final static byte[] INDEXBLOCKMAGIC =
    { 'I', 'D', 'X', 'B', 'L', 'K', 41, 43 };
  final static byte[] METABLOCKMAGIC =
    { 'M', 'E', 'T', 'A', 'B', 'L', 'K', 99 };
  final static byte[] TRAILERBLOCKMAGIC =
    { 'T', 'R', 'A', 'B', 'L', 'K', 34, 36 };

  /**
   * Maximum length of key in DBFile.
   */
  public final static int MAXIMUM_KEY_LENGTH = Integer.MAX_VALUE;

  /**
   * Default blocksize for dbfile.
   */
  public final static int DEFAULT_BLOCKSIZE = 64 * 1024;

  /**
   * Default compression: none.
   */
  public final static Compression.Algorithm DEFAULT_COMPRESSION_ALGORITHM =
    Compression.Algorithm.NONE;
  
  /** Default compression name: none. */
  public final static String DEFAULT_COMPRESSION =
    DEFAULT_COMPRESSION_ALGORITHM.getName();

  // For measuring latency of "typical" reads and writes
  private static volatile long sReadOps;
  private static volatile long sReadTime;
  private static volatile long sWriteOps;
  private static volatile long sWriteTime;

  public static final long getReadOps() {
    long ret = sReadOps;
    sReadOps = 0;
    return ret;
  }

  public static final long getReadTime() {
    long ret = sReadTime;
    sReadTime = 0;
    return ret;
  }

  public static final long getWriteOps() {
    long ret = sWriteOps;
    sWriteOps = 0;
    return ret;
  }

  public static final long getWriteTime() {
    long ret = sWriteTime;
    sWriteTime = 0;
    return ret;
  }

  /**
   * DBFile Writer.
   */
  public static class Writer implements Closeable {
    // FileSystem stream to write on.
    private FSDataOutputStream mOutputStream;
    
    // True if we opened the <code>outputStream</code> (and so will close it).
    private boolean mCloseOutputStream;

    // Name for this object used when logging or in toString.  Is either
    // the result of a toString on stream or else toString of passed file Path.
    private String mName;

    // Total uncompressed bytes, maybe calculate a compression ratio later.
    private long mTotalBytes = 0;

    // Total # of key/value entries, ie: how many times add() was called.
    private int mEntryCount = 0;

    // Used calculating average key and value lengths.
    private long mKeylength = 0;
    private long mValuelength = 0;

    // Used to ensure we write in order.
    private final RawComparator<byte[]> mRawComparator;

    // A stream made per block written.
    private DataOutputStream mOut;

    // Number of uncompressed bytes per block.  Reinitialized when we start
    // new block.
    private int mBlocksize;

    // Offset where the current block began.
    private long mBlockBegin;

    // First key in a block (Not first key in file).
    private byte[] mFirstKey = null;

    // Key previously appended.  Becomes the last key in the file.
    private byte[] mLastKeyBuffer = null;
    private int mLastKeyOffset = -1;
    private int mLastKeyLength = -1;

    // See {@link BlockIndex}. Below four fields are used to write the block
    // index.
    ArrayList<byte[]> mBlockKeys = new ArrayList<byte[]>();
    // Block offset in backing stream.
    ArrayList<Long> mBlockOffsets = new ArrayList<Long>();
    // Raw (decompressed) data size.
    ArrayList<Integer> mBlockDataSizes = new ArrayList<Integer>();

    // Meta block system.
    private ArrayList<byte[]> mMetaNames = new ArrayList<byte[]>();
    private ArrayList<Writable> mMetaData = new ArrayList<Writable>();

    // Used compression.  Used even if no compression -- 'none'.
    private final Compression.Algorithm mCompressAlgo;
    private Compressor mCompressor;

    // Special datastructure to hold fileinfo.
    private FileInfo mFileinfo = new FileInfo();

    // May be null if we were passed a stream.
    private Path mPath = null;

    /**
     * Constructor that uses all defaults for compression and block size.
     * @param fs
     * @param path
     * @throws IOException
     */
    public Writer(FileSystem fs, Path path) throws IOException {
      this(fs, path, DEFAULT_BLOCKSIZE, (Compression.Algorithm) null, null);
    }

    /**
     * Constructor that takes a Path.
     * @param fs
     * @param path
     * @param blocksize
     * @param compress
     * @param comparator
     * @throws IOException
     * @throws IOException
     */
    public Writer(FileSystem fs, Path path, int blocksize,
        String compress, final KeyValue.KeyComparator comparator) 
        throws IOException {
      this(fs, path, blocksize,
        (compress == null) ? DEFAULT_COMPRESSION_ALGORITHM :
          Compression.getCompressionAlgorithmByName(compress),
        comparator);
    }

    /**
     * Constructor that takes a Path.
     * @param fs
     * @param path
     * @param blocksize
     * @param compress
     * @param comparator
     * @throws IOException
     */
    public Writer(FileSystem fs, Path path, int blocksize,
        Compression.Algorithm compress, final KeyValue.KeyComparator comparator)
        throws IOException {
      this(fs.create(path), blocksize, compress, comparator);
      this.mCloseOutputStream = true;
      this.mName = path.toString();
      this.mPath = path;
    }

    /**
     * Constructor that takes a stream.
     * @param ostream Stream to use.
     * @param blocksize
     * @param compress
     * @param c RawComparator to use.
     * @throws IOException
     */
    public Writer(final FSDataOutputStream ostream, final int blocksize,
        final String compress, final KeyValue.KeyComparator c) 
        throws IOException {
      this(ostream, blocksize,
        Compression.getCompressionAlgorithmByName(compress), c);
    }

    /**
     * Constructor that takes a stream.
     * @param ostream Stream to use.
     * @param blocksize
     * @param compress
     * @param c
     * @throws IOException
     */
    public Writer(final FSDataOutputStream ostream, final int blocksize,
        final Compression.Algorithm  compress, final KeyValue.KeyComparator c)
        throws IOException {
      this.mOutputStream = ostream;
      this.mCloseOutputStream = false;
      this.mBlocksize = blocksize;
      this.mRawComparator = (c == null) ? Bytes.BYTES_RAWCOMPARATOR : c;
      this.mName = this.mOutputStream.toString();
      this.mCompressAlgo = (compress == null) ?
        DEFAULT_COMPRESSION_ALGORITHM : compress;
    }

    /**
     * If at block boundary, opens new block.
     * @throws IOException
     */
    private void checkBlockBoundary() throws IOException {
      if (this.mOut != null && this.mOut.size() < mBlocksize) return;
      finishBlock();
      newBlock();
    }

    /**
     * Do the cleanup if a current block.
     * @throws IOException
     */
    private void finishBlock() throws IOException {
      if (this.mOut == null) return;
      long now = System.currentTimeMillis();

      int size = releaseCompressingStream(this.mOut);
      this.mOut = null;
      this.mBlockKeys.add(mFirstKey);
      this.mBlockOffsets.add(Long.valueOf(mBlockBegin));
      this.mBlockDataSizes.add(Integer.valueOf(size));
      this.mTotalBytes += size;

      sWriteTime += System.currentTimeMillis() - now;
      sWriteOps++;
    }

    /**
     * Ready a new block for writing.
     * @throws IOException
     */
    private void newBlock() throws IOException {
      // This is where the next block begins.
      this.mBlockBegin = mOutputStream.getPos();
      this.mOut = getCompressingStream();
      this.mOut.write(DATABLOCKMAGIC);
      this.mFirstKey = null;
    }

    /**
     * Sets up a compressor and creates a compression stream on top of
     * this.outputStream.  Get one per block written.
     * @return A compressing stream; if 'none' compression, returned stream
     * does not compress.
     * @throws IOException
     * @see {@link #releaseCompressingStream(DataOutputStream)}
     */
    private DataOutputStream getCompressingStream() throws IOException {
      this.mCompressor = mCompressAlgo.getCompressor();
      // Get new DOS compression stream.  In tfile, the DOS, is not closed,
      // just finished, and that seems to be fine over there.  TODO: Check
      // no memory retention of the DOS.  Should I disable the 'flush' on the
      // DOS as the BCFile over in tfile does?  It wants to make it so flushes
      // don't go through to the underlying compressed stream.  Flush on the
      // compressed downstream should be only when done.  I was going to but
      // looks like when we call flush in here, its legitimate flush that
      // should go through to the compressor.
      OutputStream os =
        this.mCompressAlgo.createCompressionStream(this.mOutputStream,
        this.mCompressor, 0);
      return new DataOutputStream(os);
    }

    /**
     * Let go of block compressor and compressing stream gotten in call
     * {@link #getCompressingStream}.
     * @param dos
     * @return How much was written on this stream since it was taken out.
     * @see #getCompressingStream()
     * @throws IOException
     */
    private int releaseCompressingStream(final DataOutputStream dos)
        throws IOException {
      dos.flush();
      this.mCompressAlgo.returnCompressor(this.mCompressor);
      this.mCompressor = null;
      return dos.size();
    }

    /**
     * Add a meta block to the end of the file. Call before close().
     * Metadata blocks are expensive.  Fill one with a bunch of serialized data
     * rather than do a metadata block per metadata instance.  If metadata is
     * small, consider adding to file info using
     * {@link #appendFileInfo(byte[], byte[])}
     * @param metaBlockName name of the block
     * @param content will call readFields to get data later (DO NOT REUSE)
     */
    public void appendMetaBlock(String metaBlockName, Writable content) {
      byte[] key = Bytes.toBytes(metaBlockName);
      int i;
      
      for (i = 0; i < mMetaNames.size(); ++i) {
        // stop when the current key is greater than our own
        byte[] cur = mMetaNames.get(i);
        if (this.mRawComparator.compare(cur, 0, cur.length, key, 0, key.length) > 0) {
          break;
        }
      }
      
      mMetaNames.add(i, key);
      mMetaData.add(i, content);
    }

    /**
     * Add to the file info.  Added key value can be gotten out of the return
     * from {@link Reader#loadFileInfo()}.
     * @param k Key
     * @param v Value
     * @throws IOException
     */
    public void appendFileInfo(final byte[] k, final byte[] v)
        throws IOException {
      appendFileInfo(this.mFileinfo, k, v, true);
    }

    static FileInfo appendFileInfo(FileInfo fi, final byte[] k, final byte[] v,
      final boolean checkPrefix) throws IOException {
      if (k == null || v == null) {
        throw new NullPointerException("Key nor value may be null");
      }
      if (checkPrefix && Bytes.startsWith(k, FileInfo.RESERVED_PREFIX_BYTES)) {
        throw new IOException("Keys with a " + FileInfo.RESERVED_PREFIX +
          " are reserved");
      }
      fi.put(k, v);
      return fi;
    }

    /**
     * @return Path or null if we were passed a stream rather than a Path.
     */
    public Path getPath() {
      return this.mPath;
    }

    @Override
    public String toString() {
      return "writer=" + this.mName + ", compression=" +
        this.mCompressAlgo.getName();
    }

    /**
     * Add key/value to file.
     * Keys must be added in an order that agrees with the Comparator passed
     * on construction.
     * @param kv KeyValue to add.  Cannot be empty nor null.
     * @throws IOException
     */
    public void append(final KeyValue kv)
        throws IOException {
      append(kv.getBuffer(), kv.getKeyOffset(), kv.getKeyLength(),
        kv.getBuffer(), kv.getValueOffset(), kv.getValueLength());
    }

    /**
     * Add key/value to file.
     * Keys must be added in an order that agrees with the Comparator passed
     * on construction.
     * @param key Key to add.  Cannot be empty nor null.
     * @param value Value to add.  Cannot be empty nor null.
     * @throws IOException
     */
    public void append(final byte[] key, final byte[] value)
        throws IOException {
      append(key, 0, key.length, value, 0, value.length);
    }

    /**
     * Add key/value to file.
     * Keys must be added in an order that agrees with the Comparator passed
     * on construction.
     * @param key
     * @param koffset
     * @param klength
     * @param value
     * @param voffset
     * @param vlength
     * @throws IOException
     */
    private void append(final byte[] key, final int koffset, final int klength,
        final byte[] value, final int voffset, final int vlength) throws IOException {
      boolean dupKey = checkKey(key, koffset, klength);
      checkValue(value, voffset, vlength);
      if (!dupKey) 
        checkBlockBoundary();
      
      // Write length of key and value and then actual key and value bytes.
      this.mOut.writeInt(klength);
      this.mKeylength += klength;
      this.mOut.writeInt(vlength);
      this.mValuelength += vlength;
      this.mOut.write(key, koffset, klength);
      this.mOut.write(value, voffset, vlength);
      
      // Are we the first key in this block?
      if (this.mFirstKey == null) {
        // Copy the key.
        this.mFirstKey = new byte [klength];
        System.arraycopy(key, koffset, this.mFirstKey, 0, klength);
      }
      
      this.mLastKeyBuffer = key;
      this.mLastKeyOffset = koffset;
      this.mLastKeyLength = klength;
      this.mEntryCount ++;
    }

    /**
     * @param key Key to check.
     * @return the flag of duplicate Key or not
     * @throws IOException
     */
    private boolean checkKey(final byte[] key, final int offset, final int length)
        throws IOException {
      boolean dupKey = false;

      if (key == null || length <= 0) {
        throw new IOException("Key cannot be null or empty");
      }
      if (length > MAXIMUM_KEY_LENGTH) {
        throw new IOException("Key length " + length + " > " +
          MAXIMUM_KEY_LENGTH);
      }
      
      if (this.mLastKeyBuffer != null) {
        int keyComp = this.mRawComparator.compare(
        	this.mLastKeyBuffer, this.mLastKeyOffset, this.mLastKeyLength, 
        	key, offset, length);
        
        if (keyComp > 0) {
          throw new IOException("Added a key not lexically larger than" +
            " previous key=" + Bytes.toStringBinary(key, offset, length) +
            ", lastkey=" + Bytes.toStringBinary(this.mLastKeyBuffer, this.mLastKeyOffset,
                this.mLastKeyLength));
          
        } else if (keyComp == 0) {
          dupKey = true;
        }
      }
      
      return dupKey;
    }

    private void checkValue(final byte[] value, final int offset,
        final int length) throws IOException {
      if (value == null) {
        throw new IOException("Value cannot be null");
      }
    }

    public long getTotalBytes() {
      return this.mTotalBytes;
    }

    @Override
    public void close() throws IOException {
      if (this.mOutputStream == null) 
        return;
      
      // Write out the end of the data blocks, then write meta data blocks.
      // followed by fileinfo, data block index and meta block index.
      finishBlock();

      FixedFileTrailer trailer = new FixedFileTrailer();

      // Write out the metadata blocks if any.
      ArrayList<Long> metaOffsets = null;
      ArrayList<Integer> metaDataSizes = null;
      
      if (mMetaNames.size() > 0) {
        metaOffsets = new ArrayList<Long>(mMetaNames.size());
        metaDataSizes = new ArrayList<Integer>(mMetaNames.size());
        
        for (int i = 0 ; i < mMetaNames.size() ; ++ i ) {
          // store the beginning offset
          long curPos = mOutputStream.getPos();
          metaOffsets.add(curPos);
          
          // write the metadata content
          DataOutputStream dos = getCompressingStream();
          dos.write(METABLOCKMAGIC);
          mMetaData.get(i).write(dos);
          
          int size = releaseCompressingStream(dos);
          // store the metadata size
          metaDataSizes.add(size);
        }
      }

      // Write fileinfo.
      trailer.mFileinfoOffset = writeFileInfo(this.mOutputStream);

      // Write the data block index.
      trailer.mDataIndexOffset = BlockIndex.writeIndex(this.mOutputStream,
        this.mBlockKeys, this.mBlockOffsets, this.mBlockDataSizes);

      // Meta block index.
      if (mMetaNames.size() > 0) {
        trailer.mMetaIndexOffset = BlockIndex.writeIndex(this.mOutputStream,
          this.mMetaNames, metaOffsets, metaDataSizes);
      }

      // Now finish off the trailer.
      trailer.mDataIndexCount = mBlockKeys.size();
      trailer.mMetaIndexCount = mMetaNames.size();

      trailer.mTotalUncompressedBytes = mTotalBytes;
      trailer.mEntryCount = mEntryCount;
      trailer.mCompressionCodec = this.mCompressAlgo.ordinal();

      trailer.serialize(mOutputStream);

      if (this.mCloseOutputStream) {
        this.mOutputStream.close();
        this.mOutputStream = null;
      }
    }

    /**
     * Add last bits of metadata to fileinfo and then write it out.
     * Reader will be expecting to find all below.
     * @param o Stream to write on.
     * @return Position at which we started writing.
     * @throws IOException
     */
    private long writeFileInfo(FSDataOutputStream o) throws IOException {
      if (this.mLastKeyBuffer != null) {
        // Make a copy.  The copy is stuffed into HMapWritable.  Needs a clean
        // byte buffer.  Won't take a tuple.
        byte[] b = new byte[this.mLastKeyLength];
        System.arraycopy(this.mLastKeyBuffer, this.mLastKeyOffset, b, 0,
          this.mLastKeyLength);
        
        appendFileInfo(this.mFileinfo, FileInfo.LASTKEY, b, false);
      }
      
      int avgKeyLen = this.mEntryCount == 0 ? 0 :
        (int)(this.mKeylength/this.mEntryCount);
      
      appendFileInfo(this.mFileinfo, FileInfo.AVG_KEY_LEN,
        Bytes.toBytes(avgKeyLen), false);
      
      int avgValueLen = this.mEntryCount == 0 ? 0 :
        (int)(this.mValuelength/this.mEntryCount);
      
      appendFileInfo(this.mFileinfo, FileInfo.AVG_VALUE_LEN,
        Bytes.toBytes(avgValueLen), false);
      appendFileInfo(this.mFileinfo, FileInfo.COMPARATOR,
        Bytes.toBytes(this.mRawComparator.getClass().getName()), false);
      
      long pos = o.getPos();
      this.mFileinfo.write(o);
      
      return pos;
    }
  }

  /**
   * DBFile Reader.
   */
  public static class Reader implements Closeable {
    // Stream to read from.
    private FSDataInputStream mIStream;
    // True if we should close istream when done.  We don't close it if we
    // didn't open it.
    private boolean mCloseIStream;

    // These are read in when the file info is loaded.
    private DBFile.BlockIndex mBlockIndex;
    private BlockIndex mMetaIndex;
    private FixedFileTrailer mTrailer;
    private volatile boolean mFileInfoLoaded = false;

    // Filled when we read in the trailer.
    private Compression.Algorithm mCompressAlgo;

    // Last key in the file.  Filled in when we read in the file info
    private byte[] mLastkey = null;
    // Stats read in when we load file info.
    private int mAvgKeyLen = -1;
    private int mAvgValueLen = -1;

    // Used to ensure we seek correctly.
    private RawComparator<byte[]> mComparator;

    // Size of this file.
    private final long mFileSize;

    // Block cache to use.
    private final BlockCache mCache;
    
    @SuppressWarnings("unused")
	private int mCacheHits = 0;
    @SuppressWarnings("unused")
	private int mBlockLoads = 0;
    @SuppressWarnings("unused")
	private int mMetaLoads = 0;

    // Whether file is from in-memory store
    private boolean mInMemory = false;

    // Name for this object used when logging or in toString.  Is either
    // the result of a toString on the stream or else is toString of passed
    // file Path plus metadata key/value pairs.
    private String mName;

    /**
     * Opens a DBFile.  You must load the file info before you can
     * use it by calling {@link #loadFileInfo()}.
     *
     * @param fs filesystem to load from
     * @param path path within said filesystem
     * @param cache block cache. Pass null if none.
     * @throws IOException
     */
    public Reader(FileSystem fs, Path path, BlockCache cache, 
    	boolean inMemory) throws IOException {
      this(fs.open(path), fs.getFileStatus(path).getLen(), cache, inMemory);
      this.mCloseIStream = true;
      this.mName = path.toString();
    }

    /**
     * Opens a DBFile.  You must load the index before you can
     * use it by calling {@link #loadFileInfo()}.
     *
     * @param fsdis input stream.  Caller is responsible for closing the passed
     * stream.
     * @param size Length of the stream.
     * @param cache block cache. Pass null if none.
     * @throws IOException
     */
    public Reader(final FSDataInputStream fsdis, final long size,
        final BlockCache cache, final boolean inMemory) {
      this.mCache = cache;
      this.mFileSize = size;
      this.mIStream = fsdis;
      this.mCloseIStream = false;
      this.mName = (this.mIStream == null) ? "" : this.mIStream.toString();
      this.mInMemory = inMemory;
    }

    @Override
    public String toString() {
      return "reader=" + this.mName +
          (!isFileInfoLoaded() ? "":
            ", compression=" + this.mCompressAlgo.getName() +
            ", inMemory=" + this.mInMemory +
            ", firstKey=" + toStringFirstKey() +
            ", lastKey=" + toStringLastKey()) +
            ", avgKeyLen=" + this.mAvgKeyLen +
            ", avgValueLen=" + this.mAvgValueLen +
            ", entries=" + this.mTrailer.mEntryCount +
            ", length=" + this.mFileSize;
    }

    protected String toStringFirstKey() {
      return KeyValue.keyToString(getFirstKey());
    }

    protected String toStringLastKey() {
      return KeyValue.keyToString(getLastKey());
    }

    public long length() {
      return this.mFileSize;
    }

    public boolean inMemory() {
      return this.mInMemory;
    }

    /**
     * Read in the index and file info.
     * @return A map of fileinfo data.
     * See {@link Writer#appendFileInfo(byte[], byte[])}.
     * @throws IOException
     */
    public Map<byte[], byte[]> loadFileInfo()
       throws IOException {
      this.mTrailer = readTrailer();

      // Read in the fileinfo and get what we need from it.
      this.mIStream.seek(this.mTrailer.mFileinfoOffset);
      
      FileInfo fi = new FileInfo();
      fi.readFields(this.mIStream);
      
      this.mLastkey = fi.get(FileInfo.LASTKEY);
      this.mAvgKeyLen = Bytes.toInt(fi.get(FileInfo.AVG_KEY_LEN));
      this.mAvgValueLen = Bytes.toInt(fi.get(FileInfo.AVG_VALUE_LEN));
      String clazzName = Bytes.toString(fi.get(FileInfo.COMPARATOR));
      this.mComparator = getComparator(clazzName);

      // Read in the data index.
      this.mBlockIndex = BlockIndex.readIndex(this.mComparator, this.mIStream,
        this.mTrailer.mDataIndexOffset, this.mTrailer.mDataIndexCount);

      // Read in the metadata index.
      if (mTrailer.mMetaIndexCount > 0) {
        this.mMetaIndex = BlockIndex.readIndex(Bytes.BYTES_RAWCOMPARATOR,
          this.mIStream, this.mTrailer.mMetaIndexOffset, this.mTrailer.mMetaIndexCount);
      }
      
      this.mFileInfoLoaded = true;
      return fi;
    }

    protected boolean isFileInfoLoaded() {
      return this.mFileInfoLoaded;
    }

    @SuppressWarnings("unchecked")
    private RawComparator<byte[]> getComparator(final String clazzName)
        throws IOException {
      if (clazzName == null || clazzName.length() == 0) 
        return null;
      
      try {
        return (RawComparator<byte[]>)Class.forName(clazzName).newInstance();
      } catch (InstantiationException e) {
        throw new IOException(e);
      } catch (IllegalAccessException e) {
        throw new IOException(e);
      } catch (ClassNotFoundException e) {
        throw new IOException(e);
      }
    }

    /**
     * Read the trailer off the input stream.  As side effect, sets the
     * compression algorithm.
     * @return Populated FixedFileTrailer.
     * @throws IOException
     */
    private FixedFileTrailer readTrailer() throws IOException {
      FixedFileTrailer fft = new FixedFileTrailer();
      long seekPoint = this.mFileSize - FixedFileTrailer.trailerSize();
      this.mIStream.seek(seekPoint);
      fft.deserialize(this.mIStream);
      // Set up the codec.
      this.mCompressAlgo =
        Compression.Algorithm.values()[fft.mCompressionCodec];
      return fft;
    }

    /**
     * Create a Scanner on this file.  No seeks or reads are done on creation.
     * Call {@link DBFileScanner#seekTo(byte[])} to position an start the read.
     * There is nothing to clean up in a Scanner. Letting go of your references
     * to the scanner is sufficient.
     * @param pread Use positional read rather than seek+read if true (pread is
     * better for random reads, seek+read is better scanning).
     * @param cacheBlocks True if we should cache blocks read in by this scanner.
     * @return Scanner on this file.
     */
    public DBFileScanner getScanner(boolean cacheBlocks, final boolean pread) {
      return new Scanner(this, cacheBlocks, pread);
    }

    /**
     * @param key Key to search.
     * @return Block number of the block containing the key or -1 if not in this
     * file.
     */
    protected int blockContainingKey(final byte[] key, int offset, int length) {
      if (mBlockIndex == null) 
        throw new RuntimeException("Block index not loaded");
      
      return mBlockIndex.blockContainingKey(key, offset, length);
    }
    /**
     * @param metaBlockName
     * @param cacheBlock Add block to cache, if found
     * @return Block wrapped in a ByteBuffer
     * @throws IOException
     */
    public ByteBuffer getMetaBlock(String metaBlockName, boolean cacheBlock)
        throws IOException {
      if (mTrailer.mMetaIndexCount == 0) 
        return null; // there are no meta blocks
      
      if (mMetaIndex == null) 
        throw new IOException("Meta index not loaded");
      
      byte[] mbname = Bytes.toBytes(metaBlockName);
      int block = mMetaIndex.blockContainingKey(mbname, 0, mbname.length);
      if (block == -1)
        return null;
      
      long blockSize;
      if (block == mMetaIndex.mCount - 1) {
        blockSize = mTrailer.mFileinfoOffset - mMetaIndex.mBlockOffsets[block];
      } else {
        blockSize = mMetaIndex.mBlockOffsets[block+1] - mMetaIndex.mBlockOffsets[block];
      }

      long now = System.currentTimeMillis();

      // Per meta key from any given file, synchronize reads for said block
      synchronized (mMetaIndex.mBlockKeys[block]) {
        mMetaLoads++;
        
        // Check cache for block.  If found return.
        if (mCache != null) {
          ByteBuffer cachedBuf = mCache.getBlock(mName + "meta" + block);
          if (cachedBuf != null) {
            // Return a distinct 'shallow copy' of the block,
            // so pos doesnt get messed by the scanner
            mCacheHits++;
            
            return cachedBuf.duplicate();
          }
          // Cache Miss, please load.
        }

        ByteBuffer buf = decompress(mMetaIndex.mBlockOffsets[block],
          longToInt(blockSize), mMetaIndex.mBlockDataSizes[block], true);
        
        byte[] magic = new byte[METABLOCKMAGIC.length];
        buf.get(magic, 0, magic.length);

        if (!Arrays.equals(magic, METABLOCKMAGIC)) 
          throw new IOException("Meta magic is bad in block " + block);

        // Create a new ByteBuffer 'shallow copy' to hide the magic header
        buf = buf.slice();

        sReadTime += System.currentTimeMillis() - now;
        sReadOps++;

        // Cache the block
        if (cacheBlock && mCache != null) {
          mCache.cacheBlock(mName + "meta" + block, buf.duplicate(), mInMemory);
        }

        return buf;
      }
    }

    /**
     * Read in a file block.
     * @param block Index of block to read.
     * @param pread Use positional read instead of seek+read (positional is
     * better doing random reads whereas seek+read is better scanning).
     * @return Block wrapped in a ByteBuffer.
     * @throws IOException
     */
    protected ByteBuffer readBlock(int block, boolean cacheBlock, final boolean pread)
        throws IOException {
      if (mBlockIndex == null) {
        throw new IOException("Block index not loaded");
      }
      if (block < 0 || block >= mBlockIndex.mCount) {
        throw new IOException("Requested block is out of range: " + block +
          ", max: " + mBlockIndex.mCount);
      }
      
      // For any given block from any given file, synchronize reads for said
      // block.
      // Without a cache, this synchronizing is needless overhead, but really
      // the other choice is to duplicate work (which the cache would prevent you from doing).
      synchronized (mBlockIndex.mBlockKeys[block]) {
        mBlockLoads++;
        
        // Check cache for block.  If found return.
        if (mCache != null) {
          ByteBuffer cachedBuf = mCache.getBlock(mName + block);
          if (cachedBuf != null) {
            // Return a distinct 'shallow copy' of the block,
            // so pos doesnt get messed by the scanner
            mCacheHits++;
            
            return cachedBuf.duplicate();
          }
          // Carry on, please load.
        }

        // Load block from filesystem.
        long now = System.currentTimeMillis();
        long onDiskBlockSize;
        
        if (block == mBlockIndex.mCount - 1) {
          // last block!  The end of data block is first meta block if there is
          // one or if there isn't, the fileinfo offset.
          long offset = (this.mMetaIndex != null) ?
            this.mMetaIndex.mBlockOffsets[0]: this.mTrailer.mFileinfoOffset;
          onDiskBlockSize = offset - mBlockIndex.mBlockOffsets[block];
        } else {
          onDiskBlockSize = mBlockIndex.mBlockOffsets[block+1] -
          mBlockIndex.mBlockOffsets[block];
        }
        
        ByteBuffer buf = decompress(mBlockIndex.mBlockOffsets[block],
          longToInt(onDiskBlockSize), this.mBlockIndex.mBlockDataSizes[block],
          pread);

        byte[] magic = new byte[DATABLOCKMAGIC.length];
        buf.get(magic, 0, magic.length);
        
        if (!Arrays.equals(magic, DATABLOCKMAGIC)) 
          throw new IOException("Data magic is bad in block " + block);

        // 'shallow copy' to hide the header
        // NOTE: you WILL GET BIT if you call buf.array() but don't start
        //       reading at buf.arrayOffset()
        buf = buf.slice();

        sReadTime += System.currentTimeMillis() - now;
        sReadOps++;

        // Cache the block
        if (cacheBlock && mCache != null) {
          mCache.cacheBlock(mName + block, buf.duplicate(), mInMemory);
        }

        return buf;
      }
    }

    /**
     * Decompress <code>compressedSize</code> bytes off the backing
     * FSDataInputStream.
     * @param offset
     * @param compressedSize
     * @param decompressedSize
     *
     * @return
     * @throws IOException
     */
    private ByteBuffer decompress(final long offset, final int compressedSize,
      final int decompressedSize, final boolean pread) throws IOException {
      Decompressor decompressor = null;
      ByteBuffer buf = null;
      
      try {
        decompressor = this.mCompressAlgo.getDecompressor();
        // My guess is that the bounded range fis is needed to stop the
        // decompressor reading into next block -- IIRC, it just grabs a
        // bunch of data w/o regard to whether decompressor is coming to end of a
        // decompression.

        // We use a buffer of DEFAULT_BLOCKSIZE size.  This might be extreme.
        // Could maybe do with less. Study and figure it: TODO
        InputStream is = this.mCompressAlgo.createDecompressionStream(
            new BufferedInputStream(
              new BoundedRangeFileInputStream(this.mIStream, offset, compressedSize, pread),
              Math.min(DEFAULT_BLOCKSIZE, compressedSize)),
            decompressor, 0);
        
        buf = ByteBuffer.allocate(decompressedSize);
        IOUtils.readFully(is, buf.array(), 0, buf.capacity());
        is.close();
        
      } finally {
        if (null != decompressor) {
          this.mCompressAlgo.returnDecompressor(decompressor);
        }
      }
      
      return buf;
    }

    /**
     * @return First key in the file.  May be null if file has no entries.
     * Note that this is not the first rowkey, but rather the byte form of
     * the first KeyValue.
     */
    public byte[] getFirstKey() {
      if (mBlockIndex == null) 
        throw new RuntimeException("Block index not loaded");
      
      return this.mBlockIndex.isEmpty() ? null : this.mBlockIndex.mBlockKeys[0];
    }

    /**
     * @return the first row key, or null if the file is empty.
     * TODO move this to StoreFile after Ryan's patch goes in
     * to eliminate KeyValue here
     */
    public byte[] getFirstRowKey() {
      byte[] firstKey = getFirstKey();
      if (firstKey == null) return null;
      return KeyValue.createKeyValueFromKey(firstKey).getRow();
    }

    /**
     * @return number of KV entries in this DBFile
     */
    public int getEntries() {
      if (!this.isFileInfoLoaded()) 
        throw new RuntimeException("File info not loaded");
      
      return this.mTrailer.mEntryCount;
    }

    /**
     * @return Last key in the file.  May be null if file has no entries.
     * Note that this is not the last rowkey, but rather the byte form of
     * the last KeyValue.
     */
    public byte[] getLastKey() {
      if (!isFileInfoLoaded()) 
        throw new RuntimeException("Load file info first");
      
      return this.mBlockIndex.isEmpty() ? null : this.mLastkey;
    }

    /**
     * @return the last row key, or null if the file is empty.
     * TODO move this to StoreFile after Ryan's patch goes in
     * to eliminate KeyValue here
     */
    public byte[] getLastRowKey() {
      byte[] lastKey = getLastKey();
      if (lastKey == null) return null;
      return KeyValue.createKeyValueFromKey(lastKey).getRow();
    }

    /**
     * @return number of K entries in this DBFile's filter.  Returns KV count if no filter.
     */
    public int getFilterEntries() {
      return getEntries();
    }

    /**
     * @return Comparator.
     */
    public RawComparator<byte[]> getComparator() {
      return this.mComparator;
    }

    /**
     * @return index size
     */
    public long indexSize() {
      return (this.mBlockIndex != null? this.mBlockIndex.heapSize(): 0) +
        ((this.mMetaIndex != null)? this.mMetaIndex.heapSize(): 0);
    }

    /**
     * @return Midkey for this file.  We work with block boundaries only so
     * returned midkey is an approximation only.
     * @throws IOException
     */
    public byte[] midkey() throws IOException {
      if (!isFileInfoLoaded() || this.mBlockIndex.isEmpty()) 
        return null;
      
      return this.mBlockIndex.midkey();
    }

    @Override
    public void close() throws IOException {
      if (this.mCloseIStream && this.mIStream != null) {
        this.mIStream.close();
        this.mIStream = null;
      }
    }

    public String getName() {
      return mName;
    }

    /**
     * Implementation of {@link DBFileScanner} interface.
     */
    protected static class Scanner implements DBFileScanner {
      private final Reader mReader;
      private ByteBuffer mBlock;
      private int mCurrBlock;

      private final boolean mCacheBlocks;
      private final boolean mPread;

      private int mCurrKeyLen = 0;
      private int mCurrValueLen = 0;

      public int mBlockFetches = 0;

      public Scanner(Reader r, boolean cacheBlocks, final boolean pread) {
        this.mReader = r;
        this.mCacheBlocks = cacheBlocks;
        this.mPread = pread;
      }

      public KeyValue getKeyValue() {
        if (this.mBlock == null) 
          return null;
        
        return new KeyValue(this.mBlock.array(),
            this.mBlock.arrayOffset() + this.mBlock.position() - 8,
            this.mCurrKeyLen + this.mCurrValueLen+8);
      }

      public ByteBuffer getKey() {
        if (this.mBlock == null || this.mCurrKeyLen == 0) 
          throw new RuntimeException("you need to seekTo() before calling getKey()");
        
        ByteBuffer keyBuff = this.mBlock.slice();
        keyBuff.limit(this.mCurrKeyLen);
        keyBuff.rewind();
        
        // Do keyBuff.asReadOnly()?
        return keyBuff;
      }

      public ByteBuffer getValue() {
        if (mBlock == null || mCurrKeyLen == 0) 
          throw new RuntimeException("you need to seekTo() before calling getValue()");
        
        // TODO: Could this be done with one ByteBuffer rather than create two?
        ByteBuffer valueBuff = this.mBlock.slice();
        valueBuff.position(this.mCurrKeyLen);
        valueBuff = valueBuff.slice();
        valueBuff.limit(mCurrValueLen);
        valueBuff.rewind();
        
        return valueBuff;
      }

      @Override
      public boolean next() throws IOException {
        if (mBlock == null) 
          throw new IOException("Next called on non-seeked scanner");
        
        mBlock.position(mBlock.position() + mCurrKeyLen + mCurrValueLen);
        
        if (mBlock.remaining() <= 0) {
          mCurrBlock++;
          
          if (mCurrBlock >= mReader.mBlockIndex.mCount) {
            // damn we are at the end
            mCurrBlock = 0;
            mBlock = null;
            
            return false;
          }
          
          mBlock = mReader.readBlock(this.mCurrBlock, this.mCacheBlocks, this.mPread);
          mCurrKeyLen = Bytes.toInt(mBlock.array(), mBlock.arrayOffset()+mBlock.position(), 4);
          mCurrValueLen = Bytes.toInt(mBlock.array(), mBlock.arrayOffset()+mBlock.position()+4, 4);
          mBlock.position(mBlock.position()+8);
          
          mBlockFetches++;
          
          return true;
        }
        
        mCurrKeyLen = Bytes.toInt(mBlock.array(), mBlock.arrayOffset()+mBlock.position(), 4);
        mCurrValueLen = Bytes.toInt(mBlock.array(), mBlock.arrayOffset()+mBlock.position()+4, 4);
        mBlock.position(mBlock.position()+8);
        
        return true;
      }

      @Override
      public int seekTo(byte[] key) throws IOException {
        return seekTo(key, 0, key.length);
      }

      @Override
      public int seekTo(byte[] key, int offset, int length) throws IOException {
        int b = mReader.blockContainingKey(key, offset, length);
        if (b < 0) return -1; // falls before the beginning of the file! :-(
        // Avoid re-reading the same block (that'd be dumb).
        loadBlock(b, true);
        return blockSeek(key, offset, length, false);
      }

      @Override
      public int reseekTo(byte[] key) throws IOException {
        return reseekTo(key, 0, key.length);
      }

      @Override
      public int reseekTo(byte[] key, int offset, int length)
          throws IOException {
        if (this.mBlock != null && this.mCurrKeyLen != 0) {
          ByteBuffer bb = getKey();
          int compared = this.mReader.mComparator.compare(key, offset, length,
              bb.array(), bb.arrayOffset(), bb.limit());
          
          if (compared < 1) {
            //If the required key is less than or equal to current key, then
            //don't do anything.
            return compared;
          }
        }

        int b = mReader.blockContainingKey(key, offset, length);
        if (b < 0) 
          return -1;
        
        loadBlock(b, false);
        return blockSeek(key, offset, length, false);
      }

      /**
       * Within a loaded block, seek looking for the first key
       * that is smaller than (or equal to?) the key we are interested in.
       *
       * A note on the seekBefore - if you have seekBefore = true, AND the
       * first key in the block = key, then you'll get thrown exceptions.
       * @param key to find
       * @param seekBefore find the key before the exact match.
       * @return
       */
      private int blockSeek(byte[] key, int offset, int length, 
    	  boolean seekBefore) {
        int klen, vlen;
        int lastLen = 0;
        
        do {
          klen = mBlock.getInt();
          vlen = mBlock.getInt();
          
          int comp = this.mReader.mComparator.compare(key, offset, length,
            mBlock.array(), mBlock.arrayOffset() + mBlock.position(), klen);
          
          if (comp == 0) {
            if (seekBefore) {
              mBlock.position(mBlock.position() - lastLen - 16);
              mCurrKeyLen = mBlock.getInt();
              mCurrValueLen = mBlock.getInt();
              
              return 1; // non exact match.
            }
            
            mCurrKeyLen = klen;
            mCurrValueLen = vlen;
            
            return 0; // indicate exact match
          }
          
          if (comp < 0) {
            // go back one key:
            mBlock.position(mBlock.position() - lastLen - 16);
            mCurrKeyLen = mBlock.getInt();
            mCurrValueLen = mBlock.getInt();
            
            return 1;
          }
          
          mBlock.position(mBlock.position() + klen + vlen);
          lastLen = klen + vlen ;
        } while (mBlock.remaining() > 0);
        
        // ok we are at the end, so go back a littleeeeee....
        // The 8 in the below is intentionally different to the 16s in the above
        // Do the math you you'll figure it.
        mBlock.position(mBlock.position() - lastLen - 8);
        mCurrKeyLen = mBlock.getInt();
        mCurrValueLen = mBlock.getInt();
        
        return 1; // didn't exactly find it.
      }

      @Override
      public boolean seekBefore(byte[] key) throws IOException {
        return seekBefore(key, 0, key.length);
      }

      @Override
      public boolean seekBefore(byte[] key, int offset, int length)
          throws IOException {
        int b = mReader.blockContainingKey(key, offset, length);
        if (b < 0)
          return false; // key is before the start of the file.

        // Question: does this block begin with 'key'?
        if (this.mReader.mComparator.compare(mReader.mBlockIndex.mBlockKeys[b],
            0, mReader.mBlockIndex.mBlockKeys[b].length,
            key, offset, length) == 0) {
          // Ok the key we're interested in is the first of the block, so go back one.
          if (b == 0) {
            // we have a 'problem', the key we want is the first of the file.
            return false;
          }
          
          b--;
          // TODO shortcut: seek forward in this block to the last key of the block.
        }
        
        loadBlock(b, true);
        blockSeek(key, offset, length, true);
        
        return true;
      }

      public String getKeyString() {
        return Bytes.toStringBinary(mBlock.array(), mBlock.arrayOffset() +
          mBlock.position(), mCurrKeyLen);
      }

      public String getValueString() {
        return Bytes.toString(mBlock.array(), mBlock.arrayOffset() +
          mBlock.position() + mCurrKeyLen, mCurrValueLen);
      }

      public Reader getReader() {
        return this.mReader;
      }

      public boolean isSeeked(){
        return this.mBlock != null;
      }

      @Override
      public boolean seekTo() throws IOException {
        if (this.mReader.mBlockIndex.isEmpty()) 
          return false;
        
        if (mBlock != null && mCurrBlock == 0) {
          mBlock.rewind();
          mCurrKeyLen = mBlock.getInt();
          mCurrValueLen = mBlock.getInt();
          
          return true;
        }
        
        mCurrBlock = 0;
        mBlock = mReader.readBlock(this.mCurrBlock, this.mCacheBlocks, this.mPread);
        mCurrKeyLen = mBlock.getInt();
        mCurrValueLen = mBlock.getInt();
        mBlockFetches++;
        
        return true;
      }

      private void loadBlock(int bloc, boolean rewind) throws IOException {
        if (mBlock == null) {
          mBlock = mReader.readBlock(bloc, this.mCacheBlocks, this.mPread);
          mCurrBlock = bloc;
          mBlockFetches++;
          
        } else {
          if (bloc != mCurrBlock) {
            mBlock = mReader.readBlock(bloc, this.mCacheBlocks, this.mPread);
            mCurrBlock = bloc;
            mBlockFetches++;
            
          } else {
            // we are already in the same block, just rewind to seek again.
            if (rewind) {
              mBlock.rewind();
            } else {
              //Go back by (size of rowlength + size of valuelength) = 8 bytes
              mBlock.position(mBlock.position()-8);
            }
          }
        }
      }

      @Override
      public String toString() {
        return "DBFileScanner for reader " + String.valueOf(mReader);
      }
    }

    public String getTrailerInfo() {
      return mTrailer.toString();
    }
  }

  /**
   * The RFile has a fixed trailer which contains offsets to other variable
   * parts of the file.  Also includes basic metadata on this file.
   */
  private static class FixedFileTrailer {
    // Offset to the fileinfo data, a small block of vitals..
    private long mFileinfoOffset;
    // Offset to the data block index.
    private long mDataIndexOffset;
    // How many index counts are there (aka: block count)
    private int mDataIndexCount;
    // Offset to the meta block index.
    private long mMetaIndexOffset;
    // How many meta block index entries (aka: meta block count)
    private int mMetaIndexCount;
    private long mTotalUncompressedBytes;
    private int mEntryCount;
    private int mCompressionCodec;
    private int mVersion = 1;

    FixedFileTrailer() {
      super();
    }

    static int trailerSize() {
      // Keep this up to date...
      return
        ( Bytes.SIZEOF_INT * 5 ) +
        ( Bytes.SIZEOF_LONG * 4 ) +
        TRAILERBLOCKMAGIC.length;
    }

    public void serialize(DataOutputStream outputStream) throws IOException {
      outputStream.write(TRAILERBLOCKMAGIC);
      outputStream.writeLong(mFileinfoOffset);
      outputStream.writeLong(mDataIndexOffset);
      outputStream.writeInt(mDataIndexCount);
      outputStream.writeLong(mMetaIndexOffset);
      outputStream.writeInt(mMetaIndexCount);
      outputStream.writeLong(mTotalUncompressedBytes);
      outputStream.writeInt(mEntryCount);
      outputStream.writeInt(mCompressionCodec);
      outputStream.writeInt(mVersion);
    }

    public void deserialize(DataInputStream inputStream) throws IOException {
      byte[] header = new byte[TRAILERBLOCKMAGIC.length];
      inputStream.readFully(header);
      
      if ( !Arrays.equals(header, TRAILERBLOCKMAGIC)) {
        throw new IOException("Trailer 'header' is wrong; does the trailer " +
          "size match content?");
      }
      
      mFileinfoOffset         = inputStream.readLong();
      mDataIndexOffset        = inputStream.readLong();
      mDataIndexCount         = inputStream.readInt();

      mMetaIndexOffset        = inputStream.readLong();
      mMetaIndexCount         = inputStream.readInt();

      mTotalUncompressedBytes = inputStream.readLong();
      mEntryCount             = inputStream.readInt();
      mCompressionCodec       = inputStream.readInt();
      mVersion                = inputStream.readInt();

      if (mVersion != 1) 
        throw new IOException("Wrong version: " + mVersion);
    }

    @Override
    public String toString() {
      return "fileinfoOffset=" + mFileinfoOffset +
             ", dataIndexOffset=" + mDataIndexOffset +
             ", dataIndexCount=" + mDataIndexCount +
             ", metaIndexOffset=" + mMetaIndexOffset +
             ", metaIndexCount=" + mMetaIndexCount +
             ", totalBytes=" + mTotalUncompressedBytes +
             ", entryCount=" + mEntryCount +
             ", version=" + mVersion;
    }
  }

  /**
   * The block index for a RFile.
   * Used reading.
   */
  static class BlockIndex implements HeapSize {
    // How many actual items are there? The next insert location too.
    private int mCount = 0;
    private byte[][] mBlockKeys;
    private long[] mBlockOffsets;
    private int[] mBlockDataSizes;
    @SuppressWarnings("unused")
	private int mSize = 0;

    /** Needed doing lookup on blocks.
     */
    private final RawComparator<byte[]> mComparator;

    /**
     * Shutdown default constructor
     */
    @SuppressWarnings("unused")
    private BlockIndex() {
      this(null);
    }

    /**
     * @param c comparator used to compare keys.
     */
    BlockIndex(final RawComparator<byte[]>c) {
      this.mComparator = c;
      // Guess that cost of three arrays + this object is 4 * 8 bytes.
      this.mSize += (4 * 8);
    }

    /**
     * @return True if block index is empty.
     */
    public boolean isEmpty() {
      return this.mBlockKeys.length <= 0;
    }

    /**
     * Adds a new entry in the block index.
     *
     * @param key Last key in the block
     * @param offset file offset where the block is stored
     * @param dataSize the uncompressed data size
     */
    public void add(final byte[] key, final long offset, final int dataSize) {
      mBlockOffsets[mCount] = offset;
      mBlockKeys[mCount] = key;
      mBlockDataSizes[mCount] = dataSize;
      mCount++;
      mSize += (Bytes.SIZEOF_INT * 2 + key.length);
    }

    /**
     * @param key Key to find
     * @return Offset of block containing <code>key</code> or -1 if this file
     * does not contain the request.
     */
    public int blockContainingKey(final byte[] key, int offset, int length) {
      int pos = Bytes.binarySearch(mBlockKeys, key, offset, length, this.mComparator);
      if (pos < 0) {
        pos ++;
        pos *= -1;
        
        if (pos == 0) {
          // falls before the beginning of the file.
          return -1;
        }
        
        // When switched to "first key in block" index, binarySearch now returns
        // the block with a firstKey < key.  This means the value we want is potentially
        // in the next block.
        pos --; // in previous block.

        return pos;
      }
      
      // wow, a perfect hit, how unlikely?
      return pos;
    }

    /**
     * @return File midkey.  Inexact.  Operates on block boundaries.  Does
     * not go into blocks.
     */
    public byte[] midkey() throws IOException {
      int pos = ((this.mCount - 1)/2);  // middle of the index
      if (pos < 0) {
        throw new IOException("DBFile empty");
      }
      return this.mBlockKeys[pos];
    }

    /**
     * Write out index. Whatever we write here must jibe with what
     * BlockIndex#readIndex is expecting.  Make sure the two ends of the
     * index serialization match.
     * @param o
     * @param keys
     * @param offsets
     * @param sizes
     * @param c
     * @return Position at which we entered the index.
     * @throws IOException
     */
    static long writeIndex(final FSDataOutputStream o,
        final List<byte[]> keys, final List<Long> offsets,
        final List<Integer> sizes) throws IOException {
      long pos = o.getPos();
      
      // Don't write an index if nothing in the index.
      if (keys.size() > 0) {
        o.write(INDEXBLOCKMAGIC);
        
        // Write the index.
        for (int i = 0; i < keys.size(); ++i) {
          o.writeLong(offsets.get(i).longValue());
          o.writeInt(sizes.get(i).intValue());
          
          byte[] key = keys.get(i);
          Bytes.writeByteArray(o, key);
        }
      }
      
      return pos;
    }

    /**
     * Read in the index that is at <code>indexOffset</code>
     * Must match what was written by writeIndex in the Writer.close.
     * @param in
     * @param indexOffset
     * @throws IOException
     */
    static BlockIndex readIndex(final RawComparator<byte[]> c,
        final FSDataInputStream in, final long indexOffset, final int indexSize)
        throws IOException {
      BlockIndex bi = new BlockIndex(c);
      bi.mBlockOffsets = new long[indexSize];
      bi.mBlockKeys = new byte[indexSize][];
      bi.mBlockDataSizes = new int[indexSize];
      
      // If index size is zero, no index was written.
      if (indexSize > 0) {
        in.seek(indexOffset);
        byte[] magic = new byte[INDEXBLOCKMAGIC.length];
        IOUtils.readFully(in, magic, 0, magic.length);
        
        if (!Arrays.equals(magic, INDEXBLOCKMAGIC)) {
          throw new IOException("Index block magic is wrong: " +
            Arrays.toString(magic));
        }
        
        for (int i = 0; i < indexSize; ++i ) {
          long offset   = in.readLong();
          int dataSize  = in.readInt();
          
          byte[] key = Bytes.readByteArray(in);
          bi.add(key, offset, dataSize);
        }
      }
      
      return bi;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("size=" + mCount);
      for (int i = 0; i < mCount ; i++) {
        sb.append(", ");
        sb.append("key=").append(Bytes.toStringBinary(mBlockKeys[i]))
          .append(", offset=").append(mBlockOffsets[i])
          .append(", dataSize=" + mBlockDataSizes[i]);
      }
      return sb.toString();
    }

    @Override
    public long heapSize() {
      long heapsize = ClassSize.align(ClassSize.OBJECT +
          2 * Bytes.SIZEOF_INT + (3 + 1) * ClassSize.REFERENCE);
      
      //Calculating the size of blockKeys
      if (mBlockKeys != null) {
        //Adding array + references overhead
        heapsize += ClassSize.align(ClassSize.ARRAY +
            mBlockKeys.length * ClassSize.REFERENCE);
        
        //Adding bytes
        for (byte[] bs : mBlockKeys) {
          heapsize += ClassSize.align(ClassSize.ARRAY + bs.length);
        }
      }
      
      if (mBlockOffsets != null) {
        heapsize += ClassSize.align(ClassSize.ARRAY +
            mBlockOffsets.length * Bytes.SIZEOF_LONG);
      }
      if (mBlockDataSizes != null) {
        heapsize += ClassSize.align(ClassSize.ARRAY +
            mBlockDataSizes.length * Bytes.SIZEOF_INT);
      }

      return ClassSize.align(heapsize);
    }

  }

  /**
   * Metadata for this file.  Conjured by the writer.  Read in by the reader.
   */
  static class FileInfo extends DBMapWritable<byte[], byte[]> {
    static final String RESERVED_PREFIX = "dbfile.";
    static final byte[] RESERVED_PREFIX_BYTES = Bytes.toBytes(RESERVED_PREFIX);
    static final byte[] LASTKEY = Bytes.toBytes(RESERVED_PREFIX + "LASTKEY");
    static final byte[] AVG_KEY_LEN = Bytes.toBytes(RESERVED_PREFIX + "AVG_KEY_LEN");
    static final byte[] AVG_VALUE_LEN = Bytes.toBytes(RESERVED_PREFIX + "AVG_VALUE_LEN");
    static final byte[] COMPARATOR = Bytes.toBytes(RESERVED_PREFIX + "COMPARATOR");

    /**
     * Constructor.
     */
    FileInfo() {
      super();
    }
  }

  /**
   * Return true if the given file info key is reserved for internal
   * use by DBFile.
   */
  public static boolean isReservedFileInfoKey(byte[] key) {
    return Bytes.startsWith(key, FileInfo.RESERVED_PREFIX_BYTES);
  }

  /**
   * Get names of supported compression algorithms. The names are acceptable by
   * DBFile.Writer.
   *
   * @return Array of strings, each represents a supported compression
   *         algorithm. Currently, the following compression algorithms are
   *         supported.
   *         <ul>
   *         <li>"none" - No compression.
   *         <li>"gz" - GZIP compression.
   *         </ul>
   */
  public static String[] getSupportedCompressionAlgorithms() {
    return Compression.getSupportedAlgorithms();
  }

  // Utility methods.
  /**
   * @param l Long to convert to an int.
   * @return <code>l</code> cast as an int.
   */
  static int longToInt(final long l) {
    // Expecting the size() of a block not exceeding 4GB. Assuming the
    // size() will wrap to negative integer if it exceeds 2GB (From tfile).
    return (int)(l & 0x00000000ffffffffL);
  }

  /**
   * Returns all files belonging to the given region directory. Could return an
   * empty list.
   *
   * @param fs  The file system reference.
   * @param regionDir  The region directory to scan.
   * @return The list of files found.
   * @throws IOException When scanning the files fails.
   */
  static List<Path> getStoreFiles(FileSystem fs, Path regionDir)
      throws IOException {
    List<Path> res = new ArrayList<Path>();
    PathFilter dirFilter = new DirFilter(fs);
    
    FileStatus[] familyDirs = fs.listStatus(regionDir, dirFilter);
    
    for(FileStatus dir : familyDirs) {
      FileStatus[] files = fs.listStatus(dir.getPath());
      
      for (FileStatus file : files) {
        if (!file.isDir()) 
          res.add(file.getPath());
      }
    }
    
    return res;
  }

  /**
   * A {@link PathFilter} that returns directories.
   */
  static class DirFilter implements PathFilter {
    private final FileSystem mFs;

    public DirFilter(final FileSystem fs) {
      this.mFs = fs;
    }

    @Override
    public boolean accept(Path p) {
      boolean isdir = false;
      try {
        isdir = this.mFs.getFileStatus(p).isDir();
      } catch (IOException e) {
        e.printStackTrace();
      }
      return isdir;
    }
  }
  
}
