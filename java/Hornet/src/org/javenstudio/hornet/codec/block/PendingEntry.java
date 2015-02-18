package org.javenstudio.hornet.codec.block;

class PendingEntry {

    private final boolean mIsTerm;

    protected PendingEntry(boolean isTerm) {
    	mIsTerm = isTerm;
    }
	
    public final boolean isTerm() { return mIsTerm; }
    
}
