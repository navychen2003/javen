package org.javenstudio.hornet.codec.block;

import java.io.IOException;

import org.javenstudio.common.indexdb.IndexOptions;
import org.javenstudio.common.indexdb.store.ByteArrayDataInput;
import org.javenstudio.common.indexdb.util.ArrayUtil;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.store.fst.FSTArc;

//TODO: can we share this with the frame in STE?
final class IntersectFrame {

	private final IntersectEnum mEnum;
	
	protected byte[] mSuffixBytes = new byte[128];
	private final ByteArrayDataInput mSuffixesReader = new ByteArrayDataInput();

	private byte[] mStatBytes = new byte[64];
	private final ByteArrayDataInput mStatsReader = new ByteArrayDataInput();

	private byte[] mFloorData = new byte[32];
	private final ByteArrayDataInput mFloorDataReader = new ByteArrayDataInput();

	private final int mOrd;
	protected long mFp;
	protected long mFpOrig;
	private long mFpEnd;
	protected long mLastSubFP;
	
    // State in automaton
	private int mState;
	private int mMetaDataUpto;
    
    // Length of prefix shared by all terms in this block
	protected int mPrefix;

    // Number of entries (term or sub-block) in this block
	private int mEntCount;

    // Which term we will next read
	protected int mNextEnt;

    // True if this block is either not a floor block,
    // or, it's the last sub-block of a floor block
	protected boolean mIsLastInFloor;

    // True if all entries are terms
	private boolean mIsLeafBlock;

	private int mNumFollowFloorBlocks;
	private int mNextFloorLabel;
    
    // Transition[] transitions;
	protected int mCurTransitionMax;
	protected int mTransitionIndex;

	protected FSTArc<BytesRef> mArc;

	private final BlockTermState mTermState;

    // Cumulative output so far
	protected BytesRef mOutputPrefix;

	protected int mStartBytePos;
	protected int mSuffix;

	final void setNextEntry(int val) { mNextEnt = val; }
	final void setEntryCount(int val) { mEntCount = val; }
	
	final ByteArrayDataInput getSuffixesReader() { return mSuffixesReader; }
	final ByteArrayDataInput getStatsReader() { return mStatsReader; }
	final ByteArrayDataInput getFloorDataReader() { return mFloorDataReader; }
	final byte[] getSuffixBytes() { return mSuffixBytes; }
	final byte[] getFloorData() { return mFloorData; }
	final int getOrd() { return mOrd; }
	final long getFilePointerEnd() { return mFpEnd; }
	final long getLastSubFilePointer() { return mLastSubFP; }
	final boolean isLastInFloor() { return mIsLastInFloor; }
	final int getNextEntry() { return mNextEnt; }
	final int getEntryCount() { return mEntCount; }
	final int getState() { return mState; }
	final int getPrefix() { return mPrefix; }
	final int getSuffix() { return mSuffix; }
	final int getCurTransitionMax() { return mCurTransitionMax; }
	final int getTransitionIndex() { return mTransitionIndex; }
	final FSTArc<BytesRef> getArc() { return mArc; }
	final BytesRef getOutputPrefix() { return mOutputPrefix; }
	final int getStartBytePos() { return mStartBytePos; }
	final BlockTermState getTermState() { return mTermState; }
	
    public IntersectFrame(IntersectEnum e, int ord) throws IOException {
    	mEnum = e;
    	mOrd = ord;
    	mTermState = (BlockTermState)mEnum.getReader().getPostingsReader().newTermState();
    	mTermState.setTotalTermFreq(-1);
    }

    public void loadNextFloorBlock() throws IOException {
    	assert mNumFollowFloorBlocks > 0;
    	
    	do {
    		mFp = mFpOrig + (mFloorDataReader.readVLong() >>> 1);
    		mNumFollowFloorBlocks --;
    		if (mNumFollowFloorBlocks != 0) 
    			mNextFloorLabel = mFloorDataReader.readByte() & 0xff;
    		else 
    			mNextFloorLabel = 256;
    	} while (mNumFollowFloorBlocks != 0 && mNextFloorLabel <= 0); //transitions[transitionIndex].getMin());

    	load(null);
    }

    public void setState(int state) {
    	mState = state;
    	mTransitionIndex = 0;
    	//transitions = compiledAutomaton.sortedTransitions[state];
    	//if (transitions.length != 0) {
    	//  curTransitionMax = transitions[0].getMax();
    	//} else {
        mCurTransitionMax = -1;
        //}
    }

    public void load(BytesRef frameIndexData) throws IOException {
/*
      if (frameIndexData != null && transitions.length != 0) {
        // Floor frame
        if (floorData.length < frameIndexData.mLength) {
          this.floorData = new byte[ArrayUtil.oversize(frameIndexData.mLength, 1)];
        }
        System.arraycopy(frameIndexData.mBytes, frameIndexData.mOffset, floorData, 0, frameIndexData.mLength);
        floorDataReader.reset(floorData, 0, frameIndexData.mLength);
        // Skip first long -- has redundant fp, hasTerms
        // flag, isFloor flag
        final long code = floorDataReader.readVLong();
        if ((code & BlockTreeTermsWriter.OUTPUT_FLAG_IS_FLOOR) != 0) {
          numFollowFloorBlocks = floorDataReader.readVInt();
          nextFloorLabel = floorDataReader.readByte() & 0xff;
          
          // If current state is accept, we must process
          // first block in case it has empty suffix:
          if (!runAutomaton.isAccept(state)) {
            // Maybe skip floor blocks:
            while (numFollowFloorBlocks != 0 && nextFloorLabel <= transitions[0].getMin()) {
              fp = fpOrig + (floorDataReader.readVLong() >>> 1);
              numFollowFloorBlocks--;
              if (numFollowFloorBlocks != 0) {
                nextFloorLabel = floorDataReader.readByte() & 0xff;
              } else {
                nextFloorLabel = 256;
              }
            }
          }
        }
      }
*/
    	
    	mEnum.getInput().seek(mFp);
    	int code = mEnum.getInput().readVInt();
    	mEntCount = code >>> 1;
    	assert mEntCount > 0;
    	mIsLastInFloor = (code & 1) != 0;

    	// term suffixes:
    	code = mEnum.getInput().readVInt();
    	mIsLeafBlock = (code & 1) != 0;
    	int numBytes = code >>> 1;
    	if (mSuffixBytes.length < numBytes) 
    		mSuffixBytes = new byte[ArrayUtil.oversize(numBytes, 1)];
    	
    	mEnum.getInput().readBytes(mSuffixBytes, 0, numBytes);
    	mSuffixesReader.reset(mSuffixBytes, 0, numBytes);

    	// stats
    	numBytes = mEnum.getInput().readVInt();
    	if (mStatBytes.length < numBytes) 
    		mStatBytes = new byte[ArrayUtil.oversize(numBytes, 1)];
    	
    	mEnum.getInput().readBytes(mStatBytes, 0, numBytes);
    	mStatsReader.reset(mStatBytes, 0, numBytes);
    	mMetaDataUpto = 0;

    	mTermState.setTermBlockOrd(0);
    	mNextEnt = 0;
      
    	mEnum.getReader().getPostingsReader().readTermsBlock(
    			mEnum.getInput(), mEnum.getFieldReader().getFieldInfo(), mTermState);
    	if (!mIsLastInFloor) {
    		// Sub-blocks of a single floor block are always
    		// written one after another -- tail recurse:
    		mFpEnd = mEnum.getInput().getFilePointer();
    	}
    }

    // TODO: maybe add scanToLabel; should give perf boost
    public boolean next() throws IOException {
    	return mIsLeafBlock ? nextLeaf() : nextNonLeaf();
    }

    // Decodes next entry; returns true if it's a sub-block
    public boolean nextLeaf() throws IOException {
    	assert mNextEnt != -1 && mNextEnt < mEntCount: "nextEnt=" + mNextEnt + " entCount=" + 
    			mEntCount + " fp=" + mFp;
    	
    	mNextEnt ++;
    	mSuffix = mSuffixesReader.readVInt();
    	mStartBytePos = mSuffixesReader.getPosition();
    	mSuffixesReader.skipBytes(mSuffix);
    	
    	return false;
    }

    public boolean nextNonLeaf() throws IOException {
    	assert mNextEnt != -1 && mNextEnt < mEntCount: "nextEnt=" + mNextEnt + " entCount=" + 
    			mEntCount + " fp=" + mFp;
      
    	mNextEnt ++;
    	
    	final int code = mSuffixesReader.readVInt();
    	mSuffix = code >>> 1;
    	mStartBytePos = mSuffixesReader.getPosition();
    	mSuffixesReader.skipBytes(mSuffix);
    	if ((code & 1) == 0) {
    		// A normal term
    		mTermState.increaseTermBlockOrd(1);
    		return false;
    	} else {
    		// A sub-block; make sub-FP absolute:
    		mLastSubFP = mFp - mSuffixesReader.readVLong();
    		return true;
    	}
    }

    public int getTermBlockOrd() {
    	return mIsLeafBlock ? mNextEnt : mTermState.getTermBlockOrd();
    }

    public void decodeMetaData() throws IOException {
    	// lazily catch up on metadata decode:
    	final int limit = getTermBlockOrd();
    	assert limit > 0;

    	// We must set/incr state.termCount because
    	// postings impl can look at this
    	mTermState.setTermBlockOrd(mMetaDataUpto);
  
    	// TODO: better API would be "jump straight to term=N"???
    	while (mMetaDataUpto < limit) {
    		// TODO: we could make "tiers" of metadata, ie,
    		// decode docFreq/totalTF but don't decode postings
    		// metadata; this way caller could get
    		// docFreq/totalTF w/o paying decode cost for
    		// postings

    		// TODO: if docFreq were bulk decoded we could
    		// just skipN here:
    		mTermState.setDocFreq(mStatsReader.readVInt());
    		if (mEnum.getFieldReader().getFieldInfo().getIndexOptions() != IndexOptions.DOCS_ONLY) 
    			mTermState.setTotalTermFreq(mTermState.getDocFreq() + mStatsReader.readVLong());

    		mEnum.getReader().getPostingsReader().nextTerm(mEnum.getFieldReader().getFieldInfo(), mTermState);
    		mMetaDataUpto ++;
    		mTermState.increaseTermBlockOrd(1);
    	}
    }
    
}
