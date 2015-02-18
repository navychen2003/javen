package org.javenstudio.common.indexdb;

/** 
 * Thrown when an attempt is made to add more than {@link
 * #getMaxClauseCount()} clauses. This typically happens if
 * a PrefixQuery, FuzzyQuery, WildcardQuery, or TermRangeQuery 
 * is expanded to many terms during search. 
 */
public class TooManyClauses extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public TooManyClauses(int maxClauseCount) {
		super("maxClauseCount is set to " + maxClauseCount);
	}
}
