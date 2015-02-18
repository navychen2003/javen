package org.javenstudio.hornet.store.fst;

/** Represents a single arc. */
public final class FSTArc<T> {
	
    private int mLabel;
    private T mOutput;

    // From node (ord or address); currently only used when
    // building an FST w/ willPackFST=true:
    private int mNode;

    // To node (ord or address):
    private int mTarget;

    private byte mFlags;
    private T mNextFinalOutput;

    // address (into the byte[]), or ord/address if label == END_LABEL
    private int mNextArc;

    // This is non-zero if current arcs are fixed array:
    private int mPosArcsStart;
    private int mBytesPerArc;
    private int mArcIdx;
    private int mNumArcs;

    public final T getOutput() { return mOutput; }
    public final T getNextFinalOutput() { return mNextFinalOutput; }
    public final int getLabel() { return mLabel; }
    public final int getTarget() { return mTarget; }
    public final int getNode() { return mNode; }
    public final int getFlags() { return mFlags; }
    
    public final int getPosArcsStart() { return mPosArcsStart; }
    public final int getBytesPerArc() { return mBytesPerArc; }
    public final int getArcIndex() { return mArcIdx; }
    public final int getNumArcs() { return mNumArcs; }
    public final int getNextArc() { return mNextArc; }
    
    final void increaseArcIndex(int num) { mArcIdx += num; }
    
    final void setPosArcsStart(int start) { mPosArcsStart = start; }
    final void setBytesPerArc(int bytes) { mBytesPerArc = bytes; }
    final void setNumArcs(int num) { mNumArcs = num; }
    final void setArcIndex(int index) { mArcIdx = index; }
    final void setNextArc(int arc) { mNextArc = arc; }
    
    final void setFlags(int flags) { mFlags = (byte)flags; }
    final void setNextFinalOutput(T output) { mNextFinalOutput = output; }
    final void setOutput(T output) { mOutput = output; }
    final void setLabel(int label) { mLabel = label; }
    final void setTarget(int target) { mTarget = target; }
    final void setNode(int node) { mNode = node; }
    
    /** Returns this */
    public FSTArc<T> copyFrom(FSTArc<T> other) {
    	mNode = other.mNode;
    	mLabel = other.mLabel;
    	mTarget = other.mTarget;
    	mFlags = other.mFlags;
    	mOutput = other.mOutput;
    	mNextFinalOutput = other.mNextFinalOutput;
    	mNextArc = other.mNextArc;
    	mBytesPerArc = other.mBytesPerArc;
    	
    	if (mBytesPerArc != 0) {
    		mPosArcsStart = other.mPosArcsStart;
    		mArcIdx = other.mArcIdx;
    		mNumArcs = other.mNumArcs;
    	}
    	
    	return this;
    }

    public boolean flag(int flag) {
    	return FST.flag(mFlags, flag);
    }

    public boolean isLast() {
    	return flag(FST.BIT_LAST_ARC);
    }

    public boolean isFinal() {
    	return flag(FST.BIT_FINAL_ARC);
    }

    @Override
    public String toString() {
    	StringBuilder b = new StringBuilder();
    	
    	b.append("node=" + mNode);
    	b.append(" target=" + mTarget);
    	b.append(" label=" + mLabel);
    	
    	if (flag(FST.BIT_LAST_ARC)) 
    		b.append(" last");
    	
    	if (flag(FST.BIT_FINAL_ARC)) 
    		b.append(" final");
    	
    	if (flag(FST.BIT_TARGET_NEXT)) 
    		b.append(" targetNext");
    	
    	if (flag(FST.BIT_ARC_HAS_OUTPUT)) 
    		b.append(" output=" + mOutput);
    	
    	if (flag(FST.BIT_ARC_HAS_FINAL_OUTPUT)) 
    		b.append(" nextFinalOutput=" + mNextFinalOutput);
    	
    	if (mBytesPerArc != 0) 
    		b.append(" arcArray(idx=" + mArcIdx + " of " + mNumArcs + ")");
    	
    	return b.toString();
    }
    
}
