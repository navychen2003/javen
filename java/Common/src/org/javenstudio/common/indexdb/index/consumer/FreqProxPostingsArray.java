package org.javenstudio.common.indexdb.index.consumer;

import java.io.IOException;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.index.DocState;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.JvmUtil;

final class FreqProxPostingsArray extends ParallelPostingsArray {

	private final FreqProxConsumerPerField mPerField;
	
	private final int mTermFreqs[];     	// # times this term occurs in the current doc
	private final int mLastDocIDs[];  		// Last docID where this term occurred
	private final int mLastDocCodes[];  	// Code for prior doc
	private final int mLastPositions[]; 	// Last position where this term occurred
	private final int mLastOffsets[];   	// Last endOffset where this term occurred
	
	public FreqProxPostingsArray(FreqProxConsumerPerField perField, int size, 
			boolean writeFreqs, boolean writeProx, boolean writeOffsets) {
		super(size);
		
		mPerField = perField;
		mTermFreqs = writeFreqs ? new int[size] : null;
		mLastDocIDs = new int[size];
		mLastDocCodes = new int[size];
		
		if (writeProx) {
			mLastPositions = new int[size];
			mLastOffsets = writeOffsets ? new int[size] : null;
			
		} else {
			assert !writeOffsets;
			mLastPositions = null;
			mLastOffsets = null;
		}
	}

	final boolean hasFreq() { return mTermFreqs != null; }
	final boolean hasProx() { return mLastPositions != null; }
	final boolean hasOffsets() { return mLastOffsets != null; }
	
	final int getTermFreqAt(int idx) { return mTermFreqs[idx]; }
	final int getLastDocIDAt(int idx) { return mLastDocIDs[idx]; }
	final int getLastDocCodeAt(int idx) { return mLastDocCodes[idx]; }
	final int getLastPositionAt(int idx) { return mLastPositions[idx]; }
	final int getLastOffsetAt(int idx) { return mLastOffsets[idx]; }
	
	final void setLastDocCodeAt(int idx, int val) { mLastDocCodes[idx] = val; }
	
	@Override
	public ParallelPostingsArray newInstance(int size) {
		return new FreqProxPostingsArray(mPerField, size, hasFreq(), hasProx(), hasOffsets());
	}

	@Override
	public void copyTo(ParallelPostingsArray toArray, int numToCopy) {
		assert toArray instanceof FreqProxPostingsArray;
		FreqProxPostingsArray to = (FreqProxPostingsArray) toArray;
		super.copyTo(toArray, numToCopy);

		System.arraycopy(mLastDocIDs, 0, to.mLastDocIDs, 0, numToCopy);
		System.arraycopy(mLastDocCodes, 0, to.mLastDocCodes, 0, numToCopy);
		if (mLastPositions != null) {
			assert to.mLastPositions != null;
			System.arraycopy(mLastPositions, 0, to.mLastPositions, 0, numToCopy);
		}
		if (mLastOffsets != null) {
			assert to.mLastOffsets != null;
			System.arraycopy(mLastOffsets, 0, to.mLastOffsets, 0, numToCopy);
		}
		if (mTermFreqs != null) {
			assert to.mTermFreqs != null;
			System.arraycopy(mTermFreqs, 0, to.mTermFreqs, 0, numToCopy);
		}
	}

	@Override
	public int getBytesPerPosting() {
		int bytes = ParallelPostingsArray.BYTES_PER_POSTING + 2 * JvmUtil.NUM_BYTES_INT;
		if (mLastPositions != null) 
			bytes += JvmUtil.NUM_BYTES_INT;
		if (mLastOffsets != null) 
			bytes += JvmUtil.NUM_BYTES_INT;
		if (mTermFreqs != null) 
			bytes += JvmUtil.NUM_BYTES_INT;

		return bytes;
	}
	
	final void addNewTerm(int termID, IToken token) throws IOException { 
	    // First time we're seeing this term since the last flush
		final TermsHashPerField mTermsHash = mPerField.getTermsHashPerField();
		final DocState docState = mTermsHash.getDocState();
		final FieldState fieldState = mTermsHash.getFieldState();
		
	    mLastDocIDs[termID] = docState.getDocID();
	    if (!hasFreq()) {
	    	mLastDocCodes[termID] = docState.getDocID();
	    	
	    } else {
	    	mLastDocCodes[termID] = docState.getDocID() << 1;
	    	mTermFreqs[termID] = 1;
	    	
	    	if (hasProx()) {
	    		writeProx(termID, token, fieldState.getPosition());
	    		if (hasOffsets()) 
	    			writeOffsets(termID, token, fieldState.getOffset());
	    		
	    	} else 
	    		assert !hasOffsets();
	    }
		
	    fieldState.setMaxTermFrequency(Math.max(1, fieldState.getMaxTermFrequency()));
	    fieldState.increaseUniqueTermCount(1);
	}
	
	final void addTerm(int termID, IToken token) throws IOException { 
		final TermsHashPerField termsHash = mPerField.getTermsHashPerField();
		final DocState docState = termsHash.getDocState();
		final FieldState fieldState = termsHash.getFieldState();
		
		assert !hasFreq() || mTermFreqs[termID] > 0;
		
		if (!hasFreq()) {
			assert mTermFreqs == null;
			if (docState.getDocID() != mLastDocIDs[termID]) {
				assert docState.getDocID() > mLastDocIDs[termID];
				
				termsHash.writeVInt(0, mLastDocCodes[termID]);
				
				mLastDocCodes[termID] = docState.getDocID() - mLastDocIDs[termID];
				mLastDocIDs[termID] = docState.getDocID();
				
				fieldState.increaseUniqueTermCount(1);
			}
			
	    } else if (docState.getDocID() != mLastDocIDs[termID]) {
	    	// Term not yet seen in the current doc but previously
	    	// seen in other doc(s) since the last flush
	    	assert docState.getDocID() > mLastDocIDs[termID] : "id: " + docState.getDocID() + 
	    		" postings ID: "+ mLastDocIDs[termID] + " termID: " + termID;

	    	// Now that we know doc freq for previous doc,
	    	// write it & lastDocCode
	    	if (1 == mTermFreqs[termID]) {
	    		termsHash.writeVInt(0, mLastDocCodes[termID]|1);
	    	} else {
	    		termsHash.writeVInt(0, mLastDocCodes[termID]);
	    		termsHash.writeVInt(0, mTermFreqs[termID]);
	    	}
	    	
	    	mTermFreqs[termID] = 1;
	    	mLastDocCodes[termID] = (docState.getDocID() - mLastDocIDs[termID]) << 1;
	    	mLastDocIDs[termID] = docState.getDocID();
	    	
	    	if (hasProx()) {
	    		writeProx(termID, token, fieldState.getPosition());
	    		if (hasOffsets()) {
	    			mLastOffsets[termID] = 0;
	    			writeOffsets(termID, token, fieldState.getOffset());
	    		}
	    	} else 
	    		assert !hasOffsets();
	    	
	    	fieldState.setMaxTermFrequency(Math.max(1, fieldState.getMaxTermFrequency()));
	    	fieldState.increaseUniqueTermCount(1);
	    	
	    } else {
	    	fieldState.setMaxTermFrequency(Math.max(fieldState.getMaxTermFrequency(), ++mTermFreqs[termID]));
	    	
	    	if (hasProx()) 
	    		writeProx(termID, token, fieldState.getPosition()-mLastPositions[termID]);
	    	if (hasOffsets()) 
	    		writeOffsets(termID, token, fieldState.getOffset());
	    }
	}
	
	final void writeProx(final int termID, IToken token, int proxCode) {
		final TermsHashPerField termsHash = mPerField.getTermsHashPerField();
		final FieldState fieldState = termsHash.getFieldState();
		
		assert hasProx();
		final BytesRef payload = token.getPayload();

		if (payload != null && payload.getLength() > 0) {
			termsHash.writeVInt(1, (proxCode<<1)|1);
			termsHash.writeVInt(1, payload.getLength());
			termsHash.writeBytes(1, payload.getBytes(), payload.getOffset(), payload.getLength());
			mPerField.setHasPayloads(true);
		} else {
			termsHash.writeVInt(1, proxCode<<1);
		}

		mLastPositions[termID] = fieldState.getPosition();
	}

	final void writeOffsets(final int termID, IToken token, int offsetAccum) {
		final TermsHashPerField termsHash = mPerField.getTermsHashPerField();
		
		assert hasOffsets();
		final int startOffset = offsetAccum + token.getStartOffset();
		final int endOffset = offsetAccum + token.getEndOffset();
		
		assert startOffset - mLastOffsets[termID] >= 0;
		termsHash.writeVInt(1, startOffset - mLastOffsets[termID]);
		termsHash.writeVInt(1, endOffset - startOffset);

		mLastOffsets[termID] = startOffset;
	}
	
}
