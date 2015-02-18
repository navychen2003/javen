package org.javenstudio.falcon.search.cache;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.Searcher;

/**
 * Implementations of <code>CacheRegenerator</code> are used 
 * in autowarming to populate a new cache
 * based on an old cache.  <code>regenerateItem</code> is called 
 * for each item that should be inserted into the new cache.
 * <p>
 * Implementations should have a noarg constructor and be thread safe 
 * (a single instance will be used for all cache autowarmings).
 *
 */
public interface CacheRegenerator<K,V> {
	
	/**
	 * Regenerate an old cache item and insert it into <code>newCache</code>
	 *
	 * @param newSearcher the new searcher who's caches are being autowarmed
	 * @param newCache    where regenerated cache items should be stored. the target of the autowarming
	 * @param oldCache    the old cache being used as a source for autowarming
	 * @param oldKey      the key of the old cache item to regenerate in the new cache
	 * @param oldVal      the old value of the cache item
	 * @return true to continue with autowarming, false to stop
	 */
	public boolean regenerateItem(Searcher newSearcher, 
			SearchCache<K,V> newCache, SearchCache<K,V> oldCache, 
			K oldKey, V oldVal) throws ErrorException;
	
}
