package org.javenstudio.common.indexdb.index.term;

import org.javenstudio.common.indexdb.ITermState;

/**
 * Encapsulates all required internal state to position the associated
 * {@link TermsEnum} without re-seeking.
 * 
 * @see TermsEnum#seekExact(BytesRef, TermState)
 * @see TermsEnum#termState()
 * 
 */
public abstract class TermState implements ITermState {

	/**
	 * Copies the content of the given {@link TermState} to this instance
	 * 
	 * @param other
	 *          the TermState to copy
	 */
	public abstract void copyFrom(ITermState other);

	@Override
	public TermState clone() {
		try {
			return (TermState)super.clone();
		} catch (CloneNotSupportedException cnse) {
			// should not happen
			throw new RuntimeException(cnse);
		}
	} 

	@Override
  	public String toString() {
		return "TermState";
	}
	
}
