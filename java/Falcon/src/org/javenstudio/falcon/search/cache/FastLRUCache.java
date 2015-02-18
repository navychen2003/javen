package org.javenstudio.falcon.search.cache;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ConcurrentLRUCache;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.search.Searcher;

/**
 * SearchCache based on ConcurrentLRUCache implementation.
 * <p/>
 * This implementation does not use a separate cleanup thread. Instead it uses the calling thread
 * itself to do the cleanup when the size of the cache exceeds certain limits.
 * <p/>
 *
 * @see ConcurrentLRUCache
 * @see SearchCache
 * @since 1.4
 */
public class FastLRUCache<K,V> extends SearchCacheBase<K,V> implements SearchCache<K,V> {
	static final Logger LOG = Logger.getLogger(FastLRUCache.class);

	// contains the statistics objects for all open caches of the same type
	private List<ConcurrentLRUCache.Stats> mStatsList;
	private ConcurrentLRUCache<K,V> mCache;

	private String mDescription = "Concurrent LRU Cache";
	
	private long mWarmupTime = 0;
	private int mShowItems = 0;

	public FastLRUCache(Searcher searcher) { 
		super(searcher);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Object init(Map<String, String> args, Object persistence, 
			CacheRegenerator<K,V> regenerator) throws ErrorException {
		super.init(args, regenerator);
		
		String str = (String) args.get("size");
		int limit = (str == null) ? 1024 : Integer.parseInt(str);
		int minLimit;
		
		str = (String) args.get("minSize");
		if (str == null) 
			minLimit = (int) (limit * 0.9);
		else 
			minLimit = Integer.parseInt(str);
		
		if (minLimit == 0) 
			minLimit = 1;
		
		if (limit <= minLimit) 
			limit = minLimit+1;

		str = (String) args.get("acceptableSize");
		int acceptableLimit;
		if (str == null) 
			acceptableLimit = (int) (limit * 0.95);
		else 
			acceptableLimit = Integer.parseInt(str);
		
		// acceptable limit should be somewhere between minLimit and limit
		acceptableLimit = Math.max(minLimit, acceptableLimit);

		str = (String) args.get("initialSize");
		final int initialSize = (str == null) ? limit : Integer.parseInt(str);
		
		str = (String) args.get("cleanupThread");
		boolean newThread = (str == null) ? false : Boolean.parseBoolean(str);

		str = (String) args.get("showItems");
		mShowItems = (str == null) ? 0 : Integer.parseInt(str);
		
		mDescription = generateDescription(limit, initialSize, 
				minLimit, acceptableLimit, newThread);
		
		mCache = new ConcurrentLRUCache<K,V>(limit, minLimit, acceptableLimit, 
				initialSize, newThread, false, null);
		mCache.setAlive(false);

		mStatsList = (List<ConcurrentLRUCache.Stats>) persistence;
		if (mStatsList == null) {
			// must be the first time a cache of this type is being created
			// Use a CopyOnWriteArrayList since puts are very rare and 
			// iteration may be a frequent operation
			// because it is used in getStatistics()
			mStatsList = new CopyOnWriteArrayList<ConcurrentLRUCache.Stats>();

			// the first entry will be for cumulative stats of caches that have been closed.
			mStatsList.add(new ConcurrentLRUCache.Stats());
		}
		
		mStatsList.add(mCache.getStats());
		return mStatsList;
	}
  
	/**
	 * @return Returns the description of this Cache.
	 */
	protected String generateDescription(int limit, int initialSize, int minLimit, 
			int acceptableLimit, boolean newThread) {
		String description = "Concurrent LRU Cache(maxSize=" + limit 
				+ ", initialSize=" + initialSize + ", minSize="+minLimit 
				+ ", acceptableSize=" + acceptableLimit 
				+ ", cleanupThread=" + newThread;
		
		if (isAutowarmingOn()) 
			description += ", " + getAutowarmDescription();
    
		description += ')';
		
		return description;
	}

	@Override
	public int size() {
		return mCache.size();
	}

	@Override
	public V put(K key, V value) {
		return mCache.put(key, value);
	}

	@Override
	public V get(K key) {
		return mCache.get(key);
	}

	@Override
	public void clear() {
		mCache.clear();
	}

	@Override
	public void setState(SearchCache.State state) {
		super.setState(state);
		mCache.setAlive(state == State.LIVE);
	}

	@SuppressWarnings("unchecked")
	public void warm(Searcher searcher, SearchCache<K,V> old) {
		if (mRegenerator == null) return;
		
		long warmingStartTime = System.currentTimeMillis();
		FastLRUCache<K,V> other = (FastLRUCache<K,V>) old;
		
		// warm entries
		if (isAutowarmingOn()) {
			int sz = mAutowarm.getWarmCount(other.size());
			
			Map<K,V> items = other.mCache.getLatestAccessedItems(sz);
			Map.Entry<K,V>[] itemsArr = new Map.Entry[items.size()];
			
			int counter = 0;
			for (Map.Entry<K,V> mapEntry : items.entrySet()) {
				itemsArr[counter++] = (Map.Entry<K,V>) mapEntry;
			}
			
			for (int i = itemsArr.length - 1; i >= 0; i--) {
				try {
					boolean continueRegen = mRegenerator.regenerateItem(
							searcher, this, old, itemsArr[i].getKey(), itemsArr[i].getValue());
					if (!continueRegen) 
						break;
				} catch (Throwable e) {
					if (LOG.isErrorEnabled())
						LOG.error("Error during auto-warming of key:" + itemsArr[i].getKey(), e);
				}
			}
		}
		
		mWarmupTime = System.currentTimeMillis() - warmingStartTime;
	}

	@Override
	public void close() {
		// add the stats to the cumulative stats object (the first in the statsList)
		mStatsList.get(0).add(mCache.getStats());
		mStatsList.remove(mCache.getStats());
		mCache.destroy();
	}

	@Override
	public String getMBeanDescription() { 
		return mDescription;
	}
  
	@Override
	public NamedList<?> getMBeanStatistics() {
		@SuppressWarnings("unchecked")
		NamedList<Serializable> lst = (NamedMap<Serializable>)super.getMBeanStatistics();
		if (mCache == null) 
			return lst;
		
		ConcurrentLRUCache.Stats stats = mCache.getStats();
		
		long lookups = stats.getCumulativeLookups();
		long hits = stats.getCumulativeHits();
		long inserts = stats.getCumulativePuts();
		long evictions = stats.getCumulativeEvictions();
		long size = stats.getCurrentSize();
		long clookups = 0;
		long chits = 0;
		long cinserts = 0;
		long cevictions = 0;

		// NOTE: It is safe to iterate on a CopyOnWriteArrayList
		for (ConcurrentLRUCache.Stats statistiscs : mStatsList) {
			clookups += statistiscs.getCumulativeLookups();
			chits += statistiscs.getCumulativeHits();
			cinserts += statistiscs.getCumulativePuts();
			cevictions += statistiscs.getCumulativeEvictions();
		}

		lst.add("lookups", lookups);
		lst.add("hits", hits);
    	lst.add("hitratio", calcHitRatio(lookups, hits));
    	lst.add("inserts", inserts);
    	lst.add("evictions", evictions);
    	lst.add("size", size);

    	lst.add("warmupTime", mWarmupTime);
    	lst.add("cumulative_lookups", clookups);
    	lst.add("cumulative_hits", chits);
    	lst.add("cumulative_hitratio", calcHitRatio(clookups, chits));
    	lst.add("cumulative_inserts", cinserts);
    	lst.add("cumulative_evictions", cevictions);

    	if (mShowItems != 0) {
    		Map<K,V> items = mCache.getLatestAccessedItems(
    				mShowItems == -1 ? Integer.MAX_VALUE : mShowItems);
    		
    		for (Map.Entry<K,V> e : (Set<Map.Entry<K,V>>)items.entrySet()) {
    			Object k = e.getKey();
    			Object v = e.getValue();

    			String ks = "item_" + k;
    			String vs = v.toString();
    			lst.add(ks,vs);
    		}
    	}

    	return lst;
	}

	@Override
	public String toString() {
		return getMBeanName() + getMBeanStatistics().toString();
	}

}
