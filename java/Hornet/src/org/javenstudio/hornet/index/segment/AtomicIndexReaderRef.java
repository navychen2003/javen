package org.javenstudio.hornet.index.segment;

import java.util.Collections;
import java.util.List;

import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IIndexReaderRef;

/**
 * {@link IIndexReaderRef} for {@link AtomicReader} instances
 */
public final class AtomicIndexReaderRef extends IndexReaderRef 
		implements IAtomicReaderRef {
	
	/** The readers ord in the top-level's leaves array */
	private final int mOrd;
	/** The readers absolute doc base */
	private final int mDocBase;
  
	private final IAtomicReader mReader;
	private final List<IAtomicReaderRef> mLeaves;
  
	/**
	 * Creates a new {@link IAtomicReaderRef} 
	 */    
	AtomicIndexReaderRef(CompositeIndexReaderRef parent, AtomicIndexReader reader,
			int ord, int docBase, int leafOrd, int leafDocBase) {
		super(parent, ord, docBase);
		mOrd = leafOrd;
		mDocBase = leafDocBase;
		mReader = reader;
		mLeaves = mIsTopLevel ? Collections.singletonList((IAtomicReaderRef)this) : null;
	}
  
	AtomicIndexReaderRef(AtomicIndexReader atomicReader) {
		this(null, atomicReader, 0, 0, 0, 0);
	}
  
	@Override
	public final int getOrd() { 
		return mOrd;
	}
  
	@Override
	public final int getDocBase() { 
		return mDocBase; 
	}
  
	@Override
	public List<IAtomicReaderRef> getLeaves() {
		if (!mIsTopLevel)
			throw new UnsupportedOperationException("This is not a top-level context.");
		assert mLeaves != null;
		return mLeaves;
	}
  
	@Override
	public List<IIndexReaderRef> getChildren() {
		return null;
	}
  
	@Override
	public IAtomicReader getReader() {
		return mReader;
	}
	
}
