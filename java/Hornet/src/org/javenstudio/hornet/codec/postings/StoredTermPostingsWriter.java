package org.javenstudio.hornet.codec.postings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.javenstudio.common.indexdb.CorruptIndexException;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IIndexOutput;
import org.javenstudio.common.indexdb.ITermState;
import org.javenstudio.common.indexdb.IndexOptions;
import org.javenstudio.common.indexdb.index.term.PerTermState;
import org.javenstudio.common.indexdb.store.ram.RAMOutputStream;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.IOUtils;
import org.javenstudio.hornet.codec.CodecUtil;
import org.javenstudio.hornet.codec.TermPostingsWriter;
import org.javenstudio.hornet.codec.SegmentWriteState;

/**
 * Concrete class that writes the 4.0 frq/prx postings format.
 * 
 * @see StoredPostingsFormat
 */
final class StoredTermPostingsWriter extends TermPostingsWriter {

	/** 
	 * Expert: The fraction of TermDocs entries stored in skip tables,
	 * used to accelerate {@link DocsEnum#advance(int)}.  Larger values result in
	 * smaller indexes, greater acceleration, but fewer accelerable cases, while
	 * smaller values result in bigger indexes, less acceleration and more
	 * accelerable cases. More detailed experiments would be useful here. 
	 */
	static final int DEFAULT_SKIP_INTERVAL = 16;

	private static class PendingTerm {
		public final long mFreqStart;
		public final long mProxStart;
		public final int mSkipOffset;

		public PendingTerm(long freqStart, long proxStart, int skipOffset) {
			mFreqStart = freqStart;
			mProxStart = proxStart;
			mSkipOffset = skipOffset;
		}
	}
	
	private final StoredTermPostingsFormat mFormat;
	private final IDirectory mDirectory;
	private final String mSegment;
	
	private final List<PendingTerm> mPendingTerms = new ArrayList<PendingTerm>();
	
	private IIndexOutput mFreqOutput;
	private IIndexOutput mProxOutput;
	private StoredSkipListWriter mSkipListWriter;

	private RAMOutputStream mBytesWriter;
	
	private final int mSkipInterval;
  
	/**
	 * Expert: minimum docFreq to write any skip data at all
	 */
	private final int mSkipMinimum;

	/** 
	 * Expert: The maximum number of skip levels. Smaller values result in 
	 * slightly smaller indexes, but slower skipping in big posting lists.
	 */
	private final int mMaxSkipLevels = 10;
	private final int mTotalNumDocs;
	private IIndexOutput mTermsOut;

	private IndexOptions mIndexOptions;
	private boolean mStorePayloads;
	private boolean mStoreOffsets;
	// Starts a new term
	private long mFreqStart;
	private long mProxStart;
	//private FieldInfo mFieldInfo;
	private int mLastPayloadLength;
	private int mLastOffsetLength;
	private int mLastPosition;
	private int mLastOffset;
	
	private int mLastDocID;
	private int mDf;
	
	public final IDirectory getDirectory() { return mDirectory; }
	public final String getSegmentName() { return mSegment; }
	
	public StoredTermPostingsWriter(StoredTermPostingsFormat format, IDirectory dir, 
			SegmentWriteState state) throws IOException {
		this(format, dir, state, DEFAULT_SKIP_INTERVAL);
	}
  
	public StoredTermPostingsWriter(StoredTermPostingsFormat format, IDirectory dir, 
			SegmentWriteState state, int skipInterval) throws IOException {
		super();
    
		mFormat = format;
		mDirectory = dir;
		mSegment = state.getSegmentInfo().getName();
		
		mSkipInterval = skipInterval;
		mSkipMinimum = skipInterval; /* set to the same for now */
		
		boolean success = false;
		try {
			mFreqOutput = mDirectory.createOutput(mFormat.getContext(), 
					mFormat.getPostingsFreqFileName(mSegment));
			
			CodecUtil.writeHeader(mFreqOutput, mFormat.getFreqCodecName(), 
					StoredTermPostingsFormat.VERSION_CURRENT);
			
			// TODO: this is a best effort, if one of these fields has no postings
			// then we make an empty prx file, same as if we are wrapped in 
			// per-field postingsformat. maybe... we shouldn't
			// bother w/ this opto?  just create empty prx file...?
			if (state.getFieldInfos().hasProx()) {
				// At least one field does not omit TF, so create the
				// prox file
				mProxOutput = mDirectory.createOutput(mFormat.getContext(), 
						mFormat.getPostingsProxFileName(mSegment));
				
				CodecUtil.writeHeader(mProxOutput, mFormat.getProxCodecName(), 
						StoredTermPostingsFormat.VERSION_CURRENT);
			} else {
				// Every field omits TF so we will write no prox file
				mProxOutput = null;
			}
			
			success = true;
		} finally {
			if (!success) 
				IOUtils.closeWhileHandlingException(mFreqOutput, mProxOutput);
		}

		mTotalNumDocs = state.getSegmentInfo().getDocCount();

		mSkipListWriter = new StoredSkipListWriter(mFormat.getContext(), skipInterval,
				mMaxSkipLevels, mTotalNumDocs, mFreqOutput, mProxOutput);
    
		mBytesWriter = new RAMOutputStream(mFormat.getContext());
	}

	@Override
	public void start(IIndexOutput termsOut) throws IOException {
		mTermsOut = termsOut;
		CodecUtil.writeHeader(termsOut, mFormat.getTermsCodecName(), 
				StoredTermPostingsFormat.VERSION_CURRENT);
		termsOut.writeInt(mSkipInterval);  		// write skipInterval
		termsOut.writeInt(mMaxSkipLevels);  	// write maxSkipLevels
		termsOut.writeInt(mSkipMinimum);    	// write skipMinimum
	}

	@Override
	public void startTerm() {
		mFreqStart = mFreqOutput.getFilePointer();
		if (mProxOutput != null) 
			mProxStart = mProxOutput.getFilePointer();
		
		// force first payload to write its length
		mLastPayloadLength = -1;
		// force first offset to write its length
		mLastOffsetLength = -1;
		
		mSkipListWriter.resetSkip();
	}

	// Currently, this instance is re-used across fields, so
	// our parent calls setField whenever the field changes
	@Override
	public void setField(IFieldInfo fieldInfo) {
		//mFieldInfo = fieldInfo;
		mIndexOptions = fieldInfo.getIndexOptions();
    
		mStoreOffsets = mIndexOptions.compareTo(
				IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0;        
		mStorePayloads = fieldInfo.hasPayloads();
	}
  
	/** 
	 * Adds a new doc in this term.  If this returns null
	 *  then we just skip consuming positions/payloads. 
	 */
	@Override
	public void startDoc(int docID, int termDocFreq) throws IOException {
		final int delta = docID - mLastDocID;
    
		if (docID < 0 || (mDf > 0 && delta <= 0)) {
			throw new CorruptIndexException("docs out of order (" + docID + " <= " + 
					mLastDocID + " ) (freqOut: " + mFreqOutput + ")");
		}

		if ((++mDf % mSkipInterval) == 0) {
			mSkipListWriter.setSkipData(mLastDocID, mStorePayloads, 
					mLastPayloadLength, mStoreOffsets, mLastOffsetLength);
			mSkipListWriter.bufferSkip(mDf);
		}

		assert docID < mTotalNumDocs: "docID=" + docID + " totalNumDocs=" + mTotalNumDocs;

		mLastDocID = docID;
		if (mIndexOptions == IndexOptions.DOCS_ONLY) {
			mFreqOutput.writeVInt(delta);
		} else if (1 == termDocFreq) {
			mFreqOutput.writeVInt((delta<<1) | 1);
		} else {
			mFreqOutput.writeVInt(delta<<1);
			mFreqOutput.writeVInt(termDocFreq);
		}

		mLastPosition = 0;
		mLastOffset = 0;
	}

	/** Add a new position & payload */
	@Override
	public void addPosition(int position, BytesRef payload, 
			int startOffset, int endOffset) throws IOException {
		assert mIndexOptions.compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0 : 
			"invalid indexOptions: " + mIndexOptions;
		assert mProxOutput != null;

		final int delta = position - mLastPosition;
		// not quite right (if pos=0 is repeated twice we don't catch it)
		assert delta >= 0: "position=" + position + " lastPosition=" + mLastPosition; 
		
		mLastPosition = position;
		int payloadLength = 0;

		if (mStorePayloads) {
			payloadLength = payload == null ? 0 : payload.getLength();

			if (payloadLength != mLastPayloadLength) {
				mLastPayloadLength = payloadLength;
				mProxOutput.writeVInt((delta<<1)|1);
				mProxOutput.writeVInt(payloadLength);
			} else 
				mProxOutput.writeVInt(delta << 1);
		} else {
			mProxOutput.writeVInt(delta);
		}
    
		if (mStoreOffsets) {
			// don't use startOffset - lastEndOffset, because this creates lots of 
			// negative vints for synonyms,
			// and the numbers aren't that much smaller anyways.
			int offsetDelta = startOffset - mLastOffset;
			int offsetLength = endOffset - startOffset;
			assert offsetDelta >= 0 && offsetLength >= 0 : "startOffset=" + startOffset + 
					",lastOffset=" + mLastOffset + ",endOffset=" + endOffset;
			
			if (offsetLength != mLastOffsetLength) {
				mProxOutput.writeVInt(offsetDelta << 1 | 1);
				mProxOutput.writeVInt(offsetLength);
			} else 
				mProxOutput.writeVInt(offsetDelta << 1);
			
			mLastOffset = startOffset;
			mLastOffsetLength = offsetLength;
		}
    
		if (payloadLength > 0) 
			mProxOutput.writeBytes(payload.getBytes(), payload.getOffset(), payloadLength);
	}

	@Override
	public void finishDoc() {
		// do nothing
	}

	/** Called when we are done adding docs to this term */
	@Override
	public void finishTerm(ITermState stats) throws IOException {
		assert ((PerTermState)stats).getDocFreq() > 0;

		// TODO: wasteful we are counting this (counting # docs
		// for this term) in two places?
		assert ((PerTermState)stats).getDocFreq() == mDf;

		final int skipOffset;
		if (mDf >= mSkipMinimum) 
			skipOffset = (int) (mSkipListWriter.writeSkip(mFreqOutput)-mFreqStart);
		else 
			skipOffset = -1;

		mPendingTerms.add(new PendingTerm(mFreqStart, mProxStart, skipOffset));

		mLastDocID = 0;
		mDf = 0;
	}

	@Override
	public void flushTermsBlock(int start, int count) throws IOException {
		if (count == 0) {
			mTermsOut.writeByte((byte) 0);
			return;
		}

		assert start <= mPendingTerms.size();
		assert count <= start;

		final int limit = mPendingTerms.size() - start + count;
		final PendingTerm firstTerm = mPendingTerms.get(limit - count);
		
		// First term in block is abs coded:
		mBytesWriter.writeVLong(firstTerm.mFreqStart);

		if (firstTerm.mSkipOffset != -1) {
			assert firstTerm.mSkipOffset > 0;
			mBytesWriter.writeVInt(firstTerm.mSkipOffset);
		}
		
		if (mIndexOptions.compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0) 
			mBytesWriter.writeVLong(firstTerm.mProxStart);
		
		long lastFreqStart = firstTerm.mFreqStart;
		long lastProxStart = firstTerm.mProxStart;
		
		for (int idx=limit-count+1; idx<limit; idx++) {
			final PendingTerm term = mPendingTerms.get(idx);
			
			// The rest of the terms term are delta coded:
			mBytesWriter.writeVLong(term.mFreqStart - lastFreqStart);
			lastFreqStart = term.mFreqStart;
			
			if (term.mSkipOffset != -1) {
				assert term.mSkipOffset > 0;
				mBytesWriter.writeVInt(term.mSkipOffset);
			}
			
			if (mIndexOptions.compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0) {
				mBytesWriter.writeVLong(term.mProxStart - lastProxStart);
				lastProxStart = term.mProxStart;
			}
		}

		mTermsOut.writeVInt((int) mBytesWriter.getFilePointer());
		mBytesWriter.writeTo(mTermsOut);
		mBytesWriter.reset();

		// Remove the terms we just wrote:
		mPendingTerms.subList(limit-count, limit).clear();
	}

	@Override
	public void close() throws IOException {
		try {
			mFreqOutput.close();
		} finally {
			if (mProxOutput != null) 
				mProxOutput.close();
		}
	}
	
}
