package org.javenstudio.hornet.codec.postings;

import java.io.IOException;

import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IIndexInput;
import org.javenstudio.common.indexdb.IndexOptions;
import org.javenstudio.common.indexdb.index.term.DocsAndPositionsEnum;
import org.javenstudio.common.indexdb.store.IndexInput;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.BytesRef;

// TODO specialize DocsAndPosEnum too
// Decodes docs & positions. payloads nor offsets are present.
final class SegmentDocsAndPositionsEnum extends DocsAndPositionsEnum {
	
	protected final StoredTermPostingsReader mReader;
	
	protected final IIndexInput mStartFreqIn;
	private final IIndexInput mFreqIn;
    private final IIndexInput mProxIn;
    
    private int mLimit;     		// number of docs in this posting
    private int mOrd;         		// how many docs we've read
    private int mDoc = -1;      	// doc we last read
    private int mAccum;         	// accumulator for doc deltas
    private int mFreq;         		// freq we last read
    private int mPosition;

    private Bits mLiveDocs;

    private long mFreqOffset;
    private int mSkipOffset;
    private long mProxOffset;

    private int mPosPendingCount;

    private boolean mSkipped;
    private StoredSkipListReader mSkipper;
    private long mLazyProxPointer;

    public SegmentDocsAndPositionsEnum(StoredTermPostingsReader reader, 
    		IIndexInput freqIn, IIndexInput proxIn) throws IOException {
    	mReader = reader;
    	mStartFreqIn = freqIn;
    	mFreqIn = (IIndexInput) freqIn.clone();
    	mProxIn = (IIndexInput) proxIn.clone();
    }

    public SegmentDocsAndPositionsEnum reset(IFieldInfo fieldInfo, 
    		StandardTermState termState, Bits liveDocs) throws IOException {
    	assert fieldInfo.getIndexOptions() == IndexOptions.DOCS_AND_FREQS_AND_POSITIONS;
    	assert !fieldInfo.hasPayloads();

    	mLiveDocs = liveDocs;

    	// TODO: for full enum case (eg segment merging) this
    	// seek is unnecessary; maybe we can avoid in such
    	// cases
    	mFreqIn.seek(termState.mFreqOffset);
    	mLazyProxPointer = termState.mProxOffset;

    	mLimit = termState.getDocFreq();
    	assert mLimit > 0;

    	mOrd = 0;
    	mDoc = -1;
    	mAccum = 0;
    	mPosition = 0;

    	mSkipped = false;
    	mPosPendingCount = 0;

    	mFreqOffset = termState.mFreqOffset;
    	mProxOffset = termState.mProxOffset;
    	mSkipOffset = termState.mSkipOffset;
    	
    	return this;
    }

    @Override
    public int nextDoc() throws IOException {
    	while (true) {
    		if (mOrd == mLimit) 
    			return mDoc = NO_MORE_DOCS;

    		mOrd ++;

    		// Decode next doc/freq pair
    		final int code = mFreqIn.readVInt();

    		mAccum += code >>> 1;     			// shift off low bit
    		if ((code & 1) != 0)           		// if low bit is set
    			mFreq = 1;                     	// freq is one
    		else 
    			mFreq = mFreqIn.readVInt();     // else read freq
    		
    		mPosPendingCount += mFreq;

    		if (mLiveDocs == null || mLiveDocs.get(mAccum)) 
    			break;
    	}

    	mPosition = 0;

    	return (mDoc = mAccum);
    }

    @Override
    public int getDocID() {
    	return mDoc;
    }

    @Override
    public int getFreq() throws IOException {
    	return mFreq;
    }

    @Override
    public int advance(int target) throws IOException {
    	if ((target - mReader.mSkipInterval) >= mDoc && mLimit >= mReader.mSkipMinimum) {
    		// There are enough docs in the posting to have
    		// skip data, and it isn't too close

    		if (mSkipper == null) {
    			// This is the first time this enum has ever been used for skipping -- do lazy init
    			mSkipper = new StoredSkipListReader((IndexInput) mFreqIn.clone(), 
    					mReader.mMaxSkipLevels, mReader.mSkipInterval);
    		}

    		if (!mSkipped) {
    			// This is the first time this posting has
    			// skipped, since reset() was called, so now we
    			// load the skip data for this posting

    			mSkipper.init(mFreqOffset + mSkipOffset,
    					mFreqOffset, mProxOffset, mLimit, false, false);

    			mSkipped = true;
    		}

    		final int newOrd = mSkipper.skipTo(target); 
    		if (newOrd > mOrd) {
    			// Skipper moved
    			mOrd = newOrd;
    			mDoc = mAccum = mSkipper.getDoc();
    			mFreqIn.seek(mSkipper.getFreqPointer());
    			mLazyProxPointer = mSkipper.getProxPointer();
    			
    			mPosPendingCount = 0;
    			mPosition = 0;
    		}
    	}
        
    	// Now, linear scan for the rest:
    	do {
    		nextDoc();
    	} while (target > mDoc);

    	return mDoc;
    }

    @Override
    public int nextPosition() throws IOException {
    	if (mLazyProxPointer != -1) {
    		mProxIn.seek(mLazyProxPointer);
    		mLazyProxPointer = -1;
    	}

    	// scan over any docs that were iterated without their positions
    	if (mPosPendingCount > mFreq) {
    		mPosition = 0;
    		while (mPosPendingCount != mFreq) {
    			if ((mProxIn.readByte() & 0x80) == 0) 
    				mPosPendingCount --;
    		}
    	}

    	mPosition += mProxIn.readVInt();
    	mPosPendingCount --;

    	assert mPosPendingCount >= 0: "nextPosition() was called too many times " + 
    			"(more than freq() times) posPendingCount=" + mPosPendingCount;

    	return mPosition;
    }

    @Override
    public int startOffset() throws IOException {
    	return -1;
    }

    @Override
    public int endOffset() throws IOException {
    	return -1;
    }

    /** 
     * Returns the payload at this position, or null if no
     *  payload was indexed. 
     */
    @Override
    public BytesRef getPayload() throws IOException {
    	//throw new IOException("No payloads exist for this field!");
    	return null;
    }

    //@Override
    //public boolean hasPayload() {
    //	return false;
    //}
    
}
