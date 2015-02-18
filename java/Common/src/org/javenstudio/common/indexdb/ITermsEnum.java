package org.javenstudio.common.indexdb;

import java.io.IOException;

import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.BytesRefIterator;

public interface ITermsEnum extends BytesRefIterator {

	/** 
	 * Represents returned result from {@link #seekCeil}.
	 *  If status is FOUND, then the precise term was found.
	 *  If status is NOT_FOUND, then a different term was
	 *  found.  If the status is END, the end of the iteration
	 *  was hit. 
	 */
	public static enum SeekStatus {END, FOUND, NOT_FOUND};
	
	public float getBoost();
	
	/** 
	 * Attempts to seek to the exact term, returning
	 *  true if the term is found.  If this returns false, the
	 *  enum is unpositioned.  For some codecs, seekExact may
	 *  be substantially faster than {@link #seekCeil}. 
	 */
	public boolean seekExact(BytesRef text, boolean useCache) throws IOException;
  
	/** 
	 * Expert: just like {@link #seekCeil(BytesRef)} but allows
	 *  you to control whether the implementation should
	 *  attempt to use its term cache (if it uses one). 
	 */
	public SeekStatus seekCeil(BytesRef text, boolean useCache) throws IOException;
	
	/** 
	 * Seeks to the specified term, if it exists, or to the
	 *  next (ceiling) term.  Returns SeekStatus to
	 *  indicate whether exact term was found, a different
	 *  term was found, or EOF was hit.  The target term may
	 *  be before or after the current term.  If this returns
	 *  SeekStatus.END, the enum is unpositioned. 
	 */
	public SeekStatus seekCeil(BytesRef text) throws IOException;
	
	/** 
	 * Seeks to the specified term by ordinal (position) as
	 *  previously returned by {@link #ord}.  The target ord
	 *  may be before or after the current ord, and must be
	 *  within bounds. 
	 */
	public void seekExact(long ord) throws IOException;
	
	/**
	 * Expert: Seeks a specific position by {@link TermState} previously obtained
	 * from {@link #termState()}. Callers should maintain the {@link TermState} to
	 * use this method. Low-level implementations may position the TermsEnum
	 * without re-seeking the term dictionary.
	 * <p>
	 * Seeking by {@link TermState} should only be used iff the enum the state was
	 * obtained from and the enum the state is used for seeking are obtained from
	 * the same {@link IndexReader}.
	 * <p>
	 * NOTE: Using this method with an incompatible {@link TermState} might leave
	 * this {@link TermsEnum} in undefined state. On a segment level
	 * {@link TermState} instances are compatible only iff the source and the
	 * target {@link TermsEnum} operate on the same field. If operating on segment
	 * level, TermState instances must not be used across segments.
	 * <p>
	 * NOTE: A seek by {@link TermState} might not restore the
	 * {@link AttributeSource}'s state. {@link AttributeSource} states must be
	 * maintained separately if this method is used.
	 * @param term the term the TermState corresponds to
	 * @param state the {@link TermState}
	 */
	public void seekExact(BytesRef term, ITermState state) throws IOException;
	
	/** 
	 * Returns current term. Do not call this when the enum
	 *  is unpositioned. 
	 */
	public BytesRef getTerm() throws IOException;

	/** 
	 * Returns ordinal position for current term.  This is an
	 *  optional method (the codec may throw {@link
	 *  UnsupportedOperationException}).  Do not call this
	 *  when the enum is unpositioned. 
	 */
	public long getOrd() throws IOException;

	/** 
	 * Returns the number of documents containing the current
	 *  term.  Do not call this when the enum is unpositioned.
	 *  {@link SeekStatus#END}.
	 */
	public int getDocFreq() throws IOException;
	
	/** 
	 * Returns the total number of occurrences of this term
	 *  across all documents (the sum of the freq() for each
	 *  doc that has this term).  This will be -1 if the
	 *  codec doesn't support this measure.  Note that, like
	 *  other term measures, this measure does not take
	 *  deleted documents into account. 
	 */
	public long getTotalTermFreq() throws IOException;
  
	/** 
	 * Get {@link DocsEnum} for the current term.  Do not
	 *  call this when the enum is unpositioned.  This method
	 *  will not return null.
	 *  
	 * @param liveDocs unset bits are documents that should not
	 * be returned
	 * @param reuse pass a prior DocsEnum for possible reuse 
	 */
	public IDocsEnum getDocs(Bits liveDocs, IDocsEnum reuse) 
			throws IOException;
	
	/** 
	 * Get {@link DocsEnum} for the current term, with
	 *  control over whether freqs are required.  Do not
	 *  call this when the enum is unpositioned.  This method
	 *  will not return null.
	 *  
	 * @param liveDocs unset bits are documents that should not
	 * be returned
	 * @param reuse pass a prior DocsEnum for possible reuse
	 * @param flags specifies which optional per-document values
	 *        you require; see {@link DocsEnum#FLAG_FREQS} 
	 * @see #docs(Bits, DocsEnum, int) 
	 */
	public IDocsEnum getDocs(Bits liveDocs, IDocsEnum reuse, int flags) 
			throws IOException;
  
	/** 
	 * Get {@link DocsAndPositionsEnum} for the current term.
	 *  Do not call this when the enum is unpositioned.  This
	 *  method will return null if positions were not
	 *  indexed.
	 *  
	 *  @param liveDocs unset bits are documents that should not
	 *  be returned
	 *  @param reuse pass a prior DocsAndPositionsEnum for possible reuse
	 *  @see #docsAndPositions(Bits, DocsAndPositionsEnum, int) 
	 */
	public IDocsAndPositionsEnum getDocsAndPositions(Bits liveDocs, 
			IDocsAndPositionsEnum reuse) throws IOException;
	
	/** 
	 * Get {@link DocsAndPositionsEnum} for the current term,
	 *  with control over whether offsets and payloads are
	 *  required.  Some codecs may be able to optimize their
	 *  implementation when offsets and/or payloads are not required.
	 *  Do not call this when the enum is unpositioned.  This
	 *  will return null if positions were not indexed.
	 *
	 *  @param liveDocs unset bits are documents that should not
	 *  be returned
	 *  @param reuse pass a prior DocsAndPositionsEnum for possible reuse
	 *  @param flags specifies which optional per-position values you
	 *         require; see {@link DocsAndPositionsEnum#FLAG_OFFSETS} and 
	 *         {@link DocsAndPositionsEnum#FLAG_PAYLOADS}. 
	 */
	public IDocsAndPositionsEnum getDocsAndPositions(Bits liveDocs, 
			IDocsAndPositionsEnum reuse, int flags) throws IOException;
  
	/**
	 * Expert: Returns the TermsEnums internal state to position the TermsEnum
	 * without re-seeking the term dictionary.
	 * <p>
	 * NOTE: A seek by {@link TermState} might not capture the
	 * {@link AttributeSource}'s state. Callers must maintain the
	 * {@link AttributeSource} states separately
	 * 
	 * @see TermState
	 * @see #seekExact(BytesRef, TermState)
	 */
	public ITermState getTermState() throws IOException;
	
}
