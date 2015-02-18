package org.javenstudio.hornet.codec.block;

import org.javenstudio.common.indexdb.ITermState;
import org.javenstudio.common.indexdb.index.term.OrdTermState;

/**
 * Holds all state required for {@link PostingsReaderBase}
 * to produce a {@link DocsEnum} without re-seeking the
 * terms dict.
 */
public class BlockTermState extends OrdTermState {
	
	private int mDocFreq;        		// how many docs have this term
	private long mTotalTermFreq;		// total number of occurrences of this term

	private int mTermBlockOrd;    	// the term's ord in the current block
	private long mBlockFilePointer;	// fp into the terms dict primary file (_X.tim) that holds this term

	@Override
	public void copyFrom(ITermState state) {
		assert state instanceof BlockTermState : "can not copy from " + state.getClass().getName();
  
    	BlockTermState other = (BlockTermState) state;
    	super.copyFrom(state);
    
    	mDocFreq = other.mDocFreq;
    	mTotalTermFreq = other.mTotalTermFreq;
    	mTermBlockOrd = other.mTermBlockOrd;
    	mBlockFilePointer = other.mBlockFilePointer;

    	// NOTE: don't copy blockTermCount;
    	// it's "transient": used only by the "primary"
    	// termState, and regenerated on seek by TermState
	}

	public final int getDocFreq() { return mDocFreq; }
	public final long getTotalTermFreq() { return mTotalTermFreq; }
	public final int getTermBlockOrd() { return mTermBlockOrd; }
	public final long getFilePointer() { return mBlockFilePointer; }
	
	final void setDocFreq(int val) { mDocFreq = val; }
	final void setTotalTermFreq(long val) { mTotalTermFreq = val; }
	final void setTermBlockOrd(int val) { mTermBlockOrd = val; }
	final void increaseTermBlockOrd(int val) { mTermBlockOrd += val; }
	final void setFilePointer(long val) { mBlockFilePointer = val; }
	
	@Override
	public String toString() {
		return "BlockTermState: docFreq=" + mDocFreq + " totalTermFreq=" + mTotalTermFreq + 
				" termBlockOrd=" + mTermBlockOrd + " blockFP=" + mBlockFilePointer;
	}
	
}
