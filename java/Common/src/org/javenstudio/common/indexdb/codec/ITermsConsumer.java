package org.javenstudio.common.indexdb.codec;

import java.io.IOException;
import java.util.Comparator;

import org.javenstudio.common.indexdb.IMergeState;
import org.javenstudio.common.indexdb.ITermState;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.util.BytesRef;

public interface ITermsConsumer {

	/** 
	 * Starts a new term in this field; this may be called
	 *  with no corresponding call to finish if the term had
	 *  no docs. 
	 */
	public IPostingsConsumer startTerm(BytesRef text) throws IOException;

	/** Finishes the current term; numDocs must be > 0. */
	public void finishTerm(BytesRef text, ITermState stats) throws IOException;

	/** Called when we are done adding terms to this field */
	public void finish(long sumTotalTermFreq, long sumDocFreq, int docCount) 
			throws IOException;

	/** 
	 * Return the BytesRef Comparator used to sort terms
	 *  before feeding to this API. 
	 */
	public Comparator<BytesRef> getComparator() throws IOException;

	/** Default merge impl */
	public ITermState merge(IMergeState mergeState, ITermsEnum termsEnum) 
			throws IOException;
	
}
