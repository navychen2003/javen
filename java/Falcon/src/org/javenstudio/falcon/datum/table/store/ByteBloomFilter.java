package org.javenstudio.falcon.datum.table.store;

import org.javenstudio.raptor.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Implements a <i>Bloom filter</i>, as defined by Bloom in 1970.
 * <p>
 * The Bloom filter is a data structure that was introduced in 1970 and that has been adopted by
 * the networking research community in the past decade thanks to the bandwidth efficiencies that it
 * offers for the transmission of set membership information between networked hosts.  A sender encodes
 * the information into a bit vector, the Bloom filter, that is more compact than a conventional
 * representation. Computation and space costs for construction are linear in the number of elements.
 * The receiver uses the filter to test whether various elements are members of the set. Though the
 * filter will occasionally return a false positive, it will never return a false negative. When creating
 * the filter, the sender can choose its desired point in a trade-off between the false positive rate and the size.
 *
 * <p>
 * Originally inspired by
 * <a href="http://www.one-lab.org">European Commission One-Lab Project 034819</a>.
 *
 * @see BloomFilter The general behavior of a filter
 *
 * @see <a href="http://portal.acm.org/citation.cfm?id=362692&dl=ACM&coll=portal">
 * Space/Time Trade-Offs in Hash Coding with Allowable Errors</a>
 */
public class ByteBloomFilter implements BloomFilter {
  /** Current file format version */
  public static final int VERSION = 1;

  /** Bytes (B) in the array */
  protected int mByteSize;
  /** Number of hash functions */
  protected final int mHashCount;
  /** Hash type */
  protected final int mHashType;
  /** Hash Function */
  protected final Hash mHash;
  /** Keys currently in the bloom */
  protected int mKeyCount;
  /** Max Keys expected for the bloom */
  protected int mMaxKeys;
  /** Bloom bits */
  protected ByteBuffer mBloom;

  /** Bit-value lookup array to prevent doing the same work over and over */
  private static final byte[] sBitvals = {
      (byte) 0x01,
      (byte) 0x02,
      (byte) 0x04,
      (byte) 0x08,
      (byte) 0x10,
      (byte) 0x20,
      (byte) 0x40,
      (byte) 0x80
    };

  /**
   * Loads bloom filter meta data from file input.
   * @param meta stored bloom meta data
   * @throws IllegalArgumentException meta data is invalid
   */
  public ByteBloomFilter(ByteBuffer meta)
      throws IllegalArgumentException {
    int version = meta.getInt();
    if (version != VERSION) throw new IllegalArgumentException("Bad version");

    this.mByteSize = meta.getInt();
    this.mHashCount = meta.getInt();
    this.mHashType = meta.getInt();
    this.mKeyCount = meta.getInt();
    this.mMaxKeys = this.mKeyCount;

    this.mHash = Hash.getInstance(this.mHashType);
    sanityCheck();
  }

  /**
   * Determines & initializes bloom filter meta data from user config.  Call
   * {@link #allocBloom()} to allocate bloom filter data.
   * @param maxKeys Maximum expected number of keys that will be stored in this bloom
   * @param errorRate Desired false positive error rate.  Lower rate = more storage required
   * @param hashType Type of hash function to use
   * @param foldFactor When finished adding entries, you may be able to 'fold'
   * this bloom to save space.  Tradeoff potentially excess bytes in bloom for
   * ability to fold if keyCount is exponentially greater than maxKeys.
   * @throws IllegalArgumentException
   */
  public ByteBloomFilter(int maxKeys, float errorRate, int hashType, int foldFactor)
      throws IllegalArgumentException {
    /*
     * Bloom filters are very sensitive to the number of elements inserted
     * into them. For HBase, the number of entries depends on the size of the
     * data stored in the column. Currently the default region size is 256MB,
     * so entry count ~= 256MB / (average value size for column).  Despite
     * this rule of thumb, there is no efficient way to calculate the entry
     * count after compactions.  Therefore, it is often easier to use a
     * dynamic bloom filter that will add extra space instead of allowing the
     * error rate to grow.
     *
     * ( http://www.eecs.harvard.edu/~michaelm/NEWWORK/postscripts/BloomFilterSurvey.pdf )
     *
     * m denotes the number of bits in the Bloom filter (bitSize)
     * n denotes the number of elements inserted into the Bloom filter (maxKeys)
     * k represents the number of hash functions used (nbHash)
     * e represents the desired false positive rate for the bloom (err)
     *
     * If we fix the error rate (e) and know the number of entries, then
     * the optimal bloom size m = -(n * ln(err) / (ln(2)^2)
     *                         ~= n * ln(err) / ln(0.6185)
     *
     * The probability of false positives is minimized when k = m/n ln(2).
     */
    int bitSize = (int)Math.ceil(maxKeys * (Math.log(errorRate) / Math.log(0.6185)));
    int functionCount = (int)Math.ceil(Math.log(2) * (bitSize / maxKeys));

    // increase byteSize so folding is possible
    int byteSize = (bitSize + 7) / 8;
    int mask = (1 << foldFactor) - 1;
    if ( (mask & byteSize) != 0) {
      byteSize >>= foldFactor;
      ++byteSize;
      byteSize <<= foldFactor;
    }

    this.mByteSize = byteSize;
    this.mHashCount = functionCount;
    this.mHashType = hashType;
    this.mKeyCount = 0;
    this.mMaxKeys = maxKeys;

    this.mHash = Hash.getInstance(hashType);
    sanityCheck();
  }

  @Override
  public void allocBloom() {
    if (this.mBloom != null) 
      throw new IllegalArgumentException("can only create bloom once.");
    
    this.mBloom = ByteBuffer.allocate(this.mByteSize);
    assert this.mBloom.hasArray();
  }

  protected void sanityCheck() throws IllegalArgumentException {
    if (this.mByteSize <= 0) 
      throw new IllegalArgumentException("maxValue must be > 0");
    
    if (this.mHashCount <= 0) 
      throw new IllegalArgumentException("Hash function count must be > 0");
    
    if (this.mHash == null) 
      throw new IllegalArgumentException("hashType must be known");
    
    if (this.mKeyCount < 0) 
      throw new IllegalArgumentException("must have positive keyCount");
  }

  protected void bloomCheck(ByteBuffer bloom) throws IllegalArgumentException {
    if (this.mByteSize != bloom.limit()) {
      throw new IllegalArgumentException(
          "Configured bloom length should match actual length");
    }
  }

  @Override
  public void add(byte[] buf) {
    add(buf, 0, buf.length);
  }

  @Override
  public void add(byte[] buf, int offset, int len) {
    /*
     * For faster hashing, use combinatorial generation
     * http://www.eecs.harvard.edu/~kirsch/pubs/bbbf/esa06.pdf
     */
    int hash1 = this.mHash.hash(buf, offset, len, 0);
    int hash2 = this.mHash.hash(buf, offset, len, hash1);

    for (int i = 0; i < this.mHashCount; i++) {
      int hashLoc = Math.abs((hash1 + i * hash2) % (this.mByteSize * 8));
      set(hashLoc);
    }

    ++this.mKeyCount;
  }

  /**
   * Should only be used in tests when writing a bloom filter.
   */
  protected boolean contains(byte[] buf) {
    return contains(buf, 0, buf.length, this.mBloom);
  }

  /**
   * Should only be used in tests when writing a bloom filter.
   */
  protected boolean contains(byte[] buf, int offset, int length) {
    return contains(buf, offset, length, this.mBloom);
  }

  @Override
  public boolean contains(byte[] buf, ByteBuffer theBloom) {
    return contains(buf, 0, buf.length, theBloom);
  }

  @Override
  public boolean contains(byte[] buf, int offset, int length,
      ByteBuffer theBloom) {
    if (theBloom.limit() != this.mByteSize) 
      throw new IllegalArgumentException("Bloom does not match expected size");

    int hash1 = this.mHash.hash(buf, offset, length, 0);
    int hash2 = this.mHash.hash(buf, offset, length, hash1);

    for (int i = 0; i < this.mHashCount; i++) {
      int hashLoc = Math.abs((hash1 + i * hash2) % (this.mByteSize * 8));
      if (!get(hashLoc, theBloom)) 
        return false;
    }
    
    return true;
  }

  //---------------------------------------------------------------------------
  /** Private helpers */

  /**
   * Set the bit at the specified index to 1.
   *
   * @param pos index of bit
   */
  protected void set(int pos) {
    int bytePos = pos / 8;
    int bitPos = pos % 8;
    byte curByte = mBloom.get(bytePos);
    curByte |= sBitvals[bitPos];
    mBloom.put(bytePos, curByte);
  }

  /**
   * Check if bit at specified index is 1.
   *
   * @param pos index of bit
   * @return true if bit at specified index is 1, false if 0.
   */
  static boolean get(int pos, ByteBuffer theBloom) {
    int bytePos = pos / 8;
    int bitPos = pos % 8;
    byte curByte = theBloom.get(bytePos);
    curByte &= sBitvals[bitPos];
    return (curByte != 0);
  }

  @Override
  public int getKeyCount() {
    return this.mKeyCount;
  }

  @Override
  public int getMaxKeys() {
    return this.mMaxKeys;
  }

  @Override
  public int getByteSize() {
    return this.mByteSize;
  }

  @Override
  public void compactBloom() {
    // see if the actual size is exponentially smaller than expected.
    if (this.mKeyCount > 0 && this.mBloom.hasArray()) {
      int pieces = 1;
      int newByteSize = this.mByteSize;
      int newMaxKeys = this.mMaxKeys;

      // while exponentially smaller & folding is lossless
      while ( (newByteSize & 1) == 0 && newMaxKeys > (this.mKeyCount<<1) ) {
        pieces <<= 1;
        newByteSize >>= 1;
        newMaxKeys >>= 1;
      }

      // if we should fold these into pieces
      if (pieces > 1) {
        byte[] array = this.mBloom.array();
        int start = this.mBloom.arrayOffset();
        int end = start + newByteSize;
        int off = end;
        
        for (int p = 1; p < pieces; ++p) {
          for (int pos = start; pos < end; ++pos) {
            array[pos] |= array[off++];
          }
        }
        
        // folding done, only use a subset of this array
        this.mBloom.rewind();
        this.mBloom.limit(newByteSize);
        this.mBloom = this.mBloom.slice();
        this.mByteSize = newByteSize;
        this.mMaxKeys = newMaxKeys;
      }
    }
  }

  //---------------------------------------------------------------------------

  /**
   * Writes just the bloom filter to the output array
   * @param out OutputStream to place bloom
   * @throws IOException Error writing bloom array
   */
  public void writeBloom(final DataOutput out) throws IOException {
    if (!this.mBloom.hasArray()) 
      throw new IOException("Only writes ByteBuffer with underlying array.");
    
    out.write(mBloom.array(), mBloom.arrayOffset(), mBloom.limit());
  }

  @Override
  public Writable getMetaWriter() {
    return new MetaWriter();
  }

  @Override
  public Writable getDataWriter() {
    return new DataWriter();
  }

  private class MetaWriter implements Writable {
    protected MetaWriter() {}
    
    @Override
    public void readFields(DataInput arg0) throws IOException {
      throw new IOException("Cant read with this class.");
    }

    @Override
    public void write(DataOutput out) throws IOException {
      out.writeInt(VERSION);
      out.writeInt(mByteSize);
      out.writeInt(mHashCount);
      out.writeInt(mHashType);
      out.writeInt(mKeyCount);
    }
  }

  private class DataWriter implements Writable {
    protected DataWriter() {}
    
    @Override
    public void readFields(DataInput arg0) throws IOException {
      throw new IOException("Cant read with this class.");
    }

    @Override
    public void write(DataOutput out) throws IOException {
      writeBloom(out);
    }
  }

}
