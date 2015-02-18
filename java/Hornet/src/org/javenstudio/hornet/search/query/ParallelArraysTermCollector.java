package org.javenstudio.hornet.search.query;

import java.io.IOException;

import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.index.term.TermState;
import org.javenstudio.common.indexdb.util.BytePool;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.BytesRefHash;
import org.javenstudio.hornet.index.term.TermContext;

public class ParallelArraysTermCollector<Q extends IQuery> extends TermCollector {
	
	private final TermFreqBoostByteStart mArray = new TermFreqBoostByteStart(16);
	private final BytesRefHash mTerms = new BytesRefHash(
			new BytePool(new BytePool.DirectAllocator()), 16, mArray);
	
	private final ScoringRewrite<Q> mScoringRewrite;
	private ITermsEnum mTermsEnum;

	public ParallelArraysTermCollector(ScoringRewrite<Q> rewrite) { 
		mScoringRewrite = rewrite;
	}
	
	public final BytesRefHash getTerms() { return mTerms; }
	public final ITermsEnum getTermsEnum() { return mTermsEnum; }
	public final TermFreqBoostByteStart getTermArray() { return mArray; }
	
	@Override
	public void setNextEnum(ITermsEnum termsEnum) {
		mTermsEnum = termsEnum;
	}

	@Override
	public boolean collect(BytesRef bytes) throws IOException {
		final int e = mTerms.add(bytes);
		final TermState state = (TermState)mTermsEnum.getTermState();
		assert state != null; 
		
		if (e < 0 ) {
			// duplicate term: update docFreq
			final int pos = (-e)-1;
			mArray.getTermStateAt(pos).register(state, getReaderContext().getOrd(), 
					mTermsEnum.getDocFreq(), mTermsEnum.getTotalTermFreq());
			assert mArray.getBoostAt(pos) == mTermsEnum.getBoost() : "boost should be equal in all segment TermsEnums";
			
		} else {
			// new entry: we populate the entry initially
			mArray.setBoostAt(e, mTermsEnum.getBoost());
			mArray.setTermStateAt(e, new TermContext(getTopReaderContext(), state, getReaderContext().getOrd(), 
					mTermsEnum.getDocFreq(), mTermsEnum.getTotalTermFreq()));
			mScoringRewrite.checkMaxClauseCount(mTerms.size());
		}
		
		return true;
	}
	
}
