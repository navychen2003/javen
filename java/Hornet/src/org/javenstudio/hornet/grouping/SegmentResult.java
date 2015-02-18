package org.javenstudio.hornet.grouping;

import java.io.IOException;

import org.javenstudio.common.indexdb.util.BytesRef;

/**
 * Contains the local grouped segment counts for a particular segment.
 * Each <code>SegmentResult</code> must be added together.
 */
public abstract class SegmentResult {
	
    protected final int[] mCounts;
    protected final int mTotal;
    protected final int mMissing;
    protected final int mMaxTermPos;

    protected BytesRef mMergeTerm;
    protected int mMergePos;

    protected SegmentResult(int[] counts, int total, int missing, int maxTermPos) {
    	mCounts = counts;
    	mTotal = total;
    	mMissing = missing;
    	mMaxTermPos = maxTermPos;
    }

    /**
     * Go to next term in this <code>SegmentResult</code> in order to retrieve the grouped facet counts.
     *
     * @throws IOException If I/O related errors occur
     */
    protected abstract void nextTerm() throws IOException;
    
}
