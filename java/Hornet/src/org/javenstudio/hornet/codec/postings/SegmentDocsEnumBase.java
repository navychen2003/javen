package org.javenstudio.hornet.codec.postings;

import java.io.IOException;

import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IIndexInput;
import org.javenstudio.common.indexdb.IndexOptions;
import org.javenstudio.common.indexdb.index.term.DocsEnum;
import org.javenstudio.common.indexdb.store.IndexInput;
import org.javenstudio.common.indexdb.util.Bits;

abstract class SegmentDocsEnumBase extends DocsEnum {
    
	protected final StoredTermPostingsReader mReader;
	
    protected final int[] mDocs = new int[StoredTermPostingsReader.BUFFERSIZE];
    protected final int[] mFreqs = new int[StoredTermPostingsReader.BUFFERSIZE];
    
    protected final IIndexInput mFreqIn; 		// reuse
    protected final IIndexInput mStartFreqIn; 	// reuse
    protected StoredSkipListReader mSkipper; 	// reuse - lazy loaded
    
    protected boolean mIndexOmitsTF;     		// does current field omit term freq?
    protected boolean mStorePayloads;      		// does current field store payloads?
    protected boolean mStoreOffsets;         	// does current field store offsets?

    protected int mLimit;                   	// number of docs in this posting
    protected int mOrd;                     	// how many docs we've read
    protected int mDoc;                      	// doc we last read
    protected int mAccum;                    	// accumulator for doc deltas
    protected int mFreq;                      	// freq we last read
    protected int mMaxBufferedDocId;
    
    protected int mStart;
    protected int mCount;

    protected long mFreqOffset;
    protected int mSkipOffset;

    protected boolean mSkipped;
    protected final Bits mLiveDocs;
    
    protected SegmentDocsEnumBase(StoredTermPostingsReader reader, 
    		IIndexInput startFreqIn, Bits liveDocs) throws IOException {
    	mReader = reader;
    	mStartFreqIn = startFreqIn;
    	mFreqIn = (IIndexInput)startFreqIn.clone();
    	mLiveDocs = liveDocs;
    }
    
    public DocsEnum reset(IFieldInfo fieldInfo, StandardTermState termState) 
    		throws IOException {
    	mIndexOmitsTF = fieldInfo.getIndexOptions() == IndexOptions.DOCS_ONLY;
    	mStorePayloads = fieldInfo.hasPayloads();
    	mStoreOffsets = fieldInfo.getIndexOptions().compareTo(
    			IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0;
    			
    	mFreqOffset = termState.mFreqOffset;
    	mSkipOffset = termState.mSkipOffset;

    	// TODO: for full enum case (eg segment merging) this
    	// seek is unnecessary; maybe we can avoid in such
    	// cases
    	mFreqIn.seek(termState.mFreqOffset);
    	mLimit = termState.getDocFreq();
    	assert mLimit > 0;
    	
    	mOrd = 0;
    	mDoc = -1;
    	mAccum = 0;
    	mSkipped = false;

    	mStart = -1;
    	mCount = 0;
    	mMaxBufferedDocId = -1;
    	
    	return this;
    }
    
    @Override
    public final int getFreq() throws IOException {
    	assert !mIndexOmitsTF;
    	return mFreq;
    }

    @Override
    public final int getDocID() {
    	return mDoc;
    }
    
    @Override
    public final int advance(int target) throws IOException {
    	// last doc in our buffer is >= target, binary search + next()
    	if (++mStart < mCount && mMaxBufferedDocId >= target) {
    		// 32 seemed to be a sweetspot here so use binsearch if the pending results are a lot
    		if ((mCount - mStart) > 32) {
    			mStart = binarySearch(mCount - 1, mStart, target, mDocs);
    			return nextDoc();
    			
    		} else 
    			return linearScan(target);
    	}
      
    	mStart = mCount; // buffer is consumed
    	mDoc = skipTo(target);
    	
    	return mDoc;
    }
    
    private final int binarySearch(int hi, int low, int target, int[] docs) {
    	while (low <= hi) {
    		int mid = (hi + low) >>> 1;
    		int doc = docs[mid];
    		if (doc < target) {
    			low = mid + 1;
    		} else if (doc > target) {
    			hi = mid - 1;
    		} else {
    			low = mid;
    			break;
    		}
    	}
    	return low-1;
    }
    
    final int readFreq(final IIndexInput freqIn, final int code) throws IOException {
    	if ((code & 1) != 0)  			// if low bit is set
    		return 1; 					// freq is one
    	else 
    		return freqIn.readVInt(); 	// else read freq
    }
    
    protected abstract int linearScan(int scanTo) throws IOException;
    protected abstract int scanTo(int target) throws IOException;
    protected abstract int nextUnreadDoc() throws IOException;

    protected final int refill() throws IOException {
    	final int doc = nextUnreadDoc();
    	
    	mCount = 0;
    	mStart = -1;
    	
    	if (doc == NO_MORE_DOCS) 
    		return NO_MORE_DOCS;
    	
    	final int numDocs = Math.min(mDocs.length, mLimit - mOrd);
    	
    	mOrd += numDocs;
    	if (mIndexOmitsTF) 
    		mCount = fillDocs(numDocs);
    	else 
    		mCount = fillDocsAndFreqs(numDocs);
    	
    	mMaxBufferedDocId = mCount > 0 ? mDocs[mCount-1] : NO_MORE_DOCS;
    	
    	return doc;
    }
    
    private final int fillDocs(int size) throws IOException {
    	final IIndexInput freqIn = mFreqIn;
    	final int docs[] = mDocs;
    	int docAc = mAccum;
    	
    	for (int i = 0; i < size; i++) {
    		docAc += freqIn.readVInt();
    		docs[i] = docAc;
    	}
    	
    	mAccum = docAc;
    	
    	return size;
    }
    
    private final int fillDocsAndFreqs(int size) throws IOException {
    	final IIndexInput freqIn = mFreqIn;
    	final int docs[] = mDocs;
    	final int freqs[] = mFreqs;
    	int docAc = mAccum;
    	
    	for (int i = 0; i < size; i++) {
    		final int code = freqIn.readVInt();
    		docAc += code >>> 1; // shift off low bit
        	freqs[i] = readFreq(freqIn, code);
        	docs[i] = docAc;
    	}
    	
    	mAccum = docAc;
    	
    	return size;
    }

    private final int skipTo(int target) throws IOException {
    	if ((target - mReader.mSkipInterval) >= mAccum && mLimit >= mReader.mSkipMinimum) {
    		// There are enough docs in the posting to have
    		// skip data, and it isn't too close.

    		if (mSkipper == null) {
    			// This is the first time this enum has ever been used for skipping -- do lazy init
    			mSkipper = new StoredSkipListReader((IndexInput) mFreqIn.clone(), 
    					mReader.mMaxSkipLevels, mReader.mSkipInterval);
    		}

    		if (!mSkipped) {
    			// This is the first time this posting has
    			// skipped since reset() was called, so now we
    			// load the skip data for this posting

    			mSkipper.init(mFreqOffset + mSkipOffset, mFreqOffset, 0, 
    					mLimit, mStorePayloads, mStoreOffsets);

    			mSkipped = true;
    		}

    		final int newOrd = mSkipper.skipTo(target); 
    		if (newOrd > mOrd) {
    			// Skipper moved
    			mOrd = newOrd;
    			mAccum = mSkipper.getDoc();
    			mFreqIn.seek(mSkipper.getFreqPointer());
    		}
    	}
    	
    	return scanTo(target);
    }
    
}
