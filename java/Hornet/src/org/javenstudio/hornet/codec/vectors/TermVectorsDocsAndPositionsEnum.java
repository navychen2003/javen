package org.javenstudio.hornet.codec.vectors;

import java.io.IOException;

import org.javenstudio.common.indexdb.index.term.DocsAndPositionsEnum;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.BytesRef;

final class TermVectorsDocsAndPositionsEnum extends DocsAndPositionsEnum {
	
    private boolean mDidNext;
    private int mDoc = -1;
    private int mNextPos;
    private Bits mLiveDocs;
    
    private int[] mPositions;
    private int[] mStartOffsets;
    private int[] mEndOffsets;
    private int[] mPayloadOffsets;
    
    private BytesRef mPayload = new BytesRef();
    private byte[] mPayloadBytes;

    @Override
    public int getFreq() throws IOException {
    	if (mPositions != null) {
    		return mPositions.length;
    	} else {
    		assert mStartOffsets != null;
    		return mStartOffsets.length;
    	}
    }

    @Override
    public int getDocID() {
    	return mDoc;
    }

    @Override
    public int nextDoc() {
    	if (!mDidNext && (mLiveDocs == null || mLiveDocs.get(0))) {
    		mDidNext = true;
    		return (mDoc = 0);
    	} else {
    		return (mDoc = NO_MORE_DOCS);
    	}
    }

    @Override
    public int advance(int target) {
    	if (!mDidNext && target == 0) {
    		return nextDoc();
    	} else {
    		return (mDoc = NO_MORE_DOCS);
    	}
    }

    public void reset(Bits liveDocs, 
    		int[] positions, int[] startOffsets, int[] endOffsets, 
    		int[] payloadLengths, byte[] payloadBytes) {
    	mLiveDocs = liveDocs;
    	mPositions = positions;
    	mStartOffsets = startOffsets;
    	mEndOffsets = endOffsets;
    	mPayloadOffsets = payloadLengths;
    	mPayloadBytes = payloadBytes;
    	mDoc = -1;
    	mDidNext = false;
    	mNextPos = 0;
    }

    @Override
    public BytesRef getPayload() {
    	if (mPayloadOffsets == null) 
    		return null;
    	
		int off = mPayloadOffsets[mNextPos-1];
		int end = (mNextPos == mPayloadOffsets.length) ? mPayloadBytes.length : mPayloadOffsets[mNextPos];
		if (end - off == 0) 
			return null;
		
		mPayload.mBytes = mPayloadBytes;
		mPayload.mOffset = off;
		mPayload.mLength = end - off;
		
		return mPayload;
    }

    @Override
    public int nextPosition() {
    	assert (mPositions != null && mNextPos < mPositions.length) ||
        	mStartOffsets != null && mNextPos < mStartOffsets.length;

    	if (mPositions != null) {
    		return mPositions[mNextPos++];
    	} else {
    		mNextPos ++;
    		return -1;
    	}
    }

    @Override
    public int startOffset() {
    	if (mStartOffsets == null) 
    		return -1;
    	else 
    		return mStartOffsets[mNextPos-1];
    }

    @Override
    public int endOffset() {
    	if (mEndOffsets == null) 
    		return -1;
    	else 
    		return mEndOffsets[mNextPos-1];
    }
    
}
