package org.javenstudio.hornet.codec.block;

import java.io.IOException;
import java.util.Comparator;

import org.javenstudio.common.indexdb.IDocsAndPositionsEnum;
import org.javenstudio.common.indexdb.IDocsEnum;
import org.javenstudio.common.indexdb.IIndexInput;
import org.javenstudio.common.indexdb.ITermState;
import org.javenstudio.common.indexdb.IndexOptions;
import org.javenstudio.common.indexdb.index.term.TermsEnum;
import org.javenstudio.common.indexdb.store.IndexInput;
import org.javenstudio.common.indexdb.util.ArrayUtil;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.JvmUtil;
import org.javenstudio.common.indexdb.util.StringHelper;
import org.javenstudio.hornet.store.fst.BytesReader;
import org.javenstudio.hornet.store.fst.FSTArc;

//NOTE: cannot seek!
final class IntersectEnum extends TermsEnum {
	
	private final BlockTreeTermsReader mReader;
	private final BlockFieldReader mFieldReader;
	
	private final IIndexInput mInput;
	private IntersectFrame[] mStack;
    
    @SuppressWarnings({"unchecked"}) 
    private FSTArc<BytesRef>[] mArcs = new FSTArc[5];

    //private final RunAutomaton runAutomaton;
    //private final CompiledAutomaton compiledAutomaton;

    private IntersectFrame mCurrentFrame;
    private BytesRef mSavedStartTerm;

    private final BytesRef mTerm = new BytesRef();
    private final BytesReader mFstReader;

    final BlockTreeTermsReader getReader() { return mReader; }
    final BlockFieldReader getFieldReader() { return mFieldReader; }
    final IIndexInput getInput() { return mInput; }
    
    // TODO: in some cases we can filter by length?  eg
    // regexp foo*bar must be at least length 6 bytes
    public IntersectEnum(BlockTreeTermsReader reader, BlockFieldReader fieldReader, 
    		BytesRef startTerm) throws IOException {
    	//runAutomaton = compiled.runAutomaton;
    	//compiledAutomaton = compiled;
    	mReader = reader;
    	mFieldReader = fieldReader;
    	mInput = (IIndexInput)((IndexInput) mReader.getInput()).clone();
    	mStack = new IntersectFrame[5];
    	
    	for (int idx=0; idx < mStack.length; idx++) {
    		mStack[idx] = new IntersectFrame(this, idx);
    	}
    	for (int arcIdx=0; arcIdx < mArcs.length; arcIdx++) {
    		mArcs[arcIdx] = new FSTArc<BytesRef>();
    	}

    	if (mFieldReader.getIndex() == null) {
    		mFstReader = null;
    	} else {
    		mFstReader = mFieldReader.getIndex().getBytesReader(0);
    	}

    	// TODO: if the automaton is "smallish" we really
    	// should use the terms index to seek at least to
    	// the initial term and likely to subsequent terms
    	// (or, maybe just fallback to ATE for such cases).
    	// Else the seek cost of loading the frames will be
    	// too costly.

    	final FSTArc<BytesRef> arc = mFieldReader.getIndex().getFirstArc(mArcs[0]);
    	// Empty string prefix must have an output in the index!
    	assert arc.isFinal();

    	// Special pushFrame since it's the first one:
    	final IntersectFrame f = mStack[0];
    	f.mFp = f.mFpOrig = mFieldReader.getRootBlockFP();
    	f.mPrefix = 0;
    	f.setState(0); //runAutomaton.getInitialState());
    	f.mArc = arc;
    	f.mOutputPrefix = arc.getOutput();
    	f.load(mFieldReader.getRootCode());

    	// for assert:
    	assert setSavedStartTerm(startTerm);

    	mCurrentFrame = f;
    	if (startTerm != null) 
    		seekToStartTerm(startTerm);
    }

    // only for assert:
    private boolean setSavedStartTerm(BytesRef startTerm) {
    	mSavedStartTerm = startTerm == null ? null : BytesRef.deepCopyOf(startTerm);
    	return true;
    }

    @Override
    public ITermState getTermState() throws IOException {
    	mCurrentFrame.decodeMetaData();
    	return mCurrentFrame.getTermState().clone();
    }

    private IntersectFrame getFrame(int ord) throws IOException {
    	if (ord >= mStack.length) {
    		final IntersectFrame[] next = 
    				new IntersectFrame[ArrayUtil.oversize(1+ord, JvmUtil.NUM_BYTES_OBJECT_REF)];
    		System.arraycopy(mStack, 0, next, 0, mStack.length);
    		for (int stackOrd=mStack.length; stackOrd < next.length; stackOrd++) {
    			next[stackOrd] = new IntersectFrame(this, stackOrd);
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

    private IntersectFrame pushFrame(int state) throws IOException {
    	final IntersectFrame f = getFrame(mCurrentFrame == null ? 0 : 1+mCurrentFrame.getOrd());
      
    	f.mFp = f.mFpOrig = mCurrentFrame.mLastSubFP;
    	f.mPrefix = mCurrentFrame.getPrefix() + mCurrentFrame.getSuffix();
    	f.setState(state);

    	// Walk the arc through the index -- we only
    	// "bother" with this so we can get the floor data
    	// from the index and skip floor blocks when
    	// possible:
    	FSTArc<BytesRef> arc = mCurrentFrame.mArc;
    	int idx = mCurrentFrame.mPrefix;
    	assert mCurrentFrame.getSuffix() > 0;
    	
    	BytesRef output = mCurrentFrame.mOutputPrefix;
    	while (idx < f.mPrefix) {
    		final int target = mTerm.mBytes[idx] & 0xff;
    		// TODO: we could be more efficient for the next()
    		// case by using current arc as starting point,
    		// passed to findTargetArc
    		arc = mFieldReader.getIndex().findTargetArc(target, arc, getArc(1+idx), mFstReader);
    		assert arc != null;
    		output = mReader.getOutputs().add(output, arc.getOutput());
    		idx ++;
    	}

    	f.mArc = arc;
    	f.mOutputPrefix = output;
    	assert arc.isFinal();
    	f.load(mReader.getOutputs().add(output, arc.getNextFinalOutput()));
    	
    	return f;
    }

    @Override
    public BytesRef getTerm() throws IOException {
    	return mTerm;
    }

    @Override
    public int getDocFreq() throws IOException {
    	mCurrentFrame.decodeMetaData();
    	return mCurrentFrame.getTermState().getDocFreq();
    }

    @Override
    public long getTotalTermFreq() throws IOException {
    	mCurrentFrame.decodeMetaData();
    	return mCurrentFrame.getTermState().getTotalTermFreq();
    }

    @Override
    public IDocsEnum getDocs(Bits skipDocs, IDocsEnum reuse, int flags) 
    		throws IOException {
    	mCurrentFrame.decodeMetaData();
    	//if (needsFreqs && mFieldReader.getFieldInfo().getIndexOptions() == IndexOptions.DOCS_ONLY) 
    	//	return null;
    	
    	return mReader.getPostingsReader().getDocs(mFieldReader.getFieldInfo(), 
    			mCurrentFrame.getTermState(), skipDocs, reuse, flags);
    }

    @Override
    public IDocsAndPositionsEnum getDocsAndPositions(Bits skipDocs, 
    		IDocsAndPositionsEnum reuse, int flags) throws IOException {
    	if (mFieldReader.getFieldInfo().getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) < 0) {
    		// Positions were not indexed:
    		return null;
    	}

    	//if (needsOffsets && mFieldReader.getFieldInfo().getIndexOptions().compareTo(
    	//		IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) < 0) {
    	//	// Offsets were not indexed:
    	//	return null;
    	//}

    	mCurrentFrame.decodeMetaData();
    	return mReader.getPostingsReader().getDocsAndPositions(mFieldReader.getFieldInfo(), 
    			mCurrentFrame.getTermState(), skipDocs, reuse, flags);
    }

    private int getState() {
    	int state = mCurrentFrame.getState();
    	for (int idx=0; idx < mCurrentFrame.getSuffix(); idx++) {
    		state = 0; //runAutomaton.step(state,  currentFrame.suffixBytes[currentFrame.startBytePos+idx] & 0xff);
    		assert state != -1;
    	}
    	return state;
    }

    // NOTE: specialized to only doing the first-time
    // seek, but we could generalize it to allow
    // arbitrary seekExact/Ceil.  Note that this is a
    // seekFloor!
    private void seekToStartTerm(BytesRef target) throws IOException {
    	assert mCurrentFrame.getOrd() == 0;
    	if (mTerm.mLength < target.mLength) 
    		mTerm.mBytes = ArrayUtil.grow(mTerm.mBytes, target.mLength);
    	
    	FSTArc<BytesRef> arc = mArcs[0];
    	assert arc == mCurrentFrame.mArc;

    	for (int idx=0; idx <= target.mLength; idx++) {
    		while (true) {
    			final int savePos = mCurrentFrame.getSuffixesReader().getPosition();
    			final int saveStartBytePos = mCurrentFrame.getStartBytePos();
    			final int saveSuffix = mCurrentFrame.getSuffix();
    			final long saveLastSubFP = mCurrentFrame.getLastSubFilePointer();
    			final int saveTermBlockOrd = mCurrentFrame.getTermState().getTermBlockOrd();
    			final boolean isSubBlock = mCurrentFrame.next();

    			mTerm.mLength = mCurrentFrame.getPrefix() + mCurrentFrame.getSuffix();
    			if (mTerm.mBytes.length < mTerm.mLength) 
    				mTerm.mBytes = ArrayUtil.grow(mTerm.mBytes, mTerm.mLength);
    			
    			System.arraycopy(mCurrentFrame.getSuffixBytes(), mCurrentFrame.getStartBytePos(), 
    					mTerm.mBytes, mCurrentFrame.getPrefix(), mCurrentFrame.getSuffix());

    			if (isSubBlock && StringHelper.startsWith(target, mTerm)) {
    				// Recurse
    				mCurrentFrame = pushFrame(getState());
    				break;
    			} else {
    				final int cmp = mTerm.compareTo(target);
    				if (cmp < 0) {
    					if (mCurrentFrame.getNextEntry() == mCurrentFrame.getEntryCount()) {
    						if (!mCurrentFrame.isLastInFloor()) {
    							mCurrentFrame.loadNextFloorBlock();
    							continue;
    						} else 
    							return;
    					}
    					continue;
    					
    				} else if (cmp == 0) {
    					return;
    					
    				} else {
    					// Fallback to prior entry: the semantics of
    					// this method is that the first call to
    					// next() will return the term after the
    					// requested term
    					mCurrentFrame.mNextEnt --;
    					mCurrentFrame.mLastSubFP = saveLastSubFP;
    					mCurrentFrame.mStartBytePos = saveStartBytePos;
    					mCurrentFrame.mSuffix = saveSuffix;
    					mCurrentFrame.getSuffixesReader().setPosition(savePos);
    					mCurrentFrame.getTermState().setTermBlockOrd(saveTermBlockOrd);
    					
    					System.arraycopy(mCurrentFrame.getSuffixBytes(), mCurrentFrame.getStartBytePos(), 
    							mTerm.mBytes, mCurrentFrame.getPrefix(), mCurrentFrame.getSuffix());
    					
    					mTerm.mLength = mCurrentFrame.getPrefix() + mCurrentFrame.getSuffix();
    					// If the last entry was a block we don't
    					// need to bother recursing and pushing to
    					// the last term under it because the first
    					// next() will simply skip the frame anyway
    					
    					return;
    				}
    			}
    		}
    	}

    	assert false;
    }

    @Override
    public BytesRef next() throws IOException {
    	nextTerm:
    		while (true) {
    			// Pop finished frames
    			while (mCurrentFrame.getNextEntry() == mCurrentFrame.getEntryCount()) {
    				if (!mCurrentFrame.isLastInFloor()) {
    					mCurrentFrame.loadNextFloorBlock();
    				} else {
    					if (mCurrentFrame.getOrd() == 0) 
    						return null;
    					
    					final long lastFP = mCurrentFrame.mFpOrig;
    					mCurrentFrame = mStack[mCurrentFrame.getOrd()-1];
    					assert mCurrentFrame.mLastSubFP == lastFP;
    				}
    			}

    			final boolean isSubBlock = mCurrentFrame.next();

    			if (mCurrentFrame.mSuffix != 0) {
    				final int label = mCurrentFrame.mSuffixBytes[mCurrentFrame.mStartBytePos] & 0xff;
    				while (label > mCurrentFrame.getCurTransitionMax()) {
    					if (mCurrentFrame.getTransitionIndex() >= 0) { //currentFrame.transitions.length-1) {
    						// Stop processing this frame -- no further
    						// matches are possible because we've moved
    						// beyond what the max transition will allow
    						
    						// sneaky!  forces a pop above
    						mCurrentFrame.mIsLastInFloor = true;
    						mCurrentFrame.mNextEnt = mCurrentFrame.getEntryCount();
    						continue nextTerm;
    					}
    					mCurrentFrame.mTransitionIndex ++;
    					mCurrentFrame.mCurTransitionMax = 0; //currentFrame.transitions[currentFrame.transitionIndex].getMax();
    				}
    			}
    			
/*
        // First test the common suffix, if set:
        if (compiledAutomaton.commonSuffixRef != null && !isSubBlock) {
          final int termLen = currentFrame.prefix + currentFrame.suffix;
          if (termLen < compiledAutomaton.commonSuffixRef.length) {
            // No match
            continue nextTerm;
          }

          final byte[] suffixBytes = currentFrame.suffixBytes;
          final byte[] commonSuffixBytes = compiledAutomaton.commonSuffixRef.bytes;

          final int lenInPrefix = compiledAutomaton.commonSuffixRef.length - currentFrame.suffix;
          assert compiledAutomaton.commonSuffixRef.offset == 0;
          int suffixBytesPos;
          int commonSuffixBytesPos = 0;

          if (lenInPrefix > 0) {
            // A prefix of the common suffix overlaps with
            // the suffix of the block prefix so we first
            // test whether the prefix part matches:
            final byte[] termBytes = term.mBytes;
            int termBytesPos = currentFrame.prefix - lenInPrefix;
            assert termBytesPos >= 0;
            final int termBytesPosEnd = currentFrame.prefix;
            while (termBytesPos < termBytesPosEnd) {
              if (termBytes[termBytesPos++] != commonSuffixBytes[commonSuffixBytesPos++]) {
                continue nextTerm;
              }
            }
            suffixBytesPos = currentFrame.startBytePos;
          } else {
            suffixBytesPos = currentFrame.startBytePos + currentFrame.suffix - compiledAutomaton.commonSuffixRef.length;
          }

          // Test overlapping suffix part:
          final int commonSuffixBytesPosEnd = compiledAutomaton.commonSuffixRef.length;
          while (commonSuffixBytesPos < commonSuffixBytesPosEnd) {
            if (suffixBytes[suffixBytesPos++] != commonSuffixBytes[commonSuffixBytesPos++]) {
              continue nextTerm;
            }
          }
        }
*/
    	
    		// TODO: maybe we should do the same linear test
    		// that AutomatonTermsEnum does, so that if we
    		// reach a part of the automaton where .* is
    		// "temporarily" accepted, we just blindly .next()
    		// until the limit

    		// See if the term prefix matches the automaton:
    		int state = mCurrentFrame.getState();
	        for (int idx=0; idx < mCurrentFrame.getSuffix(); idx++) {
	        	state = 0; //runAutomaton.step(state,  currentFrame.suffixBytes[currentFrame.startBytePos+idx] & 0xff);
	        	if (state == -1) {
	        		// No match
	        		continue nextTerm;
	        	}
	        }
	
	        if (isSubBlock) {
	        	// Match!  Recurse:
	        	copyTerm();
	        	mCurrentFrame = pushFrame(state);
	        	
	        } else { //if (runAutomaton.isAccept(state)) {
	        	copyTerm();
	        	assert mSavedStartTerm == null || mTerm.compareTo(mSavedStartTerm) > 0: 
	        		"saveStartTerm=" + mSavedStartTerm.utf8ToString() + " term=" + mTerm.utf8ToString();
	        	
	        	return mTerm;
	        }
    	}
    }

    private void copyTerm() {
    	final int len = mCurrentFrame.getPrefix() + mCurrentFrame.getSuffix();
    	if (mTerm.mBytes.length < len) 
    		mTerm.mBytes = ArrayUtil.grow(mTerm.mBytes, len);
    	
    	System.arraycopy(mCurrentFrame.mSuffixBytes, mCurrentFrame.getStartBytePos(), 
    			mTerm.mBytes, mCurrentFrame.getPrefix(), mCurrentFrame.getSuffix());
    	
    	mTerm.mLength = len;
    }

    @Override
    public Comparator<BytesRef> getComparator() {
    	return BytesRef.getUTF8SortedAsUnicodeComparator();
    }

    @Override
    public boolean seekExact(BytesRef text, boolean useCache) throws IOException {
    	throw new UnsupportedOperationException();
    }

    @Override
    public void seekExact(long ord) throws IOException {
    	throw new UnsupportedOperationException();
    }

    @Override
    public long getOrd() throws IOException {
    	throw new UnsupportedOperationException();
    }

    @Override
    public SeekStatus seekCeil(BytesRef text, boolean useCache) throws IOException {
    	throw new UnsupportedOperationException();
    }
    
}
