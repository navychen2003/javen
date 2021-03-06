package org.javenstudio.hornet.search.query;

import java.io.IOException;

import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.IWeight;

/**
 * The Scorer for DisjunctionMaxQuery. 
 * The union of all documents generated by the the subquery scorers
 * is generated in document number order.  The score for each document 
 * is the maximum of the scores computed
 * by the subquery scorers that generate that document, 
 * plus tieBreakerMultiplier times the sum of the scores
 * for the other subqueries that generate the document.
 */
public class DisjunctionMaxScorer extends DisjunctionScorer {
	
	/** 
	 * Multiplier applied to non-maximum-scoring subqueries for 
	 * a document as they are summed into the result. 
	 */
	private final float mTieBreakerMultiplier;
	private int mDoc = -1;

	/** Used when scoring currently matching doc. */
	private float mScoreSum;
	private float mScoreMax;

	/**
	 * Creates a new instance of DisjunctionMaxScorer
	 * 
	 * @param weight
	 *          The Weight to be used.
	 * @param tieBreakerMultiplier
	 *          Multiplier applied to non-maximum-scoring subqueries for a
	 *          document as they are summed into the result.
	 * @param subScorers
	 *          The sub scorers this Scorer should iterate on
	 * @param numScorers
	 *          The actual number of scorers to iterate on. Note that the array's
	 *          length may be larger than the actual number of scorers.
	 */
	public DisjunctionMaxScorer(IWeight weight, float tieBreakerMultiplier,
			IScorer[] subScorers, int numScorers) {
		super(weight, subScorers, numScorers);
		mTieBreakerMultiplier = tieBreakerMultiplier;
	}

	@Override
	public int nextDoc() throws IOException {
		if (mNumScorers == 0) 
			return mDoc = NO_MORE_DOCS;
		
		while (mSubScorers[0].getDocID() == mDoc) {
			if (mSubScorers[0].nextDoc() != NO_MORE_DOCS) {
				heapAdjust(0);
				
			} else {
				heapRemoveRoot();
				if (mNumScorers == 0) 
					return mDoc = NO_MORE_DOCS;
			}
		}
    
		return mDoc = mSubScorers[0].getDocID();
	}

	@Override
	public int getDocID() {
		return mDoc;
	}

	/** 
	 * Determine the current document score. 
	 * Initially invalid, until {@link #nextDoc()} is called the first time.
	 * @return the score of the current generated document
	 */
	@Override
	public float getScore() throws IOException {
		int doc = mSubScorers[0].getDocID();
		mScoreSum = mScoreMax = mSubScorers[0].getScore();
		
		int size = mNumScorers;
		
		scoreAll(1, size, doc);
		scoreAll(2, size, doc);
		
		return mScoreMax + (mScoreSum - mScoreMax) * mTieBreakerMultiplier;
	}

	// Recursively iterate all subScorers that generated last doc computing sum and max
	private void scoreAll(int root, int size, int doc) throws IOException {
		if (root < size && mSubScorers[root].getDocID() == doc) {
			float sub = mSubScorers[root].getScore();
			
			mScoreSum += sub;
			mScoreMax = Math.max(mScoreMax, sub);
			
			scoreAll((root<<1)+1, size, doc);
			scoreAll((root<<1)+2, size, doc);
		}
	}

	@Override
	public float getFreq() throws IOException {
		int doc = mSubScorers[0].getDocID();
		int size = mNumScorers;
		
		return 1 + freq(1, size, doc) + freq(2, size, doc);
	}
  
	// Recursively iterate all subScorers that generated last doc computing sum and max
	private int freq(int root, int size, int doc) throws IOException {
		int freq = 0;
		if (root < size && mSubScorers[root].getDocID() == doc) {
			freq ++;
			freq += freq((root<<1)+1, size, doc);
			freq += freq((root<<1)+2, size, doc);
		}
		
		return freq;
	}

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
		
		return mDoc = mSubScorers[0].getDocID();
	}
	
}
