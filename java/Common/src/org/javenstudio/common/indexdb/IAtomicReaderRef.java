package org.javenstudio.common.indexdb;

public interface IAtomicReaderRef extends IIndexReaderRef {

	/** Returns the {@link IndexReader}, this context represents. */
	public IAtomicReader getReader();
	
	/** The readers ord in the top-level's leaves array */
	public int getOrd();
	
	/** The readers absolute doc base */
	public int getDocBase();
	
}
