package org.javenstudio.common.indexdb;

import java.io.IOException;

import org.javenstudio.common.indexdb.util.BytesRef;

public interface IDocTermOrds {

	/** 
	 * Returns a TermsEnum that implements ord.  If the
	 *  provided reader supports ord, we just return its
	 *  TermsEnum; if it does not, we build a "private" terms
	 *  index internally (WARNING: consumes RAM) and use that
	 *  index to implement ord.  This also enables ord on top
	 *  of a composite reader.  The returned TermsEnum is
	 *  unpositioned.  This returns null if there are no terms.
	 *
	 *  <p><b>NOTE</b>: you must pass the same reader that was
	 *  used when creating this class 
	 */
	public ITermsEnum getOrdTermsEnum(IAtomicReader reader) throws IOException;
	
	/**
	 * @return The number of terms in this field
	 */
	public int getNumTerms();
	
	/**
	 * @return Whether this <code>DocTermOrds</code> instance is empty.
	 */
	public boolean isEmpty();
	
	public BytesRef lookupTerm(ITermsEnum termsEnum, int ord) throws IOException;
	
}
