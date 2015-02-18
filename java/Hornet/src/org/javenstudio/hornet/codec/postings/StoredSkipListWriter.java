package org.javenstudio.hornet.codec.postings;

import java.io.IOException;
import java.util.Arrays;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IIndexOutput;
import org.javenstudio.common.indexdb.store.MultiLevelSkipListWriter;

/**
 * Implements the skip list writer for the 4.0 posting list format
 * that stores positions and payloads.
 * 
 * @see StoredPostingsFormat
 */
final class StoredSkipListWriter extends MultiLevelSkipListWriter {
	
	private int[] mLastSkipDoc;
	private int[] mLastSkipPayloadLength;
	private int[] mLastSkipOffsetLength;
	private long[] mLastSkipFreqPointer;
	private long[] mLastSkipProxPointer;
  
	private IIndexOutput mFreqOutput;
	private IIndexOutput mProxOutput;

	private int mCurDoc;
	private boolean mCurStorePayloads;
	private boolean mCurStoreOffsets;
	private int mCurPayloadLength;
	private int mCurOffsetLength;
	private long mCurFreqPointer;
	private long mCurProxPointer;

	public StoredSkipListWriter(IIndexContext context, int skipInterval, int numberOfSkipLevels, 
			int docCount, IIndexOutput freqOutput, IIndexOutput proxOutput) {
		super(context, skipInterval, numberOfSkipLevels, docCount);
		
		mFreqOutput = freqOutput;
		mProxOutput = proxOutput;
		mLastSkipDoc = new int[numberOfSkipLevels];
		mLastSkipPayloadLength = new int[numberOfSkipLevels];
		mLastSkipOffsetLength = new int[numberOfSkipLevels];
		mLastSkipFreqPointer = new long[numberOfSkipLevels];
		mLastSkipProxPointer = new long[numberOfSkipLevels];
	}

	/**
	 * Sets the values for the current skip data. 
	 */
	public void setSkipData(int doc, boolean storePayloads, int payloadLength, 
			boolean storeOffsets, int offsetLength) {
		assert storePayloads || payloadLength == -1;
		assert storeOffsets  || offsetLength == -1;
		
		mCurDoc = doc;
		mCurStorePayloads = storePayloads;
		mCurPayloadLength = payloadLength;
		mCurStoreOffsets = storeOffsets;
		mCurOffsetLength = offsetLength;
		mCurFreqPointer = mFreqOutput.getFilePointer();
		
		if (mProxOutput != null)
			mCurProxPointer = mProxOutput.getFilePointer();
	}

	@Override
	public void resetSkip() {
		super.resetSkip();
		
		Arrays.fill(mLastSkipDoc, 0);
		Arrays.fill(mLastSkipPayloadLength, -1); // we don't have to write the first length in the skip list
		Arrays.fill(mLastSkipOffsetLength, -1);  // we don't have to write the first length in the skip list
		Arrays.fill(mLastSkipFreqPointer, mFreqOutput.getFilePointer());
		
		if (mProxOutput != null)
			Arrays.fill(mLastSkipProxPointer, mProxOutput.getFilePointer());
	}
  
	@Override
	protected void writeSkipData(int level, IIndexOutput skipBuffer) throws IOException {
		// To efficiently store payloads/offsets in the posting lists we do not store the length of
		// every payload/offset. Instead we omit the length if the previous lengths were the same
		//
		// However, in order to support skipping, the length at every skip point must be known.
		// So we use the same length encoding that we use for the posting lists for the skip data as well:
		// Case 1: current field does not store payloads/offsets
		//           SkipDatum                 --> DocSkip, FreqSkip, ProxSkip
		//           DocSkip,FreqSkip,ProxSkip --> VInt
		//           DocSkip records the document number before every SkipInterval th  document in TermFreqs. 
		//           Document numbers are represented as differences from the previous value in the sequence.
		// Case 2: current field stores payloads/offsets
		//           SkipDatum                 --> DocSkip, PayloadLength?,OffsetLength?,FreqSkip,ProxSkip
		//           DocSkip,FreqSkip,ProxSkip --> VInt
		//           PayloadLength,OffsetLength--> VInt    
		//         In this case DocSkip/2 is the difference between
		//         the current and the previous value. If DocSkip
		//         is odd, then a PayloadLength encoded as VInt follows,
		//         if DocSkip is even, then it is assumed that the
		//         current payload/offset lengths equals the lengths at the previous
		//         skip point
		int delta = mCurDoc - mLastSkipDoc[level];
    
		if (mCurStorePayloads || mCurStoreOffsets) {
			assert mCurStorePayloads || mCurPayloadLength == mLastSkipPayloadLength[level];
			assert mCurStoreOffsets  || mCurOffsetLength == mLastSkipOffsetLength[level];

			if (mCurPayloadLength == mLastSkipPayloadLength[level] && 
				mCurOffsetLength == mLastSkipOffsetLength[level]) {
				// the current payload/offset lengths equals the lengths at the previous skip point,
				// so we don't store the lengths again
				skipBuffer.writeVInt(delta << 1);
				
			} else {
				// the payload and/or offset length is different from the previous one. We shift the DocSkip, 
				// set the lowest bit and store the current payload and/or offset lengths as VInts.
				skipBuffer.writeVInt(delta << 1 | 1);

				if (mCurStorePayloads) {
					skipBuffer.writeVInt(mCurPayloadLength);
					mLastSkipPayloadLength[level] = mCurPayloadLength;
				}
				
				if (mCurStoreOffsets) {
					skipBuffer.writeVInt(mCurOffsetLength);
					mLastSkipOffsetLength[level] = mCurOffsetLength;
				}
			}
		} else {
			// current field does not store payloads or offsets
			skipBuffer.writeVInt(delta);
		}

		skipBuffer.writeVInt((int) (mCurFreqPointer - mLastSkipFreqPointer[level]));
		skipBuffer.writeVInt((int) (mCurProxPointer - mLastSkipProxPointer[level]));

		mLastSkipDoc[level] = mCurDoc;
    
		mLastSkipFreqPointer[level] = mCurFreqPointer;
		mLastSkipProxPointer[level] = mCurProxPointer;
	}
	
}
