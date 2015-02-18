package org.javenstudio.common.indexdb.search;

import java.io.IOException;
import java.util.Arrays;

import org.javenstudio.common.indexdb.IDocIdSetIterator;
import org.javenstudio.common.indexdb.IFixedBitSet;
import org.javenstudio.common.indexdb.util.BitUtil;
import org.javenstudio.common.indexdb.util.Bits;

/** 
 * BitSet of fixed length (numBits), backed by accessible
 *  ({@link #getBits}) long[], accessed with an int index,
 *  implementing Bits and DocIdSet.  Unlike {@link
 *  OpenBitSet} this bit set does not auto-expand, cannot
 *  handle long index, and does not have fastXX/XX variants
 *  (just X).
 *  
 * TODO: maybe merge with BitVector?  Problem is BitVector
 * caches its cardinality...
 */
public abstract class FixedBitSet extends DocIdSet implements IFixedBitSet {
	
	protected final long[] mBits;
	protected int mNumBits;

	/** returns the number of 64 bit words it would take to hold numBits */
	public static int bits2words(int numBits) {
		int numLong = numBits >>> 6;
		if ((numBits & 63) != 0) 
			numLong ++;
		
		return numLong;
	}

	public FixedBitSet(int numBits) {
		mNumBits = numBits;
		mBits = new long[bits2words(numBits)];
	}

	/** Makes full copy. */
	public FixedBitSet(FixedBitSet other) {
		mBits = new long[other.mBits.length];
		System.arraycopy(other.mBits, 0, mBits, 0, mBits.length);
		mNumBits = other.mNumBits;
	}

	@Override
	public abstract DocIdSetIterator iterator();

	@Override
	public Bits getBits() {
		return this;
	}

	@Override
	public int length() {
		return mNumBits;
	}

	/** This DocIdSet implementation is cacheable. */
	@Override
	public boolean isCacheable() {
		return true;
	}

	/** Expert. */
	@Override
	public long[] getBitsArray() {
		return mBits;
	}

	/** 
	 * Returns number of set bits.  NOTE: this visits every
	 *  long in the backing bits array, and the result is not
	 *  internally cached! 
	 */
	@Override
	public int cardinality() {
		return (int) BitUtil.pop_array(mBits, 0, mBits.length);
	}

	@Override
	public boolean get(int index) {
		assert index >= 0 && index < mNumBits: "index=" + index;
		
		int i = index >> 6;               // div 64
		// signed shift will keep a negative index and force an
		// array-index-out-of-bounds-exception, removing the need for an explicit check.
		int bit = index & 0x3f;           // mod 64
		long bitmask = 1L << bit;
		
		return (mBits[i] & bitmask) != 0;
	}

	@Override
	public void set(int index) {
		assert index >= 0 && index < mNumBits;
		
		int wordNum = index >> 6; 	// div 64
		int bit = index & 0x3f;     // mod 64
		long bitmask = 1L << bit;
		mBits[wordNum] |= bitmask;
	}

	@Override
	public boolean getAndSet(int index) {
		assert index >= 0 && index < mNumBits;
		
		int wordNum = index >> 6;  	// div 64
		int bit = index & 0x3f;     // mod 64
		long bitmask = 1L << bit;
		boolean val = (mBits[wordNum] & bitmask) != 0;
		mBits[wordNum] |= bitmask;
		
		return val;
	}

	@Override
	public void clear(int index) {
		assert index >= 0 && index < mNumBits;
		
		int wordNum = index >> 6;
		int bit = index & 0x03f;
		long bitmask = 1L << bit;
		mBits[wordNum] &= ~bitmask;
	}

	@Override
	public boolean getAndClear(int index) {
		assert index >= 0 && index < mNumBits;
		
		int wordNum = index >> 6; 	// div 64
		int bit = index & 0x3f;     // mod 64
		long bitmask = 1L << bit;
		boolean val = (mBits[wordNum] & bitmask) != 0;
		mBits[wordNum] &= ~bitmask;
		
		return val;
	}

	/** 
	 * Returns the index of the first set bit starting at the index specified.
	 *  -1 is returned if there are no more set bits.
	 */
	@Override
	public int nextSetBit(int index) {
		assert index >= 0 && index < mNumBits;
		
		int i = index >> 6;
		final int subIndex = index & 0x3f; 	// index within the word
		long word = mBits[i] >> subIndex;	// skip all the bits to the right of index

		if (word!=0) 
			return (i<<6) + subIndex + BitUtil.ntz(word);

		while (++i < mBits.length) {
			word = mBits[i];
			if (word != 0) 
				return (i<<6) + BitUtil.ntz(word);
		}

		return -1;
	}

	/** 
	 * Returns the index of the last set bit before or on the index specified.
	 *  -1 is returned if there are no more set bits.
	 */
	@Override
	public int prevSetBit(int index) {
		assert index >= 0 && index < mNumBits: "index=" + index + " numBits=" + mNumBits;
		
		int i = index >> 6;
		final int subIndex = index & 0x3f;  // index within the word
		long word = (mBits[i] << (63-subIndex));  // skip all the bits to the left of index

		if (word != 0) 
			return (i << 6) + subIndex - Long.numberOfLeadingZeros(word); // See LUCENE-3197

		while (--i >= 0) {
			word = mBits[i];
			if (word !=0 ) 
				return (i << 6) + 63 - Long.numberOfLeadingZeros(word);
		}

		return -1;
	}

	/** 
	 * Does in-place OR of the bits provided by the
	 *  iterator. 
	 */
	public void or(IDocIdSetIterator iter) throws IOException {
		//if (iter instanceof OpenBitSetIterator && iter.docID() == -1) {
		//	final OpenBitSetIterator obs = (OpenBitSetIterator) iter;
		//	or(obs.mArr, obs.mWords);
		//	// advance after last doc that would be accepted if standard
		//	// iteration is used (to exhaust it):
		//	obs.advance(mNumBits);
		//
		//} else {
			int doc;
			while ((doc = iter.nextDoc()) < mNumBits) {
				set(doc);
			}
		//}
	}

	/** this = this OR other */
	public void or(FixedBitSet other) {
		or(other.mBits, other.mBits.length);
	}
  
	protected void or(final long[] otherArr, final int otherLen) {
		final long[] thisArr = this.mBits;
		int pos = Math.min(thisArr.length, otherLen);
		while (--pos >= 0) {
			thisArr[pos] |= otherArr[pos];
		}
	}

	/** 
	 * Does in-place AND of the bits provided by the
	 *  iterator. 
	 */
	public void and(IDocIdSetIterator iter) throws IOException {
		//if (iter instanceof OpenBitSetIterator && iter.docID() == -1) {
		//	final OpenBitSetIterator obs = (OpenBitSetIterator) iter;
		//	and(obs.mArr, obs.mWords);
		//	// advance after last doc that would be accepted if standard
		//	// iteration is used (to exhaust it):
		//	obs.advance(mNumBits);
		//	
		//} else {
			if (mNumBits == 0) return;
			int disiDoc, bitSetDoc = nextSetBit(0);
			while (bitSetDoc != -1 && (disiDoc = iter.advance(bitSetDoc)) < mNumBits) {
				clear(bitSetDoc, disiDoc);
				disiDoc ++;
				bitSetDoc = (disiDoc < mNumBits) ? nextSetBit(disiDoc) : -1;
			}
			if (bitSetDoc != -1) 
				clear(bitSetDoc, mNumBits);
		//}
	}

	/** this = this AND other */
	public void and(FixedBitSet other) {
		and(other.mBits, other.mBits.length);
	}
  
	protected void and(final long[] otherArr, final int otherLen) {
		final long[] thisArr = this.mBits;
		int pos = Math.min(thisArr.length, otherLen);
		while (--pos >= 0) {
			thisArr[pos] &= otherArr[pos];
		}
		if (thisArr.length > otherLen) 
			Arrays.fill(thisArr, otherLen, thisArr.length, 0L);
	}

	/** 
	 * Does in-place AND NOT of the bits provided by the
	 *  iterator. 
	 */
	public void andNot(IDocIdSetIterator iter) throws IOException {
		//if (iter instanceof OpenBitSetIterator && iter.docID() == -1) {
		//	final OpenBitSetIterator obs = (OpenBitSetIterator) iter;
		//	andNot(obs.mArr, obs.mWords);
		//	// advance after last doc that would be accepted if standard
		//	// iteration is used (to exhaust it):
		//	obs.advance(mNumBits);
		//	
		//} else {
			int doc;
			while ((doc = iter.nextDoc()) < mNumBits) {
				clear(doc);
			}
		//}
	}

	/** this = this AND NOT other */
	public void andNot(FixedBitSet other) {
		andNot(other.mBits, other.mBits.length);
	}
  
	protected void andNot(final long[] otherArr, final int otherLen) {
		final long[] thisArr = this.mBits;
		int pos = Math.min(thisArr.length, otherLen);
		while (--pos >= 0) {
			thisArr[pos] &= ~otherArr[pos];
		}
	}

	// NOTE: no .isEmpty() here because that's trappy (ie,
	// typically isEmpty is low cost, but this one wouldn't
	// be)

	/** 
	 * Flips a range of bits
	 *
	 * @param startIndex lower index
	 * @param endIndex one-past the last bit to flip
	 */
	public void flip(int startIndex, int endIndex) {
		assert startIndex >= 0 && startIndex < mNumBits;
		assert endIndex >= 0 && endIndex <= mNumBits;
		
		if (endIndex <= startIndex) 
			return;

		int startWord = startIndex >> 6;
		int endWord = (endIndex-1) >> 6;

		/*** Grrr, java shifting wraps around so -1L>>>64 == -1
		 * for that reason, make sure not to use endmask if the bits to flip will
		 * be zero in the last word (redefine endWord to be the last changed...)
    	long startmask = -1L << (startIndex & 0x3f);     // example: 11111...111000
    	long endmask = -1L >>> (64-(endIndex & 0x3f));   // example: 00111...111111
		 ***/

		long startmask = -1L << startIndex;
		long endmask = -1L >>> -endIndex;  // 64-(endIndex&0x3f) is the same as -endIndex due to wrap

		if (startWord == endWord) {
			mBits[startWord] ^= (startmask & endmask);
			return;
		}

		mBits[startWord] ^= startmask;

		for (int i=startWord+1; i < endWord; i++) {
			mBits[i] = ~mBits[i];
		}

		mBits[endWord] ^= endmask;
	}

	/** 
	 * Sets a range of bits
	 *
	 * @param startIndex lower index
	 * @param endIndex one-past the last bit to set
	 */
	public void set(int startIndex, int endIndex) {
		assert startIndex >= 0 && startIndex < mNumBits;
		assert endIndex >= 0 && endIndex <= mNumBits;
		
		if (endIndex <= startIndex) 
			return;

		int startWord = startIndex >> 6;
		int endWord = (endIndex-1) >> 6;

		long startmask = -1L << startIndex;
		long endmask = -1L >>> -endIndex;  // 64-(endIndex&0x3f) is the same as -endIndex due to wrap

		if (startWord == endWord) {
			mBits[startWord] |= (startmask & endmask);
			return;
		}

		mBits[startWord] |= startmask;
		Arrays.fill(mBits, startWord+1, endWord, -1L);
		mBits[endWord] |= endmask;
	}

	/** 
	 * Clears a range of bits.
	 *
	 * @param startIndex lower index
	 * @param endIndex one-past the last bit to clear
	 */
	public void clear(int startIndex, int endIndex) {
		assert startIndex >= 0 && startIndex < mNumBits;
		assert endIndex >= 0 && endIndex <= mNumBits;
		
		if (endIndex <= startIndex) 
			return;

		int startWord = startIndex >> 6;
		int endWord = (endIndex-1) >> 6;

		long startmask = -1L << startIndex;
		long endmask = -1L >>> -endIndex;  // 64-(endIndex&0x3f) is the same as -endIndex due to wrap

		// invert masks since we are clearing
		startmask = ~startmask;
		endmask = ~endmask;

		if (startWord == endWord) {
			mBits[startWord] &= (startmask | endmask);
			return;
		}

		mBits[startWord] &= startmask;
		Arrays.fill(mBits, startWord+1, endWord, 0L);
		mBits[endWord] &= endmask;
	}

	@Override
	public abstract FixedBitSet clone();

	/** returns true if both sets have the same bits set */
	@Override
	public boolean equals(Object o) {
		if (this == o) 
			return true;
		
		if (!(o instanceof FixedBitSet)) 
			return false;
		
		FixedBitSet other = (FixedBitSet) o;
		if (mNumBits != other.length()) 
			return false;
		
		return Arrays.equals(mBits, other.mBits);
	}

	@Override
	public int hashCode() {
		long h = 0;
		for (int i = mBits.length; --i>=0;) {
			h ^= mBits[i];
			h = (h << 1) | (h >>> 63); // rotate left
		}
		// fold leftmost bits into right and add a constant to prevent
		// empty sets from returning 0, which is too common.
		return (int) ((h>>32) ^ h) + 0x98761234;
	}
	
}
