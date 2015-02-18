package org.javenstudio.hornet.store.packed;

import java.io.EOFException;
import java.io.IOException;

import org.javenstudio.common.indexdb.IDataOutput;

//Packs high order byte first, to match
//IndexOutput.writeInt/Long/Short byte order
final class PackedWriterImpl extends PackedWriter {

	private final Format mFormat;
	private final BulkOperation mBulkOperation;
	private final long[] mNextBlocks;
	private final long[] mNextValues;
	private final int mIterations;
	private boolean mFinished;
	private int mOff;
	private int mWritten;

	PackedWriterImpl(Format format, IDataOutput out, int valueCount, int bitsPerValue, 
			int mem) throws IOException {
		super(out, valueCount, bitsPerValue);
		
		mFormat = format;
		mBulkOperation = BulkOperation.of(format, bitsPerValue);
		mIterations = mBulkOperation.computeIterations(valueCount, mem);
		mNextBlocks = new long[mIterations * mBulkOperation.blocks()];
		mNextValues = new long[mIterations * mBulkOperation.values()];
		
		mOff = 0;
		mWritten = 0;
		mFinished = false;
	}

	@Override
	protected Format getFormat() {
		return mFormat;
	}

	@Override
	public void add(long v) throws IOException {
		assert v >= 0 && v <= PackedInts.maxValue(mBitsPerValue);
		assert !mFinished;
		
		if (mValueCount != -1 && mWritten >= mValueCount) 
			throw new EOFException("Writing past end of stream");
		
		mNextValues[mOff++] = v;
		if (mOff == mNextValues.length) {
			flush(mNextValues.length);
			mOff = 0;
		}
		
		++ mWritten;
	}

	@Override
	public void finish() throws IOException {
		assert !mFinished;
		if (mValueCount != -1) {
			while (mWritten < mValueCount) {
				add(0L);
			}
		}
		flush(mOff);
		mFinished = true;
	}

	private void flush(int nvalues) throws IOException {
		mBulkOperation.set(mNextBlocks, 0, mNextValues, 0, mIterations);
		
		final int blocks = mFormat.nblocks(mBitsPerValue, nvalues);
		for (int i = 0; i < blocks; ++i) {
			mOutput.writeLong(mNextBlocks[i]);
		}
		mOff = 0;
	}

	@Override
	public int ord() {
		return mWritten - 1;
	}
	
}
