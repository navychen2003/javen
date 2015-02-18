package org.javenstudio.hornet.grouping.collector;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDocTermsIndex;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.UnicodeUtil;
import org.javenstudio.hornet.search.cache.FieldCache;

//Implementation for single valued facet fields.
public class SingleTermGroupFacetCollector extends TermGroupFacetCollector {
	
    protected IDocTermsIndex mFacetFieldTermsIndex;

    public SingleTermGroupFacetCollector(String groupField, String facetField, 
    		BytesRef facetPrefix, int initialSize) {
    	super(groupField, facetField, facetPrefix, initialSize);
    }

    public void collect(int doc) throws IOException {
    	int facetOrd = mFacetFieldTermsIndex.getOrd(doc);
    	if (facetOrd < mStartFacetOrd || facetOrd >= mEndFacetOrd) 
    		return;

    	int groupOrd = mGroupFieldTermsIndex.getOrd(doc);
    	int segmentGroupedFacetsIndex = (groupOrd * mFacetFieldTermsIndex.getNumOrd()) + facetOrd;
    	if (mSegmentGroupedFacetHits.exists(segmentGroupedFacetsIndex)) 
    		return;

    	mSegmentTotalCount ++;
    	mSegmentFacetCounts[facetOrd]++;

    	mSegmentGroupedFacetHits.put(segmentGroupedFacetsIndex);
    	mGroupedFacetHits.add(
    			new GroupedFacetHit(
    					groupOrd == 0 ? null : mGroupFieldTermsIndex.lookup(groupOrd, new BytesRef()),
    					facetOrd == 0 ? null : mFacetFieldTermsIndex.lookup(facetOrd, new BytesRef())
    				)
    			);
    }

    @Override
    public void setNextReader(IAtomicReaderRef context) throws IOException {
    	if (mSegmentFacetCounts != null) 
    		mSegmentResults.add(createSegmentResult());

    	mGroupFieldTermsIndex = FieldCache.DEFAULT.getTermsIndex(context.getReader(), mGroupField);
    	mFacetFieldTermsIndex = FieldCache.DEFAULT.getTermsIndex(context.getReader(), mFacetField);
    	mSegmentFacetCounts = new int[mFacetFieldTermsIndex.getNumOrd()];
    	mSegmentTotalCount = 0;

    	mSegmentGroupedFacetHits.clear();
    	
    	for (GroupedFacetHit groupedFacetHit : mGroupedFacetHits) {
    		int facetOrd = mFacetFieldTermsIndex.binarySearch(groupedFacetHit.mFacetValue, mSpare);
    		if (facetOrd < 0) 
    			continue;

    		int groupOrd = mGroupFieldTermsIndex.binarySearch(groupedFacetHit.mGroupValue, mSpare);
    		if (groupOrd < 0) 
    			continue;

    		int segmentGroupedFacetsIndex = (groupOrd * mFacetFieldTermsIndex.getNumOrd()) + facetOrd;
    		mSegmentGroupedFacetHits.put(segmentGroupedFacetsIndex);
    	}

    	if (mFacetPrefix != null) {
    		mStartFacetOrd = mFacetFieldTermsIndex.binarySearch(mFacetPrefix, mSpare);
    		if (mStartFacetOrd < 0) {
    			// Points to the ord one higher than facetPrefix
    			mStartFacetOrd = -mStartFacetOrd - 1;
    		}
    		
    		BytesRef facetEndPrefix = BytesRef.deepCopyOf(mFacetPrefix);
    		facetEndPrefix.append(UnicodeUtil.BIG_TERM);
    		
    		mEndFacetOrd = mFacetFieldTermsIndex.binarySearch(facetEndPrefix, mSpare);
    		mEndFacetOrd = -mEndFacetOrd - 1; // Points to the ord one higher than facetEndPrefix
    		
    	} else {
    		mStartFacetOrd = 0;
    		mEndFacetOrd = mFacetFieldTermsIndex.getNumOrd();
    	}
    }

    @Override
    protected SingleSegmentResult createSegmentResult() throws IOException {
    	return new SingleSegmentResult(mSegmentFacetCounts, mSegmentTotalCount, 
    			mFacetFieldTermsIndex.getTermsEnum(), mStartFacetOrd, mEndFacetOrd);
    }

}
