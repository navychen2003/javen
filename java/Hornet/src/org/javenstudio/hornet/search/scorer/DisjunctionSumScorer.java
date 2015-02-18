package org.javenstudio.hornet.search.scorer;

import java.util.List;
import java.io.IOException;

import org.javenstudio.common.indexdb.ICollector;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.IWeight;
import org.javenstudio.common.indexdb.search.Scorer;
import org.javenstudio.common.indexdb.search.ScorerDocQueue;

/** 
 * A Scorer for OR like queries, counterpart of <code>ConjunctionScorer</code>.
 * This Scorer implements {@link Scorer#advance(int)} and uses advance() on the given Scorers. 
 */
class DisjunctionSumScorer extends Scorer {
	
	/** The number of subscorers. */ 
	private final int mNrScorers;
  
	/** The subscorers. */
	protected final List<IScorer> mSubScorers;
  
	/** The minimum number of scorers that should match. */
	private final int mMinimumNrMatchers;
  
	/** 
	 * The scorerDocQueue contains all subscorers ordered by their current doc(),
	 * with the minimum at the top.
	 * <br>The scorerDocQueue is initialized the first time nextDoc() or advance() is called.
	 * <br>An exhausted scorer is immediately removed from the scorerDocQueue.
	 * <br>If less than the minimumNrMatchers scorers
	 * remain in the scorerDocQueue nextDoc() and advance() return false.
	 * <p>
	 * After each to call to nextDoc() or advance()
	 * <code>currentSumScore</code> is the total score of the current matching doc,
	 * <code>nrMatchers</code> is the number of matching scorers,
	 * and all scorers are after the matching doc, or are exhausted.
	 */
	private final ScorerDocQueue mScorerDocQueue;
  
	/** The document number of the current match. */
	private int mCurrentDoc = -1;

	/** The number of subscorers that provide the current match. */
	protected int mNrMatchers = -1;

	private double mCurrentScore = Float.NaN;
  
	/** 
	 * Construct a <code>DisjunctionScorer</code>.
	 * @param weight The weight to be used.
	 * @param subScorers A collection of at least two subscorers.
	 * @param minimumNrMatchers The positive minimum number of subscorers that should
	 * match to match this query.
	 * <br>When <code>minimumNrMatchers</code> is bigger than
	 * the number of <code>subScorers</code>,
	 * no matches will be produced.
	 * <br>When minimumNrMatchers equals the number of subScorers,
	 * it more efficient to use <code>ConjunctionScorer</code>.
	 */
	public DisjunctionSumScorer(IWeight weight, List<IScorer> subScorers, 
			int minimumNrMatchers) throws IOException {
		super(weight);
    
		mNrScorers = subScorers.size();

		if (minimumNrMatchers <= 0) 
			throw new IllegalArgumentException("Minimum nr of matchers must be positive");
		
		if (mNrScorers <= 1) 
			throw new IllegalArgumentException("There must be at least 2 subScorers");

		mMinimumNrMatchers = minimumNrMatchers;
		mSubScorers = subScorers;

		mScorerDocQueue  = initScorerDocQueue();
	}
  
	/** 
	 * Construct a <code>DisjunctionScorer</code>, using one as the minimum number
	 * of matching subscorers.
	 */
	public DisjunctionSumScorer(IWeight weight, List<IScorer> subScorers) throws IOException {
		this(weight, subScorers, 1);
	}

	/** 
	 * Called the first time nextDoc() or advance() is called to
	 * initialize <code>scorerDocQueue</code>.
	 * @return 
	 */
	private ScorerDocQueue initScorerDocQueue() throws IOException {
		final ScorerDocQueue docQueue = new ScorerDocQueue(mNrScorers);
		for (final IScorer se : mSubScorers) {
			if (se.nextDoc() != NO_MORE_DOCS) 
				docQueue.insert(se);
		}
		return docQueue; 
	}

	/** 
	 * Scores and collects all matching documents.
	 * @param collector The collector to which all matching documents are passed through.
	 */
	@Override
	public void score(ICollector collector) throws IOException {
		collector.setScorer(this);
		while (nextDoc() != NO_MORE_DOCS) {
			collector.collect(mCurrentDoc);
		}
	}

	/** 
	 * Expert: Collects matching documents in a range.  Hook for optimization.
	 * Note that {@link #nextDoc()} must be called once before this method is called
	 * for the first time.
	 * @param collector The collector to which all matching documents are passed through.
	 * @param max Do not score documents past this.
	 * @return true if more matching documents may remain.
	 */
	@Override
	public boolean score(ICollector collector, int max, int firstDocID) throws IOException {
		// firstDocID is ignored since nextDoc() sets 'currentDoc'
		collector.setScorer(this);
		while (mCurrentDoc < max) {
			collector.collect(mCurrentDoc);
			if (nextDoc() == NO_MORE_DOCS) 
				return false;
		}
		return true;
	}

	@Override
	public int nextDoc() throws IOException {
		if (mScorerDocQueue.size() < mMinimumNrMatchers || !advanceAfterCurrent()) 
			mCurrentDoc = NO_MORE_DOCS;
		
		return mCurrentDoc;
	}

	/** 
	 * Advance all subscorers after the current document determined by the
	 * top of the <code>scorerDocQueue</code>.
	 * Repeat until at least the minimum number of subscorers match on the same
	 * document and all subscorers are after that document or are exhausted.
	 * <br>On entry the <code>scorerDocQueue</code> has at least <code>minimumNrMatchers</code>
	 * available. At least the scorer with the minimum document number will be advanced.
	 * @return true iff there is a match.
	 * <br>In case there is a match, </code>currentDoc</code>, </code>currentSumScore</code>,
	 * and </code>nrMatchers</code> describe the match.
	 *
	 * TODO: Investigate whether it is possible to use advance() when
	 * the minimum number of matchers is bigger than one, ie. try and use the
	 * character of ConjunctionScorer for the minimum number of matchers.
	 * Also delay calling score() on the sub scorers until the minimum number of
	 * matchers is reached.
	 * <br>For this, a Scorer array with minimumNrMatchers elements might
	 * hold Scorers at currentDoc that are temporarily popped from scorerQueue.
	 */
	protected boolean advanceAfterCurrent() throws IOException {
		do { // repeat until minimum nr of matchers
			mCurrentDoc = mScorerDocQueue.topDoc();
			mCurrentScore = mScorerDocQueue.topScore();
			mNrMatchers = 1;
			
			do { // Until all subscorers are after currentDoc
				if (!mScorerDocQueue.topNextAndAdjustElsePop()) {
					if (mScorerDocQueue.size() == 0) 
						break; // nothing more to advance, check for last match.
				}
				if (mScorerDocQueue.topDoc() != mCurrentDoc) 
					break; // All remaining subscorers are after currentDoc.
				
				mCurrentScore += mScorerDocQueue.topScore();
				mNrMatchers ++;
			} while (true);
      
			if (mNrMatchers >= mMinimumNrMatchers) 
				return true;
			else if (mScorerDocQueue.size() < mMinimumNrMatchers) 
				return false;
			
		} while (true);
	}
  
	/** 
	 * Returns the score of the current document matching the query.
	 * Initially invalid, until {@link #nextDoc()} is called the first time.
	 */
	@Override
	public float getScore() throws IOException { 
		return (float)mCurrentScore; 
	}
   
	@Override
	public int getDocID() {
		return mCurrentDoc;
	}
  
	/** 
	 * Returns the number of subscorers matching the current document.
	 * Initially invalid, until {@link #nextDoc()} is called the first time.
	 */
	public int nrMatchers() {
		return mNrMatchers;
	}

	/**
	 * Advances to the first match beyond the current whose document number is
	 * greater than or equal to a given target. <br>
	 * The implementation uses the advance() method on the subscorers.
	 * 
	 * @param target
	 *          The target document number.
	 * @return the document whose number is greater than or equal to the given
	 *         target, or -1 if none exist.
	 */
	@Override
	public int advance(int target) throws IOException {
		if (mScorerDocQueue.size() < mMinimumNrMatchers) 
			return mCurrentDoc = NO_MORE_DOCS;
		
		if (target <= mCurrentDoc) 
			return mCurrentDoc;
		
		do {
			if (mScorerDocQueue.topDoc() >= target) {
				return advanceAfterCurrent() ? mCurrentDoc : (mCurrentDoc = NO_MORE_DOCS);
			} else if (!mScorerDocQueue.topSkipToAndAdjustElsePop(target)) {
				if (mScorerDocQueue.size() < mMinimumNrMatchers) 
					return mCurrentDoc = NO_MORE_DOCS;
			}
		} while (true);
	}
	
}
