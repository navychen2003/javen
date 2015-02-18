package org.javenstudio.raptor.bigdb.regionserver;

import java.io.IOException;

/**
 * A package protected interface for a store flushing.
 * A store flusher carries the state required to prepare/flush/commit the
 * store's cache.
 */
interface StoreFlusher {

  /**
   * Prepare for a store flush (create snapshot)
   *
   * Requires pausing writes.
   *
   * A very short operation.
   */
  void prepare();

  /**
   * Flush the cache (create the new store file)
   *
   * A length operation which doesn't require locking out any function
   * of the store.
   *
   * @throws IOException in case the flush fails
   */
  void flushCache() throws IOException;

  /**
   * Commit the flush - add the store file to the store and clear the
   * memstore snapshot.
   *
   * Requires pausing scans.
   *
   * A very short operation
   *
   * @return
   * @throws IOException
   */
  boolean commit() throws IOException;

}

