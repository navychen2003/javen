package org.javenstudio.hornet.store.fst;

/** Expert: holds a pending (seen but not yet serialized) arc. */
public class BuilderArc<T> {
	
    protected int mLabel;    			// really an "unsigned" byte
    protected BuilderNode mTarget;
    protected boolean mIsFinal;
    protected T mOutput;
    protected T mNextFinalOutput;
    
    public final int getLabel() { return mLabel; }
    public final BuilderNode getTarget() { return mTarget; }
    public final boolean isFinal() { return mIsFinal; }
    public final T getOutput() { return mOutput; }
    public final T getNextFinalOutput() { return mNextFinalOutput; }
    
    public final void setTarget(BuilderNode node) { mTarget = node; }
    
}
