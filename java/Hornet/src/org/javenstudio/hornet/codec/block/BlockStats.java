package org.javenstudio.hornet.codec.block;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.javenstudio.common.indexdb.util.ArrayUtil;
import org.javenstudio.common.indexdb.util.BytesRef;

/**
 * BlockTree statistics for a single field 
 * returned by {@link BlockFieldReader#computeStats()}.
 */
final class BlockStats {
	
	private int[] mBlockCountByPrefixLen = new int[10];
	
	private int mIndexNodeCount;
	private int mIndexArcCount;
	private int mIndexNumBytes;

	private long mTotalTermCount;
	private long mTotalTermBytes;

	private int mNonFloorBlockCount;
	private int mFloorBlockCount;
	private int mFloorSubBlockCount;
	private int mMixedBlockCount;
	private int mTermsOnlyBlockCount;
	private int mSubBlocksOnlyBlockCount;
	private int mTotalBlockCount;

	private int mStartBlockCount;
	private int mEndBlockCount;
	private long mTotalBlockSuffixBytes;
	private long mTotalBlockStatsBytes;

    // Postings impl plus the other few vInts stored in
    // the frame:
	private long mTotalBlockOtherBytes;

	private final String mSegment;
	private final String mField;

    public BlockStats(String segment, String field) {
    	mSegment = segment;
    	mField = field;
    }

    final void setIndexNodeCount(int val) { mIndexNodeCount = val; }
    final void setIndexArcCount(int val) { mIndexArcCount = val; }
    final void setIndexNumBytes(int val) { mIndexNumBytes = val; }
    
    final String getSegmentName() { return mSegment; }
    final String getFieldName() { return mField; }
    
    public void startBlock(SegmentTermsFrame frame, boolean isFloor) {
    	mTotalBlockCount ++;
    	if (isFloor) {
    		if (frame.getFilePointer() == frame.getFilePointerOrig()) 
    			mFloorBlockCount ++;
    		mFloorSubBlockCount ++;
    	} else {
    		mNonFloorBlockCount ++;
    	}

    	if (mBlockCountByPrefixLen.length <= frame.getPrefix()) 
    		mBlockCountByPrefixLen = ArrayUtil.grow(mBlockCountByPrefixLen, 1+frame.getPrefix());
      
		mBlockCountByPrefixLen[frame.getPrefix()] ++;
		mStartBlockCount ++;
		mTotalBlockSuffixBytes += frame.getSuffixesReader().length();
		mTotalBlockStatsBytes += frame.getStatsReader().length();
    }

    public void endBlock(SegmentTermsFrame frame) {
    	final int termCount = frame.isLeafBlock() ? frame.getEntryCount() : frame.getState().getTermBlockOrd();
    	final int subBlockCount = frame.getEntryCount() - termCount;
    	
    	mTotalTermCount += termCount;
    	if (termCount != 0 && subBlockCount != 0) {
    		mMixedBlockCount ++;
    	} else if (termCount != 0) {
    		mTermsOnlyBlockCount ++;
    	} else if (subBlockCount != 0) {
    		mSubBlocksOnlyBlockCount ++;
    	} else {
    		throw new IllegalStateException();
    	}
    	
    	mEndBlockCount ++;
    	
    	final long otherBytes = frame.getFilePointerEnd() - frame.getFilePointer() - 
    			frame.getSuffixesReader().length() - frame.getStatsReader().length();
    	assert otherBytes > 0 : "otherBytes=" + otherBytes + " frame.fp=" + 
    			frame.getFilePointer() + " frame.fpEnd=" + frame.getFilePointerEnd();
    	
    	mTotalBlockOtherBytes += otherBytes;
    }

    public void term(BytesRef term) {
    	mTotalTermBytes += term.mLength;
    }

    public void finish() {
    	assert mStartBlockCount == mEndBlockCount: "startBlockCount=" + mStartBlockCount + 
    			" endBlockCount=" + mEndBlockCount;
    	assert mTotalBlockCount == mFloorSubBlockCount + mNonFloorBlockCount: 
    			"floorSubBlockCount=" + mFloorSubBlockCount + " nonFloorBlockCount=" + 
    			mNonFloorBlockCount + " totalBlockCount=" + mTotalBlockCount;
    	assert mTotalBlockCount == mMixedBlockCount + mTermsOnlyBlockCount + mSubBlocksOnlyBlockCount: 
    			"totalBlockCount=" + mTotalBlockCount + " mixedBlockCount=" + mMixedBlockCount + 
    			" subBlocksOnlyBlockCount=" + mSubBlocksOnlyBlockCount + " termsOnlyBlockCount=" + 
    			mTermsOnlyBlockCount;
    }

    @Override
    public String toString() {
    	final ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
    	final PrintStream out = new PrintStream(bos);
      
    	out.println("  index FST:");
    	out.println("    " + mIndexNodeCount + " nodes");
    	out.println("    " + mIndexArcCount + " arcs");
    	out.println("    " + mIndexNumBytes + " bytes");
    	out.println("  terms:");
    	out.println("    " + mTotalTermCount + " terms");
    	out.println("    " + mTotalTermBytes + " bytes" + (mTotalTermCount != 0 ? " (" + 
    			String.format("%.1f", ((double) mTotalTermBytes)/mTotalTermCount) + " bytes/term)" : ""));
    	out.println("  blocks:");
    	out.println("    " + mTotalBlockCount + " blocks");
    	out.println("    " + mTermsOnlyBlockCount + " terms-only blocks");
    	out.println("    " + mSubBlocksOnlyBlockCount + " sub-block-only blocks");
    	out.println("    " + mMixedBlockCount + " mixed blocks");
    	out.println("    " + mFloorBlockCount + " floor blocks");
    	out.println("    " + (mTotalBlockCount-mFloorSubBlockCount) + " non-floor blocks");
    	out.println("    " + mFloorSubBlockCount + " floor sub-blocks");
    	out.println("    " + mTotalBlockSuffixBytes + " term suffix bytes" + (mTotalBlockCount != 0 ? 
    			" (" + String.format("%.1f", ((double) mTotalBlockSuffixBytes)/mTotalBlockCount) + 
    			" suffix-bytes/block)" : ""));
    	out.println("    " + mTotalBlockStatsBytes + " term stats bytes" + (mTotalBlockCount != 0 ? 
    			" (" + String.format("%.1f", ((double) mTotalBlockStatsBytes)/mTotalBlockCount) + 
    			" stats-bytes/block)" : ""));
    	out.println("    " + mTotalBlockOtherBytes + " other bytes" + (mTotalBlockCount != 0 ? 
    			" (" + String.format("%.1f", ((double) mTotalBlockOtherBytes)/mTotalBlockCount) + 
    			" other-bytes/block)" : ""));
    	
    	if (mTotalBlockCount != 0) {
    		out.println("    by prefix length:");
    		int total = 0;
    		for (int prefix=0; prefix < mBlockCountByPrefixLen.length; prefix++) {
    			final int blockCount = mBlockCountByPrefixLen[prefix];
    			total += blockCount;
    			if (blockCount != 0) 
    				out.println("      " + String.format("%2d", prefix) + ": " + blockCount);
    		}
    		assert mTotalBlockCount == total;
    	}

    	return bos.toString();
    }
    
}
