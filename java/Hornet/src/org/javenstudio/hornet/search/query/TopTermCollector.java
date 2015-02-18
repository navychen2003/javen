package org.javenstudio.hornet.search.query;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.index.term.TermState;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.index.term.TermContext;

public class TopTermCollector extends TermCollector {

	private final MaxNonCompetitiveBoost mMaxBoost = 
			new MaxNonCompetitiveBoost();

	private final Map<BytesRef,ScoreTerm> mVisitedTerms = 
			new HashMap<BytesRef,ScoreTerm>();

	private final Queue<ScoreTerm> mQueue;
	private final int mMaxSize;
	
	private ITermsEnum mTermsEnum;
	private Comparator<BytesRef> mTermComp;
	private ScoreTerm mScoreTerm;

	public TopTermCollector(Queue<ScoreTerm> stQueue, int maxSize) {
		mQueue = stQueue; 
		mMaxSize = maxSize;
	}
	
	@Override
	public void setNextEnum(ITermsEnum termsEnum) {
		mTermsEnum = termsEnum;
		mTermComp = termsEnum.getComparator();

		assert compareToLastTerm(null);

		// lazy init the initial ScoreTerm because comparator is not known on ctor:
		if (mScoreTerm == null)
			mScoreTerm = new ScoreTerm(mTermComp, new TermContext(getTopReaderContext()));
	}

	// for assert:
	private BytesRef mLastTerm = null;
	private boolean compareToLastTerm(BytesRef t) {
		if (mLastTerm == null && t != null) {
			mLastTerm = BytesRef.deepCopyOf(t);
		} else if (t == null) {
			mLastTerm = null;
		} else {
			assert mTermsEnum.getComparator().compare(mLastTerm, t) < 0: 
				"lastTerm=" + mLastTerm + " t=" + t;
			mLastTerm.copyBytes(t);
		}
		return true;
	}

	@Override
	public boolean collect(BytesRef bytes) throws IOException {
		final float boost = mTermsEnum.getBoost();

		// make sure within a single seg we always collect
		// terms in order
		assert compareToLastTerm(bytes);

		// ignore uncompetitive hits
		if (mQueue.size() == mMaxSize) {
			final ScoreTerm t = mQueue.peek();
			if (boost < t.getBoost())
				return true;
			if (boost == t.getBoost() && mTermComp.compare(bytes, t.getBytes()) > 0)
				return true;
		}
		
		ScoreTerm t = mVisitedTerms.get(bytes);
		final TermState state = (TermState)mTermsEnum.getTermState();
		assert state != null;
		
		if (t != null) {
			// if the term is already in the PQ, only update docFreq of term in PQ
			assert t.getBoost() == boost : "boost should be equal in all segment TermsEnums";
			t.getTermState().register(state, getReaderContext().getOrd(), 
					mTermsEnum.getDocFreq(), mTermsEnum.getTotalTermFreq());
			
		} else {
			// add new entry in PQ, we must clone the term, else it may get overwritten!
			mScoreTerm.getBytes().copyBytes(bytes);
			mScoreTerm.setBoost(boost);
			mVisitedTerms.put(mScoreTerm.getBytes(), mScoreTerm);
			assert mScoreTerm.getTermState().getDocFreq() == 0;
			
			mScoreTerm.getTermState().register(state, getReaderContext().getOrd(), 
					mTermsEnum.getDocFreq(), mTermsEnum.getTotalTermFreq());
			
			mQueue.offer(mScoreTerm);
			
			// possibly drop entries from queue
			if (mQueue.size() > mMaxSize) {
				mScoreTerm = mQueue.poll();
				mVisitedTerms.remove(mScoreTerm.getBytes());
				mScoreTerm.getTermState().clear(); // reset the termstate! 
				
			} else {
				mScoreTerm = new ScoreTerm(mTermComp, new TermContext(getTopReaderContext()));
			}
			
			assert mQueue.size() <= mMaxSize : "the PQ size must be limited to maxSize";
			
			// set maxBoostAtt with values to help FuzzyTermsEnum to optimize
			if (mQueue.size() == mMaxSize) {
				t = mQueue.peek();
				mMaxBoost.setMaxNonCompetitiveBoost(t.getBoost());
				mMaxBoost.setCompetitiveTerm(t.getBytes());
			}
		}

		return true;
	}
	
}
