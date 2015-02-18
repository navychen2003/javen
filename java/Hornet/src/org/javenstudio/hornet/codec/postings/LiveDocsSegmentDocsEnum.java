package org.javenstudio.hornet.codec.postings;

import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexInput;
import org.javenstudio.common.indexdb.util.Bits;

final class LiveDocsSegmentDocsEnum extends SegmentDocsEnumBase {

    public LiveDocsSegmentDocsEnum(StoredTermPostingsReader reader, 
    		IIndexInput startFreqIn, Bits liveDocs) throws IOException {
    	super(reader, startFreqIn, liveDocs);
    	assert liveDocs != null;
    }
    
    @Override
    public final int nextDoc() throws IOException {
    	final Bits liveDocs = mLiveDocs;
    	
    	for (int i = mStart+1; i < mCount; i++) {
    		int d = mDocs[i];
    		if (liveDocs.get(d)) {
    			mStart = i;
    			mFreq = mFreqs[i];
    			return mDoc = d;
    		}
    	}
    	
    	mStart = mCount;
    	mDoc = refill();
    	
    	return mDoc;
    }

    @Override
    protected final int linearScan(int scanTo) throws IOException {
    	final int[] docs = mDocs;
    	final int upTo = mCount;
    	final Bits liveDocs = mLiveDocs;
    	
    	for (int i = mStart; i < upTo; i++) {
    		int d = docs[i];
    		if (scanTo <= d && liveDocs.get(d)) {
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
    	final Bits liveDocs = mLiveDocs;
    	
    	for (int i = mOrd; i < loopLimit; i++) {
    		int code = freqIn.readVInt();
    		if (omitTF) {
    			docAcc += code;
    		} else {
    			docAcc += code >>> 1; // shift off low bit
    			frq = readFreq(freqIn, code);
    		}
    		if (docAcc >= target && liveDocs.get(docAcc)) {
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
    	int docAcc = mAccum;
    	int frq = 1;
    	
    	final IIndexInput freqIn = mFreqIn;
    	final boolean omitTF = mIndexOmitsTF;
    	final int loopLimit = mLimit;
    	final Bits liveDocs = mLiveDocs;
    	
    	for (int i = mOrd; i < loopLimit; i++) {
    		int code = freqIn.readVInt();
    		if (omitTF) {
    			docAcc += code;
    		} else {
    			docAcc += code >>> 1; // shift off low bit
    			frq = readFreq(freqIn, code);
    		}
    		if (liveDocs.get(docAcc)) {
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
    
}
