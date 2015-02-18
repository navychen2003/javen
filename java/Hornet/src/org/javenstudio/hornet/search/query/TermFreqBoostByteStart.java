package org.javenstudio.hornet.search.query;

import org.javenstudio.common.indexdb.util.ArrayUtil;
import org.javenstudio.common.indexdb.util.BytesRefHash;
import org.javenstudio.common.indexdb.util.JvmUtil;
import org.javenstudio.hornet.index.term.TermContext;

/** Special implementation of BytesStartArray that keeps parallel arrays for boost and docFreq */
public final class TermFreqBoostByteStart extends BytesRefHash.DirectBytesStartArray  {
	
	private float[] mBoost;
	private TermContext[] mTermState;

	public TermFreqBoostByteStart(int initSize) {
		super(initSize);
	}

	public TermContext[] getTermStates() { return mTermState; }
	public TermContext getTermStateAt(int index) { return mTermState[index]; }
	public void setTermStateAt(int index, TermContext state) { mTermState[index] = state; }
	
	public float[] getBoosts() { return mBoost; }
	public float getBoostAt(int index) { return mBoost[index]; }
	public void setBoostAt(int index, float boost) { mBoost[index] = boost; }
	
	@Override
	public int[] init() {
		final int[] ord = super.init();
		mBoost = new float[ArrayUtil.oversize(ord.length, JvmUtil.NUM_BYTES_FLOAT)];
		mTermState = new TermContext[ArrayUtil.oversize(ord.length, JvmUtil.NUM_BYTES_OBJECT_REF)];
		assert mTermState.length >= ord.length && mBoost.length >= ord.length;
		return ord;
	}

	@Override
	public int[] grow() {
		final int[] ord = super.grow();
		mBoost = ArrayUtil.grow(mBoost, ord.length);
		if (mTermState.length < ord.length) {
			TermContext[] tmpTermState = new TermContext[ArrayUtil.oversize(ord.length, JvmUtil.NUM_BYTES_OBJECT_REF)];
			System.arraycopy(mTermState, 0, tmpTermState, 0, mTermState.length);
			mTermState = tmpTermState;
		}     
		assert mTermState.length >= ord.length && mBoost.length >= ord.length;
		return ord;
	}

	@Override
	public int[] clear() {
		mBoost = null;
		mTermState = null;
		return super.clear();
	}
	
}
