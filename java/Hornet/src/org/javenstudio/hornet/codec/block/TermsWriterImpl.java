package org.javenstudio.hornet.codec.block;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.ITermState;
import org.javenstudio.common.indexdb.IndexOptions;
import org.javenstudio.common.indexdb.index.term.PerTermState;
import org.javenstudio.common.indexdb.store.ram.RAMOutputStream;
import org.javenstudio.common.indexdb.util.ArrayUtil;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.IntsRef;
import org.javenstudio.hornet.codec.PostingsConsumer;
import org.javenstudio.hornet.codec.TermsConsumer;
import org.javenstudio.hornet.store.fst.Builder;
import org.javenstudio.hornet.store.fst.FST;
import org.javenstudio.hornet.store.fst.FSTUtil;
import org.javenstudio.hornet.store.fst.NoOutputs;

final class TermsWriterImpl extends TermsConsumer {

	private final BlockTreeTermsWriter mWriter;
	
	private final RAMOutputStream mBytesWriter;
	private final RAMOutputStream mBytesWriter2;
	private final IFieldInfo mFieldInfo;
    
	private long mNumTerms;
	private long mSumTotalTermFreq;
	private long mSumDocFreq;
	private int mDocCount;
	private long mIndexStartFP;

    // Used only to partition terms into the block tree; we
    // don't pull an FST from this builder:
	private final NoOutputs mNoOutputs;
	private final Builder<Object> mBlockBuilder;

    // PendingTerm or PendingBlock:
	private final List<PendingEntry> mPending = new ArrayList<PendingEntry>();
	
	private final IntsRef mScratchIntsRef = new IntsRef();

    // Index into pending of most recently written block
	private int mLastBlockIndex = -1;

    // Re-used when segmenting a too-large block into floor
    // blocks:
	private int[] mSubBytes = new int[10];
	private int[] mSubTermCounts = new int[10];
	private int[] mSubTermCountSums = new int[10];
	private int[] mSubSubCounts = new int[10];

	final IFieldInfo getFieldInfo() { return mFieldInfo; }
	final Builder<Object> getBlockBuilder() { return mBlockBuilder; }
	final List<PendingEntry> getPendingEntryList() { return mPending; }
	final long getNumTerms() { return mNumTerms; }
	final long getSumTotalTermFreq() { return mSumTotalTermFreq; }
	final long getSumDocFreq() { return mSumDocFreq; }
	final int getDocCount() { return mDocCount; }
	final long getIndexStartPointer() { return mIndexStartFP; }
	
    public TermsWriterImpl(BlockTreeTermsWriter writer, 
    		IIndexContext context, IFieldInfo fieldInfo) {
    	mWriter = writer;
    	mFieldInfo = fieldInfo;
    	
        mBytesWriter = new RAMOutputStream(context);
        mBytesWriter2 = new RAMOutputStream(context);
        mNoOutputs = NoOutputs.getSingleton();

        // This Builder is just used transiently to fragment
        // terms into "good" blocks; we don't save the
        // resulting FST:
        mBlockBuilder = new Builder<Object>(FST.INPUT_TYPE.BYTE1,
        		0, 0, true, true, Integer.MAX_VALUE,
        		mNoOutputs, new FindBlocks(writer, this), false);

        mWriter.getPostingsWriter().setField(fieldInfo);
	}
    
	@Override
	public Comparator<BytesRef> getComparator() {
		return BytesRef.getUTF8SortedAsUnicodeComparator();
	}
	
    // Write the top count entries on the pending stack as
    // one or more blocks.  Returns how many blocks were
    // written.  If the entry count is <= maxItemsPerBlock
    // we just write a single block; else we break into
    // primary (initial) block and then one or more
    // following floor blocks:
    protected void writeBlocks(IntsRef prevTerm, int prefixLength, int count)
    		throws IOException {
    	if (prefixLength == 0 || count <= mWriter.getMaxItemsInBlock()) {
    		// Easy case: not floor block.  Eg, prefix is "foo",
    		// and we found 30 terms/sub-blocks starting w/ that
    		// prefix, and minItemsInBlock <= 30 <=
    		// maxItemsInBlock.
    		final PendingBlock nonFloorBlock = writeBlock(prevTerm, prefixLength, prefixLength, 
    				count, count, 0, false, -1, true);
    		nonFloorBlock.compileIndex(null, mWriter.getScratchBytes());
    		mPending.add(nonFloorBlock);
    	
    	} else {
    		// Floor block case.  Eg, prefix is "foo" but we
    		// have 100 terms/sub-blocks starting w/ that
    		// prefix.  We segment the entries into a primary
    		// block and following floor blocks using the first
    		// label in the suffix to assign to floor blocks.

    		// TODO: we could store min & max suffix start byte
    		// in each block, to make floor blocks authoritative

    		final int savLabel = prevTerm.getIntAt(prevTerm.getOffset() + prefixLength);

    		// Count up how many items fall under
    		// each unique label after the prefix.
        
    		// TODO: this is wasteful since the builder had
    		// already done this (partitioned these sub-terms
    		// according to their leading prefix byte)
        
    		final List<PendingEntry> slice = mPending.subList(
    				mPending.size()-count, mPending.size());
    		
    		int lastSuffixLeadLabel = -1;
    		int termCount = 0;
    		int subCount = 0;
    		int numSubs = 0;

    		for (PendingEntry ent : slice) {
    			// First byte in the suffix of this term
    			final int suffixLeadLabel;
    			if (ent.isTerm()) {
    				PendingTerm term = (PendingTerm) ent;
    				if (term.getTerm().getLength() == prefixLength) {
    					// Suffix is 0, ie prefix 'foo' and term is
    					// 'foo' so the term has empty string suffix
    					// in this block
    					assert lastSuffixLeadLabel == -1;
    					assert numSubs == 0;
    					
    					suffixLeadLabel = -1;
    				} else {
    					suffixLeadLabel = term.getTerm().getByteAt(
    							term.getTerm().getOffset() + prefixLength) & 0xff;
    				}
    			} else {
    				PendingBlock block = (PendingBlock) ent;
    				assert block.getPrefix().getLength() > prefixLength;
    				suffixLeadLabel = block.getPrefix().getByteAt(
    						block.getPrefix().getOffset() + prefixLength) & 0xff;
    			}

    			if (suffixLeadLabel != lastSuffixLeadLabel && (termCount + subCount) != 0) {
    				if (mSubBytes.length == numSubs) {
    					mSubBytes = ArrayUtil.grow(mSubBytes);
    					mSubTermCounts = ArrayUtil.grow(mSubTermCounts);
    					mSubSubCounts = ArrayUtil.grow(mSubSubCounts);
    				}
    			  
    				mSubBytes[numSubs] = lastSuffixLeadLabel;
    				lastSuffixLeadLabel = suffixLeadLabel;
    				mSubTermCounts[numSubs] = termCount;
    				mSubSubCounts[numSubs] = subCount;

    				termCount = subCount = 0;
    				numSubs ++;
    			}

    			if (ent.isTerm()) 
    				termCount ++;
    			else 
    				subCount ++;
    		}

    		if (mSubBytes.length == numSubs) {
    			mSubBytes = ArrayUtil.grow(mSubBytes);
    			mSubTermCounts = ArrayUtil.grow(mSubTermCounts);
    			mSubSubCounts = ArrayUtil.grow(mSubSubCounts);
    		}

    		mSubBytes[numSubs] = lastSuffixLeadLabel;
    		mSubTermCounts[numSubs] = termCount;
    		mSubSubCounts[numSubs] = subCount;
    		numSubs ++;

    		if (mSubTermCountSums.length < numSubs) 
    			mSubTermCountSums = ArrayUtil.grow(mSubTermCountSums, numSubs);

    		// Roll up (backwards) the termCounts; postings impl
    		// needs this to know where to pull the term slice
    		// from its pending terms stack:
    		int sum = 0;
    		for (int idx=numSubs-1; idx >= 0; idx--) {
    			sum += mSubTermCounts[idx];
    			mSubTermCountSums[idx] = sum;
    		}

    		// TODO: make a better segmenter?  It'd have to
    		// absorb the too-small end blocks backwards into
    		// the previous blocks

    		// Naive greedy segmentation; this is not always
    		// best (it can produce a too-small block as the
    		// last block):
    		int pendingCount = 0;
    		int startLabel = mSubBytes[0];
    		int curStart = count;
    		subCount = 0;

    		final List<PendingBlock> floorBlocks = new ArrayList<PendingBlock>();
    		PendingBlock firstBlock = null;

    		for (int sub=0; sub < numSubs; sub++) {
    			pendingCount += mSubTermCounts[sub] + mSubSubCounts[sub];
    			subCount ++;

    			// Greedily make a floor block as soon as we've
    			// crossed the min count
    			if (pendingCount >= mWriter.getMinItemsInBlock()) {
    				final int curPrefixLength;
    				if (startLabel == -1) {
    					curPrefixLength = prefixLength;
    				} else {
    					curPrefixLength = 1+prefixLength;
    					// floor term:
    					prevTerm.mInts[prevTerm.getOffset() + prefixLength] = startLabel;
    				}
    			  
    				final PendingBlock floorBlock = writeBlock(prevTerm, prefixLength, 
    						curPrefixLength, curStart, pendingCount, mSubTermCountSums[1+sub], true, 
    						startLabel, curStart == pendingCount);
    				if (firstBlock == null) 
    					firstBlock = floorBlock;
    				else 
    					floorBlocks.add(floorBlock);
    			  
    				curStart -= pendingCount;
    				pendingCount = 0;

    				assert mWriter.getMinItemsInBlock() == 1 || subCount > 1: "minItemsInBlock=" + 
    						mWriter.getMinItemsInBlock() + " subCount=" + subCount + " sub=" + sub + " of " + 
    						numSubs + " subTermCount=" + mSubTermCountSums[sub] + " subSubCount=" + 
    						mSubSubCounts[sub] + " depth=" + prefixLength;
    			  
    				subCount = 0;
    				startLabel = mSubBytes[sub+1];

    				if (curStart == 0) 
    					break;

    				if (curStart <= mWriter.getMaxItemsInBlock()) {
    					// remainder is small enough to fit into a
    					// block.  NOTE that this may be too small (<
    					// minItemsInBlock); need a true segmenter
    					// here
    					assert startLabel != -1;
    					assert firstBlock != null;
    				  
    					prevTerm.mInts[prevTerm.mOffset + prefixLength] = startLabel;
    					floorBlocks.add(writeBlock(prevTerm, prefixLength, prefixLength+1, 
    							curStart, curStart, 0, true, startLabel, true));
    				  
    					break;
    				}
    			}
    		}

    		prevTerm.mInts[prevTerm.mOffset + prefixLength] = savLabel;

    		assert firstBlock != null;
    		firstBlock.compileIndex(floorBlocks, mWriter.getScratchBytes());

    		mPending.add(firstBlock);
    	}
    	mLastBlockIndex = mPending.size()-1;
	}

    // for debugging
    @SuppressWarnings("unused")
    private String toString(BytesRef b) {
    	try {
    		return b.utf8ToString() + " " + b;
    	} catch (Throwable t) {
    		// If BytesRef isn't actually UTF8, or it's eg a
    		// prefix of UTF8 that ends mid-unicode-char, we
    		// fallback to hex:
    		return b.toString();
    	}
    }

    // Writes all entries in the pending slice as a single
    // block: 
    protected PendingBlock writeBlock(IntsRef prevTerm, int prefixLength, 
    		int indexPrefixLength, int startBackwards, int length, int futureTermCount, 
    		boolean isFloor, int floorLeadByte, boolean isLastInFloor) 
    		throws IOException {
    	assert length > 0;

    	final int start = mPending.size() - startBackwards;
    	assert start >= 0: "pending.size()=" + mPending.size() + " startBackwards=" + 
    			startBackwards + " length=" + length;

    	final List<PendingEntry> slice = mPending.subList(start, start + length);
    	final long startFP = mWriter.getTermsOutput().getFilePointer();

    	final BytesRef prefix = new BytesRef(indexPrefixLength);
    	for (int m=0; m < indexPrefixLength; m++) {
    		prefix.mBytes[m] = (byte) prevTerm.mInts[m];
    	}
    	prefix.mLength = indexPrefixLength;

    	// Write block header:
    	mWriter.getTermsOutput().writeVInt((length<<1)|(isLastInFloor ? 1:0));

    	// 1st pass: pack term suffix bytes into byte[] blob
    	// TODO: cutover to bulk int codec... simple64?

    	final boolean isLeafBlock;
    	if (mLastBlockIndex < start) {
    		// This block definitely does not contain sub-blocks:
    		isLeafBlock = true;
    	} else if (!isFloor) {
    		// This block definitely does contain at least one sub-block:
    		isLeafBlock = false;
    	} else {
    		// Must scan up-front to see if there is a sub-block
    		boolean v = true;
    		for (PendingEntry ent : slice) {
    			if (!ent.isTerm()) {
    				v = false;
    				break;
    			}
    		}
    		isLeafBlock = v;
    	}

    	final List<FST<BytesRef>> subIndices;
    	int termCount;
    	
    	if (isLeafBlock) {
    		subIndices = null;
    		
    		for (PendingEntry ent : slice) {
    			assert ent.isTerm();
    			PendingTerm term = (PendingTerm) ent;
    			final int suffix = term.getTerm().mLength - prefixLength;

    			// For leaf block we write suffix straight
    			mBytesWriter.writeVInt(suffix);
    			mBytesWriter.writeBytes(term.getTerm().mBytes, prefixLength, suffix);

    			// Write term stats, to separate byte[] blob:
    			mBytesWriter2.writeVInt(term.getTermState().getDocFreq());
    			if (mFieldInfo.getIndexOptions() != IndexOptions.DOCS_ONLY) {
    				assert term.getTermState().getTotalTermFreq() >= term.getTermState().getDocFreq();
    				
    				mBytesWriter2.writeVLong(term.getTermState().getTotalTermFreq() - 
    						term.getTermState().getDocFreq());
    			}
    		}
    		
    		termCount = length;
    		
    	} else {
    		subIndices = new ArrayList<FST<BytesRef>>();
    		termCount = 0;
    		
    		for (PendingEntry ent : slice) {
    			if (ent.isTerm()) {
    				PendingTerm term = (PendingTerm) ent;
    				final int suffix = term.getTerm().getLength() - prefixLength;

    				// For non-leaf block we borrow 1 bit to record
    				// if entry is term or sub-block
    				mBytesWriter.writeVInt(suffix<<1);
    				mBytesWriter.writeBytes(term.getTerm().mBytes, prefixLength, suffix);

    				// Write term stats, to separate byte[] blob:
    				mBytesWriter2.writeVInt(term.getTermState().getDocFreq());
    				if (mFieldInfo.getIndexOptions() != IndexOptions.DOCS_ONLY) {
    					assert term.getTermState().getTotalTermFreq() >= term.getTermState().getDocFreq();
    					
    					mBytesWriter2.writeVLong(term.getTermState().getTotalTermFreq() - 
    							term.getTermState().getDocFreq());
    				}

    				termCount ++;
    				
    			} else {
    				PendingBlock block = (PendingBlock) ent;
    				final int suffix = block.getPrefix().getLength() - prefixLength;
    				assert suffix > 0;

    				// For non-leaf block we borrow 1 bit to record
    				// if entry is term or sub-block
    				mBytesWriter.writeVInt((suffix<<1)|1);
    				mBytesWriter.writeBytes(block.getPrefix().mBytes, prefixLength, suffix);
    				assert block.getFilePointer() < startFP;

    				mBytesWriter.writeVLong(startFP - block.getFilePointer());
    				subIndices.add(block.getIndex());
    			}
    		}

    		assert subIndices.size() != 0;
    	}

    	// TODO: we could block-write the term suffix pointers;
    	// this would take more space but would enable binary
    	// search on lookup

    	// Write suffixes byte[] blob to terms dict output:
    	mWriter.getTermsOutput().writeVInt((int) (mBytesWriter.getFilePointer() << 1) | (isLeafBlock ? 1:0));
    	mBytesWriter.writeTo(mWriter.getTermsOutput());
    	mBytesWriter.reset();

    	// Write term stats byte[] blob
    	mWriter.getTermsOutput().writeVInt((int) mBytesWriter2.getFilePointer());
    	mBytesWriter2.writeTo(mWriter.getTermsOutput());
    	mBytesWriter2.reset();

    	// Have postings writer write block
    	mWriter.getPostingsWriter().flushTermsBlock(futureTermCount+termCount, termCount);

    	// Remove slice replaced by block:
    	slice.clear();

    	if (mLastBlockIndex >= start) {
    		if (mLastBlockIndex < start+length) 
    			mLastBlockIndex = start;
    		else 
    			mLastBlockIndex -= length;
    	}

    	return new PendingBlock(prefix, startFP, termCount != 0, isFloor, floorLeadByte, subIndices);
    }

    @Override
    public PostingsConsumer startTerm(BytesRef text) throws IOException {
    	mWriter.getPostingsWriter().startTerm();
    	
    	//// for debug
      	//if (fieldInfo.name.equals("id")) {
        //	postingsWriter.termID = Integer.parseInt(text.utf8ToString());
      	//} else {
        //	postingsWriter.termID = -1;
      	//}
    	
    	return mWriter.getPostingsWriter();
    }

    @Override
    public void finishTerm(BytesRef text, ITermState stats) throws IOException {
    	assert ((PerTermState)stats).getDocFreq() > 0;
      
    	mBlockBuilder.add(FSTUtil.toIntsRef(text, mScratchIntsRef), mNoOutputs.getNoOutput());
    	mPending.add(new PendingTerm(BytesRef.deepCopyOf(text), (PerTermState)stats));
    	mWriter.getPostingsWriter().finishTerm(stats);
    	mNumTerms ++;
    }

    // Finishes all terms in this field
    @Override
    public void finish(long sumTotalTermFreq, long sumDocFreq, int docCount) 
    		throws IOException {
    	if (mNumTerms > 0) {
    		mBlockBuilder.finish();

    		// We better have one final "root" block:
    		assert mPending.size() == 1 && !mPending.get(0).isTerm(): "pending.size()=" + 
    				mPending.size() + " pending=" + mPending;
    		
    		final PendingBlock root = (PendingBlock) mPending.get(0);
    		assert root.getPrefix().getLength() == 0;
    		assert root.getIndex().getEmptyOutput() != null;

    		mSumTotalTermFreq = sumTotalTermFreq;
    		mSumDocFreq = sumDocFreq;
    		mDocCount = docCount;

    		// Write FST to index
    		mIndexStartFP = mWriter.getIndexOutput().getFilePointer();
    		root.getIndex().save(mWriter.getIndexOutput());

    	} else {
    		assert sumTotalTermFreq == 0;
    		assert sumDocFreq == 0;
    		assert docCount == 0;
    	}
	}
	
}
