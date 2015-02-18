package org.javenstudio.hornet.store.packed;

import java.io.IOException;
import java.util.Arrays;

import org.javenstudio.common.indexdb.IDataInput;
import org.javenstudio.common.indexdb.util.JvmUtil;

/**
 * Packs integers into 3 shorts (48 bits per value).
 * 
 */
final class Packed16ThreeBlocks extends MutableImpl {
	public static final int MAX_SIZE = Integer.MAX_VALUE / 3;
	
	private final short[] mBlocks;

	Packed16ThreeBlocks(int valueCount) {
		super(valueCount, 48);
		if (valueCount > MAX_SIZE) 
			throw new ArrayIndexOutOfBoundsException("MAX_SIZE exceeded");
		
		mBlocks = new short[valueCount * 3];
	}

	Packed16ThreeBlocks(IDataInput in, int valueCount) throws IOException {
		this(valueCount);
		for (int i = 0; i < 3 * valueCount; ++i) {
			mBlocks[i] = in.readShort();
		}
		final int mod = mBlocks.length % 4;
		if (mod != 0) {
			for (int i = mod; i < 4; ++i) {
				in.readShort();
			}
		}
	}

	@Override
	public long get(int index) {
		final int o = index * 3;
		return (mBlocks[o] & 0xFFFFL) << 32 | (mBlocks[o+1] & 0xFFFFL) << 16 | 
				(mBlocks[o+2] & 0xFFFFL);
	}

	@Override
	public int get(int index, long[] arr, int off, int len) {
		assert len > 0 : "len must be > 0 (got " + len + ")";
		assert index >= 0 && index < mValueCount;
		assert off + len <= arr.length;

		final int gets = Math.min(mValueCount - index, len);
		for (int i = index * 3, end = (index + gets) * 3; i < end; i+=3) {
			arr[off++] = (mBlocks[i] & 0xFFFFL) << 32 | (mBlocks[i+1] & 0xFFFFL) << 16 | 
					(mBlocks[i+2] & 0xFFFFL);
		}
		return gets;
	}

	@Override
	public void set(int index, long value) {
		final int o = index * 3;
		mBlocks[o] = (short) (value >>> 32);
		mBlocks[o+1] = (short) (value >>> 16);
		mBlocks[o+2] = (short) value;
	}

	@Override
	public int set(int index, long[] arr, int off, int len) {
		assert len > 0 : "len must be > 0 (got " + len + ")";
		assert index >= 0 && index < mValueCount;
		assert off + len <= arr.length;

		final int sets = Math.min(mValueCount - index, len);
		for (int i = off, o = index * 3, end = off + sets; i < end; ++i) {
			final long value = arr[i];
			mBlocks[o++] = (short) (value >>> 32);
			mBlocks[o++] = (short) (value >>> 16);
			mBlocks[o++] = (short) value;
		}
		return sets;
	}

	@Override
	public void fill(int fromIndex, int toIndex, long val) {
		final short block1 = (short) (val >>> 32);
		final short block2 = (short) (val >>> 16);
		final short block3 = (short) val;
		
		for (int i = fromIndex * 3, end = toIndex * 3; i < end; i += 3) {
			mBlocks[i] = block1;
			mBlocks[i+1] = block2;
			mBlocks[i+2] = block3;
		}
	}

	@Override
	public void clear() {
		Arrays.fill(mBlocks, (short) 0);
	}

	@Override
	public long ramBytesUsed() {
		return JvmUtil.sizeOf(mBlocks);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(bitsPerValue=" + mBitsPerValue
				+ ", size=" + size() + ", elements.length=" + mBlocks.length + ")";
	}
	
}
