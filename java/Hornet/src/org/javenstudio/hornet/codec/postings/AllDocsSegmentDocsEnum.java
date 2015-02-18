package org.javenstudio.hornet.codec.postings;

import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexInput;

final class AllDocsSegmentDocsEnum extends SegmentDocsEnumBase {

    public AllDocsSegmentDocsEnum(StoredTermPostingsReader reader, 
    		IIndexInput startFreqIn) throws IOException {
    	super(reader, startFreqIn, null);
    	assert mLiveDocs == null;
    }
    
	@Override
    public final int nextDoc() throws IOException {
		if (++mStart < mCount) {
			mFreq = mFreqs[mStart];
			return mDoc = mDocs[mStart];
		}
		
		return mDoc = refill();
    }
    
    @Override
    protected final int linearScan(int scanTo) throws IOException {
    	final int[] docs = mDocs;
    	final int upTo = mCount;
    	
    	for (int i = mStart; i < upTo; i++) {
    		final int d = docs[i];
    		if (scanTo <= d) {
    			mStart = i;
    			mFreq = mFreqs[i];
    			return mDoc = docs[i];
    		}
    	}
    	
    	return mDoc = refill();
    }

    @Override
    protected int scanTo(int target) throws IOException { 
    	int docAcc = mAccum;
    	int frq = 1;
    	
    	final IIndexInput freqIn = mFreqIn;
    	final boolean omitTF = mIndexOmitsTF;
    	final int loopLimit = mLimit;
    	
    	for (int i = mOrd; i < loopLimit; i++) {
    		int code = freqIn.readVInt();
    		if (omitTF) {
    			docAcc += code;
    		} else {
    			docAcc += code >>> 1; // shift off low bit
    			frq = readFreq(freqIn, code);
    		}
    		if (docAcc >= target) {
    			mFreq = frq;
    			mOrd = i + 1;
    			return mAccum = docAcc;
    		}
    	}
    	
    	mOrd = mLimit;
    	mFreq = frq;
    	mAccum = docAcc;
    	
    	return NO_MORE_DOCS;
    }

    @Override
    protected final int nextUnreadDoc() throws IOException {
    	if (mOrd++ < mLimit) {
    		int code = mFreqIn.readVInt();
    		if (mIndexOmitsTF) {
    			mAccum += code;
    		} else {
    			mAccum += code >>> 1; // shift off low bit
    			mFreq = readFreq(mFreqIn, code);
    		}
    		return mAccum;
    		
    	} else 
    		return NO_MORE_DOCS;
    }
    
}
