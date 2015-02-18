package org.javenstudio.common.indexdb.index.consumer;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.util.JvmUtil;

final class TermVectorsPostingsArray extends ParallelPostingsArray {

	private final TermVectorsConsumerPerField mPerField;
	
	private final int[] mFreqs;        		// How many times this term occurred in the current doc
	private final int[] mLastOffsets;   	// Last offset we saw
	private final int[] mLastPositions;   	// Last position where this term occurred
	
	public TermVectorsPostingsArray(TermVectorsConsumerPerField perField, int size) {
		super(size);
		mPerField = perField;
		mFreqs = new int[size];
		mLastOffsets = new int[size];
		mLastPositions = new int[size];
	}

	@Override
	public ParallelPostingsArray newInstance(int size) {
		return new TermVectorsPostingsArray(mPerField, size);
	}

	@Override
	public void copyTo(ParallelPostingsArray toArray, int numToCopy) {
		assert toArray instanceof TermVectorsPostingsArray;
		TermVectorsPostingsArray to = (TermVectorsPostingsArray) toArray;

		super.copyTo(toArray, numToCopy);

		System.arraycopy(mFreqs, 0, to.mFreqs, 0, mSize);
		System.arraycopy(mLastOffsets, 0, to.mLastOffsets, 0, mSize);
		System.arraycopy(mLastPositions, 0, to.mLastPositions, 0, mSize);
	}

	@Override
	public int getBytesPerPosting() {
		return super.getBytesPerPosting() + 3 * JvmUtil.NUM_BYTES_INT;
	}

	final int getFreqAt(int pos) { return mFreqs[pos]; }
	
	final void addNewTerm(final int termID, final IToken token) {
		final TermsHashPerField termsHash = mPerField.getTermsHashPerField();
		final FieldState fieldState = termsHash.getFieldState();
		
		mFreqs[termID] = 1;

		if (mPerField.isStoreVectorOffsets()) {
			int startOffset = fieldState.getOffset() + token.getStartOffset();
			int endOffset = fieldState.getOffset() + token.getEndOffset();

			termsHash.writeVInt(1, startOffset);
			termsHash.writeVInt(1, endOffset - startOffset);
			mLastOffsets[termID] = endOffset;
		}

		if (mPerField.isStoreVectorPositions()) {
			termsHash.writeVInt(0, fieldState.getPosition());
			mLastPositions[termID] = fieldState.getPosition();
		}
	}
	
	final void addTerm(final int termID, IToken token) {
		final TermsHashPerField termsHash = mPerField.getTermsHashPerField();
		final FieldState fieldState = termsHash.getFieldState();
		
		mFreqs[termID] ++;

		if (mPerField.isStoreVectorOffsets()) {
			int startOffset = fieldState.getOffset() + token.getStartOffset();
			int endOffset = fieldState.getOffset() + token.getEndOffset();

			termsHash.writeVInt(1, startOffset - mLastOffsets[termID]);
			termsHash.writeVInt(1, endOffset - startOffset);
			mLastOffsets[termID] = endOffset;
		}

		if (mPerField.isStoreVectorPositions()) {
			termsHash.writeVInt(0, fieldState.getPosition() - mLastPositions[termID]);
			mLastPositions[termID] = fieldState.getPosition();
		}
	}
	
}
