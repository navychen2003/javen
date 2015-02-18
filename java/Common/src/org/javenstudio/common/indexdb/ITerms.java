package org.javenstudio.common.indexdb;

import java.io.IOException;
import java.util.Comparator;

import org.javenstudio.common.indexdb.util.BytesRef;

public interface ITerms {

	/** 
	 * Returns an iterator that will step through all
	 *  terms. This method will not return null.  If you have
	 *  a previous TermsEnum, for example from a different
	 *  field, you can pass it for possible reuse if the
	 *  implementation can do so. 
	 */
	public ITermsEnum iterator(ITermsEnum reuse) throws IOException;
  
	/** 
	 * Return the BytesRef Comparator used to sort terms
	 *  provided by the iterator.  This method may return null
	 *  if there are no terms.  This method may be invoked
	 *  many times; it's best to cache a single instance &
	 *  reuse it. 
	 */
	public Comparator<BytesRef> getComparator() throws IOException;

	/** 
	 * Returns the number of terms for this field, or -1 if this 
	 *  measure isn't stored by the codec. Note that, just like 
	 *  other term measures, this measure does not take deleted 
	 *  documents into account. 
	 */
	public long size() throws IOException;
  
	/** 
	 * Returns the sum of {@link TermsEnum#totalTermFreq} for
	 *  all terms in this field, or -1 if this measure isn't
	 *  stored by the codec (or if this fields omits term freq
	 *  and positions).  Note that, just like other term
	 *  measures, this measure does not take deleted documents
	 *  into account. 
	 */
	public long getSumTotalTermFreq() throws IOException;

	/** 
	 * Returns the sum of {@link TermsEnum#docFreq()} for
	 *  all terms in this field, or -1 if this measure isn't
	 *  stored by the codec.  Note that, just like other term
	 *  measures, this measure does not take deleted documents
	 *  into account. 
	 */
	public long getSumDocFreq() throws IOException;

	/** 
	 * Returns the number of documents that have at least one
	 *  term for this field, or -1 if this measure isn't
	 *  stored by the codec.  Note that, just like other term
	 *  measures, this measure does not take deleted documents
	 *  into account. 
	 */
	public int getDocCount() throws IOException;
	
}
