package org.javenstudio.hornet.grouping;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDocIdSetIterator;
import org.javenstudio.common.indexdb.IFieldComparator;
import org.javenstudio.common.indexdb.IFilter;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.ITopDocs;
import org.javenstudio.common.indexdb.IWeight;
import org.javenstudio.common.indexdb.search.Collector;
import org.javenstudio.common.indexdb.search.Filter;
import org.javenstudio.common.indexdb.search.Scorer;
import org.javenstudio.common.indexdb.search.TopDocsCollector;
import org.javenstudio.common.indexdb.util.ArrayUtil;
import org.javenstudio.common.indexdb.util.PriorityQueue;
import org.javenstudio.hornet.search.collector.TopFieldCollector;
import org.javenstudio.hornet.search.collector.TopScoreDocCollector;

// TODO: this sentence is too long for the class summary.
/** 
 * BlockGroupingCollector performs grouping with a
 *  single pass collector, as long as you are grouping by a
 *  doc block field, ie all documents sharing a given group
 *  value were indexed as a doc block using the atomic
 *  {@link IndexWriter#addDocuments IndexWriter.addDocuments()} 
 *  or {@link IndexWriter#updateDocuments IndexWriter.updateDocuments()} 
 *  API.
 *
 *  <p>This results in faster performance (~25% faster QPS)
 *  than the two-pass grouping collectors, with the tradeoff
 *  being that the documents in each group must always be
 *  indexed as a block.  This collector also fills in
 *  TopGroups.totalGroupCount without requiring the separate
 *  {@link TermAllGroupsCollector}.  However, this collector does
 *  not fill in the groupValue of each group; this field
 *  will always be null.
 *
 *  <p><b>NOTE</b>: this collector makes no effort to verify
 *  the docs were in fact indexed as a block, so it's up to
 *  you to ensure this was the case.
 *
 *  <p>See {@link grouping} for more
 *  details including a full code example.</p>
 *
 */
public class BlockGroupingCollector extends Collector {

	private int[] mPendingSubDocs;
	private float[] mPendingSubScores;
	private int mSubDocUpto;

	private final ISort mGroupSort;
	private final int mTopNGroups;
	private final IFilter mLastDocPerGroup;

	// TODO: specialize into 2 classes, static "create" method:
	private final boolean mNeedsScores;

	private final GroupQueue mGroupQueue;
	private final IFieldComparator<?>[] mComparators;
	
	private final int[] mReversed;
	private final int mCompIDXEnd;
	
	private int mBottomSlot;
	private boolean mQueueFull;
	
	private IAtomicReaderRef mCurrentReaderContext;

	private int mTopGroupDoc;
	private int mTotalHitCount;
	private int mTotalGroupCount;
	private int mDocBase;
	private int mGroupEndDocID;
	
	private IDocIdSetIterator mLastDocPerGroupBits;
	private IScorer mScorer;
	
	private boolean mGroupCompetes;

	private final static class FakeScorer extends Scorer {
		private float mScore;
		private int mDoc;

		public FakeScorer() {
			super((IWeight) null);
		}

		@Override
		public float getScore() {
			return mScore;
		}
    
		@Override
		public float getFreq() {
			throw new UnsupportedOperationException(); // TODO: wtf does this class do?
		}

		@Override
		public int getDocID() {
			return mDoc;
		}

		@Override
		public int advance(int target) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int nextDoc() {
			throw new UnsupportedOperationException();
		}
	}

	private static final class OneGroup {
		private IAtomicReaderRef mReaderContext;
		//private int mGroupOrd;
		private int mTopGroupDoc;
		private int[] mDocs;
		private float[] mScores;
		private int mCount;
		private int mComparatorSlot;
	}
  
	// Sorts by groupSort.  Not static -- uses comparators, reversed
	private final class GroupQueue extends PriorityQueue<OneGroup> {
		public GroupQueue(int size) {
			super(size);
		}

		@Override
		protected boolean lessThan(final OneGroup group1, final OneGroup group2) {
			assert group1 != group2;
			assert group1.mComparatorSlot != group2.mComparatorSlot;

			final int numComparators = mComparators.length;
			for (int compIDX=0; compIDX < numComparators; compIDX++) {
				final int c = mReversed[compIDX] * mComparators[compIDX].compare(
						group1.mComparatorSlot, group2.mComparatorSlot);
				
				if (c != 0) {
					// Short circuit
					return c > 0;
				}
			}

			// Break ties by docID; lower docID is always sorted first
			return group1.mTopGroupDoc > group2.mTopGroupDoc;
		}
	}

	// Called when we transition to another group; if the
	// group is competitive we insert into the group queue
	private void processGroup() {
		mTotalGroupCount ++;
		
		if (mGroupCompetes) {
			if (!mQueueFull) {
				// Startup transient: always add a new OneGroup
				final OneGroup og = new OneGroup();
				og.mCount = mSubDocUpto;
				og.mTopGroupDoc = mDocBase + mTopGroupDoc;
				og.mDocs = mPendingSubDocs;
				
				mPendingSubDocs = new int[10];
				if (mNeedsScores) {
					og.mScores = mPendingSubScores;
					mPendingSubScores = new float[10];
				}
				
				og.mReaderContext = mCurrentReaderContext;
				//og.groupOrd = lastGroupOrd;
				og.mComparatorSlot = mBottomSlot;
				
				final OneGroup bottomGroup = mGroupQueue.add(og);
				
				mQueueFull = mGroupQueue.size() == mTopNGroups;
				if (mQueueFull) {
					// Queue just became full; now set the real bottom
					// in the comparators:
					mBottomSlot = bottomGroup.mComparatorSlot;
					
					for (int i = 0; i < mComparators.length; i++) {
						mComparators[i].setBottom(mBottomSlot);
					}
					
				} else {
					// Queue not full yet -- just advance bottomSlot:
					mBottomSlot = mGroupQueue.size();
				}
				
			} else {
				// Replace bottom element in PQ and then updateTop
				final OneGroup og = mGroupQueue.top();
				assert og != null;
				
				og.mCount = mSubDocUpto;
				og.mTopGroupDoc = mDocBase + mTopGroupDoc;
				
				// Swap pending docs
				final int[] savDocs = og.mDocs;
				og.mDocs = mPendingSubDocs;
				mPendingSubDocs = savDocs;
				
				if (mNeedsScores) {
					// Swap pending scores
					final float[] savScores = og.mScores;
					og.mScores = mPendingSubScores;
					mPendingSubScores = savScores;
				}
				
				og.mReaderContext = mCurrentReaderContext;
				//og.groupOrd = lastGroupOrd;
				mBottomSlot = mGroupQueue.updateTop().mComparatorSlot;

				for (int i = 0; i < mComparators.length; i++) {
					mComparators[i].setBottom(mBottomSlot);
				}
			}
		}
		
		mSubDocUpto = 0;
	}

	/**
	 * Create the single pass collector.
	 *
	 *  @param groupSort The {@link Sort} used to sort the
	 *    groups.  The top sorted document within each group
	 *    according to groupSort, determines how that group
	 *    sorts against other groups.  This must be non-null,
	 *    ie, if you want to groupSort by relevance use
	 *    Sort.RELEVANCE.
	 *  @param topNGroups How many top groups to keep.
	 *  @param needsScores true if the collected documents
	 *    require scores, either because relevance is included
	 *    in the withinGroupSort or because you plan to pass true
	 *    for either getSscores or getMaxScores to {@link
	 *    #getTopGroups}
	 *  @param lastDocPerGroup a {@link Filter} that marks the
	 *    last document in each group.
	 */
	public BlockGroupingCollector(ISort groupSort, int topNGroups, boolean needsScores, 
			Filter lastDocPerGroup) throws IOException {
		if (topNGroups < 1) 
			throw new IllegalArgumentException("topNGroups must be >= 1 (got " + topNGroups + ")");

		mGroupQueue = new GroupQueue(topNGroups);
		mPendingSubDocs = new int[10];
		if (needsScores) 
			mPendingSubScores = new float[10];

    	mNeedsScores = needsScores;
    	mLastDocPerGroup = lastDocPerGroup;
    	
    	// TODO: allow null groupSort to mean "by relevance",
    	// and specialize it?
    	mGroupSort = groupSort;
    	mTopNGroups = topNGroups;

    	final ISortField[] sortFields = groupSort.getSortFields();
    	
    	mComparators = new IFieldComparator<?>[sortFields.length];
    	mCompIDXEnd = mComparators.length - 1;
    	mReversed = new int[sortFields.length];
    	
    	for (int i = 0; i < sortFields.length; i++) {
    		final ISortField sortField = sortFields[i];
    		
    		mComparators[i] = sortField.getComparator(topNGroups, i);
    		mReversed[i] = sortField.getReverse() ? -1 : 1;
    	}
	}

	// TODO: maybe allow no sort on retrieving groups?  app
	// may want to simply process docs in the group itself?
	// typically they will be presented as a "single" result
	// in the UI?

	/** 
	 * Returns the grouped results.  Returns null if the
	 *  number of groups collected is <= groupOffset.
	 *
	 *  <p><b>NOTE</b>: This collector is unable to compute
	 *  the groupValue per group so it will always be null.
	 *  This is normally not a problem, as you can obtain the
	 *  value just like you obtain other values for each
	 *  matching document (eg, via stored fields, via
	 *  FieldCache, etc.)
	 *
	 *  @param withinGroupSort The {@link Sort} used to sort
	 *    documents within each group.  Passing null is
	 *    allowed, to sort by relevance.
	 *  @param groupOffset Which group to start from
	 *  @param withinGroupOffset Which document to start from
	 *    within each group
	 *  @param maxDocsPerGroup How many top documents to keep
	 *     within each group.
	 *  @param fillSortFields If true then the Comparable
	 *     values for the sort fields will be set
	 */
	public TopGroups<?> getTopGroups(ISort withinGroupSort, int groupOffset, 
			int withinGroupOffset, int maxDocsPerGroup, boolean fillSortFields) throws IOException {
		if (mSubDocUpto != 0) 
			processGroup();
    
		if (groupOffset >= mGroupQueue.size()) 
			return null;
    
		final FakeScorer fakeScorer = new FakeScorer();
		int totalGroupedHitCount = 0;
		float maxScore = Float.MIN_VALUE;

		@SuppressWarnings("unchecked")
		final GroupDocs<Object>[] groups = new GroupDocs[mGroupQueue.size() - groupOffset];
		
		for (int downTo = mGroupQueue.size()-groupOffset-1; downTo >= 0; downTo--) {
			final OneGroup og = mGroupQueue.pop();

			// At this point we hold all docs w/ in each group,
			// unsorted; we now sort them:
			final TopDocsCollector<?> collector;
			
			if (withinGroupSort == null) {
				// Sort by score
				if (!mNeedsScores) 
					throw new IllegalArgumentException("cannot sort by relevance within group: needsScores=false");
				
				collector = TopScoreDocCollector.create(maxDocsPerGroup, true);
				
			} else {
				// Sort by fields
				collector = TopFieldCollector.create(withinGroupSort, maxDocsPerGroup, 
						fillSortFields, mNeedsScores, mNeedsScores, true);
			}

			collector.setScorer(fakeScorer);
			collector.setNextReader(og.mReaderContext);
			
			for (int docIDX=0; docIDX < og.mCount; docIDX++) {
				final int doc = og.mDocs[docIDX];
				fakeScorer.mDoc = doc;
				
				if (mNeedsScores) 
					fakeScorer.mScore = og.mScores[docIDX];
				
				collector.collect(doc);
			}
			
			totalGroupedHitCount += og.mCount;

			final Object[] groupSortValues;

			if (fillSortFields) {
				groupSortValues = new Comparable<?>[mComparators.length];
				
				for (int sortFieldIDX=0; sortFieldIDX < mComparators.length; sortFieldIDX++) {
					groupSortValues[sortFieldIDX] = 
							mComparators[sortFieldIDX].getValue(og.mComparatorSlot);
				}
				
			} else {
				groupSortValues = null;
			}

			final ITopDocs topDocs = collector.getTopDocs(withinGroupOffset, maxDocsPerGroup);

			// TODO: we could aggregate scores across children
			// by Sum/Avg instead of passing NaN:
			groups[downTo] = new GroupDocs<Object>(Float.NaN,
					topDocs.getMaxScore(),
					og.mCount,
					topDocs.getScoreDocs(),
					null,
					groupSortValues);
			
			maxScore = Math.max(maxScore, topDocs.getMaxScore());
		}

		/*
    	while (groupQueue.size() != 0) {
      	final OneGroup og = groupQueue.pop();
      	totalGroupedHitCount += og.count;
    	}
		 */

		return new TopGroups<Object>(new TopGroups<Object>(
				mGroupSort.getSortFields(),
				withinGroupSort == null ? null : withinGroupSort.getSortFields(),
				mTotalHitCount, totalGroupedHitCount, groups, maxScore),
				mTotalGroupCount);
	}

	@Override
	public void setScorer(IScorer scorer) throws IOException {
		mScorer = scorer;
		
		for (IFieldComparator<?> comparator : mComparators) {
			comparator.setScorer(scorer);
		}
	}

	@Override
	public void collect(int doc) throws IOException {
		if (doc > mGroupEndDocID) {
			// Group changed
			if (mSubDocUpto != 0) 
				processGroup();
			
			mGroupEndDocID = mLastDocPerGroupBits.advance(doc);
			mSubDocUpto = 0;
			mGroupCompetes = !mQueueFull;
		}

		mTotalHitCount ++;

		// Always cache doc/score within this group:
		if (mSubDocUpto == mPendingSubDocs.length) 
			mPendingSubDocs = ArrayUtil.grow(mPendingSubDocs);
		
		mPendingSubDocs[mSubDocUpto] = doc;
		if (mNeedsScores) {
			if (mSubDocUpto == mPendingSubScores.length) 
				mPendingSubScores = ArrayUtil.grow(mPendingSubScores);
      
			mPendingSubScores[mSubDocUpto] = mScorer.getScore();
		}
		
		mSubDocUpto++;

		if (mGroupCompetes) {
			if (mSubDocUpto == 1) {
				assert !mQueueFull;

				for (IFieldComparator<?> fc : mComparators) {
					fc.copy(mBottomSlot, doc);
					fc.setBottom(mBottomSlot);
				}
				
				mTopGroupDoc = doc;
				
			} else {
				// Compare to bottomSlot
				for (int compIDX = 0;; compIDX++) {
					final int c = mReversed[compIDX] * mComparators[compIDX].compareBottom(doc);
					
					if (c < 0) {
						// Definitely not competitive -- done
						return;
						
					} else if (c > 0) {
						// Definitely competitive.
						break;
						
					} else if (compIDX == mCompIDXEnd) {
						// Ties with bottom, except we know this docID is
						// > docID in the queue (docs are visited in
						// order), so not competitive:
						return;
					}
				}

				for (IFieldComparator<?> fc : mComparators) {
					fc.copy(mBottomSlot, doc);
					// Necessary because some comparators cache
					// details of bottom slot; this forces them to
					// re-cache:
					fc.setBottom(mBottomSlot);
				}
				
				mTopGroupDoc = doc;
			}
			
		} else {
			// We're not sure this group will make it into the
			// queue yet
			for (int compIDX = 0;; compIDX++) {
				final int c = mReversed[compIDX] * mComparators[compIDX].compareBottom(doc);
				
				if (c < 0) {
					// Definitely not competitive -- done
					return;
					
				} else if (c > 0) {
					// Definitely competitive.
					break;
					
				} else if (compIDX == mCompIDXEnd) {
					// Ties with bottom, except we know this docID is
					// > docID in the queue (docs are visited in
					// order), so not competitive:
					return;
				}
			}
			
			mGroupCompetes = true;
			
			for (IFieldComparator<?> fc : mComparators) {
				fc.copy(mBottomSlot, doc);
				// Necessary because some comparators cache
				// details of bottom slot; this forces them to
				// re-cache:
				fc.setBottom(mBottomSlot);
			}
			
			mTopGroupDoc = doc;
		}
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		return false;
	}

	@Override
	public void setNextReader(IAtomicReaderRef readerContext) throws IOException {
		if (mSubDocUpto != 0) 
			processGroup();
		
		mSubDocUpto = 0;
		mDocBase = readerContext.getDocBase();
		mLastDocPerGroupBits = mLastDocPerGroup.getDocIdSet(readerContext, 
				readerContext.getReader().getLiveDocs()).iterator();
		
		mGroupEndDocID = -1;
		mCurrentReaderContext = readerContext;
		
		for (int i=0; i < mComparators.length; i++) {
			mComparators[i] = mComparators[i].setNextReader(readerContext);
		}
	}
	
}
