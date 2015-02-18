package org.javenstudio.hornet.search;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.search.IndexSearcher;

/**
 * A class holding a subset of the {@link IndexSearcher}s leaf contexts to be
 * executed within a single thread.
 */
final class LeafSlice {
	public final IAtomicReaderRef[] mLeaves;
  
	public LeafSlice(IAtomicReaderRef... leaves) {
		mLeaves = leaves;
	}
	
}
