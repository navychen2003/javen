package org.javenstudio.hornet.search.collector;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IScoreDoc;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.ITopDocs;
import org.javenstudio.common.indexdb.search.Collector;
import org.javenstudio.common.indexdb.search.IndexSearcher;
import org.javenstudio.common.indexdb.search.ScoreDoc;
import org.javenstudio.common.indexdb.search.Scorer;
import org.javenstudio.common.indexdb.search.TopDocs;
import org.javenstudio.common.indexdb.search.TopDocsCollector;
import org.javenstudio.hornet.search.hits.HitQueue;

/**
 * A {@link Collector} implementation that collects the top-scoring hits,
 * returning them as a {@link TopDocs}. This is used by {@link IndexSearcher} to
 * implement {@link TopDocs}-based search. Hits are sorted by score descending
 * and then (when the scores are tied) docID ascending. When you create an
 * instance of this collector you should know in advance whether documents are
 * going to be collected in doc Id order or not.
 *
 * <p><b>NOTE</b>: The values {@link Float#NaN} and
 * {Float#NEGATIVE_INFINITY} are not valid scores.  This
 * collector will not properly collect hits with such
 * scores.
 */
public abstract class TopScoreDocCollector extends TopDocsCollector<IScoreDoc> {

	// Assumes docs are scored in order.
	private static class InOrderTopScoreDocCollector extends TopScoreDocCollector {
		private InOrderTopScoreDocCollector(int numHits) {
			super(numHits);
		}
    
		@Override
		public void collect(int doc) throws IOException {
			float score = mScorer.getScore();

			// This collector cannot handle these scores:
			assert score != Float.NEGATIVE_INFINITY;
			assert !Float.isNaN(score);

			mTotalHits ++;
			if (score <= mQueueTop.getScore()) {
				// Since docs are returned in-order (i.e., increasing doc Id), a document
				// with equal score to pqTop.score cannot compete since HitQueue favors
				// documents with lower doc Ids. Therefore reject those docs too.
				return;
			}
			
			mQueueTop.setDoc(doc + mDocBase);
			mQueueTop.setScore(score);
			mQueueTop = (ScoreDoc)mQueue.updateTop();
		}
    
		@Override
		public boolean acceptsDocsOutOfOrder() {
			return false;
		}
	}
  
	// Assumes docs are scored in order.
	private static class InOrderPagingScoreDocCollector extends TopScoreDocCollector {
		
		private final ScoreDoc mAfter;
		// this is always after.doc - docBase, to save an add when score == after.score
		private int mAfterDoc;
		private int mCollectedHits;

		private InOrderPagingScoreDocCollector(IScoreDoc after, int numHits) {
			super(numHits);
			mAfter = (ScoreDoc)after;
		}
    
		@Override
		public void collect(int doc) throws IOException {
			float score = mScorer.getScore();

			// This collector cannot handle these scores:
			assert score != Float.NEGATIVE_INFINITY;
			assert !Float.isNaN(score);

			mTotalHits ++;
      
			if (score > mAfter.getScore() || (score == mAfter.getScore() && doc <= mAfterDoc)) {
				// hit was collected on a previous page
				return;
			}
      
			if (score <= mQueueTop.getScore()) {
				// Since docs are returned in-order (i.e., increasing doc Id), a document
				// with equal score to pqTop.score cannot compete since HitQueue favors
				// documents with lower doc Ids. Therefore reject those docs too.
				return;
			}
			
			mCollectedHits ++;
			mQueueTop.setDoc(doc + mDocBase);
			mQueueTop.setScore(score);
			mQueueTop = (ScoreDoc)mQueue.updateTop();
		}

		@Override
		public boolean acceptsDocsOutOfOrder() {
			return false;
		}

		@Override
		public void setNextReader(IAtomicReaderRef context) {
			super.setNextReader(context);
			mAfterDoc = mAfter.getDoc() - mDocBase;
		}

		@Override
		protected int topDocsSize() {
			return mCollectedHits < mQueue.size() ? mCollectedHits : mQueue.size();
		}
    
		@Override
		protected ITopDocs newTopDocs(IScoreDoc[] results, int start) {
			return results == null ? new TopDocs(mTotalHits, new ScoreDoc[0], Float.NaN) : 
				new TopDocs(mTotalHits, results);
		}
	}

	// Assumes docs are scored out of order.
	private static class OutOfOrderTopScoreDocCollector extends TopScoreDocCollector {
		private OutOfOrderTopScoreDocCollector(int numHits) {
			super(numHits);
		}
    
		@Override
		public void collect(int doc) throws IOException {
			float score = mScorer.getScore();

			// This collector cannot handle NaN
			assert !Float.isNaN(score);

			mTotalHits ++;
			if (score < mQueueTop.getScore()) {
				// Doesn't compete w/ bottom entry in queue
				return;
			}
			
			doc += mDocBase;
			if (score == mQueueTop.getScore() && doc > mQueueTop.getDoc()) {
				// Break tie in score by doc ID:
				return;
			}
			
			mQueueTop.setDoc(doc);
			mQueueTop.setScore(score);
			mQueueTop = (ScoreDoc)mQueue.updateTop();
		}
    
		@Override
		public boolean acceptsDocsOutOfOrder() {
			return true;
		}
	}
  
	// Assumes docs are scored out of order.
	private static class OutOfOrderPagingScoreDocCollector extends TopScoreDocCollector {
		private final ScoreDoc mAfter;
		// this is always after.doc - docBase, to save an add when score == after.score
		private int mAfterDoc;
		private int mCollectedHits;

		private OutOfOrderPagingScoreDocCollector(IScoreDoc after, int numHits) {
			super(numHits);
			mAfter = (ScoreDoc)after;
		}
    
		@Override
		public void collect(int doc) throws IOException {
			float score = mScorer.getScore();

			// This collector cannot handle NaN
			assert !Float.isNaN(score);

			mTotalHits ++;
			if (score > mAfter.getScore() || (score == mAfter.getScore() && doc <= mAfterDoc)) {
				// hit was collected on a previous page
				return;
			}
			
			if (score < mQueueTop.getScore()) {
				// Doesn't compete w/ bottom entry in queue
				return;
			}
			
			doc += mDocBase;
			if (score == mQueueTop.getScore() && doc > mQueueTop.getDoc()) {
				// Break tie in score by doc ID:
				return;
			}
			
			mCollectedHits ++;
			mQueueTop.setDoc(doc);
			mQueueTop.setScore(score);
			mQueueTop = (ScoreDoc)mQueue.updateTop();
		}
    
		@Override
		public boolean acceptsDocsOutOfOrder() {
			return true;
		}
    
		@Override
		public void setNextReader(IAtomicReaderRef context) {
			super.setNextReader(context);
			mAfterDoc = mAfter.getDoc() - mDocBase;
		}
    
		@Override
		protected int topDocsSize() {
			return mCollectedHits < mQueue.size() ? mCollectedHits : mQueue.size();
		}
    
		@Override
		protected ITopDocs newTopDocs(IScoreDoc[] results, int start) {
			return results == null ? new TopDocs(mTotalHits, new ScoreDoc[0], Float.NaN) : 
				new TopDocs(mTotalHits, results);
		}
	}

	/**
	 * Creates a new {@link TopScoreDocCollector} given the number of hits to
	 * collect and whether documents are scored in order by the input
	 * {@link Scorer} to {@link #setScorer(Scorer)}.
	 *
	 * <p><b>NOTE</b>: The instances returned by this method
	 * pre-allocate a full array of length
	 * <code>numHits</code>, and fill the array with sentinel
	 * objects.
	 */
	public static TopScoreDocCollector create(int numHits, boolean docsScoredInOrder) {
		return create(numHits, null, docsScoredInOrder);
	}
  
	/**
	 * Creates a new {@link TopScoreDocCollector} given the number of hits to
	 * collect, the bottom of the previous page, and whether documents are scored in order by the input
	 * {@link Scorer} to {@link #setScorer(Scorer)}.
	 *
	 * <p><b>NOTE</b>: The instances returned by this method
	 * pre-allocate a full array of length
	 * <code>numHits</code>, and fill the array with sentinel
	 * objects.
	 */
	public static TopScoreDocCollector create(int numHits, IScoreDoc after, boolean docsScoredInOrder) {
		if (numHits <= 0) {
			throw new IllegalArgumentException("numHits must be > 0; " + 
					"please use TotalHitCountCollector if you just need the total hit count");
		}
    
		if (docsScoredInOrder) {
			return after == null 
					? new InOrderTopScoreDocCollector(numHits) 
					: new InOrderPagingScoreDocCollector(after, numHits);
		} else {
			return after == null
					? new OutOfOrderTopScoreDocCollector(numHits)
					: new OutOfOrderPagingScoreDocCollector(after, numHits);
		}
	}
  
	protected ScoreDoc mQueueTop = null;
	protected int mDocBase = 0;
	protected IScorer mScorer = null;
    
	// prevents instantiation
	private TopScoreDocCollector(int numHits) {
		super(new HitQueue(numHits, true));
		// HitQueue implements getSentinelObject to return a ScoreDoc, so we know
		// that at this point top() is already initialized.
		mQueueTop = (ScoreDoc)mQueue.top();
	}

	@Override
	protected ITopDocs newTopDocs(IScoreDoc[] results, int start) {
		if (results == null) 
			return EMPTY_TOPDOCS;
    
		// We need to compute maxScore in order to set it in TopDocs. If start == 0,
		// it means the largest element is already in results, use its score as
		// maxScore. Otherwise pop everything else, until the largest element is
		// extracted and use its score as maxScore.
		float maxScore = Float.NaN;
		
		if (start == 0) {
			maxScore = results[0].getScore();
		} else {
			for (int i = mQueue.size(); i > 1; i--) { mQueue.pop(); }
			maxScore = mQueue.pop().getScore();
		}
    
		return new TopDocs(mTotalHits, results, maxScore);
	}
  
	@Override
	public void setNextReader(IAtomicReaderRef context) {
		mDocBase = context.getDocBase();
	}
  
	@Override
	public void setScorer(IScorer scorer) throws IOException {
		mScorer = (Scorer)scorer;
	}
	
}
