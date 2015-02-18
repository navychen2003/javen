package org.javenstudio.hornet.codec.block;

import java.io.IOException;
import java.util.Comparator;

import org.javenstudio.common.indexdb.IDocsAndPositionsEnum;
import org.javenstudio.common.indexdb.IDocsEnum;
import org.javenstudio.common.indexdb.IIndexInput;
import org.javenstudio.common.indexdb.ITermState;
import org.javenstudio.common.indexdb.IndexOptions;
import org.javenstudio.common.indexdb.index.term.TermsEnum;
import org.javenstudio.common.indexdb.store.ByteArrayDataInput;
import org.javenstudio.common.indexdb.store.IndexInput;
import org.javenstudio.common.indexdb.util.ArrayUtil;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.JvmUtil;
import org.javenstudio.common.util.Logger;
import org.javenstudio.hornet.store.fst.BytesReader;
import org.javenstudio.hornet.store.fst.FSTArc;

//Iterates through terms in this field
final class SegmentTermsEnum extends TermsEnum {
	private static final Logger LOG = Logger.getLogger(SegmentTermsEnum.class);
	
	private final BlockTreeTermsReader mReader;
	private final BlockFieldReader mFieldReader;
	
	private IIndexInput mInput;

	private SegmentTermsFrame[] mStack;
	private final SegmentTermsFrame mStaticFrame;
	private SegmentTermsFrame mCurrentFrame;
	private boolean mTermExists;

	private int mTargetBeforeCurrentLength;

	private final ByteArrayDataInput mScratchReader = new ByteArrayDataInput();

    // What prefix of the current term was present in the index:
	private int mValidIndexPrefix;

    // assert only:
	private boolean mEof;

	private final BytesRef mTerm = new BytesRef();
	private final BytesReader mFstReader;

    @SuppressWarnings("unchecked")
    private FSTArc<BytesRef>[] mArcs = new FSTArc[1];

    final BlockTreeTermsReader getReader() { return mReader; }
    final BlockFieldReader getFieldReader() { return mFieldReader; }
    final IIndexInput getInput() { return mInput; }
    
    final void setTermExists(boolean exists) { mTermExists = exists; }
    final boolean isTermExists() { return mTermExists; }
    
    final void setCurrentFrame(SegmentTermsFrame frame) { mCurrentFrame = frame; }
    final SegmentTermsFrame getCurrentFrame() { return mCurrentFrame; }
    
    public SegmentTermsEnum(BlockTreeTermsReader reader, BlockFieldReader fieldReader) 
    		throws IOException {
    	mReader = reader;
    	mFieldReader = fieldReader;
    	mStack = new SegmentTermsFrame[0];
      
    	// Used to hold seek by TermState, or cached seek
    	mStaticFrame = new SegmentTermsFrame(this, -1);

    	if (mFieldReader.getIndex() == null) 
    		mFstReader = null;
    	else 
    		mFstReader = mFieldReader.getIndex().getBytesReader(0);
    	
    	// Init w/ root block; don't use index since it may
    	// not (and need not) have been loaded
    	for (int arcIdx=0; arcIdx < mArcs.length; arcIdx++) {
    		mArcs[arcIdx] = new FSTArc<BytesRef>();
    	}

    	mCurrentFrame = mStaticFrame;
    	
    	final FSTArc<BytesRef> arc;
    	if (mFieldReader.getIndex() != null) {
    		arc = mFieldReader.getIndex().getFirstArc(mArcs[0]);
    		// Empty string prefix must have an output in the index!
    		assert arc.isFinal();
    	} else {
    		arc = null;
    	}
    	
    	mCurrentFrame = mStaticFrame;
    	//mCurrentFrame = pushFrame(arc, rootCode, 0);
    	//mCurrentFrame.loadBlock();
    	mValidIndexPrefix = 0;
    }
    
    // Not private to avoid synthetic access$NNN methods
    void initIndexInput() {
    	if (mInput == null) {
    		mInput = (IIndexInput) ((IndexInput)mReader.getInput()).clone();
    		
    		//if (LOG.isDebugEnabled())
    		//	LOG.debug("initIndexInput: " + mInput);
    	}
    }

    /** 
     * Runs next() through the entire terms dict,
     *  computing aggregate statistics. 
     */
    public BlockStats computeBlockStats() throws IOException {
    	BlockStats stats = new BlockStats(mReader.getSegmentName(), mFieldReader.getFieldInfo().getName());
    	if (mFieldReader.getIndex() != null) {
    		stats.setIndexNodeCount(mFieldReader.getIndex().getNodeCount());
    		stats.setIndexArcCount(mFieldReader.getIndex().getArcCount());
    		stats.setIndexNumBytes(mFieldReader.getIndex().sizeInBytes());
    	}
      
    	mCurrentFrame = mStaticFrame;
    	
    	FSTArc<BytesRef> arc;
    	if (mFieldReader.getIndex() != null) {
    		arc = mFieldReader.getIndex().getFirstArc(mArcs[0]);
    		// Empty string prefix must have an output in the index!
    		assert arc.isFinal();
    	} else {
    		arc = null;
    	}

    	// Empty string prefix must have an output in the
    	// index!
    	mCurrentFrame = pushFrame(arc, mFieldReader.getRootCode(), 0);
    	mCurrentFrame.setFilePointerOrig(mCurrentFrame.getFilePointer());
    	mCurrentFrame.loadBlock();
    	mValidIndexPrefix = 0;

    	stats.startBlock(mCurrentFrame, !mCurrentFrame.isLastInFloor());

    	allTerms: while (true) {
    		// Pop finished blocks
    		while (mCurrentFrame.getNextEntry() == mCurrentFrame.getEntryCount()) {
    			stats.endBlock(mCurrentFrame);
    			
    			if (!mCurrentFrame.isLastInFloor()) {
    				mCurrentFrame.loadNextFloorBlock();
    				stats.startBlock(mCurrentFrame, true);
    				
    			} else {
    				if (mCurrentFrame.getOrd() == 0) 
    					break allTerms;
    				
    				final long lastFP = mCurrentFrame.getFilePointerOrig();
    				mCurrentFrame = mStack[mCurrentFrame.getOrd()-1];
    				assert lastFP == mCurrentFrame.getLastSubPointer();
    			}
    		}

    		while (true) {
    			if (mCurrentFrame.next()) {
    				// Push to new block:
    				mCurrentFrame = pushFrame(null, mCurrentFrame.getLastSubPointer(), mTerm.mLength);
    				mCurrentFrame.setFilePointerOrig(mCurrentFrame.getFilePointer());
    				// This is a "next" frame -- even if it's
    				// floor'd we must pretend it isn't so we don't
    				// try to scan to the right floor frame:
    				mCurrentFrame.setIsFloor(false);
    				//currentFrame.hasTerms = true;
    				mCurrentFrame.loadBlock();
    				
    				stats.startBlock(mCurrentFrame, !mCurrentFrame.isLastInFloor());
    			} else {
    				stats.term(mTerm);
    				break;
    			}
    		}
    	}

    	stats.finish();

    	// Put root frame back:
    	mCurrentFrame = mStaticFrame;
    	if (mFieldReader.getIndex() != null) {
    		arc = mFieldReader.getIndex().getFirstArc(mArcs[0]);
    		// Empty string prefix must have an output in the index!
    		assert arc.isFinal();
    	} else {
    		arc = null;
    	}
    	
    	mCurrentFrame = pushFrame(arc, mFieldReader.getRootCode(), 0);
    	mCurrentFrame.rewind();
    	mCurrentFrame.loadBlock();
    	mValidIndexPrefix = 0;
    	mTerm.mLength = 0;

    	return stats;
    }

    private SegmentTermsFrame getFrame(int ord) throws IOException {
    	//if (LOG.isDebugEnabled())
    	//	LOG.debug("getFrame: ord=" + ord);
    	
    	if (ord >= mStack.length) {
    		final SegmentTermsFrame[] next = 
    				new SegmentTermsFrame[ArrayUtil.oversize(1+ord, JvmUtil.NUM_BYTES_OBJECT_REF)];
    		System.arraycopy(mStack, 0, next, 0, mStack.length);
    		for (int stackOrd=mStack.length; stackOrd < next.length; stackOrd++) {
    			next[stackOrd] = new SegmentTermsFrame(this, stackOrd);
    		}
    		mStack = next;
    	}
    	
    	assert mStack[ord].getOrd() == ord;
    	return mStack[ord];
    }

    private FSTArc<BytesRef> getArc(int ord) {
    	if (ord >= mArcs.length) {
    		@SuppressWarnings({"unchecked"}) 
    		final FSTArc<BytesRef>[] next = 
    				new FSTArc[ArrayUtil.oversize(1+ord, JvmUtil.NUM_BYTES_OBJECT_REF)];
    		System.arraycopy(mArcs, 0, next, 0, mArcs.length);
    		for (int arcOrd=mArcs.length; arcOrd < next.length; arcOrd++) {
    			next[arcOrd] = new FSTArc<BytesRef>();
    		}
    		mArcs = next;
    	}
    	return mArcs[ord];
    }

    @Override
    public Comparator<BytesRef> getComparator() {
    	return BytesRef.getUTF8SortedAsUnicodeComparator();
    }

    // Pushes a frame we seek'd to
    public SegmentTermsFrame pushFrame(FSTArc<BytesRef> arc, BytesRef frameData, int length) 
    		throws IOException {
    	mScratchReader.reset(frameData.mBytes, frameData.mOffset, frameData.mLength);
    	
    	final long code = mScratchReader.readVLong();
    	final long fpSeek = code >>> BlockTreeTermsWriter.OUTPUT_FLAGS_NUM_BITS;
    	final SegmentTermsFrame f = getFrame(1+mCurrentFrame.getOrd());
    	
    	f.setHasTerms((code & BlockTreeTermsWriter.OUTPUT_FLAG_HAS_TERMS) != 0);
    	f.setHasTermsOrig(f.hasTerms());
    	f.setIsFloor((code & BlockTreeTermsWriter.OUTPUT_FLAG_IS_FLOOR) != 0);
    	if (f.isFloor()) 
    		f.setFloorData(mScratchReader, frameData);
    	
    	pushFrame(arc, fpSeek, length);

    	return f;
    }

    // Pushes next'd frame or seek'd frame; we later
    // lazy-load the frame only when needed
    public SegmentTermsFrame pushFrame(FSTArc<BytesRef> arc, long fp, int length) 
    		throws IOException {
    	final SegmentTermsFrame f = getFrame(1+mCurrentFrame.getOrd());
    	f.setArc(arc);
    	
    	if (f.getFilePointerOrig() == fp && f.getNextEntry() != -1) {
    		if (f.getPrefix() > mTargetBeforeCurrentLength) f.rewind();
    		assert length == f.getPrefix();
    		
    	} else {
    		f.setNextEntry(-1);
    		f.setPrefix(length);
    		f.getState().setTermBlockOrd(0);
    		f.setFilePointerOrig(fp);
    		f.setFilePointer(fp);
    		f.setLastSubPointer(-1);
    	}

    	return f;
    }

    // asserts only
    private boolean clearEOF() {
    	mEof = false;
    	return true;
    }

    // asserts only
    private boolean setEOF() {
    	mEof = true;
    	return true;
    }

    @Override
    public boolean seekExact(final BytesRef target, final boolean useCache) 
    		throws IOException {
    	//if (LOG.isDebugEnabled())
    	//	LOG.debug("seekExact: currentFrame vs staticFrame = " + (mCurrentFrame == mStaticFrame));
    	
    	if (mFieldReader.getIndex() == null) 
    		throw new IllegalStateException("terms index was not loaded");

    	if (mTerm.mBytes.length <= target.mLength) 
    		mTerm.mBytes = ArrayUtil.grow(mTerm.mBytes, 1+target.mLength);

    	assert clearEOF();

    	FSTArc<BytesRef> arc;
    	int targetUpto;
    	BytesRef output;

    	mTargetBeforeCurrentLength = mCurrentFrame.getOrd();

    	if (mCurrentFrame != mStaticFrame) {
    		// We are already seek'd; find the common
    		// prefix of new seek term vs current term and
    		// re-use the corresponding seek state.  For
    		// example, if app first seeks to foobar, then
    		// seeks to foobaz, we can re-use the seek state
    		// for the first 5 bytes.

    		arc = mArcs[0];
    		assert arc.isFinal();
    		output = arc.getOutput();
    		targetUpto = 0;
        
    		SegmentTermsFrame lastFrame = mStack[0];
    		assert mValidIndexPrefix <= mTerm.mLength;

    		final int targetLimit = Math.min(target.mLength, mValidIndexPrefix);
    		int cmp = 0;

    		// TODO: reverse vLong byte order for better FST
    		// prefix output sharing

    		// First compare up to valid seek frames:
    		while (targetUpto < targetLimit) {
    			cmp = (mTerm.mBytes[targetUpto]&0xFF) - (target.mBytes[target.mOffset + targetUpto]&0xFF);
    			if (cmp != 0) 
    				break;
          
    			arc = mArcs[1+targetUpto];
    			assert arc.getLabel() == (target.mBytes[target.mOffset + targetUpto] & 0xFF): 
    				"arc.label=" + (char) arc.getLabel() + " targetLabel=" + 
    				(char) (target.mBytes[target.mOffset + targetUpto] & 0xFF);
    			
    			if (arc.getOutput() != mReader.getNoOutput()) 
    				output = mReader.getOutputs().add(output, arc.getOutput());
    			
    			if (arc.isFinal()) 
    				lastFrame = mStack[1+lastFrame.getOrd()];
    			
    			targetUpto ++;
    		}

    		if (cmp == 0) {
    			final int targetUptoMid = targetUpto;

    			// Second compare the rest of the term, but
    			// don't save arc/output/frame; we only do this
    			// to find out if the target term is before,
    			// equal or after the current term
    			final int targetLimit2 = Math.min(target.mLength, mTerm.mLength);
    			
    			while (targetUpto < targetLimit2) {
    				cmp = (mTerm.mBytes[targetUpto]&0xFF) - (target.mBytes[target.mOffset + targetUpto]&0xFF);
    				if (cmp != 0) 
    					break;
    				
    				targetUpto ++;
    			}

    			if (cmp == 0) 
    				cmp = mTerm.mLength - target.mLength;
          
    			targetUpto = targetUptoMid;
    		}

    		if (cmp < 0) {
    			// Common case: target term is after current
    			// term, ie, app is seeking multiple terms
    			// in sorted order
    			mCurrentFrame = lastFrame;

    		} else if (cmp > 0) {
    			// Uncommon case: target term
    			// is before current term; this means we can
    			// keep the currentFrame but we must rewind it
    			// (so we scan from the start)
    			mTargetBeforeCurrentLength = 0;
    			
    			mCurrentFrame = lastFrame;
    			mCurrentFrame.rewind();
    			
    		} else {
    			// Target is exactly the same as current term
    			assert mTerm.mLength == target.mLength;
    			if (mTermExists) 
    				return true;
    			
    			//mValidIndexPrefix = mCurrentFrame.depth;
    			//mTerm.length = target.length;
    			//return mTermExists;
    		}
    	} else {
    		mTargetBeforeCurrentLength = -1;
    		arc = mFieldReader.getIndex().getFirstArc(mArcs[0]);

    		// Empty string prefix must have an output (block) in the index!
    		assert arc.isFinal();
    		assert arc.getOutput() != null;

    		output = arc.getOutput();
    		mCurrentFrame = mStaticFrame;

    		//mTerm.length = 0;
    		targetUpto = 0;
    		mCurrentFrame = pushFrame(arc, mReader.getOutputs().add(output, arc.getNextFinalOutput()), 0);
    	}

    	while (targetUpto < target.mLength) {
    		final int targetLabel = target.mBytes[target.mOffset + targetUpto] & 0xFF;
    		final FSTArc<BytesRef> nextArc = mFieldReader.getIndex().findTargetArc(
    				targetLabel, arc, getArc(1+targetUpto), mFstReader);

    		if (nextArc == null) {
    			// Index is exhausted
    			mValidIndexPrefix = mCurrentFrame.getPrefix();
    			//mValidIndexPrefix = targetUpto;

    			mCurrentFrame.scanToFloorFrame(target);

    			if (!mCurrentFrame.hasTerms()) {
    				mTermExists = false;
    				mTerm.mBytes[targetUpto] = (byte) targetLabel;
    				mTerm.mLength = 1+targetUpto;
    				
    				//if (LOG.isDebugEnabled()) {
    	    		//	LOG.debug("seekExact: " + mCurrentFrame 
    	    		//			+ " has no terms, targetUpto=" + targetUpto 
    	    		//			+ " targetLabel=" + targetLabel);
    	    		//	mCurrentFrame.printInfo();
    				//}
    				
    				return false;
    			}

    	    	//if (LOG.isDebugEnabled()) {
    			//	LOG.debug("seekExact: " + mCurrentFrame 
    			//			+ " has terms, targetUpto=" + targetUpto 
	    		//			+ " targetLabel=" + targetLabel);
    			//	mCurrentFrame.printInfo();
    			//}
    			
    			mCurrentFrame.loadBlock();

    	    	//if (LOG.isDebugEnabled()) {
    			//	LOG.debug("seekExact: " + mCurrentFrame + " loadBlock done");
    			//	mCurrentFrame.printInfo();
    			//}
    			
    			final SeekStatus result = mCurrentFrame.scanToTerm(target, true);            
    			if (result == SeekStatus.FOUND) 
    				return true;
    			else 
    				return false;
    			
    		} else {
    			// Follow this arc
    			arc = nextArc;
    			mTerm.mBytes[targetUpto] = (byte) targetLabel;
    			// Aggregate output as we go:
    			assert arc.getOutput() != null;
    			
    			if (arc.getOutput() != mReader.getNoOutput()) 
    				output = mReader.getOutputs().add(output, arc.getOutput());
    			
    			targetUpto ++;

    			if (arc.isFinal()) {
    				mCurrentFrame = pushFrame(arc, mReader.getOutputs().add(
    						output, arc.getNextFinalOutput()), targetUpto);
    			}
    		}
    	}

    	//validIndexPrefix = targetUpto;
    	mValidIndexPrefix = mCurrentFrame.getPrefix();

    	mCurrentFrame.scanToFloorFrame(target);

    	// Target term is entirely contained in the index:
    	if (!mCurrentFrame.hasTerms()) {
    		mTermExists = false;
    		mTerm.mLength = targetUpto;
    		
    		//if (LOG.isDebugEnabled()) {
    		//	LOG.debug("seekExact: " + mCurrentFrame 
    		//			+ " has no terms, targetUpto=" + targetUpto);
    		//	mCurrentFrame.printInfo();
			//}
    		
    		return false;
    	}

    	//if (LOG.isDebugEnabled()) {
		//	LOG.debug("seekExact: " + mCurrentFrame 
		//			+ " has terms, targetUpto=" + targetUpto);
		//	mCurrentFrame.printInfo();
		//}
    	
    	mCurrentFrame.loadBlock();

    	//if (LOG.isDebugEnabled()) {
		//	LOG.debug("seekExact: " + mCurrentFrame + " loadBlock done");
		//	mCurrentFrame.printInfo();
		//}
    	
    	final SeekStatus result = mCurrentFrame.scanToTerm(target, true);            
    	if (result == SeekStatus.FOUND) 
    		return true;
    	else 
    		return false;
    }

    @Override
    public SeekStatus seekCeil(final BytesRef target, final boolean useCache) 
    		throws IOException {
    	if (mFieldReader.getIndex() == null) 
    		throw new IllegalStateException("terms index was not loaded");
 
    	if (mTerm.mBytes.length <= target.mLength) 
    		mTerm.mBytes = ArrayUtil.grow(mTerm.mBytes, 1+target.mLength);

    	assert clearEOF();

    	FSTArc<BytesRef> arc;
    	int targetUpto;
    	BytesRef output;

    	mTargetBeforeCurrentLength = mCurrentFrame.getOrd();

    	if (mCurrentFrame != mStaticFrame) {
    		// We are already seek'd; find the common
    		// prefix of new seek term vs current term and
    		// re-use the corresponding seek state.  For
    		// example, if app first seeks to foobar, then
    		// seeks to foobaz, we can re-use the seek state
    		// for the first 5 bytes.

    		arc = mArcs[0];
    		assert arc.isFinal();
    		output = arc.getOutput();
    		targetUpto = 0;
        
    		SegmentTermsFrame lastFrame = mStack[0];
    		assert mValidIndexPrefix <= mTerm.mLength;

    		final int targetLimit = Math.min(target.mLength, mValidIndexPrefix);
    		int cmp = 0;

    		// TOOD: we should write our vLong backwards (MSB
    		// first) to get better sharing from the FST

    		// First compare up to valid seek frames:
    		while (targetUpto < targetLimit) {
    			cmp = (mTerm.mBytes[targetUpto]&0xFF) - (target.mBytes[target.mOffset + targetUpto]&0xFF);
    			if (cmp != 0) 
    				break;
          
    			arc = mArcs[1+targetUpto];
    			assert arc.getLabel() == (target.mBytes[target.mOffset + targetUpto] & 0xFF): 
    				"arc.label=" + (char) arc.getLabel() + " targetLabel=" + 
    				(char) (target.mBytes[target.mOffset + targetUpto] & 0xFF);
    		  
    			// TOOD: we could save the outputs in local
    			// byte[][] instead of making new objs ever
    			// seek; but, often the FST doesn't have any
    			// shared bytes (but this could change if we
    			// reverse vLong byte order)
    			if (arc.getOutput() != mReader.getNoOutput()) 
    				output = mReader.getOutputs().add(output, arc.getOutput());
    		  
    			if (arc.isFinal()) 
    				lastFrame = mStack[1+lastFrame.getOrd()];
    		  
    			targetUpto ++;
    		}

    		if (cmp == 0) {
    			final int targetUptoMid = targetUpto;
    			// Second compare the rest of the term, but
    			// don't save arc/output/frame:
    			final int targetLimit2 = Math.min(target.mLength, mTerm.mLength);
    		  
    			while (targetUpto < targetLimit2) {
    				cmp = (mTerm.mBytes[targetUpto]&0xFF) - (target.mBytes[target.mOffset + targetUpto]&0xFF);
    				if (cmp != 0) 
    					break;
    			  
    				targetUpto ++;
    			}

    			if (cmp == 0) 
    				cmp = mTerm.mLength - target.mLength;
    		  
    			targetUpto = targetUptoMid;
    		}

    		if (cmp < 0) {
    			// Common case: target term is after current
    			// term, ie, app is seeking multiple terms
    			// in sorted order
    			mCurrentFrame = lastFrame;

    		} else if (cmp > 0) {
    			// Uncommon case: target term
    			// is before current term; this means we can
    			// keep the currentFrame but we must rewind it
    			// (so we scan from the start)
    			mTargetBeforeCurrentLength = 0;
    		  
    			mCurrentFrame = lastFrame;
    			mCurrentFrame.rewind();
    		  
    		} else {
    			// Target is exactly the same as current term
    			assert mTerm.mLength == target.mLength;
    			if (mTermExists) 
    				return SeekStatus.FOUND;
    		}
    		
    	} else {
    		mTargetBeforeCurrentLength = -1;
    		arc = mFieldReader.getIndex().getFirstArc(mArcs[0]);

    		// Empty string prefix must have an output (block) in the index!
    		assert arc.isFinal();
    		assert arc.getOutput() != null;

    		output = arc.getOutput();
    		mCurrentFrame = mStaticFrame;

    		//mTerm.length = 0;
    		targetUpto = 0;
    		mCurrentFrame = pushFrame(arc, mReader.getOutputs().add(
    				output, arc.getNextFinalOutput()), 0);
    	}

    	while (targetUpto < target.mLength) {
    		final int targetLabel = target.mBytes[target.mOffset + targetUpto] & 0xFF;
    		final FSTArc<BytesRef> nextArc = mFieldReader.getIndex().findTargetArc(
    				targetLabel, arc, getArc(1+targetUpto), mFstReader);

    		if (nextArc == null) {
    			// Index is exhausted
    			mValidIndexPrefix = mCurrentFrame.getPrefix();
    			//mValidIndexPrefix = targetUpto;

    			mCurrentFrame.scanToFloorFrame(target);
    			mCurrentFrame.loadBlock();

    			final SeekStatus result = mCurrentFrame.scanToTerm(target, false);
    			if (result == SeekStatus.END) {
    				mTerm.copyBytes(target);
    				mTermExists = false;

    				if (next() != null) 
    					return SeekStatus.NOT_FOUND;
    				else 
    					return SeekStatus.END;
    			} else 
    				return result;
        	
    		} else {
    			// Follow this arc
    			mTerm.mBytes[targetUpto] = (byte) targetLabel;
    			arc = nextArc;
        	
    			// Aggregate output as we go:
    			assert arc.getOutput() != null;
    			if (arc.getOutput() != mReader.getNoOutput()) 
    				output = mReader.getOutputs().add(output, arc.getOutput());

    			targetUpto ++;

    			if (arc.isFinal()) {
    				mCurrentFrame = pushFrame(arc, mReader.getOutputs().add(
    						output, arc.getNextFinalOutput()), targetUpto);
    			}
    		}
    	}

    	//mValidIndexPrefix = targetUpto;
    	mValidIndexPrefix = mCurrentFrame.getPrefix();

    	mCurrentFrame.scanToFloorFrame(target);
    	mCurrentFrame.loadBlock();

    	final SeekStatus result = mCurrentFrame.scanToTerm(target, false);

    	if (result == SeekStatus.END) {
    		mTerm.copyBytes(target);
    		mTermExists = false;
    		
    		if (next() != null) 
    			return SeekStatus.NOT_FOUND;
    		else 
    			return SeekStatus.END;
    		
    	} else 
    		return result;
    }

    /** 
     * Decodes only the term bytes of the next term.  If caller then asks for
     * metadata, ie docFreq, totalTermFreq or pulls a D/&PEnum, we then (lazily)
     * decode all metadata up to the current term. 
     */
    @Override
    public BytesRef next() throws IOException {
    	//if (LOG.isDebugEnabled())
    	//	LOG.debug("next: input=" + mReader.getInput());
    	
    	if (mReader.getInput() == null) {
    		// Fresh TermsEnum; seek to first term:
    		final FSTArc<BytesRef> arc;
    		if (mFieldReader.getIndex() != null) {
    			arc = mFieldReader.getIndex().getFirstArc(mArcs[0]);
    			// Empty string prefix must have an output in the index!
    			assert arc.isFinal();
    		} else {
    			arc = null;
    		}
    		
    		mCurrentFrame = pushFrame(arc, mFieldReader.getRootCode(), 0);
    		mCurrentFrame.loadBlock();
    	}

    	mTargetBeforeCurrentLength = mCurrentFrame.getOrd();
    	assert !mEof;

    	if (mCurrentFrame == mStaticFrame) {
    		// If seek was previously called and the term was
    		// cached, or seek(TermState) was called, usually
    		// caller is just going to pull a D/&PEnum or get
    		// docFreq, etc.  But, if they then call next(),
    		// this method catches up all internal state so next()
    		// works properly:
    		final boolean result = seekExact(mTerm, false);
    		assert result;
    	}

    	// Pop finished blocks
    	while (mCurrentFrame.getNextEntry() == mCurrentFrame.getEntryCount()) {
    		if (!mCurrentFrame.isLastInFloor()) {
    			mCurrentFrame.loadNextFloorBlock();
    			
    		} else {
    			if (mCurrentFrame.getOrd() == 0) {
    				assert setEOF();
    				
    				mTerm.mLength = 0;
    				mValidIndexPrefix = 0;
    				mCurrentFrame.rewind();
    				mTermExists = false;
    				
    				return null;
    			}
    			
    			final long lastFP = mCurrentFrame.getFilePointerOrig();
    			mCurrentFrame = mStack[mCurrentFrame.getOrd()-1];

    			if (mCurrentFrame.getNextEntry() == -1 || mCurrentFrame.getLastSubPointer() != lastFP) {
    				// We popped into a frame that's not loaded
    				// yet or not scan'd to the right entry
    				mCurrentFrame.scanToFloorFrame(mTerm);
    				mCurrentFrame.loadBlock();
    				mCurrentFrame.scanToSubBlock(lastFP);
    			}

    			// Note that the seek state (last seek) has been
    			// invalidated beyond this depth
    			mValidIndexPrefix = Math.min(mValidIndexPrefix, mCurrentFrame.getPrefix());
    		}
    	}

    	while (true) {
    		if (!mCurrentFrame.isLoadedBlock()) { 
    			if (LOG.isDebugEnabled()) { 
    				LOG.debug("next: " + mCurrentFrame + " not loaded");
    				mCurrentFrame.printInfo();
    			}
    			return null;
    		}
    		
    		if (mCurrentFrame.next()) {
    			// Push to new block:
    			mCurrentFrame = pushFrame(null, mCurrentFrame.getLastSubPointer(), mTerm.mLength);
    			// This is a "next" frame -- even if it's
    			// floor'd we must pretend it isn't so we don't
    			// try to scan to the right floor frame:
    			mCurrentFrame.setIsFloor(false);
    			//mCurrentFrame.hasTerms = true;
    			mCurrentFrame.loadBlock();
    			
    		} else {
    			return mTerm;
    		}
    	}
    }

    @Override
    public BytesRef getTerm() {
    	assert !mEof;
    	return mTerm;
    }

    @Override
    public int getDocFreq() throws IOException {
    	assert !mEof;
    	mCurrentFrame.decodeMetaData();
    	return mCurrentFrame.getState().getDocFreq();
    }

    @Override
    public long getTotalTermFreq() throws IOException {
    	assert !mEof;
    	mCurrentFrame.decodeMetaData();
    	return mCurrentFrame.getState().getTotalTermFreq();
    }

    @Override
    public IDocsEnum getDocs(Bits skipDocs, IDocsEnum reuse, int flags) 
    		throws IOException {
    	//if (needsFreqs && mFieldReader.getFieldInfo().getIndexOptions() == IndexOptions.DOCS_ONLY) 
    	//	return null;
    	
    	assert !mEof;
    	mCurrentFrame.decodeMetaData();
    	
    	return mReader.getPostingsReader().getDocs(mFieldReader.getFieldInfo(), 
    			mCurrentFrame.getState(), skipDocs, reuse, flags);
    }

    @Override
    public IDocsAndPositionsEnum getDocsAndPositions(Bits skipDocs, IDocsAndPositionsEnum reuse, 
    		int flags) throws IOException {
    	if (mFieldReader.getFieldInfo().getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) < 0) {
    		// Positions were not indexed:
    		return null;
    	}

    	//if (needsOffsets && mFieldReader.getFieldInfo().getIndexOptions().compareTo(
    	//		IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) < 0) {
    	//	// Offsets were not indexed:
    	//	return null;
    	//}

    	assert !mEof;
    	mCurrentFrame.decodeMetaData();
    	
    	return mReader.getPostingsReader().getDocsAndPositions(mFieldReader.getFieldInfo(), 
    			mCurrentFrame.getState(), skipDocs, reuse, flags);
    }

    @Override
    public void seekExact(BytesRef target, ITermState otherState) throws IOException {
    	assert clearEOF();
    	if (target.compareTo(mTerm) != 0 || !mTermExists) {
    		assert otherState != null && otherState instanceof BlockTermState;
    		
    		mCurrentFrame = mStaticFrame;
    		mCurrentFrame.getState().copyFrom(otherState);
    		mTerm.copyBytes(target);
    		
    		mCurrentFrame.setMetaDataUpto(mCurrentFrame.getTermBlockOrd());
    		assert mCurrentFrame.getMetaDataUpto() > 0;
    		
    		mValidIndexPrefix = 0;
    	}
    }
    
    @Override
    public ITermState getTermState() throws IOException {
    	assert !mEof;
    	mCurrentFrame.decodeMetaData();
    	ITermState ts = mCurrentFrame.getState().clone();
    	
    	return ts;
    }

    @Override
    public void seekExact(long ord) throws IOException {
    	throw new UnsupportedOperationException();
    }

    @Override
    public long getOrd() {
    	throw new UnsupportedOperationException();
    }
    
    @Override
    public String toString() { 
    	return getClass().getSimpleName() + "{hashCode=" + hashCode() 
    			+ ",currentFrame=" + mCurrentFrame + ",termExists=" + mTermExists + "}";
    }
    
}
