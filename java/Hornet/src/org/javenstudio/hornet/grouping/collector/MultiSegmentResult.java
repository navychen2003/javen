package org.javenstudio.hornet.grouping.collector;

import java.io.IOException;

import org.javenstudio.common.indexdb.index.term.TermsEnum;
import org.javenstudio.hornet.grouping.SegmentResult;

public class MultiSegmentResult extends SegmentResult {

    protected final TermsEnum mTenum;

    public MultiSegmentResult(int[] counts, int total, int missingCountIndex, 
    		TermsEnum tenum, int startFacetOrd, int endFacetOrd) throws IOException {
    	super(counts, total - counts[missingCountIndex], counts[missingCountIndex],
    			endFacetOrd == missingCountIndex + 1 ?  missingCountIndex : endFacetOrd);
    	
    	mTenum = tenum;
    	mMergePos = startFacetOrd;
    	
    	if (tenum != null) {
    		tenum.seekExact(mMergePos);
    		mMergeTerm = tenum.getTerm();
    	}
    }

    public final TermsEnum getTermsEnum() { 
    	return mTenum;
    }
    
    @Override
    protected void nextTerm() throws IOException {
    	mMergeTerm = mTenum.next();
    }
	
}
