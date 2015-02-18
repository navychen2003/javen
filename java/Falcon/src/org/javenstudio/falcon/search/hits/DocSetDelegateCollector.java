package org.javenstudio.falcon.search.hits;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ICollector;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.hornet.search.OpenBitSet;

/**
 *
 */
public class DocSetDelegateCollector extends DocSetCollector {
	
	private final ICollector mCollector;

	public DocSetDelegateCollector(int smallSetSize, int maxDoc, ICollector collector) {
		super(smallSetSize, maxDoc);
		mCollector = collector;
	}

	@Override
	public void collect(int doc) throws IOException {
		mCollector.collect(doc);
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

	@Override
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
		mCollector.setScorer(scorer);
	}

	@Override
	public void setNextReader(IAtomicReaderRef context) throws IOException {
		mCollector.setNextReader(context);
		mBase = context.getDocBase();
	}
	
}
