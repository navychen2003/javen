package org.javenstudio.falcon.datum.table.store;

/**
 * Implementors of this interface want to be notified when an DBRegion
 * determines that a cache flush is needed. A FlushRequester (or null)
 * must be passed to the DBRegion constructor so it knows who to call when it
 * has a filled memstore.
 */
public interface FlushRequester {
  /**
   * Tell the listener the cache needs to be flushed.
   *
   * @param region the DBRegion requesting the cache flush
   */
  void request(DBRegion region);
}
