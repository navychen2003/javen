package org.javenstudio.hornet.codec.vectors;

import java.io.IOException;
import java.util.Comparator;

import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.index.term.Terms;
import org.javenstudio.common.indexdb.util.BytesRef;

final class TermVectorsTerms extends Terms {
	
	private final StoredTermVectorsReader mReader;
	
    private final int mNumTerms;
    private final long mTvfFPStart;
    private final boolean mStorePositions;
    private final boolean mStoreOffsets;
    private final boolean mStorePayloads;

    public TermVectorsTerms(StoredTermVectorsReader reader, long tvfFP) 
    		throws IOException {
    	mReader = reader;
    	
    	mReader.getTvfStream().seek(tvfFP);
    	mNumTerms = mReader.getTvfStream().readVInt();
    	
    	final byte bits = mReader.getTvfStream().readByte();
    	
    	mStorePositions = (bits & StoredTermVectorsFormat.STORE_POSITIONS_WITH_TERMVECTOR) != 0;
    	mStoreOffsets = (bits & StoredTermVectorsFormat.STORE_OFFSET_WITH_TERMVECTOR) != 0;
    	mStorePayloads = (bits & StoredTermVectorsFormat.STORE_PAYLOAD_WITH_TERMVECTOR) != 0;
    	mTvfFPStart = mReader.getTvfStream().getFilePointer();
    }

    @Override
    public ITermsEnum iterator(ITermsEnum reuse) throws IOException {
    	final TermVectorsTermsEnum termsEnum;
    	if (reuse instanceof TermVectorsTermsEnum) {
    		TermVectorsTermsEnum reuseEnum = (TermVectorsTermsEnum) reuse;
    		if (!reuseEnum.canReuse(mReader.getTvfStream())) 
    			termsEnum = new TermVectorsTermsEnum(mReader);
    		else
    			termsEnum = reuseEnum;
    	} else {
    		termsEnum = new TermVectorsTermsEnum(mReader);
    	}
    	
    	termsEnum.reset(mNumTerms, mTvfFPStart, 
    			mStorePositions, mStoreOffsets, mStorePayloads);
    	
    	return termsEnum;
    }

    @Override
    public long size() {
    	return mNumTerms;
    }

    @Override
    public long getSumTotalTermFreq() {
    	return -1;
    }

    @Override
    public long getSumDocFreq() {
    	// Every term occurs in just one doc:
    	return mNumTerms;
    }

    @Override
    public int getDocCount() {
    	return 1;
    }

    @Override
    public Comparator<BytesRef> getComparator() {
    	// TODO: really indexer hardwires
    	// this...?  I guess codec could buffer and re-sort...
    	return BytesRef.getUTF8SortedAsUnicodeComparator();
    }

    public boolean hasOffsets() {
    	return mStoreOffsets;
    }

    public boolean hasPositions() {
    	return mStorePositions;
    }
    
    public boolean hasPayloads() {
    	return mStorePayloads;
    }
    
}
