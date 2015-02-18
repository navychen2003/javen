package org.javenstudio.hornet.index.term;

import java.io.IOException;
import java.util.Comparator;

import org.javenstudio.common.indexdb.IBytesReader;
import org.javenstudio.common.indexdb.IDocsAndPositionsEnum;
import org.javenstudio.common.indexdb.IDocsEnum;
import org.javenstudio.common.indexdb.IIntsReader;
import org.javenstudio.common.indexdb.ITermState;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.index.term.DocTermsIndex;
import org.javenstudio.common.indexdb.index.term.OrdTermState;
import org.javenstudio.common.indexdb.index.term.TermsEnum;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.BytesRef;

public final class DocTermsIndexImpl extends DocTermsIndex {
	
	private final IBytesReader mBytes;
    private final IIntsReader mTermOrdToBytesOffset;
    private final IIntsReader mDocToTermOrd;
    private final int mNumOrd;

    public DocTermsIndexImpl(IBytesReader bytes, IIntsReader termOrdToBytesOffset, 
    		IIntsReader docToTermOrd, int numOrd) {
    	mBytes = bytes;
    	mDocToTermOrd = docToTermOrd;
    	mTermOrdToBytesOffset = termOrdToBytesOffset;
    	mNumOrd = numOrd;
    }

    @Override
    public IIntsReader getDocToOrd() {
    	return mDocToTermOrd;
    }

    @Override
    public int getNumOrd() {
    	return mNumOrd;
    }

    @Override
    public int getOrd(int docID) {
    	return (int) mDocToTermOrd.get(docID);
    }

    @Override
    public int size() {
    	return mDocToTermOrd.size();
    }

    @Override
    public BytesRef lookup(int ord, BytesRef ret) {
    	return mBytes.fill(ret, mTermOrdToBytesOffset.get(ord));
    }

    @Override
    public ITermsEnum getTermsEnum() {
    	return new DocTermsIndexEnum();
    }

    class DocTermsIndexEnum extends TermsEnum {
    	private final BytesRef mTerm = new BytesRef();
    	private final byte[][] mBlocks;
    	private final int[] mBlockEnds;
    	private int mCurrentOrd;
    	private int mCurrentBlockNumber;
    	private int mEnd;  // end position in the current block
    	
    	public DocTermsIndexEnum() {
    		mCurrentOrd = 0;
    		mCurrentBlockNumber = 0;
    		mBlocks = mBytes.getBlocks();
    		mBlockEnds = mBytes.getBlockEnds();
    		mCurrentBlockNumber = mBytes.fillAndGetIndex(mTerm, mTermOrdToBytesOffset.get(0));
    		mEnd = mBlockEnds[mCurrentBlockNumber];
    	}

    	@Override
    	public SeekStatus seekCeil(BytesRef text, boolean useCache /* ignored */) throws IOException {
    		int low = 1;
    		int high = mNumOrd - 1;
        
    		while (low <= high) {
    			int mid = (low + high) >>> 1;
    			seekExact(mid);
    			int cmp = mTerm.compareTo(text);

    			if (cmp < 0)
    				low = mid + 1;
    			else if (cmp > 0)
    				high = mid - 1;
    			else
    				return SeekStatus.FOUND; // key found
    		}
        
    		if (low == mNumOrd) {
    			return SeekStatus.END;
    		} else {
    			seekExact(low);
    			return SeekStatus.NOT_FOUND;
    		}
    	}

    	public void seekExact(long ord) throws IOException {
    		assert(ord >= 0 && ord <= mNumOrd);
    		
    		// TODO: if gap is small, could iterate from current position?  Or let user decide that?
    		mCurrentBlockNumber = mBytes.fillAndGetIndex(mTerm, mTermOrdToBytesOffset.get((int)ord));
    		mEnd = mBlockEnds[mCurrentBlockNumber];
    		mCurrentOrd = (int)ord;
    	}

    	@Override
    	public BytesRef next() throws IOException {
    		int start = mTerm.mOffset + mTerm.mLength;
    		if (start >= mEnd) {
    			// switch byte blocks
    			if (mCurrentBlockNumber +1 >= mBlocks.length) 
    				return null;
    			
    			mCurrentBlockNumber ++;
    			mTerm.mBytes = mBlocks[mCurrentBlockNumber];
    			mEnd = mBlockEnds[mCurrentBlockNumber];
    			start = 0;
    			if (mEnd <= 0) return null;  // special case of empty last array
    		}

    		mCurrentOrd ++;

    		byte[] block = mTerm.mBytes;
    		if ((block[start] & 128) == 0) {
    			mTerm.mLength = block[start];
    			mTerm.mOffset = start+1;
    		} else {
    			mTerm.mLength = (((block[start] & 0x7f)) << 8) | (block[1+start] & 0xff);
    			mTerm.mOffset = start+2;
    		}

    		return mTerm;
    	}

    	@Override
    	public BytesRef getTerm() throws IOException {
    		return mTerm;
    	}

    	@Override
    	public long getOrd() throws IOException {
    		return mCurrentOrd;
    	}

    	@Override
    	public int getDocFreq() {
    		throw new UnsupportedOperationException();
    	}

    	@Override
    	public long getTotalTermFreq() {
    		return -1;
    	}

    	@Override
    	public IDocsEnum getDocs(Bits liveDocs, IDocsEnum reuse, int flags) 
    			throws IOException {
    		throw new UnsupportedOperationException();
    	}

    	@Override
    	public IDocsAndPositionsEnum getDocsAndPositions(Bits liveDocs, 
    			IDocsAndPositionsEnum reuse, int flags) throws IOException {
    		throw new UnsupportedOperationException();
    	}

    	@Override
    	public Comparator<BytesRef> getComparator() {
    		return BytesRef.getUTF8SortedAsUnicodeComparator();
    	}

    	@Override
    	public void seekExact(BytesRef term, ITermState state) throws IOException {
    		assert state != null && state instanceof OrdTermState;
    		this.seekExact(((OrdTermState)state).mOrd);
    	}

    	@Override
    	public ITermState getTermState() throws IOException {
    		OrdTermState state = new OrdTermState();
    		state.mOrd = mCurrentOrd;
    		return state;
    	}
    }
    
}
