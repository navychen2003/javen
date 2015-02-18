package org.javenstudio.common.indexdb;

public interface ITermContext {

	public IIndexReaderRef getTopReader();
	
	/** expert: only available for queries that want to lie about docfreq */
	public void setDocFreq(int docFreq);
  
	/**
	 *  Returns the accumulated document frequency of all {@link TermState}
	 *         instances passed to {@link #register(TermState, int, int, long)}.
	 * @return the accumulated document frequency of all {@link TermState}
	 *         instances passed to {@link #register(TermState, int, int, long)}.
	 */
	public int getDocFreq();
	
	/**
	 *  Returns the accumulated term frequency of all {@link TermState}
	 *         instances passed to {@link #register(TermState, int, int, long)}.
	 * @return the accumulated term frequency of all {@link TermState}
	 *         instances passed to {@link #register(TermState, int, int, long)}.
	 */
	public long getTotalTermFreq();
  
	/**
	 * Returns the {@link TermState} for an leaf ordinal or <code>null</code> if no
	 * {@link TermState} for the ordinal was registered.
	 * 
	 * @param ord
	 *          the readers leaf ordinal to get the {@link TermState} for.
	 * @return the {@link TermState} for the given readers ord or <code>null</code> if no
	 *         {@link TermState} for the reader was registered
	 */
	public ITermState get(int ord);
  
	/**
	 * Clears the {@link TermContext} internal state and removes all
	 * registered {@link TermState}s
	 */
	public void clear();
  
}
