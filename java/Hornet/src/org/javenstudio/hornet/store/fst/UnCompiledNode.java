package org.javenstudio.hornet.store.fst;

import org.javenstudio.common.indexdb.util.ArrayUtil;
import org.javenstudio.common.indexdb.util.JvmUtil;

/** Expert: holds a pending (seen but not yet serialized) Node. */
public final class UnCompiledNode<T> implements BuilderNode {
	
    private final Builder<T> mOwner;
    private int mNumArcs;
    private BuilderArc<T>[] mArcs;
    // TODO: instead of recording isFinal/output on the
    // node, maybe we should use -1 arc to mean "end" (like
    // we do when reading the FST).  Would simplify much
    // code here...
    private T mOutput;
    private boolean mIsFinal;
    private long mInputCount;

    /** This node's depth, starting from the automaton root. */
    private final int mDepth;

    public final int getDepth() { return mDepth; }
    public final long getInputCount() { return mInputCount; }
    public final int getNumArcs() { return mNumArcs; }
    public final BuilderArc<T> getArcAt(int index) { return mArcs[index]; }
    public final T getOutput() { return mOutput; }
    public final boolean isFinal() { return mIsFinal; }
    
    public final void setNumArcs(int num) { mNumArcs = num; }
    public final void setInputCount(long count) { mInputCount = count; }
    
    final void setIsFinal(boolean isFinal) { mIsFinal = isFinal; }
    final void setOutput(T output) { mOutput = output; }
    final void increaseInputCount(int count) { mInputCount += count; }
    
    /**
     * @param depth
     *          The node's depth starting from the automaton root. Needed for
     *          LUCENE-2934 (node expansion based on conditions other than the
     *          fanout size).
     */
    @SuppressWarnings({"unchecked"})
    public UnCompiledNode(Builder<T> owner, int depth) {
    	mOwner = owner;
    	mArcs = (BuilderArc<T>[]) new BuilderArc[1];
    	mArcs[0] = new BuilderArc<T>();
    	mOutput = owner.NO_OUTPUT;
    	mDepth = depth;
    }

    @Override
    public boolean isCompiled() {
    	return false;
    }

    public void clear() {
    	mNumArcs = 0;
    	mIsFinal = false;
    	mOutput = mOwner.NO_OUTPUT;
    	mInputCount = 0;

    	// We don't clear the depth here because it never changes 
    	// for nodes on the frontier (even when reused).
    }

    public T getLastOutput(int labelToMatch) {
    	assert mNumArcs > 0;
    	assert mArcs[mNumArcs-1].getLabel() == labelToMatch;
    	return mArcs[mNumArcs-1].getOutput();
    }

    public void addArc(int label, BuilderNode target) {
    	assert label >= 0;
    	assert mNumArcs == 0 || label > mArcs[mNumArcs-1].getLabel(): "arc[-1].label=" + 
    			mArcs[mNumArcs-1].getLabel() + " new label=" + label + " numArcs=" + mNumArcs;
    	
    	if (mNumArcs == mArcs.length) {
    		@SuppressWarnings({"unchecked"}) 
    		final BuilderArc<T>[] newArcs =
    				new BuilderArc[ArrayUtil.oversize(mNumArcs+1, JvmUtil.NUM_BYTES_OBJECT_REF)];
    		System.arraycopy(mArcs, 0, newArcs, 0, mArcs.length);
    		for (int arcIdx=mNumArcs; arcIdx < newArcs.length; arcIdx++) {
    			newArcs[arcIdx] = new BuilderArc<T>();
    		}
    		mArcs = newArcs;
    	}
    	
    	final BuilderArc<T> arc = mArcs[mNumArcs++];
    	arc.mLabel = label;
    	arc.mTarget = target;
    	arc.mOutput = arc.mNextFinalOutput = mOwner.NO_OUTPUT;
    	arc.mIsFinal = false;
    }

    public void replaceLast(int labelToMatch, BuilderNode target, T nextFinalOutput, boolean isFinal) {
    	assert mNumArcs > 0;
    	final BuilderArc<T> arc = mArcs[mNumArcs-1];
    	assert arc.getLabel() == labelToMatch: "arc.label=" + arc.getLabel() + " vs " + labelToMatch;
    	arc.mTarget = target;
    	//assert target.node != -2;
    	arc.mNextFinalOutput = nextFinalOutput;
    	arc.mIsFinal = isFinal;
    }

    public void deleteLast(int label, BuilderNode target) {
    	assert mNumArcs > 0;
    	assert label == mArcs[mNumArcs-1].getLabel();
    	assert target == mArcs[mNumArcs-1].getTarget();
    	mNumArcs --;
    }

    public void setLastOutput(int labelToMatch, T newOutput) {
    	assert mOwner.validOutput(newOutput);
    	assert mNumArcs > 0;
    	final BuilderArc<T> arc = mArcs[mNumArcs-1];
    	assert arc.getLabel() == labelToMatch;
    	arc.mOutput = newOutput;
    }

    // pushes an output prefix forward onto all arcs
    public void prependOutput(T outputPrefix) {
    	assert mOwner.validOutput(outputPrefix);

    	for (int arcIdx=0; arcIdx < mNumArcs; arcIdx++) {
    		mArcs[arcIdx].mOutput = mOwner.getFst().getOutputs().add(outputPrefix, mArcs[arcIdx].getOutput());
    		assert mOwner.validOutput(mArcs[arcIdx].getOutput());
    	}

    	if (mIsFinal) {
    		mOutput = mOwner.getFst().getOutputs().add(outputPrefix, mOutput);
    		assert mOwner.validOutput(mOutput);
    	}
    }
    
}
