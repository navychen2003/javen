package org.javenstudio.hornet.store.fst;

import org.javenstudio.common.indexdb.util.IntsRef;

final class ArcAndState<T> {
    private final FSTArc<T> mArc;
    private final IntsRef mChain;

    public ArcAndState(FSTArc<T> arc, IntsRef chain) {
    	mArc = arc;
    	mChain = chain;
    }
    
    public final FSTArc<T> getArc() { return mArc; }
    public final IntsRef getChain() { return mChain; }
}
