package org.javenstudio.hornet.codec.postings;

import java.io.IOException;

import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IIndexInput;
import org.javenstudio.common.indexdb.IndexOptions;
import org.javenstudio.common.indexdb.index.term.DocsAndPositionsEnum;
import org.javenstudio.common.indexdb.store.IndexInput;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.BytesRef;

//Decodes docs & positions & (payloads and/or offsets)
final class SegmentFullPositionsEnum extends DocsAndPositionsEnum {
	
	protected final StoredTermPostingsReader mReader;
	
	protected final IIndexInput mStartFreqIn;
    private final IIndexInput mFreqIn;
    private final IIndexInput mProxIn;

    private int mLimit;       		// number of docs in this posting
    private int mOrd;        		// how many docs we've read
    private int mDoc = -1;    		// doc we last read
    private int mAccum;        		// accumulator for doc deltas
    private int mFreq;           	// freq we last read
    private int mPosition;

    private Bits mLiveDocs;

    private long mFreqOffset;
    private int mSkipOffset;
    private long mProxOffset;

    private int mPosPendingCount;
    private int mPayloadLength;
    private boolean mPayloadPending;

    private boolean mSkipped;
    private StoredSkipListReader mSkipper;
    private BytesRef mPayload;
    private long mLazyProxPointer;
    
    private boolean mStorePayloads;
    private boolean mStoreOffsets;
    
    private int mOffsetLength;
    private int mStartOffset;

    public SegmentFullPositionsEnum(StoredTermPostingsReader reader, 
    		IIndexInput freqIn, IIndexInput proxIn) throws IOException {
    	mReader = reader;
    	mStartFreqIn = freqIn;
    	mFreqIn = (IndexInput) freqIn.clone();
    	mProxIn = (IndexInput) proxIn.clone();
    }

    public SegmentFullPositionsEnum reset(IFieldInfo fieldInfo, 
    		StandardTermState termState, Bits liveDocs) throws IOException {
    	mStoreOffsets = fieldInfo.getIndexOptions().compareTo(
    			IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0;
    	mStorePayloads = fieldInfo.hasPayloads();
    	
    	assert fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0;
    	assert mStorePayloads || mStoreOffsets;
    	
    	if (mPayload == null) {
    		mPayload = new BytesRef();
    		mPayload.mBytes = new byte[1];
    	}

    	mLiveDocs = liveDocs;

    	// TODO: for full enum case (eg segment merging) this
    	// seek is unnecessary; maybe we can avoid in such
    	// cases
    	mFreqIn.seek(termState.mFreqOffset);
    	mLazyProxPointer = termState.mProxOffset;

    	mLimit = termState.getDocFreq();
    	mOrd = 0;
    	mDoc = -1;
    	mAccum = 0;
    	mPosition = 0;
    	mStartOffset = 0;

    	mSkipped = false;
    	mPosPendingCount = 0;
    	mPayloadPending = false;

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

    		mAccum += code >>> 1; 			// shift off low bit
    		if ((code & 1) != 0)  			// if low bit is set
    			mFreq = 1; 					// freq is one
    		else 
    			mFreq = mFreqIn.readVInt(); // else read freq
        
    		mPosPendingCount += mFreq;

    		if (mLiveDocs == null || mLiveDocs.get(mAccum)) 
    			break;
    	}

    	mPosition = 0;
    	mStartOffset = 0;

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
    					mFreqOffset, mProxOffset, mLimit, mStorePayloads, mStoreOffsets);

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
    			mStartOffset = 0;
    			mPayloadPending = false;
    			
    			mPayloadLength = mSkipper.getPayloadLength();
    			mOffsetLength = mSkipper.getOffsetLength();
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
      
    	if (mPayloadPending && mPayloadLength > 0) {
    		// payload of last position was never retrieved -- skip it
    		mProxIn.seek(mProxIn.getFilePointer() + mPayloadLength);
    		mPayloadPending = false;
    	}

    	// scan over any docs that were iterated without their positions
    	while (mPosPendingCount > mFreq) {
    		final int code = mProxIn.readVInt();

    		if (mStorePayloads) {
    			if ((code & 1) != 0) {
    				// new payload length
    				mPayloadLength = mProxIn.readVInt();
    				assert mPayloadLength >= 0;
    			}
    			assert mPayloadLength != -1;
    		}
        
    		if (mStoreOffsets) {
    			if ((mProxIn.readVInt() & 1) != 0) {
    				// new offset length
    				mOffsetLength = mProxIn.readVInt();
    			}
    		}
        
    		if (mStorePayloads) 
    			mProxIn.seek(mProxIn.getFilePointer() + mPayloadLength);
        
    		mPosPendingCount --;
    		mPosition = 0;
    		mStartOffset = 0;
    		mPayloadPending = false;
    	}

    	// read next position
    	if (mPayloadPending && mPayloadLength > 0) {
    		// payload wasn't retrieved for last position
    		mProxIn.seek(mProxIn.getFilePointer() + mPayloadLength);
    	}

    	int code = mProxIn.readVInt();
    	if (mStorePayloads) {
    		if ((code & 1) != 0) {
    			// new payload length
    			mPayloadLength = mProxIn.readVInt();
    			assert mPayloadLength >= 0;
    		}
    		assert mPayloadLength != -1;
          
    		mPayloadPending = true;
    		code >>>= 1;
    	}
    	mPosition += code;
      
    	if (mStoreOffsets) {
    		int offsetCode = mProxIn.readVInt();
    		if ((offsetCode & 1) != 0) {
    			// new offset length
    			mOffsetLength = mProxIn.readVInt();
    		}
    		mStartOffset += offsetCode >>> 1;
    	}

    	mPosPendingCount --;
    	assert mPosPendingCount >= 0: "nextPosition() was called too many times " + 
    			"(more than freq() times) posPendingCount=" + mPosPendingCount;

    	return mPosition;
    }

    @Override
    public int startOffset() throws IOException {
    	return mStoreOffsets ? mStartOffset : -1;
    }

    @Override
    public int endOffset() throws IOException {
    	return mStoreOffsets ? mStartOffset + mOffsetLength : -1;
    }

    /** 
     * Returns the payload at this position, or null if no
     *  payload was indexed. 
     */
    @Override
    public BytesRef getPayload() throws IOException {
    	if (mStorePayloads) {
    		assert mLazyProxPointer == -1;
    		assert mPosPendingCount < mFreq;
    		
    		if (!mPayloadPending) {
    			throw new IOException("Either no payload exists at this term position " + 
    					"or an attempt was made to load it more than once.");
    		}
    		
    		if (mPayloadLength > mPayload.mBytes.length) 
    			mPayload.grow(mPayloadLength);

    		mProxIn.readBytes(mPayload.mBytes, 0, mPayloadLength);
    		mPayload.mLength = mPayloadLength;
    		mPayloadPending = false;

    		return mPayload;
    	} else {
    		throw new IOException("No payloads exist for this field!");
    	}
    }

    //@Override
    public boolean hasPayload() {
    	return mPayloadPending && mPayloadLength > 0;
    }
    
}
