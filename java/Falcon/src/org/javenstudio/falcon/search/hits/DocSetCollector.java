package org.javenstudio.falcon.search.hits;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.search.Collector;
import org.javenstudio.hornet.search.OpenBitSet;

/**
 *
 */
public class DocSetCollector extends Collector {
	
	// in case there aren't that many hits, we may not want a very sparse
	// bit array.  Optimistically collect the first few docs in an array
	// in case there are only a few.
	protected final int[] mScratch;
		
	protected final int mMaxDoc;
	protected final int mSmallSetSize;
	
	protected OpenBitSet mBits;
	protected int mBase;
	protected int mPos = 0;

	public DocSetCollector(int smallSetSize, int maxDoc) {
		mSmallSetSize = smallSetSize;
		mMaxDoc = maxDoc;
		mScratch = new int[smallSetSize];
	}

	@Override
	public void collect(int doc) throws IOException {
		doc += mBase;
		
		// optimistically collect the first docs in an array
		// in case the total number will be small enough to represent
		// as a small set like SortedIntDocSet instead...
		// Storing in this array will be quicker to convert
		// than scanning through a potentially huge bit vector.
		// FUTURE: when search methods all start returning docs in order, maybe
		// we could have a ListDocSet() and use the collected array directly.
		if (mPos < mScratch.length) {
			mScratch[mPos] = doc;
			
		} else {
			// this conditional could be removed if BitSet was preallocated, but that
			// would take up more memory, and add more GC time...
			if (mBits == null) 
				mBits = new OpenBitSet(mMaxDoc);
			
			mBits.fastSet(doc);
		}

		mPos ++;
	}

	public DocSet getDocSet() {
		if (mPos <= mScratch.length) {
			// assumes docs were collected in sorted order!
			return new SortedIntDocSet(mScratch, mPos);
			
		} else {
			// set the bits for ids that were collected in the array
			for (int i=0; i < mScratch.length; i++) {
				mBits.fastSet(mScratch[i]);
			}
			
			return new BitDocSet(mBits, mPos);
		}
	}

	@Override
	public void setScorer(IScorer scorer) throws IOException {
		// do nothing
	}

	@Override
	public void setNextReader(IAtomicReaderRef context) throws IOException {
		mBase = context.getDocBase();
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		return false;
	}
	
}
