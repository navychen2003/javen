package org.javenstudio.hornet.store;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements a combination of {@link java.util.WeakHashMap} and
 * {@link java.util.IdentityHashMap}.
 * Useful for caches that need to key off of a {@code ==} comparison
 * instead of a {@code .equals}.
 * 
 * <p>This class is not a general-purpose {@link java.util.Map}
 * implementation! It intentionally violates
 * Map's general contract, which mandates the use of the equals method
 * when comparing objects. This class is designed for use only in the
 * rare cases wherein reference-equality semantics are required.
 * 
 * <p>This implementation was forked from <a href="http://cxf.apache.org/">Apache CXF</a>
 * but modified to <b>not</b> implement the {@link java.util.Map} interface and
 * without any set views on it, as those are error-prone and inefficient,
 * if not implemented carefully. The map only contains {@link Iterator} implementations
 * on the values and not-GCed keys. Lucene's implementation also supports {@code null}
 * keys, but those are never weak!
 *
 */
public final class WeakIdentityMap<K,V> {
	
	private final ReferenceQueue<Object> mQueue = new ReferenceQueue<Object>();
	private final Map<IdentityWeakReference, V> mBackingStore;

	/** Creates a new {@code WeakIdentityMap} based on a non-synchronized {@link HashMap}. */
	public static final <K,V> WeakIdentityMap<K,V> newHashMap() {
		return new WeakIdentityMap<K,V>(new HashMap<IdentityWeakReference,V>());
	}

	/** Creates a new {@code WeakIdentityMap} based on a {@link ConcurrentHashMap}. */
	public static final <K,V> WeakIdentityMap<K,V> newConcurrentHashMap() {
		return new WeakIdentityMap<K,V>(new ConcurrentHashMap<IdentityWeakReference,V>());
	}

	private WeakIdentityMap(Map<IdentityWeakReference, V> backingStore) {
		mBackingStore = backingStore;
	}

	/** Removes all of the mappings from this map. */
	public void clear() {
		mBackingStore.clear();
		reap();
	}

	/** Returns {@code true} if this map contains a mapping for the specified key. */
	public boolean containsKey(Object key) {
		reap();
		return mBackingStore.containsKey(new IdentityWeakReference(key, null));
	}

	/** Returns the value to which the specified key is mapped. */
	public V get(Object key) {
		reap();
		return mBackingStore.get(new IdentityWeakReference(key, null));
	}

	/** 
	 * Associates the specified value with the specified key in this map.
	 * If the map previously contained a mapping for this key, the old value
	 * is replaced. 
	 */
	public V put(K key, V value) {
		reap();
		return mBackingStore.put(new IdentityWeakReference(key, mQueue), value);
	}

	/** Returns {@code true} if this map contains no key-value mappings. */
	public boolean isEmpty() {
		return size() == 0;
	}

	/** 
	 * Removes the mapping for a key from this weak hash map if it is present.
	 * Returns the value to which this map previously associated the key,
	 * or {@code null} if the map contained no mapping for the key.
	 * A return value of {@code null} does not necessarily indicate that
	 * the map contained.
	 */
	public V remove(Object key) {
		reap();
		return mBackingStore.remove(new IdentityWeakReference(key, null));
	}

	/** 
	 * Returns the number of key-value mappings in this map. This result is a snapshot,
	 * and may not reflect unprocessed entries that will be removed before next
	 * attempted access because they are no longer referenced.
	 */
	public int size() {
		if (mBackingStore.isEmpty())
			return 0;
		
		reap();
		return mBackingStore.size();
	}
  
	/** 
	 * Returns an iterator over all weak keys of this map.
	 * Keys already garbage collected will not be returned.
	 * This Iterator does not support removals. 
	 */
	public Iterator<K> keyIterator() {
		reap();
		
		final Iterator<IdentityWeakReference> iterator = 
				mBackingStore.keySet().iterator();
		
		// IMPORTANT: Don't use oal.util.FilterIterator here:
		// We need *strong* reference to current key after setNext()!!!
		return new Iterator<K>() {
			// holds strong reference to next element in backing iterator:
			private Object mNext = null;
			// the backing iterator was already consumed:
			private boolean mNextIsSet = false;
    
			@Override
			public boolean hasNext() {
				return mNextIsSet ? true : setNext();
			}
      
			@Override
			@SuppressWarnings("unchecked")
			public K next() {
				if (mNextIsSet || setNext()) {
					try {
						assert mNextIsSet;
						return (K) mNext;
					} finally {
						// release strong reference and invalidate current value:
						mNextIsSet = false;
						mNext = null;
					}
				}
				throw new NoSuchElementException();
			}
      
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
      
			private boolean setNext() {
				assert !mNextIsSet;
				while (iterator.hasNext()) {
					mNext = iterator.next().get();
					
					if (mNext == null) {
						// already garbage collected!
						continue;
					}
					
					// unfold "null" special value
					if (mNext == NULL) 
						mNext = null;
					
					return mNextIsSet = true;
				}
				return false;
			}
		};
	}
  
	/** 
	 * Returns an iterator over all values of this map.
	 * This iterator may return values whose key is already
	 * garbage collected while iterator is consumed. 
	 */
	public Iterator<V> valueIterator() {
		reap();
		return mBackingStore.values().iterator();
	}

	private void reap() {
		Reference<?> zombie;
		while ((zombie = mQueue.poll()) != null) {
			mBackingStore.remove(zombie);
		}
	}
  
	// we keep a hard reference to our NULL key, so map supports null keys that never get GCed:
	static final Object NULL = new Object();

	private static final class IdentityWeakReference extends WeakReference<Object> {
		private final int mHash;
    
		IdentityWeakReference(Object obj, ReferenceQueue<Object> queue) {
			super(obj == null ? NULL : obj, queue);
			mHash = System.identityHashCode(obj);
		}

		@Override
		public int hashCode() {
			return mHash;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) 
				return true;
			
			if (o instanceof IdentityWeakReference) {
				final IdentityWeakReference ref = (IdentityWeakReference)o;
				if (this.get() == ref.get()) 
					return true;
			}
			
			return false;
		}
	}
	
}

