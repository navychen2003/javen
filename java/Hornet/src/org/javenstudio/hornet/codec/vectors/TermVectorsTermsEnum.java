package org.javenstudio.hornet.codec.vectors;

import java.io.IOException;
import java.util.Comparator;

import org.javenstudio.common.indexdb.IDocsAndPositionsEnum;
import org.javenstudio.common.indexdb.IDocsEnum;
import org.javenstudio.common.indexdb.IIndexInput;
import org.javenstudio.common.indexdb.index.term.TermsEnum;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.BytesRef;

final class TermVectorsTermsEnum extends TermsEnum {
	
	private final StoredTermVectorsReader mReader;
	
    private final IIndexInput mOrigTvfIn;
    private final IIndexInput mTvfIn;
    
    private BytesRef mLastTerm = new BytesRef();
    private BytesRef mTerm = new BytesRef();
    
    private boolean mStorePositions;
    private boolean mStoreOffsets;
    private boolean mStorePayloads;
    
    private long mTvfFP;
    private int mNumTerms;
    private int mNextTerm;
    private int mFreq;

    private int[] mPositions;
    private int[] mStartOffsets;
    private int[] mEndOffsets;
    
    // one shared byte[] for any term's payloads
    private int[] mPayloadOffsets;
    private int mLastPayloadLength;
    private byte[] mPayloadData;

    // NOTE: tvf is pre-positioned by caller
    public TermVectorsTermsEnum(StoredTermVectorsReader reader) {
    	mReader = reader;
    	mOrigTvfIn = mReader.getTvfStream();
    	mTvfIn = mOrigTvfIn.clone();
    }

    public boolean canReuse(IIndexInput tvf) {
    	return tvf == mOrigTvfIn;
    }

    public void reset(int numTerms, long tvfFPStart, 
    		boolean storePositions, boolean storeOffsets, boolean storePayloads) 
    		throws IOException {
    	mNumTerms = numTerms;
    	mStorePositions = storePositions;
    	mStoreOffsets = storeOffsets;
    	mStorePayloads = storePayloads;
    	mNextTerm = 0;
    	mTvfIn.seek(tvfFPStart);
    	mTvfFP = 1+tvfFPStart;
    	mPositions = null;
    	mStartOffsets = null;
    	mEndOffsets = null;
    	mPayloadOffsets = null;
    	mPayloadData = null;
    	mLastPayloadLength = -1;
    }

    // NOTE: slow!  (linear scan)
    @Override
    public SeekStatus seekCeil(BytesRef text, boolean useCache)
    		throws IOException {
    	if (mNextTerm != 0) {
    		final int cmp = text.compareTo(mTerm);
    		if (cmp < 0) {
    			mNextTerm = 0;
    			mTvfIn.seek(mTvfFP);
    		} else if (cmp == 0) {
    			return SeekStatus.FOUND;
    		}
    	}

    	while (next() != null) {
    		final int cmp = text.compareTo(mTerm);
    		if (cmp < 0) {
    			return SeekStatus.NOT_FOUND;
    		} else if (cmp == 0) {
    			return SeekStatus.FOUND;
    		}
    	}

    	return SeekStatus.END;
    }

    @Override
    public void seekExact(long ord) {
    	throw new UnsupportedOperationException();
    }

    @Override
    public BytesRef next() throws IOException {
    	if (mNextTerm >= mNumTerms) 
    		return null;
    	
    	mTerm.copyBytes(mLastTerm);
    	
    	final int start = mTvfIn.readVInt();
    	final int deltaLen = mTvfIn.readVInt();
    	
    	mTerm.mLength = start + deltaLen;
    	mTerm.grow(mTerm.mLength);
    	
    	mTvfIn.readBytes(mTerm.mBytes, start, deltaLen);
    	mFreq = mTvfIn.readVInt();

    	if (mStorePayloads) {
    		mPositions = new int[mFreq];
    		mPayloadOffsets = new int[mFreq];
    		
    		int totalPayloadLength = 0;
    		int pos = 0;
    		
    		for (int posUpto=0; posUpto < mFreq; posUpto++) {
    			int code = mTvfIn.readVInt();
    			pos += code >>> 1;
    		
    			mPositions[posUpto] = pos;
    			if ((code & 1) != 0) {
    				// length change
    				mLastPayloadLength = mTvfIn.readVInt();
    			}
    			
    			mPayloadOffsets[posUpto] = totalPayloadLength;
    			totalPayloadLength += mLastPayloadLength;
    			assert totalPayloadLength >= 0;
    		}
    		
    		mPayloadData = new byte[totalPayloadLength];
    		mTvfIn.readBytes(mPayloadData, 0, mPayloadData.length);
    		
    	} else if (mStorePositions) { // no payloads
    		// TODO: we could maybe reuse last array, if we can
    		// somehow be careful about consumer never using two
    		// D&PEnums at once...
    		mPositions = new int[mFreq];
    		int pos = 0;
    		
    		for (int posUpto=0; posUpto < mFreq; posUpto++) {
    			pos += mTvfIn.readVInt();
    			mPositions[posUpto] = pos;
    		}
    	}

    	if (mStoreOffsets) {
    		mStartOffsets = new int[mFreq];
    		mEndOffsets = new int[mFreq];
    		
    		int offset = 0;
    		
    		for (int posUpto=0; posUpto < mFreq; posUpto++) {
    			mStartOffsets[posUpto] = offset + mTvfIn.readVInt();
    			offset = mEndOffsets[posUpto] = mStartOffsets[posUpto] + mTvfIn.readVInt();
    		}
    	}

    	mLastTerm.copyBytes(mTerm);
    	mNextTerm ++;
    	
    	return mTerm;
    }

    @Override
    public BytesRef getTerm() {
    	return mTerm;
    }

    @Override
    public long getOrd() {
    	throw new UnsupportedOperationException();
    }

    @Override
    public int getDocFreq() {
    	return 1;
    }

    @Override
    public long getTotalTermFreq() {
    	return mFreq;
    }

    @Override
    public IDocsEnum getDocs(Bits liveDocs, IDocsEnum reuse, int flags) throws IOException {
    	final TermVectorsDocsEnum docsEnum;
    	if (reuse != null && reuse instanceof TermVectorsDocsEnum) {
    		docsEnum = (TermVectorsDocsEnum) reuse;
    	} else {
    		docsEnum = new TermVectorsDocsEnum();
    	}
    	
    	docsEnum.reset(liveDocs, mFreq);
    	
    	return docsEnum;
    }

    @Override
    public IDocsAndPositionsEnum getDocsAndPositions(Bits liveDocs, 
    		IDocsAndPositionsEnum reuse, int flags) throws IOException {
      if (!mStorePositions && !mStoreOffsets) 
    	  return null;
      
      final TermVectorsDocsAndPositionsEnum docsAndPositionsEnum;
      if (reuse != null && reuse instanceof TermVectorsDocsAndPositionsEnum) {
    	  docsAndPositionsEnum = (TermVectorsDocsAndPositionsEnum) reuse;
      } else {
    	  docsAndPositionsEnum = new TermVectorsDocsAndPositionsEnum();
      }
      
      docsAndPositionsEnum.reset(liveDocs, mPositions, 
    		  mStartOffsets, mEndOffsets, mPayloadOffsets, mPayloadData);
      
      return docsAndPositionsEnum;
    }

    @Override
    public Comparator<BytesRef> getComparator() {
    	return BytesRef.getUTF8SortedAsUnicodeComparator();
    }
    
}
