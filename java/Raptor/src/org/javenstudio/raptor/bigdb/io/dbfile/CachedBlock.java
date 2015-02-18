package org.javenstudio.raptor.bigdb.io.dbfile;

import java.nio.ByteBuffer;

import org.javenstudio.raptor.bigdb.io.HeapSize;
import org.javenstudio.raptor.bigdb.util.Bytes;
import org.javenstudio.raptor.bigdb.util.ClassSize;

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

  private final String blockName;
  private final ByteBuffer buf;
  private volatile long accessTime;
  private long size;
  private BlockPriority priority;

  public CachedBlock(String blockName, ByteBuffer buf, long accessTime) {
    this(blockName, buf, accessTime, false);
  }

  public CachedBlock(String blockName, ByteBuffer buf, long accessTime,
      boolean inMemory) {
    this.blockName = blockName;
    this.buf = buf;
    this.accessTime = accessTime;
    this.size = ClassSize.align(blockName.length()) +
    ClassSize.align(buf.capacity()) + PER_BLOCK_OVERHEAD;
    if(inMemory) {
      this.priority = BlockPriority.MEMORY;
    } else {
      this.priority = BlockPriority.SINGLE;
    }
  }

  /**
   * Block has been accessed.  Update its local access time.
   */
  public void access(long accessTime) {
    this.accessTime = accessTime;
    if(this.priority == BlockPriority.SINGLE) {
      this.priority = BlockPriority.MULTI;
    }
  }

  public long heapSize() {
    return size;
  }

  public int compareTo(CachedBlock that) {
    if(this.accessTime == that.accessTime) return 0;
    return this.accessTime < that.accessTime ? 1 : -1;
  }

  public ByteBuffer getBuffer() {
    return this.buf;
  }

  public String getName() {
    return this.blockName;
  }

  public BlockPriority getPriority() {
    return this.priority;
  }
}
