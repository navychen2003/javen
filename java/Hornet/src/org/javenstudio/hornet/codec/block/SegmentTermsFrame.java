package org.javenstudio.hornet.codec.block;

import java.io.IOException;

import org.javenstudio.common.indexdb.IndexOptions;
import org.javenstudio.common.indexdb.ITermsEnum.SeekStatus;
import org.javenstudio.common.indexdb.store.ByteArrayDataInput;
import org.javenstudio.common.indexdb.util.ArrayUtil;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.util.Logger;
import org.javenstudio.hornet.store.fst.FSTArc;

// Not static -- references term, postingsReader,
// fieldInfo, in
final class SegmentTermsFrame {
	private static final Logger LOG = Logger.getLogger(SegmentTermsFrame.class);

	private final SegmentTermsEnum mEnum;
	
	private byte[] mSuffixBytes = new byte[128];
	private final ByteArrayDataInput mSuffixesReader = new ByteArrayDataInput();

	private byte[] mStatBytes = new byte[64];
	private final ByteArrayDataInput mStatsReader = new ByteArrayDataInput();

	private byte[] mFloorData = new byte[32];
	private final ByteArrayDataInput mFloorDataReader = new ByteArrayDataInput();
	
    // Our index in stack[]:
	private final int mOrd;

	private boolean mHasTerms;
	private boolean mHasTermsOrig;
	private boolean mIsFloor;
	
	private FSTArc<BytesRef> mArc;

    // File pointer where this block was loaded from
	private long mFp;
	private long mFpOrig;
	private long mFpEnd;
    
    // Length of prefix shared by all terms in this block
	private int mPrefix;

    // Number of entries (term or sub-block) in this block
	private int mEntCount;

    // Which term we will next read, or -1 if the block
    // isn't loaded yet
	private int mNextEnt;

    // True if this block is either not a floor block,
    // or, it's the last sub-block of a floor block
	private boolean mIsLastInFloor;

    // True if all entries are terms
	private boolean mIsLeafBlock;

	private long mLastSubFP;

	private int mNextFloorLabel;
	private int mNumFollowFloorBlocks;

    // Next term to decode metaData; we decode metaData
    // lazily so that scanning to find the matching term is
    // fast and only if you find a match and app wants the
    // stats or docs/positions enums, will we decode the
    // metaData
	private int mMetaDataUpto;

	private final BlockTermState mState;
    
	private int mStartBytePos;
	private int mSuffix;
	private long mSubCode;

	void setArc(FSTArc<BytesRef> arc) { mArc = arc; }
	
	final ByteArrayDataInput getSuffixesReader() { return mSuffixesReader; }
	final ByteArrayDataInput getStatsReader() { return mStatsReader; }
	final ByteArrayDataInput getFloorDataReader() { return mFloorDataReader; }
	
	final BlockTermState getState() { return mState; }
	
	final boolean isLastInFloor() { return mIsLastInFloor; }
	final boolean isLeafBlock() { return mIsLeafBlock; }
	
	final int getNextEntry() { return mNextEnt; }
	final int getEntryCount() { return mEntCount; }
	
	final void setNextEntry(int val) { mNextEnt = val; }
	final void setEntryCount(int count) { mEntCount = count; }
	
	final int getOrd() { return mOrd; }
	final boolean hasTerms() { return mHasTerms; }
	final boolean hasTermsOrig() { return mHasTermsOrig; }
	final boolean isFloor() { return mIsFloor; }
	
	final void setHasTerms(boolean val) { mHasTerms = val; }
	final void setHasTermsOrig(boolean val) { mHasTermsOrig = val; }
	final void setIsFloor(boolean val) { mIsFloor = val; }
	
	final long getFilePointer() { return mFp; }
	final long getFilePointerOrig() { return mFpOrig; }
	final long getFilePointerEnd() { return mFpEnd; }
	final long getLastSubPointer() { return mLastSubFP; }
	
	final void setFilePointer(long fp) { mFp = fp; }
	final void setFilePointerOrig(long fp) { mFpOrig = fp; }
	final void setFilePointerEnd(long fp) { mFpEnd = fp; }
	final void setLastSubPointer(long fp) { mLastSubFP = fp; }
	
	final int getMetaDataUpto() { return mMetaDataUpto; }
	final void setMetaDataUpto(int upto) { mMetaDataUpto = upto; }
	
	final int getPrefix() { return mPrefix; }
	final int getSuffix() { return mSuffix; }
	
	final void setPrefix(int val) { mPrefix = val; }
	final void setSuffix(int val) { mSuffix = val; }
	
    public SegmentTermsFrame(SegmentTermsEnum e, int ord) throws IOException {
    	mEnum = e;
    	mOrd = ord;
    	mState = (BlockTermState)mEnum.getReader().getPostingsReader().newTermState();
    	mState.setTotalTermFreq(-1);
    }

    public void setFloorData(ByteArrayDataInput in, BytesRef source) throws IOException {
    	final int numBytes = source.mLength - (in.getPosition() - source.mOffset);
    	if (numBytes > mFloorData.length) 
    		mFloorData = new byte[ArrayUtil.oversize(numBytes, 1)];
    	
    	System.arraycopy(source.mBytes, source.mOffset+in.getPosition(), mFloorData, 0, numBytes);
    	mFloorDataReader.reset(mFloorData, 0, numBytes);
    	mNumFollowFloorBlocks = mFloorDataReader.readVInt();
    	mNextFloorLabel = mFloorDataReader.readByte() & 0xff;
    }

    public int getTermBlockOrd() {
    	return mIsLeafBlock ? mNextEnt : mState.getTermBlockOrd();
    }

    public void loadNextFloorBlock() throws IOException {
    	assert mArc == null || mIsFloor: "arc=" + mArc + " isFloor=" + mIsFloor;
    	
    	mFp = mFpEnd;
    	mNextEnt = -1;
    	
    	loadBlock();
    }

    /** 
     * Does initial decode of next block of terms; this
     * doesn't actually decode the docFreq, totalTermFreq,
     * postings details (frq/prx offset, etc.) metadata;
     * it just loads them as byte[] blobs which are then      
     * decoded on-demand if the metadata is ever requested
     * for any term in this block.  This enables terms-only
     * intensive consumes (eg certain MTQs, respelling) to
     * not pay the price of decoding metadata they won't
     * use. 
     */
    public void loadBlock() throws IOException {
    	//if (LOG.isDebugEnabled())
    	//	LOG.debug("loadBlock: " + this);
    	
    	// Clone the IndexInput lazily, so that consumers
    	// that just pull a TermsEnum to
    	// seekExact(TermState) don't pay this cost:
    	mEnum.initIndexInput();

    	if (mNextEnt != -1) {
    		// Already loaded
    		return;
    	}

    	mEnum.getReader().getInput().seek(mFp);
    	int code = mEnum.getReader().getInput().readVInt();
    	mEntCount = code >>> 1;
    	assert mEntCount > 0;
    	mIsLastInFloor = (code & 1) != 0;
    	assert mArc == null || (mIsLastInFloor || mIsFloor);

    	// TODO: if suffixes were stored in random-access
    	// array structure, then we could do binary search
    	// instead of linear scan to find target term; eg
    	// we could have simple array of offsets

    	// term suffixes:
    	code = mEnum.getReader().getInput().readVInt();
    	mIsLeafBlock = (code & 1) != 0;
    	int numBytes = code >>> 1;
    	if (numBytes <= 0) {
    		throw new IOException("Term suffixes is empty: numBytes=" + numBytes + " entCount=" 
    				+ mEntCount + " input=" + mEnum.getReader().getInput());
    	}
    	
    	if (mSuffixBytes.length < numBytes) 
    		mSuffixBytes = new byte[ArrayUtil.oversize(numBytes, 1)];
    	
    	mEnum.getReader().getInput().readBytes(mSuffixBytes, 0, numBytes);
    	mSuffixesReader.reset(mSuffixBytes, 0, numBytes);

    	// stats
    	numBytes = mEnum.getReader().getInput().readVInt();
    	if (mStatBytes.length < numBytes) 
    		mStatBytes = new byte[ArrayUtil.oversize(numBytes, 1)];
    	
    	mEnum.getReader().getInput().readBytes(mStatBytes, 0, numBytes);
    	mStatsReader.reset(mStatBytes, 0, numBytes);
    	mMetaDataUpto = 0;

    	mState.setTermBlockOrd(0);
    	mNextEnt = 0;
    	mLastSubFP = -1;

    	// TODO: we could skip this if !hasTerms; but
    	// that's rare so won't help much
    	mEnum.getReader().getPostingsReader().readTermsBlock(
    			mEnum.getReader().getInput(), mEnum.getFieldReader().getFieldInfo(), mState);

    	// Sub-blocks of a single floor block are always
    	// written one after another -- tail recurse:
    	mFpEnd = mEnum.getReader().getInput().getFilePointer();
    }

    public void rewind() throws IOException {
    	// Force reload:
    	mFp = mFpOrig;
    	mNextEnt = -1;
    	mHasTerms = mHasTermsOrig;
    	
    	if (mIsFloor) {
    		mFloorDataReader.rewind();
    		mNumFollowFloorBlocks = mFloorDataReader.readVInt();
    		mNextFloorLabel = mFloorDataReader.readByte() & 0xff;
    	}

    	/*
      	// Keeps the block loaded, but rewinds its state:
      	if (nextEnt > 0 || fp != fpOrig) {
        	if (fp != fpOrig) {
          		fp = fpOrig;
          		nextEnt = -1;
        	} else {
          		nextEnt = 0;
        	}
        	hasTerms = hasTermsOrig;
        	if (isFloor) {
          	floorDataReader.rewind();
          	numFollowFloorBlocks = floorDataReader.readVInt();
          	nextFloorLabel = floorDataReader.readByte() & 0xff;
        	}
        	assert suffixBytes != null;
        	suffixesReader.rewind();
        	assert statBytes != null;
        	statsReader.rewind();
        	metaDataUpto = 0;
        	state.termBlockOrd = 0;
        	// TODO: skip this if !hasTerms?  Then postings
        	// impl wouldn't have to write useless 0 byte
        	postingsReader.resetTermsBlock(fieldInfo, state);
        	lastSubFP = -1;
      	}
    	 */
    }

    // return false if term exists
    public boolean next() throws IOException {
    	try {
    		boolean result = mIsLeafBlock ? nextLeaf() : nextNonLeaf();
    		
    		//if (LOG.isDebugEnabled()) { 
    		//	LOG.debug("next: " + this + " result=" + result);
    		//	printInfo();
    		//}
    		
    		return result;
    	} catch (RuntimeException e) { 
    		if (LOG.isWarnEnabled()) { 
    			LOG.warn("next: " + this + " error: " + e);
    			printInfo();
    		}
    		
    		throw e;
    	}
    }

    final boolean isLoadedBlock() { 
    	return mEnum.getInput() != null;
    }
    
    final void printInfo() { 
		if (LOG.isDebugEnabled()) { 
			LOG.debug("printInfo: " + this 
					+ " reader=" + mEnum.getReader() 
					+ " input=" + mEnum.getInput() 
					+ " hasTerms=" + hasTerms() 
					+ " hasTermsOrig=" + hasTermsOrig() 
					+ " isLastInFloor=" + isLastInFloor() 
					+ " isFloor=" + isFloor() 
					+ " prefix=" + getPrefix() 
					+ " suffix=" + getSuffix() 
					+ " isLeafBlock=" + mIsLeafBlock 
					+ " numFollowFloorBlocks=" + mNumFollowFloorBlocks 
					+ " lastSubPointer=" + getLastSubPointer() 
					+ " filePointerOrig=" + getFilePointerOrig() 
					+ " filePointer=" + getFilePointer() 
					+ " nextEntry=" + getNextEntry() 
					+ " entryCount=" + getEntryCount());
		}
    }
    
    // Decodes next entry; returns true if it's a sub-block
    private boolean nextLeaf() throws IOException {
    	assert mNextEnt != -1 && mNextEnt < mEntCount: "nextEnt=" + mNextEnt + 
    			" entCount=" + mEntCount + " fp=" + mFp;
    	
    	mNextEnt ++;
    	mSuffix = mSuffixesReader.readVInt();
    	mStartBytePos = mSuffixesReader.getPosition();
    	
    	mEnum.getTerm().mLength = mPrefix + mSuffix;
    	if (mEnum.getTerm().mBytes.length < mEnum.getTerm().mLength) 
    		mEnum.getTerm().grow(mEnum.getTerm().mLength);
      
    	mSuffixesReader.readBytes(mEnum.getTerm().mBytes, mPrefix, mSuffix);
    	// A normal term
    	mEnum.setTermExists(true);
    	
    	return false;
    }

    private boolean nextNonLeaf() throws IOException {
    	assert mNextEnt != -1 && mNextEnt < mEntCount: "nextEnt=" + mNextEnt + 
    			" entCount=" + mEntCount + " fp=" + mFp;
    	
    	mNextEnt ++;
    	final int code = mSuffixesReader.readVInt();
    	mSuffix = code >>> 1;
    	mStartBytePos = mSuffixesReader.getPosition();
    	
    	mEnum.getTerm().mLength = mPrefix + mSuffix;
    	if (mEnum.getTerm().mBytes.length < mEnum.getTerm().mLength) 
    		mEnum.getTerm().grow(mEnum.getTerm().mLength);
    	
    	mSuffixesReader.readBytes(mEnum.getTerm().mBytes, mPrefix, mSuffix);
    	if ((code & 1) == 0) {
    		// A normal term
    		mEnum.setTermExists(true);
    		mSubCode = 0;
    		mState.increaseTermBlockOrd(1);
    		return false;
    		
    	} else {
    		// A sub-block; make sub-FP absolute:
    		mEnum.setTermExists(false);
    		mSubCode = mSuffixesReader.readVLong();
    		mLastSubFP = mFp - mSubCode;
    		return true;
    	}
    }
    
    // TODO: make this array'd so we can do bin search?
    // likely not worth it?  need to measure how many
    // floor blocks we "typically" get
    public void scanToFloorFrame(BytesRef target) throws IOException {
    	if (!mIsFloor || target.mLength <= mPrefix) 
    		return;

    	final int targetLabel = target.mBytes[target.mOffset + mPrefix] & 0xFF;
    	if (targetLabel < mNextFloorLabel) 
    		return;

    	assert mNumFollowFloorBlocks != 0;

    	long newFP = mFpOrig;
    	while (true) {
    		final long code = mFloorDataReader.readVLong();
    		newFP = mFpOrig + (code >>> 1);
    		mHasTerms = (code & 1) != 0;
        
    		mIsLastInFloor = mNumFollowFloorBlocks == 1;
    		mNumFollowFloorBlocks --;

    		if (mIsLastInFloor) {
    			mNextFloorLabel = 256;
    			break;
    		} else {
    			mNextFloorLabel = mFloorDataReader.readByte() & 0xff;
    			if (targetLabel < mNextFloorLabel) 
    				break;
    		}
    	}

    	if (newFP != mFp) {
    		// Force re-load of the block:
    		mNextEnt = -1;
    		mFp = newFP;
    	}
    }

    public void decodeMetaData() throws IOException {
    	// lazily catch up on metadata decode:
    	final int limit = getTermBlockOrd();
    	assert limit > 0;

    	// We must set/incr state.termCount because
    	// postings impl can look at this
    	mState.setTermBlockOrd(mMetaDataUpto);
  
    	// TODO: better API would be "jump straight to term=N"???
    	while (mMetaDataUpto < limit) {
    		// TODO: we could make "tiers" of metadata, ie,
    		// decode docFreq/totalTF but don't decode postings
    		// metadata; this way caller could get
    		// docFreq/totalTF w/o paying decode cost for
    		// postings

    		// TODO: if docFreq were bulk decoded we could
    		// just skipN here:
    		mState.setDocFreq(mStatsReader.readVInt());
    		if (mEnum.getFieldReader().getFieldInfo().getIndexOptions() != IndexOptions.DOCS_ONLY) 
    			mState.setTotalTermFreq(mState.getDocFreq() + mStatsReader.readVLong());

    		mEnum.getReader().getPostingsReader().nextTerm(mEnum.getFieldReader().getFieldInfo(), mState);
    		mMetaDataUpto ++;
    		mState.increaseTermBlockOrd(1);
    	}
    }

    // Used only by assert
    private boolean prefixMatches(BytesRef target) {
    	for (int bytePos=0; bytePos < mPrefix; bytePos++) {
    		if (target.mBytes[target.mOffset + bytePos] != mEnum.getTerm().mBytes[bytePos]) 
    			return false;
    	}

    	return true;
    }

    // Scans to sub-block that has this target fp; only
    // called by next(); NOTE: does not set
    // startBytePos/suffix as a side effect
    public void scanToSubBlock(long subFP) throws IOException {
    	assert !mIsLeafBlock;
    	//assert nextEnt == 0;
    	if (mLastSubFP == subFP) 
    		return;
      
    	assert subFP < mFp : "fp=" + mFp + " subFP=" + subFP;
    	final long targetSubCode = mFp - subFP;
      
    	while (true) {
    		assert mNextEnt < mEntCount;
    		mNextEnt ++;
    		
    		final int code = mSuffixesReader.readVInt();
    		mSuffixesReader.skipBytes(mIsLeafBlock ? code : code >>> 1);
    		
    		if ((code & 1) != 0) {
    			final long subCode = mSuffixesReader.readVLong();
    			if (targetSubCode == subCode) {
    				mLastSubFP = subFP;
    				return;
    			}
    		} else {
    			mState.increaseTermBlockOrd(1);
    		}
    	}
    }

    // NOTE: sets startBytePos/suffix as a side effect
    public SeekStatus scanToTerm(BytesRef target, boolean exactOnly) throws IOException {
    	return mIsLeafBlock ? scanToTermLeaf(target, exactOnly) : scanToTermNonLeaf(target, exactOnly);
    }

    // Target's prefix matches this block's prefix; we
    // scan the entries check if the suffix matches.
    public SeekStatus scanToTermLeaf(BytesRef target, boolean exactOnly) throws IOException {
    	assert mNextEnt != -1;
    	mEnum.setTermExists(true);
    	mSubCode = 0;

    	if (mNextEnt == mEntCount) {
    		if (exactOnly) fillTerm();
    		return SeekStatus.END;
    	}

    	assert prefixMatches(target);

    	// Loop over each entry (term or sub-block) in this block:
    	//nextTerm: while(nextEnt < entCount) {
    	nextTerm: while (true) {
    		mNextEnt ++;
    		mSuffix = mSuffixesReader.readVInt();

    		final int termLen = mPrefix + mSuffix;
    		mStartBytePos = mSuffixesReader.getPosition();
    		mSuffixesReader.skipBytes(mSuffix);

    		final int targetLimit = target.mOffset + (target.mLength < termLen ? target.mLength : termLen);
    		int targetPos = target.mOffset + mPrefix;

    		// Loop over bytes in the suffix, comparing to
    		// the target
    		int bytePos = mStartBytePos;
    		while (true) {
    			final int cmp;
    			final boolean stop;
    			
    			if (targetPos < targetLimit) {
    				cmp = (mSuffixBytes[bytePos++]&0xFF) - (target.mBytes[targetPos++]&0xFF);
    				stop = false;
    			} else {
    				assert targetPos == targetLimit;
    				cmp = termLen - target.mLength;
    				stop = true;
    			}

    			if (cmp < 0) {
    				// Current entry is still before the target;
    				// keep scanning
    				if (mNextEnt == mEntCount) {
    					if (exactOnly) 
    						fillTerm();
    					// We are done scanning this block
    					break nextTerm;
    					
    				} else 
    					continue nextTerm;
    				
    			} else if (cmp > 0) {
    				// Done!  Current entry is after target --
    				// return NOT_FOUND:
    				fillTerm();

    				if (!exactOnly && !mEnum.isTermExists()) {
    					// We are on a sub-block, and caller wants
    					// us to position to the next term after
    					// the target, so we must recurse into the
    					// sub-frame(s):
    					mEnum.setCurrentFrame(mEnum.pushFrame(null, 
    							mEnum.getCurrentFrame().mLastSubFP, termLen));
    					mEnum.getCurrentFrame().loadBlock();
    					
    					while (mEnum.getCurrentFrame().next()) {
    						mEnum.setCurrentFrame(mEnum.pushFrame(null, 
    								mEnum.getCurrentFrame().mLastSubFP, mEnum.getTerm().mLength));
    						mEnum.getCurrentFrame().loadBlock();
    					}
    				}
            
    				return SeekStatus.NOT_FOUND;
    				
    			} else if (stop) {
    				// Exact match!
    				// This cannot be a sub-block because we
    				// would have followed the index to this
    				// sub-block from the start:

    				assert mEnum.isTermExists();
    				fillTerm();
    				return SeekStatus.FOUND;
    			}
    		}
    	}

    	// It is possible (and OK) that terms index pointed us
    	// at this block, but, we scanned the entire block and
    	// did not find the term to position to.  This happens
    	// when the target is after the last term in the block
    	// (but, before the next term in the index).  EG
    	// target could be foozzz, and terms index pointed us
    	// to the foo* block, but the last term in this block
    	// was fooz (and, eg, first term in the next block will
    	// bee fop).
    	if (exactOnly) 
    		fillTerm();

    	// TODO: not consistent that in the
    	// not-exact case we don't next() into the next
    	// frame here
    	return SeekStatus.END;
    }

    // Target's prefix matches this block's prefix; we
    // scan the entries check if the suffix matches.
    public SeekStatus scanToTermNonLeaf(BytesRef target, boolean exactOnly) throws IOException {
    	assert mNextEnt != -1;
    	if (mNextEnt == mEntCount) {
    		if (exactOnly) {
    			fillTerm();
    			mEnum.setTermExists(mSubCode == 0);
    		}
    		return SeekStatus.END;
    	}

    	assert prefixMatches(target);

    	// Loop over each entry (term or sub-block) in this block:
    	//nextTerm: while(nextEnt < entCount) {
    	nextTerm: while (true) {
    		mNextEnt ++;

    		final int code = mSuffixesReader.readVInt();
    		mSuffix = code >>> 1;

    		mEnum.setTermExists((code & 1) == 0);
    		final int termLen = mPrefix + mSuffix;
    		
    		mStartBytePos = mSuffixesReader.getPosition();
    		mSuffixesReader.skipBytes(mSuffix);
    		
    		if (mEnum.isTermExists()) {
    			mState.increaseTermBlockOrd(1);
    			mSubCode = 0;
    		} else {
    			mSubCode = mSuffixesReader.readVLong();
    			mLastSubFP = mFp - mSubCode;
    		}

    		final int targetLimit = target.mOffset + (target.mLength < termLen ? target.mLength : termLen);
    		int targetPos = target.mOffset + mPrefix;

    		// Loop over bytes in the suffix, comparing to
    		// the target
    		int bytePos = mStartBytePos;
    		
    		while (true) {
    			final int cmp;
    			final boolean stop;
    			if (targetPos < targetLimit) {
    				cmp = (mSuffixBytes[bytePos++]&0xFF) - (target.mBytes[targetPos++]&0xFF);
    				stop = false;
    			} else {
    				assert targetPos == targetLimit;
    				cmp = termLen - target.mLength;
    				stop = true;
    			}

    			if (cmp < 0) {
    				// Current entry is still before the target;
    				// keep scanning
    				if (mNextEnt == mEntCount) {
    					if (exactOnly) {
    						fillTerm();
    						//termExists = true;
    					}
    					// We are done scanning this block
    					break nextTerm;
    					
    				} else 
    					continue nextTerm;
    				
    			} else if (cmp > 0) {
    				// Done!  Current entry is after target --
    				// return NOT_FOUND:
    				fillTerm();

    				if (!exactOnly && !mEnum.isTermExists()) {
    					// We are on a sub-block, and caller wants
    					// us to position to the next term after
    					// the target, so we must recurse into the
    					// sub-frame(s):
    					mEnum.setCurrentFrame(mEnum.pushFrame(null, 
    							mEnum.getCurrentFrame().mLastSubFP, termLen));
    					mEnum.getCurrentFrame().loadBlock();
    					
    					while (mEnum.getCurrentFrame().next()) {
    						mEnum.setCurrentFrame(mEnum.pushFrame(null, 
    								mEnum.getCurrentFrame().mLastSubFP, mEnum.getTerm().mLength));
    						mEnum.getCurrentFrame().loadBlock();
    					}
    				}
            
    				return SeekStatus.NOT_FOUND;
    				
    			} else if (stop) {
    				// Exact match!
    				// This cannot be a sub-block because we
    				// would have followed the index to this
    				// sub-block from the start:
    				assert mEnum.isTermExists();
    				fillTerm();
    				
    				return SeekStatus.FOUND;
    			}
    		}
    	}

    	// It is possible (and OK) that terms index pointed us
    	// at this block, but, we scanned the entire block and
    	// did not find the term to position to.  This happens
    	// when the target is after the last term in the block
    	// (but, before the next term in the index).  EG
    	// target could be foozzz, and terms index pointed us
    	// to the foo* block, but the last term in this block
    	// was fooz (and, eg, first term in the next block will
    	// bee fop).
    	if (exactOnly) 
    		fillTerm();

    	// TODO: not consistent that in the
    	// not-exact case we don't next() into the next
    	// frame here
    	return SeekStatus.END;
    }

    private void fillTerm() {
    	final int termLength = mPrefix + mSuffix;
    	mEnum.getTerm().mLength = mPrefix + mSuffix;
    	if (mEnum.getTerm().mBytes.length < termLength) 
    		mEnum.getTerm().grow(termLength);
    	
    	System.arraycopy(mSuffixBytes, mStartBytePos, mEnum.getTerm().mBytes, mPrefix, mSuffix);
    }
    
    @Override
    public String toString() { 
    	return getClass().getSimpleName() + "{hashCode=" + hashCode() 
    			+ ",ord=" + mOrd + ",reader=" + mEnum.getReader() + "}";
    }
    
}
