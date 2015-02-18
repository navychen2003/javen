package org.javenstudio.hornet.store.packed;

import java.io.IOException;
import java.util.Arrays;

import org.javenstudio.common.indexdb.IDataInput;
import org.javenstudio.common.indexdb.util.JvmUtil;

/**
 * Direct wrapping of 16-bits values to a backing array.
 * 
 */
final class Direct16 extends MutableImpl {
	private final short[] mValues;

	Direct16(int valueCount) {
		super(valueCount, 16);
		mValues = new short[valueCount];
	}

	Direct16(IDataInput in, int valueCount) throws IOException {
		this(valueCount);
		for (int i = 0; i < valueCount; ++i) {
			mValues[i] = in.readShort();
		}
		final int mod = valueCount % 4;
		if (mod != 0) {
			for (int i = mod; i < 4; ++i) {
				in.readShort();
			}
		}
	}

	@Override
	public long get(final int index) {
		return mValues[index] & 0xFFFFL;
	}

	@Override
	public void set(final int index, final long value) {
		mValues[index] = (short) (value);
	}

	@Override
	public long ramBytesUsed() {
		return JvmUtil.sizeOf(mValues);
	}

	@Override
	public void clear() {
		Arrays.fill(mValues, (short) 0L);
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
			arr[o] = mValues[i] & 0xFFFFL;
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
			mValues[i] = (short) arr[o];
		}
		return sets;
	}

	@Override
	public void fill(int fromIndex, int toIndex, long val) {
		assert val == (val & 0xFFFFL);
		Arrays.fill(mValues, fromIndex, toIndex, (short) val);
	}
	
}
