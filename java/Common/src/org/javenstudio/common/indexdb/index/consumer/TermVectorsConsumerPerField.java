package org.javenstudio.common.indexdb.index.consumer;

import java.io.IOException;

import org.javenstudio.common.indexdb.IField;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.codec.ITermVectorsFormat;
import org.javenstudio.common.indexdb.store.ByteSliceReader;
import org.javenstudio.common.indexdb.util.BytesRef;

final class TermVectorsConsumerPerField extends TermsHashConsumerPerField {

	private final TermVectorsConsumer mTermsWriter;
	
	private boolean mStoreVectors = false;
	private boolean mStoreVectorPositions = false;
	private boolean mStoreVectorOffsets = false;
	private int mMaxNumPostings = 0;
	
	public TermVectorsConsumerPerField(
			TermsHashPerField termsHashPerField, TermVectorsConsumer parent, 
			IFieldInfo fieldInfo) { 
		super(termsHashPerField, parent, fieldInfo);
		mTermsWriter = parent;
	}
	
	@Override
	protected ParallelPostingsArray createPostingsArray(int size) {
		return new TermVectorsPostingsArray(this, size);
	}
	
	@Override
	public int getStreamCount() { return 2; }
	
	final boolean isStoreVectors() { return mStoreVectors; }
	final boolean isStoreVectorPositions() { return mStoreVectorPositions; }
	final boolean isStoreVectorOffsets() { return mStoreVectorOffsets; }
	
	@Override
	public boolean start(IField[] fields, int count) {
		mStoreVectors = false;
		mStoreVectorPositions = false;
		mStoreVectorOffsets = false;

		for (int i=0; i < count; i++) {
			IField field = fields[i];
			if (field.getFieldType().isIndexed() && field.getFieldType().isStoreTermVectors()) {
				mStoreVectors = true;
				mStoreVectorPositions |= field.getFieldType().isStoreTermVectorPositions();
				mStoreVectorOffsets |= field.getFieldType().isStoreTermVectorOffsets();
			}
		}

		if (mStoreVectors) {
			mTermsWriter.setHasVectors(true);
			if (getTermsHashPerField().getBytesHash().size() != 0) {
				// Only necessary if previous doc hit a
				// non-aborting exception while writing vectors in
				// this field:
				getTermsHashPerField().reset();
			}
		}

		return mStoreVectors;
	}

	/** 
	 * Called once per field per document if term vectors
	 *  are enabled, to write the vectors to
	 *  RAMOutputStream, which is then quickly flushed to
	 *  the real term vectors files in the Directory. 
	 */  
	@Override
	public void finish() throws IOException {
		if (!mStoreVectors || getTermsHashPerField().getBytesHash().size() == 0) 
			return;

		mTermsWriter.addFieldToFlush(this);
	}
	
	final void shrinkHash() {
		getTermsHashPerField().shrinkHash(mMaxNumPostings);
		mMaxNumPostings = 0;
	}

	@Override
	public void newTerm(final int termID, final IToken token) {
		TermVectorsPostingsArray postings = (TermVectorsPostingsArray) 
				getTermsHashPerField().getPostingsArray();

		postings.addNewTerm(termID, token);
	}
	
	@Override
	public void addTerm(final int termID, IToken token) {
		TermVectorsPostingsArray postings = (TermVectorsPostingsArray) 
				getTermsHashPerField().getPostingsArray();
		
		postings.addTerm(termID, token);
	}
	
	final void finishDocument() throws IOException {
		// This is called once, after inverting all occurrences
		// of a given field in the doc.  At this point we flush
		// our hash into the DocWriter.
		
		final TermsHashPerField termsHash = getTermsHashPerField();
		final ByteBlockPool termBytePool = termsHash.getPrimaryBytePool();
		final TermVectorsPostingsArray postings = (TermVectorsPostingsArray)termsHash.getPostingsArray();
		final int numPostings = termsHash.getBytesHash().size();
		assert numPostings >= 0;
		
		if (numPostings > mMaxNumPostings)
			mMaxNumPostings = numPostings;
		
		final BytesRef flushTerm = mTermsWriter.getFlushTerm();
		final ITermVectorsFormat.Writer tvWriter = mTermsWriter.getTermVectorsWriter();
		
		final ByteSliceReader posReader = isStoreVectorPositions() ? mTermsWriter.getPositionReader() : null;
		final ByteSliceReader offReader = isStoreVectorOffsets() ? mTermsWriter.getOffsetReader() : null;

		final int[] termIDs = termsHash.sortPostings(tvWriter.getComparator());

		//assert mTermsWriter.vectorFieldsInOrder(mFieldInfo);
		
		tvWriter.startField(getFieldInfo(), numPostings, 
				isStoreVectorPositions(), isStoreVectorOffsets());
    
		for (int j=0; j < numPostings; j++) {
			final int termID = termIDs[j];
			final int freq = postings.getFreqAt(termID);

			// Get BytesRef
			termBytePool.setBytesRef(flushTerm, postings.getTextStartAt(termID));
			tvWriter.startTerm(flushTerm, freq);
      
			if (isStoreVectorPositions() || isStoreVectorOffsets()) {
				if (posReader != null) 
					termsHash.initReader(posReader, termID, 0);
				if (offReader != null) 
					termsHash.initReader(offReader, termID, 1);
				tvWriter.addProx(freq, posReader, offReader);
			}
		}

		termsHash.reset();
		// commit the termVectors once successful - FI will otherwise reset them
		getFieldInfo().setStoreTermVectors();
	}
	
}
