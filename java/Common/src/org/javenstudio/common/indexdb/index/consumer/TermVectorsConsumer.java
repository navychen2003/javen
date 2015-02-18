package org.javenstudio.common.indexdb.index.consumer;

import java.io.IOException;

import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.codec.ISegmentWriteState;
import org.javenstudio.common.indexdb.codec.ITermVectorsFormat;
import org.javenstudio.common.indexdb.index.DocState;
import org.javenstudio.common.indexdb.index.DocumentWriter;
import org.javenstudio.common.indexdb.store.ByteSliceReader;
import org.javenstudio.common.indexdb.util.ArrayUtil;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.IOUtils;
import org.javenstudio.common.indexdb.util.JvmUtil;

public final class TermVectorsConsumer extends TermsHashConsumer {

	private final BytesRef mFlushTerm = new BytesRef();
	
	// Used by perField when serializing the term vectors
	private final ByteSliceReader mVectorSliceReaderPos = new ByteSliceReader();
	private final ByteSliceReader mVectorSliceReaderOff = new ByteSliceReader();
	private boolean mHasVectors = false;

	private final ITermVectorsFormat mTermVectorsFormat;
	private ITermVectorsFormat.Writer mVectorsWriter = null;
	//private int mFreeCount = 0;
	private int mLastDocID = 0;
	
	private TermVectorsConsumerPerField[] mPerFields;
	private int mNumVectorFields = 0;
	
	public TermVectorsConsumer(DocumentWriter writer) { 
		super(writer);
		
		mTermVectorsFormat = writer.getSegmentWriter().getIndexWriter()
				.getIndexFormat().getTermVectorsFormat();
	}
	
	@Override
	public TermsHashConsumerPerField addField(
			TermsHashPerField termsHashPerField, IFieldInfo fieldInfo) { 
		return new TermVectorsConsumerPerField(
				termsHashPerField, this, fieldInfo);
	}
	
	final ITermVectorsFormat getTermVectorsFormat() { return mTermVectorsFormat; }
	final void setHasVectors(boolean hasVectors) { mHasVectors = hasVectors; }
	
	final BytesRef getFlushTerm() { return mFlushTerm; }
	final ITermVectorsFormat.Writer getTermVectorsWriter() { return mVectorsWriter; }
	
	final ByteSliceReader getPositionReader() { return mVectorSliceReaderPos; }
	final ByteSliceReader getOffsetReader() { return mVectorSliceReaderOff; }
	
	final void addFieldToFlush(TermVectorsConsumerPerField fieldToFlush) {
	    if (mNumVectorFields == mPerFields.length) {
	    	int newSize = ArrayUtil.oversize(mNumVectorFields + 1, JvmUtil.NUM_BYTES_OBJECT_REF);
	    	TermVectorsConsumerPerField[] newArray = new TermVectorsConsumerPerField[newSize];
	    	System.arraycopy(mPerFields, 0, newArray, 0, mNumVectorFields);
	    	mPerFields = newArray;
	    }

	    mPerFields[mNumVectorFields++] = fieldToFlush;
	}
	
	/** Fills in no-term-vectors for all docs we haven't seen
	 *  since the last doc that had term vectors. */
	private void fill(int docID) throws IOException {
		while (mLastDocID < docID) {
			mVectorsWriter.startDocument(0);
			mLastDocID ++;
		}
	}
	
	private final void initTermVectorsWriter() throws IOException {
		if (mVectorsWriter == null) { 
			mVectorsWriter = getTermVectorsFormat().createWriter(
					getDocumentWriter().getDirectory(), 
					getDocumentWriter().getSegmentInfo().getName());
			mLastDocID = 0;
		}
	}
	
	private void reset() {
	    mPerFields = new TermVectorsConsumerPerField[1];
	    mNumVectorFields = 0;
	}
	
	@Override
	public void startDocument() throws IOException {
		//assert clearLastVectorFieldName();
		reset();
	}
	
	@Override
	public void finishDocument(TermsHash termsHash) throws IOException {
		if (!mHasVectors) 
			return;

		final DocState docState = getDocumentWriter().getDocState();
		
		initTermVectorsWriter();
		fill(docState.getDocID());

		// Append term vectors to the real outputs:
		mVectorsWriter.startDocument(mNumVectorFields);
		for (int i = 0; i < mNumVectorFields; i++) {
			mPerFields[i].finishDocument();
		}

		assert mLastDocID == docState.getDocID() : "lastDocID=" + mLastDocID 
				+ " docState.docID=" + docState.getDocID();
		mLastDocID ++;

		termsHash.reset();
		reset();
	}

	@Override
	public void abort() {
		mHasVectors = false;

		if (mVectorsWriter != null) {
			mVectorsWriter.abort();
			mVectorsWriter = null;
		}

		mLastDocID = 0;
		reset();
	}
	
	@Override
	public void flush(TermsHashConsumerPerField.List fieldsToFlush, 
			ISegmentWriteState state) throws IOException { 
		if (mVectorsWriter != null) {
			int numDocs = state.getSegmentInfo().getDocCount();
			// At least one doc in this run had term vectors enabled
			try {
				fill(numDocs);
				mVectorsWriter.finish(state.getFieldInfos(), numDocs);
			} finally {
				IOUtils.close(mVectorsWriter);
				mVectorsWriter = null;

				mLastDocID = 0;
				mHasVectors = false;
			}
		}

		for (final TermsHashConsumerPerField field : fieldsToFlush.values() ) {
			TermVectorsConsumerPerField perField = (TermVectorsConsumerPerField) field;
			perField.getTermsHashPerField().reset();
			perField.shrinkHash();
		}
	}
	
}
