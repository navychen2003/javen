package org.javenstudio.common.indexdb.analysis;

import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.NumericTerm;

/**
 * Numeric Token implements for int/long/float/double.
 */
public final class NumericToken extends Token {

	private NumericTerm mTerm;
	private BytesRef mBytes = new BytesRef();
	
	NumericToken(NumericTerm term) { 
		super();
		mTerm = term;
		
		if (term == null) 
			throw new NullPointerException();
	}
	
	@Override
	public int fillBytesRef(BytesRef bytes) {
		return mTerm.fillBytesRef(bytes);
	}
	
	@Override
	public int fillBytesRef() {
	    return mTerm.fillBytesRef(mBytes);
	}

	@Override
	public BytesRef getBytesRef() {
	    return mBytes;
	}
	
	@Override
	public Object clone() {
		NumericToken t = (NumericToken)super.clone();
		t.mTerm = new NumericTerm(mTerm);
		t.mBytes = BytesRef.deepCopyOf(mBytes);
	    
	    return t;
	}
	
	@Override
	protected void toString(StringBuilder sbuf) { 
		super.toString(sbuf);
		sbuf.append(",value=").append(mTerm.getRawValue());
	}
	
}
