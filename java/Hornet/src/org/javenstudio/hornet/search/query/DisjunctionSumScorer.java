package org.javenstudio.hornet.search.query;

import java.util.List;
import java.io.IOException;

import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.IWeight;

/** 
 * A Scorer for OR like queries, counterpart of <code>ConjunctionScorer</code>.
 * This Scorer implements {@link Scorer#advance(int)} and uses advance() on the given Scorers. 
 */
public class DisjunctionSumScorer extends DisjunctionScorer { 
	
	/** The minimum number of scorers that should match. */
	private final int mMinimumNrMatchers;
  
	/** The document number of the current match. */
	private int mDoc = -1;

	/** The number of subscorers that provide the current match. */
	protected int mNrMatchers = -1;

	private double mScore = Float.NaN;
  
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
		super(weight, subScorers.toArray(new IScorer[subScorers.size()]), 
				subScorers.size());

		if (minimumNrMatchers <= 0) 
			throw new IllegalArgumentException("Minimum nr of matchers must be positive");
		
		if (mNumScorers <= 1) 
			throw new IllegalArgumentException("There must be at least 2 subScorers");
		
		mMinimumNrMatchers = minimumNrMatchers;
	}
  
	/** 
	 * Construct a <code>DisjunctionScorer</code>, using one as the minimum number
	 * of matching subscorers.
	 */
	public DisjunctionSumScorer(IWeight weight, List<IScorer> subScorers) throws IOException {
		this(weight, subScorers, 1);
	}

	@Override
	public int nextDoc() throws IOException {
		assert mDoc != NO_MORE_DOCS;
		
		while (true) {
			while (mSubScorers[0].getDocID() == mDoc) {
				if (mSubScorers[0].nextDoc() != NO_MORE_DOCS) {
					heapAdjust(0);
					
				} else {
					heapRemoveRoot();
					if (mNumScorers < mMinimumNrMatchers) 
						return mDoc = NO_MORE_DOCS;
				}
			}
			
			afterNext();
			if (mNrMatchers >= mMinimumNrMatchers) 
				break;
		}
    
		return mDoc;
	}
  
	private void afterNext() throws IOException {
		final IScorer sub = mSubScorers[0];
		mDoc = sub.getDocID();
		
		if (mDoc == NO_MORE_DOCS) {
			mNrMatchers = Integer.MAX_VALUE; // stop looping
			
		} else {
			mScore = sub.getScore();
			mNrMatchers = 1;
			
			countMatches(1);
			countMatches(2);
		}
	}
  
	// TODO: this currently scores, but so did the previous impl
	// TODO: remove recursion.
	// TODO: if we separate scoring, out of here, modify this
	// and afterNext() to terminate when nrMatchers == minimumNrMatchers
	// then also change freq() to just always compute it from scratch
	private void countMatches(int root) throws IOException {
		if (root < mNumScorers && mSubScorers[root].getDocID() == mDoc) {
			mNrMatchers++;
			mScore += mSubScorers[root].getScore();
			
			countMatches((root<<1)+1);
			countMatches((root<<1)+2);
		}
	}
  
	/** 
	 * Returns the score of the current document matching the query.
	 * Initially invalid, until {@link #nextDoc()} is called the first time.
	 */
	@Override
	public float getScore() throws IOException { 
		return (float)mScore; 
	}
   
	@Override
	public int getDocID() {
		return mDoc;
	}

	@Override
	public float getFreq() throws IOException {
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
		if (mNumScorers == 0) 
			return mDoc = NO_MORE_DOCS;
		
		while (mSubScorers[0].getDocID() < target) {
			if (mSubScorers[0].advance(target) != NO_MORE_DOCS) {
				heapAdjust(0);
				
			} else {
				heapRemoveRoot();
				if (mNumScorers == 0) 
					return mDoc = NO_MORE_DOCS;
			}
		}
    
		afterNext();

		if (mNrMatchers >= mMinimumNrMatchers) 
			return mDoc;
		else 
			return nextDoc();
	}
	
}
