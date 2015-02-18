package org.javenstudio.falcon.search.dataimport;

import java.util.Iterator;

/**
 * <p>
 * A cache that allows a DIH entity's data to persist locally prior being joined
 * to other data and/or indexed.
 * </p>
 * 
 */
public interface ImportCache extends Iterable<ImportRow> {
	
	/**
	 * <p>
	 * Opens the cache using the specified properties. The {@link Context}
	 * includes any parameters needed by the cache impl. This must be called
	 * before any read/write operations are permitted.
	 * <p>
	 */
	public void open(ImportContext context);
  
	/**
	 * <p>
	 * Releases resources used by this cache, if possible. The cache is flushed
	 * but not destroyed.
	 * </p>
	 */
	public void close();
  
	/**
	 * <p>
	 * Persists any pending data to the cache
	 * </p>
	 */
	public void flush();
  
	/**
	 * <p>
	 * Closes the cache, if open. Then removes all data, possibly removing the
	 * cache entirely from persistent storage.
	 * </p>
	 */
	public void destroy();
  
	/**
	 * <p>
	 * Adds a document. If a document already exists with the same key, both
	 * documents will exist in the cache, as the cache allows duplicate keys. To
	 * update a key's documents, first call delete(Object key).
	 * </p>
	 */
	public void add(ImportRow rec);
  
	/**
	 * <p>
	 * Returns an iterator, allowing callers to iterate through the entire cache
	 * in key, then insertion, order.
	 * </p>
	 */
	@Override
	public Iterator<ImportRow> iterator();
  
	/**
	 * <p>
	 * Returns an iterator, allowing callers to iterate through all documents that
	 * match the given key in insertion order.
	 * </p>
	 */
	public Iterator<ImportRow> iterator(Object key);
  
	/**
	 * <p>
	 * Delete all documents associated with the given key
	 * </p>
	 */
	public void delete(Object key);
  
	/**
	 * <p>
	 * Delete all data from the cache,leaving the empty cache intact.
	 * </p>
	 */
	public void deleteAll();
	
}
