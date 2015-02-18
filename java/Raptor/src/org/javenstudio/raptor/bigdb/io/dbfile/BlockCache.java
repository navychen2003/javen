package org.javenstudio.raptor.bigdb.io.dbfile;

import java.nio.ByteBuffer;

/**
 * Block cache interface.
 * TODO: Add filename or hash of filename to block cache key.
 */
public interface BlockCache {
  /**
   * Add block to cache.
   * @param blockName Zero-based file block number.
   * @param buf The block contents wrapped in a ByteBuffer.
   * @param inMemory Whether block should be treated as in-memory
   */
  public void cacheBlock(String blockName, ByteBuffer buf, boolean inMemory);

  /**
   * Add block to cache (defaults to not in-memory).
   * @param blockName Zero-based file block number.
   * @param buf The block contents wrapped in a ByteBuffer.
   */
  public void cacheBlock(String blockName, ByteBuffer buf);

  /**
   * Fetch block from cache.
   * @param blockName Block number to fetch.
   * @return Block or null if block is not in the cache.
   */
  public ByteBuffer getBlock(String blockName);

  /**
   * Shutdown the cache.
   */
  public void shutdown();
}
