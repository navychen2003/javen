package org.javenstudio.hornet.util;

import java.util.Arrays;

import org.javenstudio.common.indexdb.util.BitUtil;

/**
 * A native int set where one value is reserved to mean "EMPTY"
 *
 */
public class SentinelIntSet {
	
	private final int mEmptyVal;
	private int mRehashCount;   // the count at which a rehash should be done
	private int[] mKeys;
	private int mCount;

	/**
	 * @param size  The minimum number of elements this set should be able to 
	 * hold without re-hashing (i.e. the slots are guaranteed not to change)
	 * @param emptyVal The integer value to use for EMPTY
	 */
	public SentinelIntSet(int size, int emptyVal) {
		mEmptyVal = emptyVal;
		
		int tsize = Math.max(BitUtil.nextHighestPowerOfTwo(size), 1);
		
		mRehashCount = tsize - (tsize>>2);
		if (size >= mRehashCount) {  // should be able to hold "size" w/o rehashing
			tsize <<= 1;
			mRehashCount = tsize - (tsize>>2);
		}
		
		mKeys = new int[tsize];
		
		if (emptyVal != 0)
			clear();
	}

	public void clear() {
		Arrays.fill(mKeys, mEmptyVal);
		mCount = 0;
	}

	public int hash(int key) {
		return key;
	}

	public int size() { return mCount; }
	public int getKeySize() { return mKeys.length; }

	/** returns the slot for this key */
	public int getSlot(int key) {
		assert key != mEmptyVal;
		
		int h = hash(key);
		int s = h & (mKeys.length-1);
		
		if (mKeys[s] == key || mKeys[s]== mEmptyVal) 
			return s;

		int increment = (h>>7)|1;
		do {
			s = (s + increment) & (mKeys.length-1);
		} while (mKeys[s] != key && mKeys[s] != mEmptyVal);
		
		return s;
	}

	/** returns the slot for this key, or -slot-1 if not found */
	public int find(int key) {
		assert key != mEmptyVal;
		
		int h = hash(key);
		int s = h & (mKeys.length-1);
		
		if (mKeys[s] == key) 
			return s;
		
		if (mKeys[s] == mEmptyVal) 
			return -s-1;

		int increment = (h>>7)|1;
		for (;;) {
			s = (s + increment) & (mKeys.length-1);
			
			if (mKeys[s] == key) 
				return s;
			
			if (mKeys[s] == mEmptyVal) 
				return -s-1;
		}
	}

	public boolean exists(int key) {
		return find(key) >= 0;
	}

	public int put(int key) {
		int s = find(key);
		if (s < 0) {
			mCount ++;
			
			if (mCount >= mRehashCount) {
				rehash();
				s = getSlot(key);
				
			} else {
				s = -s-1;
			}
			
			mKeys[s] = key;
		}
		
		return s;
	}

	public void rehash() {
		int newSize = mKeys.length << 1;
		int[] oldKeys = mKeys;
		
		mKeys = new int[newSize];
		
		if (mEmptyVal != 0) 
			Arrays.fill(mKeys, mEmptyVal);

		for (int i=0; i < oldKeys.length; i++) {
			int key = oldKeys[i];
			if (key == mEmptyVal) 
				continue;
			
			int newSlot = getSlot(key);
			mKeys[newSlot] = key;
		}
		
		mRehashCount = newSize - (newSize>>2);
	}
	
}
