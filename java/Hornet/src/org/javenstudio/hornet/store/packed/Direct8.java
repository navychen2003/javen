package org.javenstudio.hornet.store.packed;

import java.io.IOException;
import java.util.Arrays;

import org.javenstudio.common.indexdb.IDataInput;
import org.javenstudio.common.indexdb.util.JvmUtil;

/**
 * Direct wrapping of 8-bits values to a backing array.
 * 
 */
final class Direct8 extends MutableImpl {
	private final byte[] mValues;

	Direct8(int valueCount) {
		super(valueCount, 8);
		mValues = new byte[valueCount];
	}

	Direct8(IDataInput in, int valueCount) throws IOException {
		this(valueCount);
		for (int i = 0; i < valueCount; ++i) {
			mValues[i] = in.readByte();
		}
		final int mod = valueCount % 8;
		if (mod != 0) {
			for (int i = mod; i < 8; ++i) {
				in.readByte();
			}
		}
	}

	@Override
	public long get(final int index) {
		return mValues[index] & 0xFFL;
	}

	@Override
	public void set(final int index, final long value) {
		mValues[index] = (byte) (value);
	}

	@Override
	public long ramBytesUsed() {
		return JvmUtil.sizeOf(mValues);
	}

	@Override
	public void clear() {
		Arrays.fill(mValues, (byte) 0L);
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
			arr[o] = mValues[i] & 0xFFL;
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
			mValues[i] = (byte) arr[o];
		}
		return sets;
	}

	@Override
	public void fill(int fromIndex, int toIndex, long val) {
		assert val == (val & 0xFFL);
		Arrays.fill(mValues, fromIndex, toIndex, (byte) val);
	}
	
}
