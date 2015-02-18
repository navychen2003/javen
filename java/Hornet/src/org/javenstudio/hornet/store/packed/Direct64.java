package org.javenstudio.hornet.store.packed;

import java.io.IOException;
import java.util.Arrays;

import org.javenstudio.common.indexdb.IDataInput;
import org.javenstudio.common.indexdb.util.JvmUtil;

/**
 * Direct wrapping of 64-bits values to a backing array.
 * 
 */
final class Direct64 extends MutableImpl {
	private final long[] mValues;

	Direct64(int valueCount) {
		super(valueCount, 64);
		mValues = new long[valueCount];
	}

	Direct64(IDataInput in, int valueCount) throws IOException {
		this(valueCount);
		for (int i = 0; i < valueCount; ++i) {
			mValues[i] = in.readLong();
		}
	}

	@Override
	public long get(final int index) {
		return mValues[index];
	}

	@Override
	public void set(final int index, final long value) {
		mValues[index] = (value);
	}

	@Override
	public long ramBytesUsed() {
		return JvmUtil.sizeOf(mValues);
	}

	@Override
	public void clear() {
		Arrays.fill(mValues, 0L);
	}

	@Override
	public Object getArray() {
		return mValues;
	}

	@Override
	public boolean hasArray() {
		return true;
	}

	@Override
	public int get(int index, long[] arr, int off, int len) {
		assert len > 0 : "len must be > 0 (got " + len + ")";
		assert index >= 0 && index < mValueCount;
		assert off + len <= arr.length;

		final int gets = Math.min(mValueCount - index, len);
		System.arraycopy(mValues, index, arr, off, gets);
		return gets;
	}

	@Override
	public int set(int index, long[] arr, int off, int len) {
		assert len > 0 : "len must be > 0 (got " + len + ")";
		assert index >= 0 && index < mValueCount;
		assert off + len <= arr.length;

		final int sets = Math.min(mValueCount - index, len);
		System.arraycopy(arr, off, mValues, index, sets);
		return sets;
	}

	@Override
	public void fill(int fromIndex, int toIndex, long val) {
		Arrays.fill(mValues, fromIndex, toIndex, val);
	}
	
}
