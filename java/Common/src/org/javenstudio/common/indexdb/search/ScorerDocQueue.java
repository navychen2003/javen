package org.javenstudio.common.indexdb.search;

import java.io.IOException;

import org.javenstudio.common.indexdb.IScorer;

/** 
 * A ScorerDocQueue maintains a partial ordering of its Scorers such that the
 * least Scorer can always be found in constant time.  Put()'s and pop()'s
 * require log(size) time. The ordering is by Scorer.doc().
 */
public class ScorerDocQueue {  // later: SpansQueue for spans with doc and term positions
	
	private final HeapedScorerDoc[] mHeap;
	private final int mMaxSize;
	private int mSize;
  
	private HeapedScorerDoc mTopHSD; // same as heap[1], only for speed
	
	private class HeapedScorerDoc {
		private final IScorer mScorer;
		private int mDoc;
    
		HeapedScorerDoc(IScorer s) { this(s, s.getDocID()); }
    
		HeapedScorerDoc(IScorer scorer, int doc) {
			mScorer = scorer;
			mDoc = doc;
		}
    
		void adjust() { mDoc = mScorer.getDocID(); }
	}
  
	/** Create a ScorerDocQueue with a maximum size. */
	public ScorerDocQueue(int maxSize) {
		// assert maxSize >= 0;
		mSize = 0;
		int heapSize = maxSize + 1;
		mHeap = new HeapedScorerDoc[heapSize];
		mMaxSize = maxSize;
		mTopHSD = mHeap[1]; // initially null
	}

	/**
	 * Adds a Scorer to a ScorerDocQueue in log(size) time.
	 * If one tries to add more Scorers than maxSize
	 * a RuntimeException (ArrayIndexOutOfBound) is thrown.
	 */
	public final void put(IScorer scorer) {
		mSize ++;
		mHeap[mSize] = new HeapedScorerDoc(scorer);
		upHeap();
	}

	/**
	 * Adds a Scorer to the ScorerDocQueue in log(size) time if either
	 * the ScorerDocQueue is not full, or not lessThan(scorer, top()).
	 * @param scorer
	 * @return true if scorer is added, false otherwise.
	 */
	public boolean insert(IScorer scorer){
		if (mSize < mMaxSize) {
			put(scorer);
			return true;
		}
		
		int docNr = scorer.getDocID();
		if ((mSize > 0) && (! (docNr < mTopHSD.mDoc))) { // heap[1] is top()
			mHeap[1] = new HeapedScorerDoc(scorer, docNr);
			downHeap();
			return true;
		} else 
			return false;
	}

	/** 
	 * Returns the least Scorer of the ScorerDocQueue in constant time.
	 * Should not be used when the queue is empty.
	 */
	public final IScorer top() {
		// assert size > 0;
		return mTopHSD.mScorer;
	}

	/** 
	 * Returns document number of the least Scorer of the ScorerDocQueue
	 * in constant time.
	 * Should not be used when the queue is empty.
	 */
	public final int topDoc() {
		// assert size > 0;
		return mTopHSD.mDoc;
	}
  
	public final float topScore() throws IOException {
		// assert size > 0;
		return mTopHSD.mScorer.getScore();
	}

	public final boolean topNextAndAdjustElsePop() throws IOException {
		return checkAdjustElsePop(mTopHSD.mScorer.nextDoc() != DocIdSetIterator.NO_MORE_DOCS);
	}

	public final boolean topSkipToAndAdjustElsePop(int target) throws IOException {
		return checkAdjustElsePop(mTopHSD.mScorer.advance(target) != DocIdSetIterator.NO_MORE_DOCS);
	}
  
	private boolean checkAdjustElsePop(boolean cond) {
		if (cond) { // see also adjustTop
			mTopHSD.mDoc = mTopHSD.mScorer.getDocID();
		} else { // see also popNoResult
			mHeap[1] = mHeap[mSize]; // move last to first
			mHeap[mSize] = null;
			mSize --;
		}
		downHeap();
		return cond;
	}

	/** 
	 * Removes and returns the least scorer of the ScorerDocQueue in log(size)
	 * time.
	 * Should not be used when the queue is empty.
	 */
	public final IScorer pop() {
		// assert size > 0;
		IScorer result = mTopHSD.mScorer;
		popNoResult();
		return result;
	}
  
	/** 
	 * Removes the least scorer of the ScorerDocQueue in log(size) time.
	 * Should not be used when the queue is empty.
	 */
	private final void popNoResult() {
		mHeap[1] = mHeap[mSize]; // move last to first
		mHeap[mSize] = null;
		mSize --;
		downHeap();	// adjust heap
	}

	/** 
	 * Should be called when the scorer at top changes doc() value.
	 * Still log(n) worst case, but it's at least twice as fast to <pre>
	 *  { pq.top().change(); pq.adjustTop(); }
	 * </pre> instead of <pre>
	 *  { o = pq.pop(); o.change(); pq.push(o); }
	 * </pre>
	 */
	public final void adjustTop() {
		// assert size > 0;
		mTopHSD.adjust();
		downHeap();
	}

	/** Returns the number of scorers currently stored in the ScorerDocQueue. */
	public final int size() {
		return mSize;
	}

	/** Removes all entries from the ScorerDocQueue. */
	public final void clear() {
		for (int i = 0; i <= mSize; i++) {
			mHeap[i] = null;
		}
		mSize = 0;
	}

	private final void upHeap() {
		int i = mSize;
		HeapedScorerDoc node = mHeap[i];	// save bottom node
		int j = i >>> 1;
		
		while ((j > 0) && (node.mDoc < mHeap[j].mDoc)) {
			mHeap[i] = mHeap[j];			// shift parents down
			i = j;
			j = j >>> 1;
		}
		
		mHeap[i] = node;					// install saved node
		mTopHSD = mHeap[1];
	}

	private final void downHeap() {
		int i = 1;
		HeapedScorerDoc node = mHeap[i];	// save top node
		int j = i << 1;						// find smaller child
		int k = j + 1;
		
		if ((k <= mSize) && (mHeap[k].mDoc < mHeap[j].mDoc)) 
			j = k;
		
		while ((j <= mSize) && (mHeap[j].mDoc < node.mDoc)) {
			mHeap[i] = mHeap[j];			// shift up child
			i = j;
			j = i << 1;
			k = j + 1;
			
			if (k <= mSize && (mHeap[k].mDoc < mHeap[j].mDoc)) 
				j = k;
		}
		
		mHeap[i] = node;					// install saved node
		mTopHSD = mHeap[1];
	}
	
}
