package org.javenstudio.hornet.grouping.collector;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.index.term.TermsEnum;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.UnicodeUtil;
import org.javenstudio.hornet.index.term.DocTermOrds;
import org.javenstudio.hornet.index.term.DocTermOrds.TermOrdsIterator;
import org.javenstudio.hornet.search.cache.FieldCache;

//Implementation for multi valued facet fields.
public class MultiTermGroupFacetCollector extends TermGroupFacetCollector {
	
    protected DocTermOrds mFacetFieldDocTermOrds;
    protected TermsEnum mFacetOrdTermsEnum;
    protected TermOrdsIterator mReuse;

    public MultiTermGroupFacetCollector(String groupField, String facetField, 
    		BytesRef facetPrefix, int initialSize) {
    	super(groupField, facetField, facetPrefix, initialSize);
    }

    @Override
    public void collect(int doc) throws IOException {
    	int groupOrd = mGroupFieldTermsIndex.getOrd(doc);
      
    	if (mFacetFieldDocTermOrds.isEmpty()) {
    		int segmentGroupedFacetsIndex = groupOrd * (mFacetFieldDocTermOrds.getNumTerms() + 1);
    		if (mFacetPrefix != null || mSegmentGroupedFacetHits.exists(segmentGroupedFacetsIndex)) 
    			return;

    		mSegmentTotalCount ++;
    		mSegmentFacetCounts[mFacetFieldDocTermOrds.getNumTerms()]++;

    		mSegmentGroupedFacetHits.put(segmentGroupedFacetsIndex);
    		mGroupedFacetHits.add(
    				new GroupedFacetHit(groupOrd == 0 ? null : 
    					mGroupFieldTermsIndex.lookup(groupOrd, new BytesRef()), null)
    				);
    	  
    		return;
    	}

    	if (mFacetOrdTermsEnum != null) 
    		mReuse = mFacetFieldDocTermOrds.lookup(doc, mReuse);
    	
    	boolean first = true;
    	int chunk;
    	int[] buffer = new int[5];
    	
    	do {
    		chunk = mReuse != null ? mReuse.read(buffer) : 0;
    		if (first && chunk == 0) {
    			chunk = 1;
    			// this facet ord is reserved for docs not containing facet field.
    			buffer[0] = mFacetFieldDocTermOrds.getNumTerms(); 
    		}
    		first = false;

    		for (int pos = 0; pos < chunk; pos++) {
    			int facetOrd = buffer[pos];
    			if (facetOrd < mStartFacetOrd || facetOrd >= mEndFacetOrd) 
    				continue;

    			int segmentGroupedFacetsIndex = (groupOrd * (mFacetFieldDocTermOrds.getNumTerms() + 1)) + facetOrd;
    			if (mSegmentGroupedFacetHits.exists(segmentGroupedFacetsIndex)) 
    				continue;

    			mSegmentTotalCount ++;
    			mSegmentFacetCounts[facetOrd]++;

    			mSegmentGroupedFacetHits.put(segmentGroupedFacetsIndex);
    			mGroupedFacetHits.add(
    					new GroupedFacetHit(
    							groupOrd == 0 ? null : mGroupFieldTermsIndex.lookup(groupOrd, new BytesRef()),
    							facetOrd == mFacetFieldDocTermOrds.getNumTerms() ? null : 
    								BytesRef.deepCopyOf(mFacetFieldDocTermOrds.lookupTerm(
    										mFacetOrdTermsEnum, facetOrd))
    							)
    					);
    		}
    	} while (chunk >= buffer.length);
    }

    @Override
    public void setNextReader(IAtomicReaderRef context) throws IOException {
    	if (mSegmentFacetCounts != null) 
    		mSegmentResults.add(createSegmentResult());

    	mReuse = null;
    	mGroupFieldTermsIndex = FieldCache.DEFAULT.getTermsIndex(context.getReader(), mGroupField);
    	mFacetFieldDocTermOrds = (DocTermOrds)FieldCache.DEFAULT.getDocTermOrds(context.getReader(), mFacetField);
    	mFacetOrdTermsEnum = (TermsEnum)mFacetFieldDocTermOrds.getOrdTermsEnum(context.getReader());
    	
    	// [facetFieldDocTermOrds.numTerms() + 1] for all possible facet values 
    	// and docs not containing facet field
    	mSegmentFacetCounts = new int[mFacetFieldDocTermOrds.getNumTerms() + 1];
    	mSegmentTotalCount = 0;

    	mSegmentGroupedFacetHits.clear();
    	
    	for (GroupedFacetHit groupedFacetHit : mGroupedFacetHits) {
    		int groupOrd = mGroupFieldTermsIndex.binarySearch(groupedFacetHit.getGroupValue(), mSpare);
    		if (groupOrd < 0) 
    			continue;

    		int facetOrd;
    		if (groupedFacetHit.mFacetValue != null) {
    			if (mFacetOrdTermsEnum == null || !mFacetOrdTermsEnum.seekExact(groupedFacetHit.mFacetValue, true)) 
    				continue;
    			
    			facetOrd = (int) mFacetOrdTermsEnum.getOrd();
    		} else {
    			facetOrd = mFacetFieldDocTermOrds.getNumTerms();
    		}

    		// (facetFieldDocTermOrds.numTerms() + 1) for all possible facet values 
    		// and docs not containing facet field
    		int segmentGroupedFacetsIndex = (groupOrd * (mFacetFieldDocTermOrds.getNumTerms() + 1)) + facetOrd;
    		mSegmentGroupedFacetHits.put(segmentGroupedFacetsIndex);
    	}

    	if (mFacetPrefix != null) {
    		TermsEnum.SeekStatus seekStatus;
    		if (mFacetOrdTermsEnum != null) 
    			seekStatus = mFacetOrdTermsEnum.seekCeil(mFacetPrefix, true);
    		else 
    			seekStatus = TermsEnum.SeekStatus.END;

    		if (seekStatus != TermsEnum.SeekStatus.END) {
    			mStartFacetOrd = (int) mFacetOrdTermsEnum.getOrd();
    			
    		} else {
    			mStartFacetOrd = 0;
    			mEndFacetOrd = 0;
    			
    			return;
    		}

    		BytesRef facetEndPrefix = BytesRef.deepCopyOf(mFacetPrefix);
    		facetEndPrefix.append(UnicodeUtil.BIG_TERM);
    		
    		seekStatus = mFacetOrdTermsEnum.seekCeil(facetEndPrefix, true);
    		if (seekStatus != TermsEnum.SeekStatus.END) 
    			mEndFacetOrd = (int) mFacetOrdTermsEnum.getOrd();
    		else 
    			mEndFacetOrd = mFacetFieldDocTermOrds.getNumTerms(); // Don't include null...
    		
    	} else {
    		mStartFacetOrd = 0;
    		mEndFacetOrd = mFacetFieldDocTermOrds.getNumTerms() + 1;
    	}
    }

    @Override
    protected MultiSegmentResult createSegmentResult() throws IOException {
    	return new MultiSegmentResult(mSegmentFacetCounts, mSegmentTotalCount, 
    			mFacetFieldDocTermOrds.getNumTerms(), mFacetOrdTermsEnum, mStartFacetOrd, mEndFacetOrd);
    }

}
