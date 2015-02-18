package org.javenstudio.common.indexdb.index;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IMergeOne;
import org.javenstudio.common.indexdb.MergeAbortedException;

public class CheckAbort {
	
    private double mWorkCount;
    private final IMergeOne mMerge;
    private final IDirectory mDir;
    
    public CheckAbort(IMergeOne merge, IDirectory dir) {
    	mMerge = merge;
    	mDir = dir;
    }

    /**
     * Records the fact that roughly units amount of work
     * have been done since this method was last called.
     * When adding time-consuming code into SegmentMerger,
     * you should test different values for units to ensure
     * that the time in between calls to merge.checkAborted
     * is up to ~ 1 second.
     */
    public void work(double units) throws MergeAbortedException {
    	mWorkCount += units;
    	if (mWorkCount >= 10000.0) {
    		mMerge.checkAborted(mDir);
    		mWorkCount = 0;
    	}
    }
    
    /** If you use this: IW.close(false) cannot abort your merge! */
    public static final CheckAbort NONE = new CheckAbort(null, null) {
    	@Override
    	public void work(double units) {
    		// do nothing
    	}
    };
    
}
