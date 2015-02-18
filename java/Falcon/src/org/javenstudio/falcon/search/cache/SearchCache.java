package org.javenstudio.falcon.search.cache;

import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.Searcher;
import org.javenstudio.falcon.util.InfoMBean;

/**
 * Primary API for dealing with internal caches.
 * 
 */
public interface SearchCache<K,V> extends InfoMBean {
	static final Logger LOG = Logger.getLogger(SearchCache.class);

	/** 
	 * Enumeration of possible States for cache instances.
	 * :TODO: only state that seems to ever be set is LIVE ?
	 */
	public enum State { 
		/** :TODO */
		CREATED, 
		/** :TODO */
		STATICWARMING, 
		/** :TODO */
		AUTOWARMING, 
		/** :TODO */
		LIVE 
	}
	
	public Searcher getSearcher();
	
	/**
	 * The initialization routine.  Instance specific arguments are passed in
	 * the <code>args</code> map.
	 * <p>
	 * The persistence object will exist across different lifetimes of similar caches.
	 * For example, all filter caches will share the same persistence object, sometimes
	 * at the same time (it must be threadsafe).  If null is passed, then the cache
	 * implementation should create and return a new persistence object.  If not null,
	 * the passed in object should be returned again.
	 * <p>
	 * Since it will exist across the lifetime of many caches, care should be taken to
	 * not reference any particular cache instance and prevent it from being
	 * garbage collected (no using inner classes unless they are static).
	 * <p>
	 * The persistence object is designed to be used as a way for statistics
	 * to accumulate across all instances of the same type of cache, however the
	 * object may be of any type desired by the cache implementation.
	 * <p>
	 * The {@link CacheRegenerator} is what the cache uses during auto-warming to
	 * renenerate an item in the new cache from an entry in the old cache.
	 */
	// I don't think we need a factory for faster creation given that these
	// will be associated with slow-to-create Searchers.
	// change to NamedList when other plugins do?
	public Object init(Map<String, String> args, Object persistence, 
			CacheRegenerator<K,V> regenerator) throws ErrorException;

	/**
	 * Name the Cache can be referenced with by RequestHandlers.
	 *
	 * This method must return the identifier that the Cache instance 
	 * expects RequestHandlers to use when requesting access to it 
	 * from the Searcher.  It is <strong>strongly</strong> 
	 * recommended that this method return the value of the "name" 
	 * parameter from the init args.
	 *
	 * :TODO: verify this.
	 */
	public String getName();

	// Should SearchCache just extend the java.util.Map interface?
	// Following the conventions of the java.util.Map interface in any case.

	/** :TODO: copy from Map */
	public int size();

	/** :TODO: copy from Map */
	public V put(K key, V value);

	/** :TODO: copy from Map */
	public V get(K key);

	/** :TODO: copy from Map */
	public void clear();

	/**
	 * Set different cache states.
	 * The state a cache is in can have an effect on how statistics are kept.
	 * The cache user (IdnexingSearcher) will take care of switching
	 * cache states.
	 */
	public void setState(State state);

	/**
	 * Returns the last State set on this instance
	 *
	 * @see #setState
	 */
	public State getState();

	/**
	 * Warm this cache associated with <code>searcher</code> using the <code>old</code>
	 * cache object.  <code>this</code> and <code>old</code> will have the same concrete type.
	 */
	// Q: an alternative to passing the searcher here would be to pass it in
	// init and have the cache implementation save it.
	public void warm(Searcher searcher, SearchCache<K,V> old);

	/** Frees any non-memory resources */
	public void close();
	
}
