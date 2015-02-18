package org.javenstudio.cocoka.util;

// This is an array whose index ranges from min to max (inclusive).
public class RangeIntArray {
    private int[] mData;
    private int mOffset;

    public RangeIntArray(int min, int max) {
        mData = new int[max - min + 1];
        mOffset = min;
    }

    // Wraps around an existing array
    public RangeIntArray(int[] src, int min, int max) {
        mData = src;
        mOffset = min;
    }

    public void put(int i, int object) {
        mData[i - mOffset] = object;
    }

    public int get(int i) {
        return mData[i - mOffset];
    }

    public int indexOf(int object) {
        for (int i = 0; i < mData.length; i++) {
            if (mData[i] == object) return i + mOffset;
        }
        return Integer.MAX_VALUE;
    }
}
