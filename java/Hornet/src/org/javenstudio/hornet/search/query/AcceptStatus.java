package org.javenstudio.hornet.search.query;

import org.javenstudio.common.indexdb.util.BytesRef;

/** 
 * Return value, if term should be accepted or the iteration should
 * {@code END}. The {@code *_SEEK} values denote, that after handling the current term
 * the enum should call {@link #nextSeekTerm} and step forward.
 * @see #accept(BytesRef)
 */
public enum AcceptStatus {
	
	/** Accept the term and position the enum at the next term. */
	YES, 
	
	/** 
	 * Accept the term and advance ({@link FilteredTermsEnum#nextSeekTerm(BytesRef)})
	 * to the next term. 
	 */
	YES_AND_SEEK, 
	
	/** Reject the term and position the enum at the next term. */
	NO, 
	
	/** 
	 * Reject the term and advance ({@link FilteredTermsEnum#nextSeekTerm(BytesRef)})
	 * to the next term. 
	 */
	NO_AND_SEEK, 
	
	/** Reject the term and stop enumerating. */
	END
	
}