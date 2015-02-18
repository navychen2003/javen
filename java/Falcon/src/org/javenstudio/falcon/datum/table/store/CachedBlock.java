package org.javenstudio.falcon.datum.table.store;

import java.nio.ByteBuffer;

/**
 * Represents an entry in the {@link LruBlockCache}.
 *
 * <p>Makes the block memory-aware with {@link HeapSize} and Comparable
 * to sort by access time for the LRU.  It also takes care of priority by
 * either instantiating as in-memory or handling the transition from single
 * to multiple access.
 */
public class CachedBlock implements HeapSize, Comparable<CachedBlock> {

  public final static long PER_BLOCK_OVERHEAD = ClassSize.align(
    ClassSize.OBJECT + (3 * ClassSize.REFERENCE) + (2 * Bytes.SIZEOF_LONG) +
    ClassSize.STRING + ClassSize.BYTE_BUFFER);

  static enum BlockPriority {
    /**
     * Accessed a single time (used for scan-resistance)
     */
    SINGLE,
    /**
     * Accessed multiple times
     */
    MULTI,
    /**
     * Block from in-memory store
     */
    MEMORY
  };

  private final String mBlockName;
  private final ByteBuffer mBuf;
  private volatile long mAccessTime;
  private long mSize;
  private BlockPriority mPriority;

  public CachedBlock(String blockName, ByteBuffer buf, long accessTime) {
    this(blockName, buf, accessTime, false);
  }

  public CachedBlock(String blockName, ByteBuffer buf, long accessTime,
      boolean inMemory) {
    this.mBlockName = blockName;
    this.mBuf = buf;
    this.mAccessTime = accessTime;
    this.mSize = ClassSize.align(blockName.length()) +
    ClassSize.align(buf.capacity()) + PER_BLOCK_OVERHEAD;
    if (inMemory) {
      this.mPriority = BlockPriority.MEMORY;
    } else {
      this.mPriority = BlockPriority.SINGLE;
    }
  }

  /**
   * Block has been accessed.  Update its local access time.
   */
  public void access(long accessTime) {
    this.mAccessTime = accessTime;
    if (this.mPriority == BlockPriority.SINGLE) {
      this.mPriority = BlockPriority.MULTI;
    }
  }

  @Override
  public long heapSize() {
    return mSize;
  }

  @Override
  public int compareTo(CachedBlock that) {
    if (this.mAccessTime == that.mAccessTime) return 0;
    return this.mAccessTime < that.mAccessTime ? 1 : -1;
  }

  public ByteBuffer getBuffer() {
    return this.mBuf;
  }

  public String getName() {
    return this.mBlockName;
  }

  public BlockPriority getPriority() {
    return this.mPriority;
  }
}
