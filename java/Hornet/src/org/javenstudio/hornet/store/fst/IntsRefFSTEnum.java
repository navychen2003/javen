package org.javenstudio.hornet.store.fst;

import java.io.IOException;

import org.javenstudio.common.indexdb.util.ArrayUtil;
import org.javenstudio.common.indexdb.util.IntsRef;

/** 
 * Enumerates all input (IntsRef) + output pairs in an
 *  FST.
 *
 */
public final class IntsRefFSTEnum<T> extends FSTEnum<T> {
	
	private final IntsRef mCurrent = new IntsRef(10);
	private final InputOutput<T> mResult = new InputOutput<T>();
	private IntsRef mTarget;

	/** Holds a single input (IntsRef) + output pair. */
	public static class InputOutput<T> {
		private IntsRef mInput;
		private T mOutput;
		
		public final IntsRef getInput() { return mInput; }
		public final T getOutput() { return mOutput; }
	}

	/** 
	 * doFloor controls the behavior of advance: if it's true
	 *  doFloor is true, advance positions to the biggest
	 *  term before target. 
	 */
	public IntsRefFSTEnum(FST<T> fst) {
		super(fst);
		mResult.mInput = mCurrent;
		mCurrent.mOffset = 1;
	}

	public InputOutput<T> current() {
		return mResult;
	}

	public InputOutput<T> next() throws IOException {
		doNext();
		return setResult();
	}

	/** Seeks to smallest term that's >= target. */
	public InputOutput<T> seekCeil(IntsRef target) throws IOException {
		mTarget = target;
		mTargetLength = target.mLength;
		super.doSeekCeil();
		return setResult();
	}

	/** Seeks to biggest term that's <= target. */
	public InputOutput<T> seekFloor(IntsRef target) throws IOException {
		mTarget = target;
		mTargetLength = target.mLength;
		super.doSeekFloor();
		return setResult();
	}

	/** 
	 * Seeks to exactly this term, returning null if the term
	 *  doesn't exist.  This is faster than using {@link
	 *  #seekFloor} or {@link #seekCeil} because it
	 *  short-circuits as soon the match is not found. 
	 */
	public InputOutput<T> seekExact(IntsRef target) throws IOException {
		mTarget = target;
		mTargetLength = target.mLength;
		if (super.doSeekExact()) {
			assert mUpto == 1+target.mLength;
			return setResult();
			
		} else 
			return null;
	}

	@Override
	protected int getTargetLabel() {
		if (mUpto-1 == mTarget.mLength) 
			return FST.END_LABEL;
		else 
			return mTarget.mInts[mTarget.mOffset + mUpto - 1];
	}

	@Override
	protected int getCurrentLabel() {
		// current.offset fixed at 1
		return mCurrent.mInts[mUpto];
	}

	@Override
	protected void setCurrentLabel(int label) {
		mCurrent.mInts[mUpto] = label;
	}

	@Override
	protected void grow() {
		mCurrent.mInts = ArrayUtil.grow(mCurrent.mInts, mUpto+1);
	}

	private InputOutput<T> setResult() {
		if (mUpto == 0) {
			return null;
		} else {
			mCurrent.mLength = mUpto-1;
			mResult.mOutput = mOutput[mUpto];
			return mResult;
		}
	}
	
}
