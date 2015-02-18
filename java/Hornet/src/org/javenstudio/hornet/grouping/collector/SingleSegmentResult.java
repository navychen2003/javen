package org.javenstudio.hornet.grouping.collector;

import java.io.IOException;

import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.hornet.grouping.SegmentResult;

public class SingleSegmentResult extends SegmentResult {
	
    protected final ITermsEnum mTenum;

    public SingleSegmentResult(int[] counts, int total, ITermsEnum tenum, 
    		int startFacetOrd, int endFacetOrd) throws IOException {
    	super(counts, total - counts[0], counts[0], endFacetOrd);
    	
    	mTenum = tenum;
    	mMergePos = startFacetOrd == 0 ? 1 : startFacetOrd;
    	
    	if (mMergePos < mMaxTermPos) {
    		tenum.seekExact(mMergePos);
    		mMergeTerm = tenum.getTerm();
    	}
    }

    public final ITermsEnum getTermsEnum() { 
    	return mTenum;
    }
    
    @Override
    protected void nextTerm() throws IOException {
    	mMergeTerm = mTenum.next();
    }
    
}
