package org.javenstudio.hornet.store.packed;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDataOutput;
import org.javenstudio.common.indexdb.IIntsMutable;
import org.javenstudio.common.indexdb.IIntsWriter;

/**     
 * Implements {@link PackedInts.Mutable}, but grows the
 * bit count of the underlying packed ints on-demand.
 *
 */
public class GrowableWriter extends IntsMutable implements IIntsWriter {

	private final float mAcceptableOverheadRatio;
	private long mCurrentMaxValue;
	private IIntsMutable mCurrent;

	public GrowableWriter(int startBitsPerValue, int valueCount, float acceptableOverheadRatio) {
		mAcceptableOverheadRatio = acceptableOverheadRatio;
		mCurrent = PackedInts.getMutable(valueCount, startBitsPerValue, mAcceptableOverheadRatio);
		mCurrentMaxValue = PackedInts.maxValue(mCurrent.getBitsPerValue());
	}

	@Override
	public long get(int index) {
		return mCurrent.get(index);
	}

	@Override
	public int size() {
		return mCurrent.size();
	}

	@Override
	public int getBitsPerValue() {
		return mCurrent.getBitsPerValue();
	}

	@Override
	public IIntsMutable getMutable() {
		return mCurrent;
	}

	@Override
	public Object getArray() {
		return mCurrent.getArray();
	}

	@Override
	public boolean hasArray() {
		return mCurrent.hasArray();
	}

	private void ensureCapacity(long value) {
		assert value >= 0;
		if (value <= mCurrentMaxValue) 
			return;
		
		final int bitsRequired = PackedInts.bitsRequired(value);
		final int valueCount = size();
		
		IIntsMutable next = PackedInts.getMutable(valueCount, bitsRequired, mAcceptableOverheadRatio);
		PackedInts.copy(mCurrent, 0, next, 0, valueCount, PackedInts.DEFAULT_BUFFER_SIZE);
		
		mCurrent = next;
		mCurrentMaxValue = PackedInts.maxValue(mCurrent.getBitsPerValue());
	}

	@Override
	public void set(int index, long value) {
		ensureCapacity(value);
		mCurrent.set(index, value);
	}

	@Override
	public void clear() {
		mCurrent.clear();
	}

	@Override
	public GrowableWriter resize(int newSize) {
		GrowableWriter next = new GrowableWriter(getBitsPerValue(), newSize, mAcceptableOverheadRatio);
		final int limit = Math.min(size(), newSize);
		PackedInts.copy(mCurrent, 0, next, 0, limit, PackedInts.DEFAULT_BUFFER_SIZE);
		return next;
	}

	@Override
	public int get(int index, long[] arr, int off, int len) {
		return mCurrent.get(index, arr, off, len);
	}

	@Override
	public int set(int index, long[] arr, int off, int len) {
		long max = 0;
		for (int i = off, end = off + len; i < end; ++i) {
			max |= arr[i];
		}
		ensureCapacity(max);
		return mCurrent.set(index, arr, off, len);
	}

	@Override
	public void fill(int fromIndex, int toIndex, long val) {
		ensureCapacity(val);
		mCurrent.fill(fromIndex, toIndex, val);
	}

	@Override
	public long ramBytesUsed() {
		return mCurrent.ramBytesUsed();
	}

	@Override
	public void save(IDataOutput out) throws IOException {
		mCurrent.save(out);
	}

}
