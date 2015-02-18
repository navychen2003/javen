package org.javenstudio.common.indexdb.index.segment;

import org.javenstudio.common.indexdb.util.Bits;

/**
 * Exposes a slice of an existing Bits as a new Bits.
 *
 */
public final class BitsSlice implements Bits {
	
	private final Bits mParent;
	private final int mStart;
	private final int mLength;

	// start is inclusive; end is exclusive (length = end-start)
	public BitsSlice(Bits parent, ReaderSlice slice) {
		mParent = parent;
		mStart = slice.getStart();
		mLength = slice.getLength();
		
		assert mLength >= 0: "length=" + mLength;
	}
    
	public boolean get(int doc) {
		if (doc >= mLength) 
			throw new RuntimeException("doc " + doc + " is out of bounds 0 .. " + (mLength-1));
		
		assert doc < mLength: "doc=" + doc + " length=" + mLength;
		
		return mParent.get(doc + mStart);
	}

	public int length() {
		return mLength;
	}
	
}
