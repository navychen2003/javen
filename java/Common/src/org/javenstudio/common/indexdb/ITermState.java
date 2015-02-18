package org.javenstudio.common.indexdb;

public interface ITermState extends Cloneable {

	/**
	 * Copies the content of the given {@link TermState} to this instance
	 * 
	 * @param other
	 *          the TermState to copy
	 */
	public void copyFrom(ITermState other);
	
}
