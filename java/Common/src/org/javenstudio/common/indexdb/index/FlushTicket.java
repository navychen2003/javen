package org.javenstudio.common.indexdb.index;

import java.io.IOException;

public class FlushTicket {
	
	private final FrozenDeletes mFrozenDeletes;
	private final boolean mGlobal;
	private boolean mPublished = false;
    
	private FlushedSegment mSegment = null;
	private boolean mFailed = false;

	public FlushTicket(FrozenDeletes frozenDeletes) { 
		this(frozenDeletes, false);
	}
	
    public FlushTicket(FrozenDeletes frozenDeletes, boolean global) {
    	assert frozenDeletes != null;
    	mFrozenDeletes = frozenDeletes;
    	mGlobal = global;
    }

    public void publish(SegmentWriter writer) throws IOException {
    	assert !mPublished : "ticket was already publised - can not publish twice";
      	mPublished = true;
      	// if its a global ticket - no segment to publish
      	writer.finishFlush(mGlobal?null:mSegment, mFrozenDeletes);
    }
    
    public void setSegment(FlushedSegment segment) {
    	assert !mFailed;
    	mSegment = segment;
    }
    
    public void setFailed() {
    	assert mSegment == null;
    	mFailed = true;
    }

    public boolean canPublish() {
    	return mGlobal || mSegment != null || mFailed;
    }
    
    public boolean isGlobal() { 
    	return mGlobal;
    }
    
}
