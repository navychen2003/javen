package org.javenstudio.common.indexdb.index.term;

import org.javenstudio.common.indexdb.ITermState;

/**
 * An ordinal based {@link TermState}
 * 
 */
public class OrdTermState extends TermState {
	
	public long mOrd = -1;
  
	@Override
	public void copyFrom(ITermState other) {
		assert other instanceof OrdTermState : "can not copy from " + other.getClass().getName();
    	mOrd = ((OrdTermState) other).mOrd;
	}

	@Override
	public String toString() {
		return "OrdTermState{ord=" + mOrd + "}";
	}
	
}
