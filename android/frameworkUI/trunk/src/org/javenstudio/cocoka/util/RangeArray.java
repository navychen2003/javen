package org.javenstudio.cocoka.util;

// This is an array whose index ranges from min to max (inclusive).
public class RangeArray<T> {
    private T[] mData;
    private int mOffset;

    @SuppressWarnings("unchecked")
	public RangeArray(int min, int max) {
        mData = (T[]) new Object[max - min + 1];
        mOffset = min;
    }

    // Wraps around an existing array
    public RangeArray(T[] src, int min, int max) {
        if (max - min + 1 != src.length) {
            throw new AssertionError();
        }
        mData = src;
        mOffset = min;
    }

    public void put(int i, T object) {
        mData[i - mOffset] = object;
    }

    public T get(int i) {
        return mData[i - mOffset];
    }

    public int indexOf(T object) {
        for (int i = 0; i < mData.length; i++) {
            if (mData[i] == object) return i + mOffset;
        }
        return Integer.MAX_VALUE;
    }
}
