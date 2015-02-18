package org.javenstudio.hornet.codec.postings;

import org.javenstudio.common.indexdb.ITermState;
import org.javenstudio.common.indexdb.store.ByteArrayDataInput;
import org.javenstudio.hornet.codec.block.BlockTermState;

//Must keep final because we do non-standard clone
final class StandardTermState extends BlockTermState {
	
	protected long mFreqOffset;
    protected long mProxOffset;
    protected int mSkipOffset;

    // Only used by the "primary" TermState -- clones don't
    // copy this (basically they are "transient"):
    // TODO: should this NOT be in the TermState...?
    protected ByteArrayDataInput mBytesReader;
    protected byte[] mBytes;

    @Override
    public StandardTermState clone() {
    	StandardTermState other = new StandardTermState();
    	other.copyFrom(this);
    	return other;
    }

    @Override
    public void copyFrom(ITermState state) {
    	super.copyFrom(state);
    	
    	StandardTermState other = (StandardTermState) state;
    	mFreqOffset = other.mFreqOffset;
    	mProxOffset = other.mProxOffset;
    	mSkipOffset = other.mSkipOffset;

    	// Do not copy bytes, bytesReader (else TermState is
    	// very heavy, ie drags around the entire block's
    	// byte[]).  On seek back, if next() is in fact used
    	// (rare!), they will be re-read from disk.
    }

    @Override
    public String toString() {
    	return super.toString() + " freqFP=" + mFreqOffset + " proxFP=" + mProxOffset + 
    			" skipOffset=" + mSkipOffset;
    }
    
}
