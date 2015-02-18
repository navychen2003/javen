package org.javenstudio.hornet.search.query;

import java.util.ArrayList;
import java.util.Collection;

import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.IWeight;
import org.javenstudio.common.indexdb.search.Scorer;

/**
 * Base class for Scorers that score disjunctions.
 * Currently this just provides helper methods to manage the heap.
 */
public abstract class DisjunctionScorer extends Scorer {
	
	protected final IScorer mSubScorers[];
	protected int mNumScorers;
  
	protected DisjunctionScorer(IWeight weight, IScorer subScorers[], int numScorers) {
		super(weight);
		mSubScorers = subScorers;
		mNumScorers = numScorers;
		heapify();
	}
  
	/** 
	 * Organize subScorers into a min heap with scorers generating 
	 * the earliest document on top.
	 */
	protected final void heapify() {
		for (int i = (mNumScorers >> 1) - 1; i >= 0; i--) {
			heapAdjust(i);
		}
	}
  
	/** 
	 * The subtree of subScorers at root is a min heap except possibly for its root element.
	 * Bubble the root down as required to make the subtree a heap.
	 */
	protected final void heapAdjust(int root) {
		IScorer scorer = mSubScorers[root];
		int doc = scorer.getDocID();
		int i = root;
		
		while (i <= (mNumScorers >> 1) - 1) {
			int lchild = (i << 1) + 1;
			IScorer lscorer = mSubScorers[lchild];
			
			int ldoc = lscorer.getDocID();
			int rdoc = Integer.MAX_VALUE, rchild = (i << 1) + 2;
			
			IScorer rscorer = null;
			
			if (rchild < mNumScorers) {
				rscorer = mSubScorers[rchild];
				rdoc = rscorer.getDocID();
			}
			
			if (ldoc < doc) {
				if (rdoc < ldoc) {
					mSubScorers[i] = rscorer;
					mSubScorers[rchild] = scorer;
					i = rchild;
					
				} else {
					mSubScorers[i] = lscorer;
					mSubScorers[lchild] = scorer;
					i = lchild;
				}
				
			} else if (rdoc < doc) {
				mSubScorers[i] = rscorer;
				mSubScorers[rchild] = scorer;
				i = rchild;
				
			} else {
				return;
			}
		}
	}

	/** 
	 * Remove the root Scorer from subScorers and re-establish it as a heap
	 */
	protected final void heapRemoveRoot() {
		if (mNumScorers == 1) {
			mSubScorers[0] = null;
			mNumScorers = 0;
			
		} else {
			mSubScorers[0] = mSubScorers[mNumScorers - 1];
			mSubScorers[mNumScorers - 1] = null;
			--mNumScorers;
			
			heapAdjust(0);
		}
	}
  
	@Override
	public final Collection<IScorer.IChild> getChildren() {
		ArrayList<IScorer.IChild> children = new ArrayList<IScorer.IChild>(mNumScorers);
		for (int i = 0; i < mNumScorers; i++) {
			children.add(new ChildScorer(mSubScorers[i], "SHOULD"));
		}
		return children;
	}
	
}
