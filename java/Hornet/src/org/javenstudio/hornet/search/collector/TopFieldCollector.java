package org.javenstudio.hornet.search.collector;

import java.io.IOException;

import org.javenstudio.common.indexdb.IScoreDoc;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.ITopDocs;
import org.javenstudio.common.indexdb.search.Collector;
import org.javenstudio.common.indexdb.search.FieldComparator;
import org.javenstudio.common.indexdb.search.FieldDoc;
import org.javenstudio.common.indexdb.search.ScoreDoc;
import org.javenstudio.common.indexdb.search.Scorer;
import org.javenstudio.common.indexdb.search.Sort;
import org.javenstudio.common.indexdb.search.SortField;
import org.javenstudio.common.indexdb.search.TopDocs;
import org.javenstudio.common.indexdb.search.TopDocsCollector;
import org.javenstudio.common.indexdb.search.TopFieldDocs;
import org.javenstudio.common.indexdb.util.PriorityQueue;
import org.javenstudio.hornet.search.cache.FieldValueHitQueue;

/**
 * A {@link Collector} that sorts by {@link SortField} using
 * {@link FieldComparator}s.
 * <p/>
 * See the {@link #create(Sort, int, boolean, boolean, boolean, boolean)} method
 * for instantiating a TopFieldCollector.
 * 
 */
public abstract class TopFieldCollector extends TopDocsCollector<FieldValueHitQueue.Entry> {
  
	// TODO: one optimization we could do is to pre-fill
	// the queue with sentinel value that guaranteed to
	// always compare lower than a real hit; this would
	// save having to check queueFull on each insert

	private static final ScoreDoc[] EMPTY_SCOREDOCS = new ScoreDoc[0];
  
	private final boolean mFillFields;

	/**
	 * Stores the maximum score value encountered, needed for normalizing. If
	 * document scores are not tracked, this value is initialized to NaN.
	 */
	protected float mMaxScore = Float.NaN;

	protected final int mNumHits;
	protected FieldValueHitQueue.Entry mBottom = null;
	protected boolean mQueueFull = false;
	protected int mDocBase = 0;
  
	public float getMaxScore() { return mMaxScore; }
	public void setMaxScore(float score) { mMaxScore = score; }
  
	// Declaring the constructor private prevents extending this class by anyone
	// else. Note that the class cannot be final since it's extended by the
	// internal versions. If someone will define a constructor with any other
	// visibility, then anyone will be able to extend the class, which is not what
	// we want.
	protected TopFieldCollector(PriorityQueue<FieldValueHitQueue.Entry> pq, 
			int numHits, boolean fillFields) {
		super(pq);
		mNumHits = numHits;
		mFillFields = fillFields;
	}

	/**
	 * Creates a new {@link TopFieldCollector} from the given
	 * arguments.
	 *
	 * <p><b>NOTE</b>: The instances returned by this method
	 * pre-allocate a full array of length
	 * <code>numHits</code>.
	 * 
	 * @param sort
	 *          the sort criteria (SortFields).
	 * @param numHits
	 *          the number of results to collect.
	 * @param fillFields
	 *          specifies whether the actual field values should be returned on
	 *          the results (FieldDoc).
	 * @param trackDocScores
	 *          specifies whether document scores should be tracked and set on the
	 *          results. Note that if set to false, then the results' scores will
	 *          be set to Float.NaN. Setting this to true affects performance, as
	 *          it incurs the score computation on each competitive result.
	 *          Therefore if document scores are not required by the application,
	 *          it is recommended to set it to false.
	 * @param trackMaxScore
	 *          specifies whether the query's maxScore should be tracked and set
	 *          on the resulting {@link TopDocs}. Note that if set to false,
	 *          {@link TopDocs#getMaxScore()} returns Float.NaN. Setting this to
	 *          true affects performance as it incurs the score computation on
	 *          each result. Also, setting this true automatically sets
	 *          <code>trackDocScores</code> to true as well.
	 * @param docsScoredInOrder
	 *          specifies whether documents are scored in doc Id order or not by
	 *          the given {@link Scorer} in {@link #setScorer(Scorer)}.
	 * @return a {@link TopFieldCollector} instance which will sort the results by
	 *         the sort criteria.
	 * @throws IOException
	 */
	public static TopFieldCollector create(ISort sort, int numHits,
			boolean fillFields, boolean trackDocScores, boolean trackMaxScore,
			boolean docsScoredInOrder) throws IOException {
		return create(sort, numHits, null, fillFields, trackDocScores, 
				trackMaxScore, docsScoredInOrder);
	}

	/**
	 * Creates a new {@link TopFieldCollector} from the given
	 * arguments.
	 *
	 * <p><b>NOTE</b>: The instances returned by this method
	 * pre-allocate a full array of length
	 * <code>numHits</code>.
	 * 
	 * @param sort
	 *          the sort criteria (SortFields).
	 * @param numHits
	 *          the number of results to collect.
	 * @param after
	 *          only hits after this FieldDoc will be collected
	 * @param fillFields
	 *          specifies whether the actual field values should be returned on
	 *          the results (FieldDoc).
	 * @param trackDocScores
	 *          specifies whether document scores should be tracked and set on the
	 *          results. Note that if set to false, then the results' scores will
	 *          be set to Float.NaN. Setting this to true affects performance, as
	 *          it incurs the score computation on each competitive result.
	 *          Therefore if document scores are not required by the application,
	 *          it is recommended to set it to false.
	 * @param trackMaxScore
	 *          specifies whether the query's maxScore should be tracked and set
	 *          on the resulting {@link TopDocs}. Note that if set to false,
	 *          {@link TopDocs#getMaxScore()} returns Float.NaN. Setting this to
	 *          true affects performance as it incurs the score computation on
	 *          each result. Also, setting this true automatically sets
	 *          <code>trackDocScores</code> to true as well.
	 * @param docsScoredInOrder
	 *          specifies whether documents are scored in doc Id order or not by
	 *          the given {@link Scorer} in {@link #setScorer(Scorer)}.
	 * @return a {@link TopFieldCollector} instance which will sort the results by
	 *         the sort criteria.
	 * @throws IOException
	 */
	public static TopFieldCollector create(ISort sort, int numHits, FieldDoc after,
			boolean fillFields, boolean trackDocScores, boolean trackMaxScore,
			boolean docsScoredInOrder) throws IOException {

		if (sort.getSortFields().length == 0) 
			throw new IllegalArgumentException("Sort must contain at least one field");
    
		if (numHits <= 0) {
			throw new IllegalArgumentException("numHits must be > 0; " + 
					"please use TotalHitCountCollector if you just need the total hit count");
		}

		FieldValueHitQueue<FieldValueHitQueue.Entry> queue = FieldValueHitQueue.create(
				sort.getSortFields(), numHits);

		if (after == null) {
			if (queue.getComparators().length == 1) {
				if (docsScoredInOrder) {
					if (trackMaxScore) {
						return new OneComparatorScoringMaxScoreCollector(queue, numHits, fillFields);
					} else if (trackDocScores) {
						return new OneComparatorScoringNoMaxScoreCollector(queue, numHits, fillFields);
					} else {
						return new OneComparatorNonScoringCollector(queue, numHits, fillFields);
					}
				} else {
					if (trackMaxScore) {
						return new OutOfOrderOneComparatorScoringMaxScoreCollector(queue, numHits, fillFields);
					} else if (trackDocScores) {
						return new OutOfOrderOneComparatorScoringNoMaxScoreCollector(queue, numHits, fillFields);
					} else {
						return new OutOfOrderOneComparatorNonScoringCollector(queue, numHits, fillFields);
					}
				}
			}

			// multiple comparators.
			if (docsScoredInOrder) {
				if (trackMaxScore) {
					return new MultiComparatorScoringMaxScoreCollector(queue, numHits, fillFields);
				} else if (trackDocScores) {
					return new MultiComparatorScoringNoMaxScoreCollector(queue, numHits, fillFields);
				} else {
					return new MultiComparatorNonScoringCollector(queue, numHits, fillFields);
				}
			} else {
				if (trackMaxScore) {
					return new OutOfOrderMultiComparatorScoringMaxScoreCollector(queue, numHits, fillFields);
				} else if (trackDocScores) {
					return new OutOfOrderMultiComparatorScoringNoMaxScoreCollector(queue, numHits, fillFields);
				} else {
					return new OutOfOrderMultiComparatorNonScoringCollector(queue, numHits, fillFields);
				}
			}
		} else {
			if (after.getFields() == null) {
				throw new IllegalArgumentException("after.fields wasn't set; " + 
						"you must pass fillFields=true for the previous search");
			}

			if (after.getFieldSize() != sort.getSortFields().length) {
				throw new IllegalArgumentException("after.fields has " + after.getFieldSize() + 
						" values but sort has " + sort.getSortFields().length);
			}

			return new PagingFieldCollector(queue, after, numHits, fillFields, 
					trackDocScores, trackMaxScore);
		}
	}
  
  	protected void add(int slot, int doc, float score) {
  		mBottom = mQueue.add(new FieldValueHitQueue.Entry(slot, mDocBase + doc, score));
  		mQueueFull = mTotalHits == mNumHits;
  	}

  	/**
  	 * Only the following callback methods need to be overridden since
  	 * topDocs(int, int) calls them to return the results.
  	 */
  	@Override
  	protected void populateResults(IScoreDoc[] results, int howMany) {
  		if (mFillFields) {
  			// avoid casting if unnecessary.
  			FieldValueHitQueue<FieldValueHitQueue.Entry> queue = (FieldValueHitQueue<FieldValueHitQueue.Entry>) mQueue;
  			for (int i = howMany - 1; i >= 0; i--) {
  				results[i] = queue.fillFields(queue.pop());
  			}
  		} else {
  			for (int i = howMany - 1; i >= 0; i--) {
  				FieldValueHitQueue.Entry entry = mQueue.pop();
  				results[i] = new FieldDoc(entry.getDoc(), entry.getScore());
  			}
  		}
  	}
  
  	@Override
  	protected ITopDocs newTopDocs(IScoreDoc[] results, int start) {
  		if (results == null) {
  			results = EMPTY_SCOREDOCS;
  			// Set maxScore to NaN, in case this is a maxScore tracking collector.
  			mMaxScore = Float.NaN;
  		}

  		// If this is a maxScoring tracking collector and there were no results, 
  		return new TopFieldDocs(mTotalHits, results, 
  				((FieldValueHitQueue<FieldValueHitQueue.Entry>) mQueue).getSortFields(), 
  				mMaxScore);
  	}
  
  	@Override
  	public boolean acceptsDocsOutOfOrder() {
  		return false;
  	}
  	
}
