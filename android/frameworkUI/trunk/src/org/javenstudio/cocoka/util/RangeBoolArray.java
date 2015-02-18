package org.javenstudio.cocoka.util;

// This is an array whose index ranges from min to max (inclusive).
public class RangeBoolArray {
    private boolean[] mData;
    private int mOffset;

    public RangeBoolArray(int min, int max) {
        mData = new boolean[max - min + 1];
        mOffset = min;
    }

    // Wraps around an existing array
    public RangeBoolArray(boolean[] src, int min, int max) {
        mData = src;
        mOffset = min;
    }

    public void put(int i, boolean object) {
        mData[i - mOffset] = object;
    }

    public boolean get(int i) {
        return mData[i - mOffset];
    }

    public int indexOf(boolean object) {
        for (int i = 0; i < mData.length; i++) {
            if (mData[i] == object) return i + mOffset;
        }
        return Integer.MAX_VALUE;
    }
}
