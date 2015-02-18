package org.javenstudio.common.indexdb.store;

import java.io.IOException;
import java.util.Arrays;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IIndexInput;
import org.javenstudio.common.indexdb.IIndexOutput;
import org.javenstudio.common.indexdb.util.MutableBits;

/** 
 * Optimized implementation of a vector of bits.  This is more-or-less like
 *  java.util.BitSet, but also includes the following:
 *  <ul>
 *  <li>a count() method, which efficiently computes the number of one bits;</li>
 *  <li>optimized read from and write to disk;</li>
 *  <li>inlinable get() method;</li>
 *  <li>store and load, as bit set or d-gaps, depending on sparseness;</li> 
 *  </ul>
 *
 * pkg-private: if this thing is generally useful then it can go back in .util,
 * but the serialization must be here underneath the codec.
 */
public final class BitVector implements Cloneable, MutableBits {

	private byte[] mBits;
	private int mSize;
	private int mCount;
	private int mVersion;

	/** Constructs a vector capable of holding <code>n</code> bits. */
	public BitVector(int n) {
		mSize = n;
		mBits = new byte[getNumBytes(mSize)];
		mCount = 0;
	}

	BitVector(byte[] bits, int size) {
		mBits = bits;
		mSize = size;
		mCount = -1;
	}
  
	private int getNumBytes(int size) {
		int bytesLength = size >>> 3;
		if ((size & 7) != 0) 
			bytesLength ++;
		
		return bytesLength;
	}
  
	@Override
	public BitVector clone() {
		byte[] copyBits = new byte[mBits.length];
		System.arraycopy(mBits, 0, copyBits, 0, mBits.length);
		BitVector clone = new BitVector(copyBits, mSize);
		clone.mCount = mCount;
		return clone;
	}
  
	/** Sets the value of <code>bit</code> to one. */
	public final void set(int bit) {
		if (bit >= mSize) 
			throw new ArrayIndexOutOfBoundsException("bit=" + bit + " size=" + mSize);
		
		mBits[bit >> 3] |= 1 << (bit & 7);
		mCount = -1;
	}

	/** 
	 * Sets the value of <code>bit</code> to true, and
	 *  returns true if bit was already set 
	 */
	public final boolean getAndSet(int bit) {
		if (bit >= mSize) 
			throw new ArrayIndexOutOfBoundsException("bit=" + bit + " size=" + mSize);
		
		final int pos = bit >> 3;
		final int v = mBits[pos];
		final int flag = 1 << (bit & 7);
		if ((flag & v) != 0)
			return true;
		
		mBits[pos] = (byte) (v | flag);
		if (mCount != -1) {
			mCount++;
			assert mCount <= mSize;
		}
		
		return false;
	}

	/** Sets the value of <code>bit</code> to zero. */
	public final void clear(int bit) {
		if (bit >= mSize) 
			throw new ArrayIndexOutOfBoundsException(bit);
		
		mBits[bit >> 3] &= ~(1 << (bit & 7));
		mCount = -1;
	}

	public final boolean getAndClear(int bit) {
		if (bit >= mSize) 
			throw new ArrayIndexOutOfBoundsException(bit);
		
		final int pos = bit >> 3;
		final int v = mBits[pos];
		final int flag = 1 << (bit & 7);
		if ((flag & v) == 0) 
			return false;
		
		mBits[pos] &= ~flag;
		if (mCount != -1) {
			mCount--;
			assert mCount >= 0;
		}
		
		return true;
	}

	/** 
	 * Returns <code>true</code> if <code>bit</code> is one and
	 * <code>false</code> if it is zero. 
	 */
	public final boolean get(int bit) {
		assert bit >= 0 && bit < mSize: "bit " + bit + " is out of bounds 0.." + (mSize-1);
		return (mBits[bit >> 3] & (1 << (bit & 7))) != 0;
	}

	/** 
	 * Returns the number of bits in this vector.  This is also one greater than
	 * the number of the largest valid bit number. 
	 */
	public final int size() {
		return mSize;
	}

	@Override
	public int length() {
		return mSize;
	}

	/** 
	 * Returns the total number of one bits in this vector.  This is efficiently
	 * computed and cached, so that, if the vector is not changed, no
	 * recomputation is done for repeated calls. 
	 */
	public final int count() {
		// if the vector has been modified
		if (mCount == -1) {
			int c = 0;
			int end = mBits.length;
			for (int i = 0; i < end; i++) {
				c += BYTE_COUNTS[mBits[i] & 0xFF];	  // sum bits per byte
			}
			mCount = c;
		}
		assert mCount <= mSize: "count=" + mCount + " size=" + mSize;
		return mCount;
	}

	/** For testing */
	public final int getRecomputedCount() {
		int c = 0;
		int end = mBits.length;
		for (int i = 0; i < end; i++) {
			c += BYTE_COUNTS[mBits[i] & 0xFF];	  // sum bits per byte
		}
		return c;
	}

	private static final byte[] BYTE_COUNTS = {	  // table of bits/byte
		0, 1, 1, 2, 1, 2, 2, 3, 1, 2, 2, 3, 2, 3, 3, 4,
		1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
		1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
		2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
		1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
		2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
		2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
		3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
		1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
		2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
		2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
		3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
		2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
		3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
		3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
		4, 5, 5, 6, 5, 6, 6, 7, 5, 6, 6, 7, 6, 7, 7, 8
	};

	private static String CODEC = "BitVector";

	// Version before version tracking was added:
	public final static int VERSION_PRE = -1;

	// First version:
	public final static int VERSION_START = 0;

	// Changed DGaps to encode gaps between cleared bits, not
	// set:
	public final static int VERSION_DGAPS_CLEARED = 1;

	// Increment version to change it:
	public final static int VERSION_CURRENT = VERSION_DGAPS_CLEARED;

	public int getVersion() {
		return mVersion;
	}

	/** 
	 * Writes this vector to the file <code>name</code> in Directory
	 * <code>d</code>, in a format that can be read by the constructor {@link
	 * #BitVector(Directory, String, IOContext)}. 
	 */
	public final void write(IIndexContext context, IDirectory d, String name) throws IOException {
		assert !(d instanceof CompoundFileDirectory);
		IIndexOutput output = d.createOutput(context, name);
		try {
			output.writeInt(-2);
			context.writeCodecHeader(output, CODEC, VERSION_CURRENT);
			if (isSparse()) { 
				// sparse bit-set more efficiently saved as d-gaps.
				writeClearedDgaps(output);
			} else {
				writeBits(output);
			}
			assert verifyCount();
		} finally {
			output.close();
		}
	}

	/** Invert all bits */
	public void invertAll() {
		if (mCount != -1) {
			mCount = mSize - mCount;
		}
		if (mBits.length > 0) {
			for (int idx=0; idx < mBits.length; idx++) {
				mBits[idx] = (byte) (~mBits[idx]);
			}
			clearUnusedBits();
		}
	}

	private void clearUnusedBits() {
		// Take care not to invert the "unused" bits in the
		// last byte:
		if (mBits.length > 0) {
			final int lastNBits = mSize & 7;
			if (lastNBits != 0) {
				final int mask = (1 << lastNBits)-1;
				mBits[mBits.length-1] &= mask;
			}
		}
	}

	/** Set all bits */
	public void setAll() {
		Arrays.fill(mBits, (byte) 0xff);
		clearUnusedBits();
		mCount = mSize;
	}
     
	/** Write as a bit set */
	private void writeBits(IIndexOutput output) throws IOException {
		output.writeInt(size());        // write size
		output.writeInt(count());       // write count
		output.writeBytes(mBits, mBits.length);
	}
  
	/** Write as a d-gaps list */
	private void writeClearedDgaps(IIndexOutput output) throws IOException {
		output.writeInt(-1);            // mark using d-gaps                         
		output.writeInt(size());        // write size
		output.writeInt(count());       // write count
		
		int last=0;
		int numCleared = size()-count();
		
		for (int i=0; i < mBits.length && numCleared>0; i++) {
			if (mBits[i] != (byte) 0xff) {
				output.writeVInt(i-last);
				output.writeByte(mBits[i]);
				
				last = i;
				numCleared -= (8-BYTE_COUNTS[mBits[i] & 0xFF]);
				assert numCleared >= 0 || (i == (mBits.length-1) && numCleared == -(8-(mSize&7)));
			}
		}
	}

	/** 
	 * Indicates if the bit vector is sparse and should be saved as a d-gaps list, 
	 * or dense, and should be saved as a bit set. 
	 */
	private boolean isSparse() {
		final int clearedCount = size() - count();
		if (clearedCount == 0) 
			return true;
		
		final int avgGapLength = mBits.length / clearedCount;

		// expected number of bytes for vInt encoding of each gap
		final int expectedDGapBytes;
		if (avgGapLength <= (1<< 7)) {
			expectedDGapBytes = 1;
		} else if (avgGapLength <= (1<<14)) {
			expectedDGapBytes = 2;
		} else if (avgGapLength <= (1<<21)) {
			expectedDGapBytes = 3;
		} else if (avgGapLength <= (1<<28)) {
			expectedDGapBytes = 4;
		} else {
			expectedDGapBytes = 5;
		}

		// +1 because we write the byte itself that contains the
		// set bit
		final int bytesPerSetBit = expectedDGapBytes + 1;
    
		// note: adding 32 because we start with ((int) -1) to indicate d-gaps format.
		final long expectedBits = 32 + 8 * bytesPerSetBit * clearedCount;

		// note: factor is for read/write of byte-arrays being faster than vints.  
		final long factor = 10;  
		return factor * expectedBits < size();
	}

	/** 
	 * Constructs a bit vector from the file <code>name</code> in Directory
	 * <code>d</code>, as written by the {@link #write} method.
	 */
	public BitVector(IIndexContext context, IDirectory d, String name) throws IOException {
		IIndexInput input = d.openInput(context, name);

		try {
			final int firstInt = input.readInt();

			if (firstInt == -2) {
				// New format, with full header & version:
				mVersion = context.checkCodecHeader(input, CODEC, VERSION_START, VERSION_CURRENT);
				mSize = input.readInt();
			} else {
				mVersion = VERSION_PRE;
				mSize = firstInt;
			}
			
			if (mSize == -1) {
				if (mVersion >= VERSION_DGAPS_CLEARED) 
					readClearedDgaps(input);
				else 
					readSetDgaps(input);
			} else {
				readBits(input);
			}

			if (mVersion < VERSION_DGAPS_CLEARED) 
				invertAll();

			assert verifyCount();
		} finally {
			input.close();
		}
	}

	// asserts only
	private boolean verifyCount() {
		assert mCount != -1;
		final int countSav = mCount;
		mCount = -1;
		assert countSav == count(): "saved count was " + countSav + " but recomputed count is " + mCount;
		return true;
	}

	/** Read as a bit set */
	private void readBits(IIndexInput input) throws IOException {
		mCount = input.readInt();        		// read count
		mBits = new byte[getNumBytes(mSize)];	// allocate bits
		input.readBytes(mBits, 0, mBits.length);
	}

	/** read as a d-gaps list */ 
	private void readSetDgaps(IIndexInput input) throws IOException {
		mSize = input.readInt();       			// (re)read size
		mCount = input.readInt();        		// read count
		mBits = new byte[getNumBytes(mSize)];	// allocate bits
		
		int last=0;
		int n = count();
		
		while (n>0) {
			last += input.readVInt();
			mBits[last] = input.readByte();
			n -= BYTE_COUNTS[mBits[last] & 0xFF];
			assert n >= 0;
		}          
	}

	/** read as a d-gaps cleared bits list */ 
	private void readClearedDgaps(IIndexInput input) throws IOException {
		mSize = input.readInt();       // (re)read size
		mCount = input.readInt();        // read count
		mBits = new byte[getNumBytes(mSize)];     // allocate bits
		
		Arrays.fill(mBits, (byte) 0xff);
		clearUnusedBits();
		
		int last=0;
		int numCleared = size()-count();
		
		while (numCleared>0) {
			last += input.readVInt();
			mBits[last] = input.readByte();
			numCleared -= 8-BYTE_COUNTS[mBits[last] & 0xFF];
			assert numCleared >= 0 || (last == (mBits.length-1) && numCleared == -(8-(mSize&7)));
		}
	}
  
}
