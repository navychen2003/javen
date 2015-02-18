package org.javenstudio.common.indexdb.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An identity hash set implemented using open addressing. No null keys are allowed.
 * 
 * TODO: If this is useful outside this class, make it public - needs some work
 */
final class IdentityHashSet<KType> implements Iterable<KType> {
	/**
	 * Default load factor.
	 */
	public final static float DEFAULT_LOAD_FACTOR = 0.75f;

	/**
	 * Minimum capacity for the set.
	 */
	public final static int MIN_CAPACITY = 4;

	/**
	 * All of set entries. Always of power of two length.
	 */
	/* packaged */ Object[] mKeys;

	/**
	 * Cached number of assigned slots.
	 */
	/* packaged */ int mAssigned;

	/**
	 * The load factor for this set (fraction of allocated or deleted slots before
	 * the buffers must be rehashed or reallocated).
	 */
	/* packaged */ final float mLoadFactor;

	/**
	 * Cached capacity threshold at which we must resize the buffers.
	 */
	private int mResizeThreshold;
    
    /**
     * Creates a hash set with the default capacity of 16.
     * load factor of {@value #DEFAULT_LOAD_FACTOR}. `
     */
    public IdentityHashSet() {
    	this(16, DEFAULT_LOAD_FACTOR);
    }
    
    /**
     * Creates a hash set with the given capacity, load factor of
     * {@value #DEFAULT_LOAD_FACTOR}.
     */
    public IdentityHashSet(int initialCapacity) {
    	this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }
    
    /**
     * Creates a hash set with the given capacity and load factor.
     */
    public IdentityHashSet(int initialCapacity, float loadFactor) {
    	initialCapacity = Math.max(MIN_CAPACITY, initialCapacity);
      
    	assert initialCapacity > 0 : "Initial capacity must be between (0, "
          	+ Integer.MAX_VALUE + "].";
    	assert loadFactor > 0 && loadFactor < 1 : "Load factor must be between (0, 1).";
    	mLoadFactor = loadFactor;
    	allocateBuffers(roundCapacity(initialCapacity));
    }
    
    /**
     * Adds a reference to the set. Null keys are not allowed.
     */
    public boolean add(KType e) {
    	assert e != null : "Null keys not allowed.";
    	if (mAssigned >= mResizeThreshold)
    		expandAndRehash();
      
    	final int mask = mKeys.length - 1;
    	int slot = rehash(e) & mask;
    	Object existing;
    	while ((existing = mKeys[slot]) != null) {
    		if (e == existing) {
    			return false; // already found.
    		}
    		slot = (slot + 1) & mask;
    	}
    	mAssigned++;
    	mKeys[slot] = e;
    	return true;
    }

    /**
     * Checks if the set contains a given ref.
     */
    public boolean contains(KType e) {
    	final int mask = mKeys.length - 1;
    	int slot = rehash(e) & mask;
    	Object existing;
    	while ((existing = mKeys[slot]) != null) {
    		if (e == existing) {
    			return true;
    		}
    		slot = (slot + 1) & mask;
    	}
    	return false;
    }

    /** 
     * Rehash via MurmurHash.
     * 
     * <p>The implementation is based on the
     * finalization step from Austin Appleby's
     * <code>MurmurHash3</code>.
     * 
     * @see "http://sites.google.com/site/murmurhash/"
     */
    private static int rehash(Object o) {
    	int k = System.identityHashCode(o);
    	k ^= k >>> 16;
    	k *= 0x85ebca6b;
    	k ^= k >>> 13;
    	k *= 0xc2b2ae35;
    	k ^= k >>> 16;
    	return k;
    }
    
    /**
     * Expand the internal storage buffers (capacity) or rehash current keys and
     * values if there are a lot of deleted slots.
     */
    private void expandAndRehash() {
    	final Object[] oldKeys = this.mKeys;
      
    	assert mAssigned >= mResizeThreshold;
    	allocateBuffers(nextCapacity(mKeys.length));
      
    	/**
    	 * Rehash all assigned slots from the old hash table.
    	 */
    	final int mask = mKeys.length - 1;
    	for (int i = 0; i < oldKeys.length; i++) {
    		final Object key = oldKeys[i];
    		if (key != null) {
    			int slot = rehash(key) & mask;
    			while (mKeys[slot] != null) {
    				slot = (slot + 1) & mask;
    			}
    			mKeys[slot] = key;
    		}
    	}
    	Arrays.fill(oldKeys, null);
    }
    
    /**
     * Allocate internal buffers for a given capacity.
     * 
     * @param capacity
     *          New capacity (must be a power of two).
     */
    private void allocateBuffers(int capacity) {
      	mKeys = new Object[capacity];
      	mResizeThreshold = (int) (capacity * DEFAULT_LOAD_FACTOR);
    }
    
    /**
     * Return the next possible capacity, counting from the current buffers' size.
     */
    protected int nextCapacity(int current) {
    	assert current > 0 && Long.bitCount(current) == 1 : "Capacity must be a power of two.";
    	assert ((current << 1) > 0) : "Maximum capacity exceeded ("
          	+ (0x80000000 >>> 1) + ").";
      
    	if (current < MIN_CAPACITY / 2) current = MIN_CAPACITY / 2;
    	return current << 1;
    }
    
    /**
     * Round the capacity to the next allowed value.
     */
    protected int roundCapacity(int requestedCapacity) {
    	// Maximum positive integer that is a power of two.
    	if (requestedCapacity > (0x80000000 >>> 1)) return (0x80000000 >>> 1);
      
    	int capacity = MIN_CAPACITY;
    	while (capacity < requestedCapacity) {
    		capacity <<= 1;
    	}

    	return capacity;
    }
    
    public void clear() {
    	mAssigned = 0;
    	Arrays.fill(mKeys, null);
    }
    
    public int size() {
    	return mAssigned;
    }
    
    public boolean isEmpty() {
    	return size() == 0;
    }

    @Override
    public Iterator<KType> iterator() {
    	return new Iterator<KType>() {
    		private Object mNextElement = fetchNext();
    		private int mPos = -1;

    		@Override
    		public boolean hasNext() {
    			return mNextElement != null;
    		}

    		@SuppressWarnings("unchecked")
    		@Override
    		public KType next() {
    			Object r = mNextElement;
    			if (r == null) {
    				throw new NoSuchElementException();
    			}
    			mNextElement = fetchNext();
    			return (KType) r;
    		}

    		private Object fetchNext() {
    			mPos ++;
    			while (mPos < mKeys.length && mKeys[mPos] == null) {
    				mPos ++;
    			}

    			return (mPos >= mKeys.length ? null : mKeys[mPos]);
    		}

    		@Override
    		public void remove() {
    			throw new UnsupportedOperationException();
    		}
    	};
    }
    
}