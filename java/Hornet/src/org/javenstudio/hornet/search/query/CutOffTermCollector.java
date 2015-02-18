package org.javenstudio.hornet.search.query;

import java.io.IOException;

import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.index.term.TermState;
import org.javenstudio.common.indexdb.util.BytePool;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.BytesRefHash;
import org.javenstudio.hornet.index.term.TermContext;

public class CutOffTermCollector extends TermCollector {
	
	private final TermStateByteStart mArray = new TermStateByteStart(16);
    private final BytesRefHash mPendingTerms = new BytesRefHash(
    		new BytePool(new BytePool.DirectAllocator()), 16, mArray);
    private final int mDocCountCutoff, mTermCountLimit;
    
    private int mDocVisitCount = 0;
    private boolean mHasCutOff = false;
    private ITermsEnum mTermsEnum;

	public CutOffTermCollector(int docCountCutoff, int termCountLimit) {
		mDocCountCutoff = docCountCutoff;
		mTermCountLimit = termCountLimit;
	}

	public final BytesRefHash getPendingTerms() { return mPendingTerms; }
	public final TermStateByteStart getTermArray() { return mArray; }
	public final ITermsEnum getTermsEnum() { return mTermsEnum; }
	public final boolean hasCutOff() { return mHasCutOff; }
	
	@Override
	public void setNextEnum(ITermsEnum termsEnum) {
		mTermsEnum = termsEnum;
	}
  
	@Override
	public boolean collect(BytesRef bytes) throws IOException {
		int pos = mPendingTerms.add(bytes);
		mDocVisitCount += mTermsEnum.getDocFreq();
		if (mPendingTerms.size() >= mTermCountLimit || mDocVisitCount >= mDocCountCutoff) {
			mHasCutOff = true;
			return false;
		}
  
		final TermState termState = (TermState)mTermsEnum.getTermState();
		assert termState != null;
		
		if (pos < 0) {
			pos = (-pos)-1;
			mArray.getTermStateAt(pos).register(termState, getReaderContext().getOrd(), 
					mTermsEnum.getDocFreq(), mTermsEnum.getTotalTermFreq());
		} else {
			mArray.setTermStateAt(pos, new TermContext(getTopReaderContext(), termState, getReaderContext().getOrd(), 
					mTermsEnum.getDocFreq(), mTermsEnum.getTotalTermFreq()));
		}
		
		return true;
	}
	
}
