package org.javenstudio.falcon.search.hits;

import org.javenstudio.common.indexdb.util.BitUtil;

/**
 * <code>HashDocSet</code> represents an unordered set of Document Ids
 * using a primitive int hash table.  It can be a better choice if there are few docs
 * in the set because it takes up less memory and is faster to iterate and take
 * set intersections.
 *
 * @since 0.9
 */
public final class HashDocSet extends DocSetBase {
	
	/** 
	 * Default load factor to use for HashDocSets.  We keep track of the inverse
	 *  since multiplication is so much faster than division.  The default
	 *  is 1.0f / 0.75f
	 */
	static float DEFAULT_INVERSE_LOAD_FACTOR = 1.0f /0.75f;

	// public final static int MAX_SIZE = config.getInt("//HashDocSet/@maxSize",-1);

	// docs are numbered from 0, so a neg number must be used for missing.
	// an alternative to having to init the array to EMPTY at the start is
	//
	private final static int EMPTY = -1;
	
	private final int[] mTable;
	private final int mSize;

	private final int mMask;

	public HashDocSet(HashDocSet set) {
		mTable = set.mTable.clone();
		mSize = set.mSize;
		mMask = set.mMask;
	}

	/** Create a HashDocSet from a list of *unique* ids */
	public HashDocSet(int[] docs, int offset, int len) {
		this(docs, offset, len, DEFAULT_INVERSE_LOAD_FACTOR);
	}

	/** Create a HashDocSet from a list of *unique* ids */  
	public HashDocSet(int[] docs, int offset, int len, float inverseLoadFactor) {
		int tsize = Math.max(BitUtil.nextHighestPowerOfTwo(len), 1);
		if (tsize < len * inverseLoadFactor) 
			tsize <<= 1;
		
		mMask = tsize-1;
		mTable = new int[tsize];
		
		// (for now) better then: Arrays.fill(table, EMPTY);
		for (int i = tsize-1; i >= 0; i--) { 
			mTable[i] = EMPTY; 
		}

		int end = offset + len;
		for (int i = offset; i < end; i++) {
			put(docs[i]);
		}

		mSize = len;
	}

	protected void put(int doc) {
		int s = doc & mMask;
		
		while (mTable[s] != EMPTY) {
			// Adding an odd number to this power-of-two hash table is
			// guaranteed to do a full traversal, so instead of re-hashing
			// we jump straight to a "linear" traversal.
			// The key is that we provide many different ways to do the
			// traversal (tablesize/2) based on the last hash code (the doc).
			// Rely on loop invariant code motion to eval ((doc>>7)|1) only once.
			// otherwise, we would need to pull the first case out of the loop.
			s = (s + ((doc>>7)|1)) & mMask;
		}
		
		mTable[s] = doc;
	}

	public boolean exists(int doc) {
		int s = doc & mMask;
		
		for (;;) {
			int v = mTable[s];
			
			if (v == EMPTY) return false;
			if (v == doc) return true;
			
			// see put() for algorithm details.
			s = (s + ((doc>>7)|1)) & mMask;
		}
	}

	public int size() {
		return mSize;
	}

	public DocIterator iterator() {
		return new DocIterator() {
			private int mPos = 0;
			@SuppressWarnings("unused")
			private int mDoc;
			
			{ goNext(); }

			@Override
			public boolean hasNext() {
				return mPos < mTable.length;
			}

			@Override
			public Integer next() {
				return nextDoc();
			}

			@Override
			public void remove() {
				// do nothing
			}

			private void goNext() {
				while (mPos < mTable.length && mTable[mPos] == EMPTY) { 
					mPos++; 
				}
			}

			// modify to return -1 at end of iteration?
			@Override
			public int nextDoc() {
				int doc = mTable[mPos];
				mPos ++;
				goNext();
				return doc;
			}

			@Override
			public float score() {
				return 0.0f;
			}
		};
	}

	@Override
	public long getMemorySize() {
		return (mTable.length<<2) + 20;
	}

	@Override
	public DocSet intersection(DocSet other) {
		if (other instanceof HashDocSet) {
			// set "a" to the smallest doc set for the most efficient
			// intersection.
			final HashDocSet a = size()<=other.size() ? this : (HashDocSet)other;
			final HashDocSet b = size()<=other.size() ? (HashDocSet)other : this;

			int[] result = new int[a.size()];
			int resultCount = 0;
			
			for (int i=0; i < a.mTable.length; i++) {
				int id = a.mTable[i];
				if (id >= 0 && b.exists(id)) 
					result[resultCount++] = id;
			}
			
			return new HashDocSet(result, 0, resultCount);

		} else {
			int[] result = new int[size()];
			int resultCount = 0;
			
			for (int i=0; i < mTable.length; i++) {
				int id = mTable[i];
				if (id >= 0 && other.exists(id)) 
					result[resultCount++] = id;
			}
			
			return new HashDocSet(result,0,resultCount);
		}
	}

	@Override
	public int intersectionSize(DocSet other) {
		if (other instanceof HashDocSet) {
			// set "a" to the smallest doc set for the most efficient
			// intersection.
			final HashDocSet a = size()<=other.size() ? this : (HashDocSet)other;
			final HashDocSet b = size()<=other.size() ? (HashDocSet)other : this;

			int resultCount = 0;
			
			for (int i=0; i < a.mTable.length; i++) {
				int id = a.mTable[i];
				if (id >= 0 && b.exists(id)) 
					resultCount ++;
			}
			
			return resultCount;
			
		} else {
			int resultCount = 0;
			
			for (int i=0; i < mTable.length; i++) {
				int id = mTable[i];
				if (id >= 0 && other.exists(id)) 
					resultCount ++;
			}
			
			return resultCount;
		}
	}

	@Override
	public boolean intersects(DocSet other) {
		if (other instanceof HashDocSet) {
			// set "a" to the smallest doc set for the most efficient
			// intersection.
			final HashDocSet a = size()<=other.size() ? this : (HashDocSet)other;
			final HashDocSet b = size()<=other.size() ? (HashDocSet)other : this;

			for (int i=0; i < a.mTable.length; i++) {
				int id = a.mTable[i];
				if (id >= 0 && b.exists(id)) 
					return true;
			}
			
			return false;
			
		} else {
			for (int i=0; i < mTable.length; i++) {
				int id = mTable[i];
				if (id >= 0 && other.exists(id)) 
					return true;
			}
			
			return false;
		}
	}

	@Override
	public DocSet andNot(DocSet other) {
		int[] result = new int[size()];
		int resultCount = 0;

		for (int i=0; i < mTable.length; i++) {
			int id = mTable[i];
			if (id >= 0 && !other.exists(id)) 
				result[resultCount++]=id;
		}
		
		return new HashDocSet(result,0,resultCount);
	}

	@Override
	public DocSet union(DocSet other) {
		if (other instanceof HashDocSet) {
			// set "a" to the smallest doc set
			final HashDocSet a = size()<=other.size() ? this : (HashDocSet)other;
			final HashDocSet b = size()<=other.size() ? (HashDocSet)other : this;

			int[] result = new int[a.size()+b.size()];
			int resultCount = 0;
			
			// iterate over the largest table first, adding w/o checking.
			for (int i=0; i < b.mTable.length; i++) {
				int id = b.mTable[i];
				if (id >= 0) 
					result[resultCount++]=id;
			}

			// now iterate over smaller set, adding all not already in larger set.
			for (int i=0; i < a.mTable.length; i++) {
				int id = a.mTable[i];
				if (id>=0 && !b.exists(id)) 
					result[resultCount++] = id;
			}

			return new HashDocSet(result,0,resultCount);
			
		} else {
			return other.union(this);
		}
	}

	@Override
	protected HashDocSet clone() {
		return new HashDocSet(this);
	}

	// don't implement andNotSize() and unionSize() on purpose... they are implemented
	// in BaseDocSet in terms of intersectionSize().
}
