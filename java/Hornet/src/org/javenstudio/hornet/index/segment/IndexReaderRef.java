package org.javenstudio.hornet.index.segment;

import java.util.List;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.IIndexReaderRef;

/**
 * A struct like class that represents a hierarchical relationship between
 * {@link IndexReader} instances. 
 */
public abstract class IndexReaderRef implements IIndexReaderRef {
	
	/** The reader context for this reader's immediate parent, or null if none */
	protected final CompositeIndexReaderRef mParent;
	
	/** 
	 * <code>true</code> if this context struct represents the top level reader 
	 * within the hierarchical context 
	 */
	protected final boolean mIsTopLevel;
	
	/** the doc base for this reader in the parent, <tt>0</tt> if parent is null */
	protected final int mDocBaseInParent;
	
	/** the ord for this reader in the parent, <tt>0</tt> if parent is null */
	protected final int mOrdInParent;
  
	protected IndexReaderRef(CompositeIndexReaderRef parent, 
			int ordInParent, int docBaseInParent) {
		if (!(this instanceof CompositeIndexReaderRef || this instanceof AtomicIndexReaderRef))
			throw new Error("This class should never be extended by custom code!");
		
		mParent = parent;
		mDocBaseInParent = docBaseInParent;
		mOrdInParent = ordInParent;
		mIsTopLevel = parent==null;
	}
  
	@Override
	public boolean isTopLevel() { 
		return mIsTopLevel;
	}
  
	@Override
	public IIndexReaderRef getParent() { 
		return mParent;
	}
  
	/** Returns the {@link IndexReader}, this context represents. */
	public abstract IIndexReader getReader();
  
  	/**
  	 * Returns the context's leaves if this context is a top-level context.
  	 * For convenience, if this is an {@link AtomicReaderContext} this
  	 * returns itself as the only leaf.
  	 * <p>Note: this is convenience method since leaves can always be obtained by
  	 * walking the context tree using {@link #children()}.
  	 * @throws UnsupportedOperationExceception if this is not a top-level context.
  	 * @see #children()
  	 */
  	public abstract List<IAtomicReaderRef> getLeaves();
  
  	/**
  	 * Returns the context's children iff this context is a composite context
  	 * otherwise <code>null</code>.
  	 */
  	public abstract List<IIndexReaderRef> getChildren();
  	
}
