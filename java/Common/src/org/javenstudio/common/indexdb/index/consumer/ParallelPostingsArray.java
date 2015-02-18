package org.javenstudio.common.indexdb.index.consumer;

import org.javenstudio.common.indexdb.util.ArrayUtil;
import org.javenstudio.common.indexdb.util.JvmUtil;

public class ParallelPostingsArray {
	public final static int BYTES_PER_POSTING = 3 * JvmUtil.NUM_BYTES_INT;

	protected final int mSize;
	protected final int[] mTextStarts;
	protected final int[] mIntStarts;
	protected final int[] mByteStarts;

	public ParallelPostingsArray(final int size) {
		mSize = size;
		mTextStarts = new int[size];
		mIntStarts = new int[size];
		mByteStarts = new int[size];
	}

	public int getBytesPerPosting() { return BYTES_PER_POSTING; }
	public int getSize() { return mSize; }
	
	public int[] getTextStarts() { return mTextStarts; }
	public int[] getIntStarts() { return mIntStarts; }
	public int[] getByteStarts() { return mByteStarts; }
	
	public int getTextStartAt(int pos) { return mTextStarts[pos]; }
	public int getIntStartAt(int pos) { return mIntStarts[pos]; }
	public int getByteStartAt(int pos) { return mByteStarts[pos]; }

	public void setTextStartAt(int pos, int val) { mTextStarts[pos] = val; }
	public void setIntStartAt(int pos, int val) { mIntStarts[pos] = val; }
	public void setByteStartAt(int pos, int val) { mByteStarts[pos] = val; }
	
	public ParallelPostingsArray newInstance(int size) {
		return new ParallelPostingsArray(size);
	}

	public ParallelPostingsArray grow() {
		int newSize = ArrayUtil.oversize(mSize + 1, getBytesPerPosting());
		ParallelPostingsArray newArray = newInstance(newSize);
		copyTo(newArray, mSize);
		return newArray;
	}

	public void copyTo(ParallelPostingsArray toArray, int numToCopy) {
		System.arraycopy(mTextStarts, 0, toArray.mTextStarts, 0, numToCopy);
		System.arraycopy(mIntStarts, 0, toArray.mIntStarts, 0, numToCopy);
		System.arraycopy(mByteStarts, 0, toArray.mByteStarts, 0, numToCopy);
	}
	
}
