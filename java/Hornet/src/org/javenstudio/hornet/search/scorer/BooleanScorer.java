package org.javenstudio.hornet.search.scorer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IBooleanClause;
import org.javenstudio.common.indexdb.IBooleanWeight;
import org.javenstudio.common.indexdb.ICollector;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.IWeight;
import org.javenstudio.common.indexdb.search.Collector;
import org.javenstudio.common.indexdb.search.Scorer;

/** 
 * Description from Doug Cutting (excerpted from
 * LUCENE-1483):
 *
 * BooleanScorer uses an array to score windows of
 * 2K docs. So it scores docs 0-2K first, then docs 2K-4K,
 * etc. For each window it iterates through all query terms
 * and accumulates a score in table[doc%2K]. It also stores
 * in the table a bitmask representing which terms
 * contributed to the score. Non-zero scores are chained in
 * a linked list. At the end of scoring each window it then
 * iterates through the linked list and, if the bitmask
 * matches the boolean constraints, collects a hit. For
 * boolean queries with lots of frequent terms this can be
 * much faster, since it does not need to update a priority
 * queue for each posting, instead performing constant-time
 * operations per posting. The only downside is that it
 * results in hits being delivered out-of-order within the
 * window, which means it cannot be nested within other
 * scorers. But it works well as a top-level scorer.
 *
 * The new BooleanScorer2 implementation instead works by
 * merging priority queues of postings, albeit with some
 * clever tricks. For example, a pure conjunction (all terms
 * required) does not require a priority queue. Instead it
 * sorts the posting streams at the start, then repeatedly
 * skips the first to to the last. If the first ever equals
 * the last, then there's a hit. When some terms are
 * required and some terms are optional, the conjunction can
 * be evaluated first, then the optional terms can all skip
 * to the match and be added to the score. Thus the
 * conjunction can reduce the number of priority queue
 * updates for the optional terms. 
 */
public final class BooleanScorer extends Scorer {
  
	private static final class BooleanScorerCollector extends Collector {
		private BucketTable mBucketTable;
		private int mMask;
		private IScorer mScorer;
    
		public BooleanScorerCollector(int mask, BucketTable bucketTable) {
			mMask = mask;
			mBucketTable = bucketTable;
		}
    
		@Override
		public void collect(final int doc) throws IOException {
			final BucketTable table = mBucketTable;
			final int i = doc & BucketTable.MASK;
			final Bucket bucket = table.mBuckets[i];
      
			if (bucket.mDoc != doc) {           		// invalid bucket
				bucket.mDoc = doc;                		// set doc
				bucket.mScore = mScorer.getScore();  	// initialize score
				bucket.mBits = mMask;              		// initialize mask
				bucket.mCoord = 1;                  	// initialize coord
				bucket.mNext = table.mFirst;          	// push onto valid list
				table.mFirst = bucket;
			} else {                               		// valid bucket
				bucket.mScore += mScorer.getScore();  	// increment score
				bucket.mBits |= mMask;               	// add bits in mask
				bucket.mCoord ++;                     	// increment coord
			}
		}
    
		@Override
		public void setNextReader(IAtomicReaderRef context) {
			// not needed by this implementation
		}
    
		@Override
		public void setScorer(IScorer scorer) throws IOException {
			mScorer = scorer;
		}
    
		@Override
		public boolean acceptsDocsOutOfOrder() {
			return true;
		}
	}
  
	// An internal class which is used in score(Collector, int) for setting the
	// current score. This is required since Collector exposes a setScorer method
	// and implementations that need the score will call scorer.score().
	// Therefore the only methods that are implemented are score() and doc().
	private static final class BucketScorer extends Scorer {
		private float mScore;
		private int mDoc = NO_MORE_DOCS;
		private int mFreq;
    
		public BucketScorer(IWeight weight) { super(weight); }
    
		@Override
		public int advance(int target) throws IOException { return NO_MORE_DOCS; }

		@Override
		public int getDocID() { return mDoc; }

		@Override
		public float getFreq() { return mFreq; }

		@Override
		public int nextDoc() throws IOException { return NO_MORE_DOCS; }
    
		@Override
		public float getScore() throws IOException { return mScore; }
	}

	static final class Bucket {
		private int mDoc = -1;            // tells if bucket is valid
		private float mScore;             // incremental score
		// TODO: break out bool anyProhibited, int
		// numRequiredMatched; then we can remove 32 limit on
		// required clauses
		private int mBits;                // used for bool constraints
		private int mCoord;               // count of terms in score
		private Bucket mNext;             // next valid bucket
	}
  
	/** A simple hash table of document scores within a range. */
	static final class BucketTable {
		public static final int SIZE = 1 << 11;
		public static final int MASK = SIZE - 1;

		private final Bucket[] mBuckets = new Bucket[SIZE];
		private Bucket mFirst = null;                          // head of valid list
  
		public BucketTable() {
			// Pre-fill to save the lazy init when collecting
			// each sub:
			for (int idx=0; idx < SIZE; idx++) {
				mBuckets[idx] = new Bucket();
			}
		}

		public Collector newCollector(int mask) {
			return new BooleanScorerCollector(mask, this);
		}

		public int size() { return SIZE; }
	}

	static final class SubScorer {
		public IScorer mScorer;
		// TODO: re-enable this if BQ ever sends us required clauses
		//public boolean required = false;
		public boolean mProhibited;
		public Collector mCollector;
		public SubScorer mNext;

		public SubScorer(IScorer scorer, boolean required, boolean prohibited,
				Collector collector, SubScorer next) throws IOException {
			if (required) 
				throw new IllegalArgumentException("this scorer cannot handle required=true");
			
			mScorer = scorer;
			// TODO: re-enable this if BQ ever sends us required clauses
			//this.required = required;
			mProhibited = prohibited;
			mCollector = collector;
			mNext = next;
		}
	}
  
	private SubScorer mScorers = null;
	private BucketTable mBucketTable = new BucketTable();
	private final float[] mCoordFactors;
	// TODO: re-enable this if BQ ever sends us required clauses
	//private int requiredMask = 0;
	private final int mMinNrShouldMatch;
	private int mEnd;
	private Bucket mCurrent;
	// Any time a prohibited clause matches we set bit 0:
	private static final int PROHIBITED_MASK = 1;
  
	public BooleanScorer(IBooleanWeight weight, boolean disableCoord, int minNrShouldMatch,
			List<IScorer> optionalScorers, List<IScorer> prohibitedScorers, int maxCoord) 
			throws IOException {
		super(weight);
		mMinNrShouldMatch = minNrShouldMatch;

		if (optionalScorers != null && optionalScorers.size() > 0) {
			for (IScorer scorer : optionalScorers) {
				if (scorer.nextDoc() != NO_MORE_DOCS) {
					mScorers = new SubScorer(scorer, false, false, 
							mBucketTable.newCollector(0), mScorers);
				}
			}
		}
    
		if (prohibitedScorers != null && prohibitedScorers.size() > 0) {
			for (IScorer scorer : prohibitedScorers) {
				if (scorer.nextDoc() != NO_MORE_DOCS) {
					mScorers = new SubScorer(scorer, false, true, 
							mBucketTable.newCollector(PROHIBITED_MASK), mScorers);
				}
			}
		}

		mCoordFactors = new float[optionalScorers.size() + 1];
		for (int i = 0; i < mCoordFactors.length; i++) {
			mCoordFactors[i] = disableCoord ? 1.0f : weight.coord(i, maxCoord); 
		}
	}

	// firstDocID is ignored since nextDoc() initializes 'current'
	@Override
	public boolean score(ICollector collector, int max, int firstDocID) throws IOException {
		// Make sure it's only BooleanScorer that calls us:
		assert firstDocID == -1;
		boolean more;
		Bucket tmp;
		BucketScorer bs = new BucketScorer(mWeight);

		// The internal loop will set the score and doc before calling collect.
		collector.setScorer(bs);
		do {
			mBucketTable.mFirst = null;
      
			while (mCurrent != null) {         // more queued 
				// check prohibited & required
				if ((mCurrent.mBits & PROHIBITED_MASK) == 0) {
					// TODO: re-enable this if BQ ever sends us required clauses
					//&& (current.bits & requiredMask) == requiredMask) {
          
					// TODO: can we remove this?  
					if (mCurrent.mDoc >= max){
						tmp = mCurrent;
						mCurrent = mCurrent.mNext;
						tmp.mNext = mBucketTable.mFirst;
						mBucketTable.mFirst = tmp;
						continue;
					}
          
					if (mCurrent.mCoord >= mMinNrShouldMatch) {
						bs.mScore = mCurrent.mScore * mCoordFactors[mCurrent.mCoord];
						bs.mDoc = mCurrent.mDoc;
						bs.mFreq = mCurrent.mCoord;
						collector.collect(mCurrent.mDoc);
					}
				}
        
				mCurrent = mCurrent.mNext;         // pop the queue
			}
      
			if (mBucketTable.mFirst != null){
				mCurrent = mBucketTable.mFirst;
				mBucketTable.mFirst = mCurrent.mNext;
				return true;
			}

			// refill the queue
			more = false;
			mEnd += BucketTable.SIZE;
			
			for (SubScorer sub = mScorers; sub != null; sub = sub.mNext) {
				int subScorerDocID = sub.mScorer.getDocID();
				if (subScorerDocID != NO_MORE_DOCS) {
					more |= sub.mScorer.score(sub.mCollector, mEnd, subScorerDocID);
				}
			}
			
			mCurrent = mBucketTable.mFirst;
		} while (mCurrent != null || more);

		return false;
	}
  
	@Override
	public int advance(int target) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getDocID() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int nextDoc() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public float getScore() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void score(ICollector collector) throws IOException {
		score(collector, Integer.MAX_VALUE, -1);
	}
  
	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("boolean(");
		for (SubScorer sub = mScorers; sub != null; sub = sub.mNext) {
			buffer.append(sub.mScorer.toString());
			buffer.append(" ");
		}
		buffer.append(")");
		return buffer.toString();
	}
  
	@Override
	public Collection<IChild> getChildren() {
		List<IChild> children = new ArrayList<IChild>();
		for (SubScorer sub = mScorers; sub != null; sub = sub.mNext) {
			children.add(new ChildScorer((Scorer)sub.mScorer, sub.mProhibited ? 
					IBooleanClause.Occur.MUST_NOT.toString() : IBooleanClause.Occur.SHOULD.toString()));
		}
		return children;
	}
	
}
