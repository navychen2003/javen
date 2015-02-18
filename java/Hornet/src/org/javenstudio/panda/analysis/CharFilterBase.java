package org.javenstudio.panda.analysis;

import java.io.Reader;
import java.util.Arrays;

import org.javenstudio.common.indexdb.util.ArrayUtil;

/**
 * Base utility class for implementing a {@link CharFilter}.
 * You subclass this, and then record mappings by calling
 * {@link #addOffCorrectMap}, and then invoke the correct
 * method to correct an offset.
 */
public abstract class CharFilterBase extends CharFilter {

	private int mOffsets[];
	private int mDiffs[];
	private int mSize = 0;
  
	public CharFilterBase(Reader in) {
		super(in);
	}

	/** Retrieve the corrected offset. */
	@Override
	protected int correct(int currentOff) {
		if (mOffsets == null || currentOff < mOffsets[0]) 
			return currentOff;
    
		int hi = mSize - 1;
		if (currentOff >= mOffsets[hi])
			return currentOff + mDiffs[hi];

		int lo = 0;
		int mid = -1;
    
		while (hi >= lo) {
			mid = (lo + hi) >>> 1;
			if (currentOff < mOffsets[mid])
				hi = mid - 1;
			else if (currentOff > mOffsets[mid])
				lo = mid + 1;
			else
				return currentOff + mDiffs[mid];
		}

		if (currentOff < mOffsets[mid])
			return mid == 0 ? currentOff : currentOff + mDiffs[mid-1];
		else
			return currentOff + mDiffs[mid];
	}
  
	protected int getLastCumulativeDiff() {
		return mOffsets == null ? 0 : mDiffs[mSize-1];
	}

	/**
	 * <p>
	 *   Adds an offset correction mapping at the given output stream offset.
	 * </p>
	 * <p>
	 *   Assumption: the offset given with each successive call to this method
	 *   will not be smaller than the offset given at the previous invocation.
	 * </p>
	 *
	 * @param off The output stream offset at which to apply the correction
	 * @param cumulativeDiff The input offset is given by adding this
	 *                       to the output offset
	 */
	protected void addOffCorrectMap(int off, int cumulativeDiff) {
		if (mOffsets == null) {
			mOffsets = new int[64];
			mDiffs = new int[64];
			
		} else if (mSize == mOffsets.length) {
			mOffsets = ArrayUtil.grow(mOffsets);
			mDiffs = ArrayUtil.grow(mDiffs);
		}
    
		assert (mSize == 0 || off >= mOffsets[mSize - 1]) 
			: "Offset #" + mSize + "(" + off + ") is less than the last recorded offset " 
			+ mOffsets[mSize - 1] + "\n" + Arrays.toString(mOffsets) + "\n" + Arrays.toString(mDiffs);
    
		if (mSize == 0 || off != mOffsets[mSize - 1]) {
			mOffsets[mSize] = off;
			mDiffs[mSize++] = cumulativeDiff;
			
		} else { // Overwrite the diff at the last recorded offset
			mDiffs[mSize - 1] = cumulativeDiff;
		}
	}
	
}
