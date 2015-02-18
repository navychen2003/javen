package org.javenstudio.hornet.codec.postings;

import java.io.IOException;
import java.util.Arrays;

import org.javenstudio.common.indexdb.store.IndexInput;
import org.javenstudio.common.indexdb.store.MultiLevelSkipListReader;

/**
 * Implements the skip list reader for the 4.0 posting list format
 * that stores positions and payloads.
 * 
 * @see StoredPostingsFormat
 */
public class StoredSkipListReader extends MultiLevelSkipListReader {
	
	private boolean mCurrentFieldStoresPayloads;
	private boolean mCurrentFieldStoresOffsets;
	
	private long mFreqPointer[];
	private long mProxPointer[];
	private int mPayloadLength[];
	private int mOffsetLength[];
  
	private long mLastFreqPointer;
	private long mLastProxPointer;
	private int mLastPayloadLength;
	private int mLastOffsetLength;

	public StoredSkipListReader(IndexInput skipStream, int maxSkipLevels, int skipInterval) {
		super(skipStream, maxSkipLevels, skipInterval);
		
		mFreqPointer = new long[maxSkipLevels];
		mProxPointer = new long[maxSkipLevels];
		mPayloadLength = new int[maxSkipLevels];
		mOffsetLength = new int[maxSkipLevels];
	}

	public void init(long skipPointer, long freqBasePointer, long proxBasePointer, 
			int df, boolean storesPayloads, boolean storesOffsets) {
		super.init(skipPointer, df);
		
		mCurrentFieldStoresPayloads = storesPayloads;
		mCurrentFieldStoresOffsets = storesOffsets;
		mLastFreqPointer = freqBasePointer;
		mLastProxPointer = proxBasePointer;

		Arrays.fill(mFreqPointer, freqBasePointer);
		Arrays.fill(mProxPointer, proxBasePointer);
		Arrays.fill(mPayloadLength, 0);
		Arrays.fill(mOffsetLength, 0);
	}

	/** 
	 * Returns the freq pointer of the doc to which the last call of 
	 * {@link MultiLevelSkipListReader#skipTo(int)} has skipped. 
	 */
	public long getFreqPointer() {
		return mLastFreqPointer;
	}

	/** 
	 * Returns the prox pointer of the doc to which the last call of 
	 * {@link MultiLevelSkipListReader#skipTo(int)} has skipped. 
	 */
	public long getProxPointer() {
		return mLastProxPointer;
	}
  
	/** 
	 * Returns the payload length of the payload stored just before 
	 * the doc to which the last call of {@link MultiLevelSkipListReader#skipTo(int)} 
	 * has skipped. 
	 */
	public int getPayloadLength() {
		return mLastPayloadLength;
	}
  
	/** 
	 * Returns the offset length (endOffset-startOffset) of the position stored just before 
	 * the doc to which the last call of {@link MultiLevelSkipListReader#skipTo(int)} 
	 * has skipped. 
	 */
	public int getOffsetLength() {
		return mLastOffsetLength;
	}
  
	@Override
	protected void seekChild(int level) throws IOException {
		super.seekChild(level);
		
		mFreqPointer[level] = mLastFreqPointer;
		mProxPointer[level] = mLastProxPointer;
		mPayloadLength[level] = mLastPayloadLength;
		mOffsetLength[level] = mLastOffsetLength;
	}
  
	@Override
	protected void setLastSkipData(int level) {
		super.setLastSkipData(level);
		
		mLastFreqPointer = mFreqPointer[level];
		mLastProxPointer = mProxPointer[level];
		mLastPayloadLength = mPayloadLength[level];
		mLastOffsetLength = mOffsetLength[level];
	}

	@Override
	protected int readSkipData(int level, IndexInput skipStream) throws IOException {
		int delta;
		if (mCurrentFieldStoresPayloads || mCurrentFieldStoresOffsets) {
			// the current field stores payloads and/or offsets.
			// if the doc delta is odd then we have
			// to read the current payload/offset lengths
			// because it differs from the lengths of the
			// previous payload/offset
			delta = skipStream.readVInt();
			if ((delta & 1) != 0) {
				if (mCurrentFieldStoresPayloads) 
					mPayloadLength[level] = skipStream.readVInt();
				
				if (mCurrentFieldStoresOffsets) 
					mOffsetLength[level] = skipStream.readVInt();
			}
			delta >>>= 1;
		} else {
			delta = skipStream.readVInt();
		}

		mFreqPointer[level] += skipStream.readVInt();
		mProxPointer[level] += skipStream.readVInt();
    
		return delta;
	}
	
}
