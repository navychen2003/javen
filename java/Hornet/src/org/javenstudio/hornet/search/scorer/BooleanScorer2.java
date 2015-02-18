package org.javenstudio.hornet.search.scorer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.javenstudio.common.indexdb.IBooleanClause;
import org.javenstudio.common.indexdb.ICollector;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.IBooleanWeight;
import org.javenstudio.common.indexdb.search.Scorer;

/** 
 * See the description in BooleanScorer.java, comparing
 * BooleanScorer & BooleanScorer2 
 * 
 * An alternative to BooleanScorer that also allows a minimum number
 * of optional scorers that should match.
 * <br>Implements skipTo(), and has no limitations on the numbers of added scorers.
 * <br>Uses ConjunctionScorer, DisjunctionScorer, ReqOptScorer and ReqExclScorer.
 */
public class BooleanScorer2 extends Scorer {
  
	private final List<IScorer> mRequiredScorers;
	private final List<IScorer> mOptionalScorers;
	private final List<IScorer> mProhibitedScorers;

	private class Coordinator {
		private float[] mCoordFactors = null;
		private int mMaxCoord = 0; // to be increased for each non prohibited scorer
		private int mNrMatchers; // to be increased by score() of match counting scorers.
    
		public void init(boolean disableCoord) { // use after all scorers have been added.
			mCoordFactors = new float[mOptionalScorers.size() + mRequiredScorers.size() + 1];
			for (int i = 0; i < mCoordFactors.length; i++) {
				mCoordFactors[i] = disableCoord ? 1.0f : ((IBooleanWeight)mWeight).coord(i, mMaxCoord);
			}
		}
	}

	private final Coordinator mCoordinator;

	/** 
	 * The scorer to which all scoring will be delegated,
	 * except for computing and using the coordination factor.
	 */
	private final IScorer mCountingSumScorer;

	/** The number of optionalScorers that need to match (if there are any) */
	private final int mMinNrShouldMatch;

	private int mDoc = -1;

	/**
	 * Creates a {@link Scorer} with the given similarity and lists of required,
	 * prohibited and optional scorers. In no required scorers are added, at least
	 * one of the optional scorers will have to match during the search.
	 * 
	 * @param weight
	 *          The BooleanWeight to be used.
	 * @param disableCoord
	 *          If this parameter is true, coordination level matching 
	 *          ({@link Similarity#coord(int, int)}) is not used.
	 * @param minNrShouldMatch
	 *          The minimum number of optional added scorers that should match
	 *          during the search. In case no required scorers are added, at least
	 *          one of the optional scorers will have to match during the search.
	 * @param required
	 *          the list of required scorers.
	 * @param prohibited
	 *          the list of prohibited scorers.
	 * @param optional
	 *          the list of optional scorers.
	 */
	public BooleanScorer2(IBooleanWeight weight, boolean disableCoord, int minNrShouldMatch,
			List<IScorer> required, List<IScorer> prohibited, List<IScorer> optional, int maxCoord) 
			throws IOException {
		super(weight);
		if (minNrShouldMatch < 0) 
			throw new IllegalArgumentException("Minimum number of optional scorers should not be negative");
		
		mCoordinator = new Coordinator();
		mMinNrShouldMatch = minNrShouldMatch;
		mCoordinator.mMaxCoord = maxCoord;

		mOptionalScorers = optional;
		mRequiredScorers = required;    
		mProhibitedScorers = prohibited;
    
		mCoordinator.init(disableCoord);
		mCountingSumScorer = makeCountingSumScorer(disableCoord);
	}
  
	/** Count a scorer as a single match. */
	private class SingleMatchScorer extends Scorer {
		private IScorer mScorer;
		private int mLastScoredDoc = -1;
		// Save the score of lastScoredDoc, so that we don't compute it more than
		// once in score().
		private float mLastDocScore = Float.NaN;

		SingleMatchScorer(IScorer scorer) {
			super(scorer.getWeight());
			mScorer = scorer;
		}

		@Override
		public float getScore() throws IOException {
			int doc = getDocID();
			if (doc >= mLastScoredDoc) {
				if (doc > mLastScoredDoc) {
					mLastDocScore = mScorer.getScore();
					mLastScoredDoc = doc;
				}
				mCoordinator.mNrMatchers++;
			}
			return mLastDocScore;
		}

		@Override
		public int getDocID() {
			return mScorer.getDocID();
		}

		@Override
		public int nextDoc() throws IOException {
			return mScorer.nextDoc();
		}

		@Override
		public int advance(int target) throws IOException {
			return mScorer.advance(target);
		}
	}

	private IScorer countingDisjunctionSumScorer(final List<IScorer> scorers,
			int minNrShouldMatch) throws IOException {
		// each scorer from the list counted as a single matcher
		return new DisjunctionSumScorer(mWeight, scorers, minNrShouldMatch) {
				private int lastScoredDoc = -1;
				// Save the score of lastScoredDoc, so that we don't compute it more than
				// once in score().
				private float lastDocScore = Float.NaN;
			
				@Override 
				public float getScore() throws IOException {
					int doc = getDocID();
					if (doc >= lastScoredDoc) {
						if (doc > lastScoredDoc) {
							lastDocScore = super.getScore();
							lastScoredDoc = doc;
						}
						mCoordinator.mNrMatchers += super.mNrMatchers;
					}
					return lastDocScore;
				}
			};
	}

	private IScorer countingConjunctionSumScorer(boolean disableCoord,
			List<IScorer> requiredScorers) throws IOException {
		// each scorer from the list counted as a single matcher
		final int requiredNrMatchers = requiredScorers.size();
		return new ConjunctionScorer(mWeight, disableCoord ? 1.0f : 
					((IBooleanWeight)mWeight).coord(requiredScorers.size(), requiredScorers.size()), 
					requiredScorers) {
				private int mLastScoredDoc = -1;
				// Save the score of lastScoredDoc, so that we don't compute it more than
				// once in score().
				private float mLastDocScore = Float.NaN;
				
				@Override 
				public float getScore() throws IOException {
					int doc = getDocID();
					if (doc >= mLastScoredDoc) {
						if (doc > mLastScoredDoc) {
							mLastDocScore = super.getScore();
							mLastScoredDoc = doc;
						}
						mCoordinator.mNrMatchers += requiredNrMatchers;
					}
					// All scorers match, so defaultSimilarity super.score() always has 1 as
					// the coordination factor.
					// Therefore the sum of the scores of the requiredScorers
					// is used as score.
					return mLastDocScore;
				}
			};
	}

	private IScorer dualConjunctionSumScorer(boolean disableCoord,
			IScorer req1, IScorer req2) throws IOException { // non counting.
		return new ConjunctionScorer(mWeight, disableCoord ? 1.0f : 
						((IBooleanWeight)mWeight).coord(2, 2), req1, req2);
		// All scorers match, so defaultSimilarity always has 1 as
		// the coordination factor.
		// Therefore the sum of the scores of two scorers
		// is used as score.
	}

	/** 
	 * Returns the scorer to be used for match counting and score summing.
	 * Uses requiredScorers, optionalScorers and prohibitedScorers.
	 */
	private IScorer makeCountingSumScorer(boolean disableCoord) throws IOException { 
		// each scorer counted as a single matcher
		return (mRequiredScorers.size() == 0)
				? makeCountingSumScorerNoReq(disableCoord)
				: makeCountingSumScorerSomeReq(disableCoord);
	}

	private IScorer makeCountingSumScorerNoReq(boolean disableCoord) throws IOException { 
		// No required scorers
		// minNrShouldMatch optional scorers are required, but at least 1
		int nrOptRequired = (mMinNrShouldMatch < 1) ? 1 : mMinNrShouldMatch;
		IScorer requiredCountingSumScorer;
		
		if (mOptionalScorers.size() > nrOptRequired) {
			requiredCountingSumScorer = countingDisjunctionSumScorer(mOptionalScorers, nrOptRequired);
		} else if (mOptionalScorers.size() == 1) {
			requiredCountingSumScorer = new SingleMatchScorer(mOptionalScorers.get(0));
		} else {
			requiredCountingSumScorer = countingConjunctionSumScorer(disableCoord, mOptionalScorers);
		}
		
		return addProhibitedScorers(requiredCountingSumScorer);
	}

	private IScorer makeCountingSumScorerSomeReq(boolean disableCoord) throws IOException { 
		// At least one required scorer.
		if (mOptionalScorers.size() == mMinNrShouldMatch) { // all optional scorers also required.
			ArrayList<IScorer> allReq = new ArrayList<IScorer>(mRequiredScorers);
			allReq.addAll(mOptionalScorers);
			return addProhibitedScorers(countingConjunctionSumScorer(disableCoord, allReq));
			
		} else { // optionalScorers.size() > minNrShouldMatch, and at least one required scorer
			IScorer requiredCountingSumScorer = (mRequiredScorers.size() == 1)
					? new SingleMatchScorer(mRequiredScorers.get(0))
            		: countingConjunctionSumScorer(disableCoord, mRequiredScorers);
			if (mMinNrShouldMatch > 0) { // use a required disjunction scorer over the optional scorers
				return addProhibitedScorers( 
						dualConjunctionSumScorer( // non counting
								disableCoord, requiredCountingSumScorer,
								countingDisjunctionSumScorer(mOptionalScorers, mMinNrShouldMatch)));
			} else { // minNrShouldMatch == 0
				return new ReqOptSumScorer(
						addProhibitedScorers(requiredCountingSumScorer), 
							(mOptionalScorers.size() == 1)
							? new SingleMatchScorer(mOptionalScorers.get(0))
							// require 1 in combined, optional scorer.
							: countingDisjunctionSumScorer(mOptionalScorers, 1));
			}
		}
	}
  
	/** 
	 * Returns the scorer to be used for match counting and score summing.
	 * Uses the given required scorer and the prohibitedScorers.
	 * @param requiredCountingSumScorer A required scorer already built.
	 */
	private IScorer addProhibitedScorers(IScorer requiredCountingSumScorer) throws IOException {
		return (mProhibitedScorers.size() == 0)
				? requiredCountingSumScorer // no prohibited
				: new ReqExclScorer(requiredCountingSumScorer,
						((mProhibitedScorers.size() == 1)
                                ? mProhibitedScorers.get(0)
                                : new DisjunctionSumScorer(mWeight, mProhibitedScorers)));
	}

	/** 
	 * Scores and collects all matching documents.
	 * @param collector The collector to which all matching documents are passed through.
	 */
	@Override
	public void score(ICollector collector) throws IOException {
		collector.setScorer(this);
		while ((mDoc = mCountingSumScorer.nextDoc()) != NO_MORE_DOCS) {
			collector.collect(mDoc);
		}
	}
  
	@Override
	public boolean score(ICollector collector, int max, int firstDocID) throws IOException {
		mDoc = firstDocID;
		collector.setScorer(this);
		while (mDoc < max) {
			collector.collect(mDoc);
			mDoc = mCountingSumScorer.nextDoc();
		}
		return mDoc != NO_MORE_DOCS;
	}

	@Override
	public int getDocID() { return mDoc; }
  
	@Override
	public int nextDoc() throws IOException {
		return mDoc = mCountingSumScorer.nextDoc();
	}
  
	@Override
	public float getScore() throws IOException {
		mCoordinator.mNrMatchers = 0;
		float sum = mCountingSumScorer.getScore();
		return sum * mCoordinator.mCoordFactors[mCoordinator.mNrMatchers];
	}

	@Override
	public float getFreq() {
		return mCoordinator.mNrMatchers;
	}

	@Override
	public int advance(int target) throws IOException {
		return mDoc = mCountingSumScorer.advance(target);
	}

	@Override
	public Collection<IChild> getChildren() {
		ArrayList<IChild> children = new ArrayList<IChild>();
		for (IScorer s : mOptionalScorers) {
			children.add(new ChildScorer((Scorer)s, 
					IBooleanClause.Occur.SHOULD.toString()));
		}
		for (IScorer s : mProhibitedScorers) {
			children.add(new ChildScorer((Scorer)s, 
					IBooleanClause.Occur.MUST_NOT.toString()));
		}
		for (IScorer s : mRequiredScorers) {
			children.add(new ChildScorer((Scorer)s, 
					IBooleanClause.Occur.MUST.toString()));
		}
		return children;
	}
	
}
