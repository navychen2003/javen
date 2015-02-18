package org.javenstudio.hornet.search.query;

import org.javenstudio.common.indexdb.util.ArrayUtil;
import org.javenstudio.common.indexdb.util.BytesRefHash;
import org.javenstudio.common.indexdb.util.JvmUtil;
import org.javenstudio.hornet.index.term.TermContext;

/** Special implementation of BytesStartArray that keeps parallel arrays for {@link TermContext} */
public class TermStateByteStart extends BytesRefHash.DirectBytesStartArray {
	
	private TermContext[] mTermState;

	public TermStateByteStart(int initSize) {
		super(initSize);
	}

	public TermContext[] getTermStates() { return mTermState; }
	public TermContext getTermStateAt(int index) { return mTermState[index]; }
	
	public void setTermStateAt(int index, TermContext state) { 
		mTermState[index] = state;
	}
	
	@Override
	public int[] init() {
		final int[] ord = super.init();
		mTermState = new TermContext[ArrayUtil.oversize(ord.length, JvmUtil.NUM_BYTES_OBJECT_REF)];
		assert mTermState.length >= ord.length;
		return ord;
	}

	@Override
	public int[] grow() {
		final int[] ord = super.grow();
		if (mTermState.length < ord.length) {
			TermContext[] tmpTermState = new TermContext[ArrayUtil.oversize(
					ord.length, JvmUtil.NUM_BYTES_OBJECT_REF)];
			System.arraycopy(mTermState, 0, tmpTermState, 0, mTermState.length);
			mTermState = tmpTermState;
		}      
		assert mTermState.length >= ord.length;
		return ord;
	}

	@Override
	public int[] clear() {
		mTermState = null;
		return super.clear();
	}
	
}
