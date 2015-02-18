package org.javenstudio.hornet.store.packed;

import java.io.IOException;
import java.util.Arrays;

import org.javenstudio.common.indexdb.IDataInput;
import org.javenstudio.common.indexdb.util.JvmUtil;

/**
 * Direct wrapping of 32-bits values to a backing array.
 * 
 */
final class Direct32 extends MutableImpl {
	private final int[] mValues;

	Direct32(int valueCount) {
		super(valueCount, 32);
		mValues = new int[valueCount];
	}

	Direct32(IDataInput in, int valueCount) throws IOException {
		this(valueCount);
		for (int i = 0; i < valueCount; ++i) {
			mValues[i] = in.readInt();
		}
		final int mod = valueCount % 2;
		if (mod != 0) {
			for (int i = mod; i < 2; ++i) {
				in.readInt();
			}
		}
	}

	@Override
	public long get(final int index) {
		return mValues[index] & 0xFFFFFFFFL;
	}

	@Override
	public void set(final int index, final long value) {
		mValues[index] = (int) (value);
	}

	@Override
	public long ramBytesUsed() {
		return JvmUtil.sizeOf(mValues);
	}

	@Override
	public void clear() {
		Arrays.fill(mValues, (int) 0L);
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
		for (int i = index, o = off, end = index + gets; i < end; ++i, ++o) {
			arr[o] = mValues[i] & 0xFFFFFFFFL;
		}
		return gets;
	}

	@Override
	public int set(int index, long[] arr, int off, int len) {
		assert len > 0 : "len must be > 0 (got " + len + ")";
		assert index >= 0 && index < mValueCount;
		assert off + len <= arr.length;

		final int sets = Math.min(mValueCount - index, len);
		for (int i = index, o = off, end = index + sets; i < end; ++i, ++o) {
			mValues[i] = (int) arr[o];
		}
		return sets;
	}

	@Override
	public void fill(int fromIndex, int toIndex, long val) {
		assert val == (val & 0xFFFFFFFFL);
		Arrays.fill(mValues, fromIndex, toIndex, (int) val);
	}
	
}
