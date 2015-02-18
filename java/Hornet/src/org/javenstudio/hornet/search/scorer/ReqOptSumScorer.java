package org.javenstudio.hornet.search.scorer;

import java.io.IOException;

import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.search.Scorer;

/** 
 * A Scorer for queries with a required part and an optional part.
 * Delays skipTo() on the optional part until a score() is needed.
 * <br>
 * This <code>Scorer</code> implements {@link Scorer#advance(int)}.
 */
class ReqOptSumScorer extends Scorer {
	
	/** 
	 * The scorers passed from the constructor.
	 * These are set to null as soon as their next() or skipTo() returns false.
	 */
	private IScorer mReqScorer;
	private IScorer mOptScorer;

	/** 
	 * Construct a <code>ReqOptScorer</code>.
	 * @param reqScorer The required scorer. This must match.
	 * @param optScorer The optional scorer. This is used for scoring only.
	 */
	public ReqOptSumScorer(IScorer reqScorer, IScorer optScorer) {
		super(reqScorer.getWeight());
		mReqScorer = reqScorer;
		mOptScorer = optScorer;
	}

	@Override
	public int nextDoc() throws IOException {
		return mReqScorer.nextDoc();
	}
  
	@Override
	public int advance(int target) throws IOException {
		return mReqScorer.advance(target);
	}
  
	@Override
	public int getDocID() {
		return mReqScorer.getDocID();
	}
  
	/** 
	 * Returns the score of the current document matching the query.
	 * Initially invalid, until {@link #nextDoc()} is called the first time.
	 * @return The score of the required scorer, eventually increased by the score
	 * of the optional scorer when it also matches the current document.
	 */
	@Override
	public float getScore() throws IOException {
		int curDoc = mReqScorer.getDocID();
		float reqScore = mReqScorer.getScore();
		if (mOptScorer == null) 
			return reqScore;
    
		int optScorerDoc = mOptScorer.getDocID();
		if (optScorerDoc < curDoc && (optScorerDoc = mOptScorer.advance(curDoc)) == NO_MORE_DOCS) {
			mOptScorer = null;
			return reqScore;
		}
    
		return optScorerDoc == curDoc ? reqScore + mOptScorer.getScore() : reqScore;
	}

}

