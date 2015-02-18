package org.javenstudio.common.indexdb;

import java.util.List;

public interface IIndexReaderRef {

	/** 
	 * <code>true</code> if this context struct represents the top level reader 
	 * within the hierarchical context 
	 */
	public boolean isTopLevel();
	
	public IIndexReaderRef getParent();
	
	/** Returns the {@link IndexReader}, this context represents. */
	public IIndexReader getReader();
  
	/**
	 * Returns the context's leaves if this context is a top-level context.
	 * For convenience, if this is an {@link AtomicReaderContext} this
	 * returns itself as the only leaf.
	 * <p>Note: this is convenience method since leaves can always be obtained by
	 * walking the context tree using {@link #children()}.
	 * @throws UnsupportedOperationExceception if this is not a top-level context.
	 * @see #children()
	 */
	public List<IAtomicReaderRef> getLeaves();
  
	/**
	 * Returns the context's children iff this context is a composite context
	 * otherwise <code>null</code>.
	 */
	public List<IIndexReaderRef> getChildren();
	
}
