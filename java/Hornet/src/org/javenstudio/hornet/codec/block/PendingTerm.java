package org.javenstudio.hornet.codec.block;

import org.javenstudio.common.indexdb.index.term.PerTermState;
import org.javenstudio.common.indexdb.util.BytesRef;

final class PendingTerm extends PendingEntry {

    private final BytesRef mTerm;
    private final PerTermState mStats;

    public PendingTerm(BytesRef term, PerTermState stats) {
    	super(true);
    	mTerm = term;
    	mStats = stats;
    }

    public final BytesRef getTerm() { return mTerm; }
    public final PerTermState getTermState() { return mStats; }
    
    @Override
    public String toString() {
    	return mTerm.utf8ToString();
    }
	
}
